package dev.sfcrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class SpellCraftingManager {

    public record CraftResult(boolean success, String message, ItemStack item) {}

    private final Plugin plugin;
    private final OraxenItemResolver resolver;
    private final SkillBridge skillBridge;
    private final CharacterBridge characterBridge;

    private boolean enabled;
    private Material outputMaterial = Material.ENCHANTED_BOOK;
    private String nameFormat = "&d{spell_name} &8- &7{class_name}";
    private List<String> loreFormat = List.of(
        "&8Tipo: &7{spell_type}",
        "&8Clase: &7{class_name}",
        "&8Puntos usados: &f{points_used}&7/&f{points_total}",
        "&8----------------",
        "{stats_lines}"
    );
    private final Map<String, Integer> classBonusPoints = new LinkedHashMap<>();
    private final Map<String, SkillAliasRule> skillBonusRules = new LinkedHashMap<>();
    private final Map<String, List<String>> skillAliases = new LinkedHashMap<>();
    private final Map<String, CrystalDefinition> crystals = new LinkedHashMap<>();
    private final Map<String, SpellBookDefinition> books = new LinkedHashMap<>();
    private final Map<String, SpellTemplate> templates = new LinkedHashMap<>();

    public SpellCraftingManager(Plugin plugin, OraxenItemResolver resolver, SkillBridge skillBridge, CharacterBridge characterBridge) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.skillBridge = skillBridge;
        this.characterBridge = characterBridge;
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("spellcraft.enabled", true);
        outputMaterial = parseMaterial(plugin.getConfig().getString("spellcraft.output.material"), Material.ENCHANTED_BOOK);
        nameFormat = plugin.getConfig().getString("spellcraft.output.name-format", "&d{spell_name} &8- &7{class_name}");
        loreFormat = plugin.getConfig().getStringList("spellcraft.output.lore");
        if (loreFormat == null || loreFormat.isEmpty()) {
            loreFormat = List.of(
                "&8Tipo: &7{spell_type}",
                "&8Clase: &7{class_name}",
                "&8Puntos usados: &f{points_used}&7/&f{points_total}",
                "&8----------------",
                "{stats_lines}"
            );
        }

        classBonusPoints.clear();
        skillBonusRules.clear();
        skillAliases.clear();
        crystals.clear();
        books.clear();
        templates.clear();

        loadClassBonuses();
        loadSkillAliases();
        loadSkillBonusRules();
        loadCrystals();
        loadBooks();
        loadTemplates();

        if (crystals.isEmpty() || books.isEmpty() || templates.isEmpty()) {
            loadFallbackDefaults();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getTemplateKeys() {
        return templates.keySet();
    }

    public Set<String> getCrystalKeys() {
        return crystals.keySet();
    }

    public Set<String> getBookKeys() {
        return books.keySet();
    }

    public Set<String> getStatKeys(String templateKey) {
        SpellTemplate template = templates.get(normalize(templateKey));
        if (template == null) {
            return Collections.emptySet();
        }
        return template.stats().keySet();
    }

    public CraftResult craftSpell(Player player, String templateKey, String crystalKey, String bookKey, Map<String, Integer> allocatedPoints) {
        if (!enabled) {
            return new CraftResult(false, ChatColor.RED + "El sistema de spellcraft esta deshabilitado.", null);
        }
        SpellTemplate template = templates.get(normalize(templateKey));
        if (template == null) {
            return new CraftResult(false, ChatColor.RED + "Plantilla invalida: " + templateKey, null);
        }
        CrystalDefinition crystal = crystals.get(normalize(crystalKey));
        if (crystal == null) {
            return new CraftResult(false, ChatColor.RED + "Cristal invalido: " + crystalKey, null);
        }
        SpellBookDefinition book = books.get(normalize(bookKey));
        if (book == null) {
            return new CraftResult(false, ChatColor.RED + "Libro invalido: " + bookKey, null);
        }
        if (!book.allowedTypes().isEmpty() && !book.allowedTypes().contains(template.type())) {
            return new CraftResult(false, ChatColor.RED + "Ese libro no puede crear hechizos de tipo " + template.typeDisplay() + ".", null);
        }

        String classKey = characterBridge.getActiveClassKey(player);
        if (classKey.isBlank()) {
            return new CraftResult(false, ChatColor.RED + "No tenes una clase activa en SFCharacter.", null);
        }
        if (!template.allowedClasses().isEmpty() && !template.allowedClasses().contains(classKey)) {
            return new CraftResult(false, ChatColor.RED + "Tu clase actual no puede crear esa plantilla.", null);
        }

        for (Map.Entry<String, Integer> requirement : template.requiredSkills().entrySet()) {
            int current = resolveSkillLevel(player, requirement.getKey());
            if (current < requirement.getValue()) {
                return new CraftResult(
                    false,
                    ChatColor.RED + "Te falta skill " + requirement.getKey() + " " + requirement.getValue() + " (actual " + current + ").",
                    null
                );
            }
        }

        for (String statKey : allocatedPoints.keySet()) {
            if (!template.stats().containsKey(statKey)) {
                return new CraftResult(false, ChatColor.RED + "Stat invalida para esta plantilla: " + statKey, null);
            }
        }

        int pointsTotal = crystal.points() + book.bonusPoints() + classBonusPoints.getOrDefault(classKey, 0);
        for (Map.Entry<String, SkillAliasRule> entry : skillBonusRules.entrySet()) {
            int skillLevel = resolveSkillLevel(player, entry.getKey());
            pointsTotal += entry.getValue().pointsFromLevel(skillLevel);
        }

        int pointsUsed = 0;
        for (Map.Entry<String, StatDefinition> statEntry : template.stats().entrySet()) {
            int assigned = Math.max(0, allocatedPoints.getOrDefault(statEntry.getKey(), 0));
            StatDefinition statDefinition = statEntry.getValue();
            if (assigned < statDefinition.minPoints()) {
                return new CraftResult(false, ChatColor.RED + "La stat " + statEntry.getKey() + " requiere minimo " + statDefinition.minPoints() + " puntos.", null);
            }
            if (assigned > statDefinition.maxPoints()) {
                return new CraftResult(false, ChatColor.RED + "La stat " + statEntry.getKey() + " tiene maximo " + statDefinition.maxPoints() + " puntos.", null);
            }
            pointsUsed += assigned;
        }
        if (pointsUsed <= 0) {
            return new CraftResult(false, ChatColor.RED + "Debes asignar al menos 1 punto.", null);
        }
        if (pointsUsed > pointsTotal) {
            return new CraftResult(false, ChatColor.RED + "No tenes suficientes puntos. Usados: " + pointsUsed + " / Disponibles: " + pointsTotal, null);
        }

        if (!hasAtLeast(player, crystal.itemRef(), 1)) {
            return new CraftResult(false, ChatColor.RED + "No tenes el cristal requerido en inventario.", null);
        }
        if (!hasAtLeast(player, book.itemRef(), 1)) {
            return new CraftResult(false, ChatColor.RED + "No tenes el libro requerido en inventario.", null);
        }

        if (!consume(player, crystal.itemRef(), 1)) {
            return new CraftResult(false, ChatColor.RED + "No se pudo consumir el cristal.", null);
        }
        if (!consume(player, book.itemRef(), 1)) {
            giveBack(player, crystal.itemRef(), 1);
            return new CraftResult(false, ChatColor.RED + "No se pudo consumir el libro.", null);
        }

        String className = characterBridge.getActiveClassDisplay(player);
        ItemStack spellItem = buildSpellItem(player, template, pointsTotal, pointsUsed, allocatedPoints, classKey, className);

        plugin.getServer().getPluginManager().callEvent(
                new SpellCraftCompleteEvent(player, template.key(), crystal.key(), pointsUsed));

        return new CraftResult(true, ChatColor.GREEN + "Hechizo creado: " + ChatColor.AQUA + template.displayName(), spellItem);
    }

    private ItemStack buildSpellItem(
        Player player,
        SpellTemplate template,
        int pointsTotal,
        int pointsUsed,
        Map<String, Integer> allocatedPoints,
        String classKey,
        String className
    ) {
        ItemStack item = new ItemStack(outputMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String displayName = colorize(nameFormat
            .replace("{spell_name}", template.displayName())
            .replace("{spell_type}", template.typeDisplay())
            .replace("{class_name}", className)
        );
        meta.setDisplayName(displayName);

        List<String> statLines = new ArrayList<>();
        for (Map.Entry<String, StatDefinition> entry : template.stats().entrySet()) {
            String statKey = entry.getKey();
            int points = Math.max(0, allocatedPoints.getOrDefault(statKey, 0));
            double value = entry.getValue().baseValue() + (entry.getValue().valuePerPoint() * points);
            String line = "&7- &f" + entry.getValue().displayName() + ": &b" + fmt(value) + " &8(" + points + " pts)";
            statLines.add(colorize(line));
        }

        List<String> lore = new ArrayList<>();
        for (String rawLine : loreFormat) {
            if (rawLine == null) {
                continue;
            }
            if (rawLine.contains("{stats_lines}")) {
                lore.addAll(statLines);
                continue;
            }
            lore.add(colorize(rawLine
                .replace("{spell_name}", template.displayName())
                .replace("{spell_type}", template.typeDisplay())
                .replace("{class_name}", className)
                .replace("{points_used}", String.valueOf(pointsUsed))
                .replace("{points_total}", String.valueOf(pointsTotal))
            ));
        }
        meta.setLore(lore);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "spell_template"), PersistentDataType.STRING, template.key());
        pdc.set(new NamespacedKey(plugin, "spell_type"), PersistentDataType.STRING, template.type());
        pdc.set(new NamespacedKey(plugin, "spell_class"), PersistentDataType.STRING, classKey);
        pdc.set(new NamespacedKey(plugin, "spell_points_used"), PersistentDataType.INTEGER, pointsUsed);
        pdc.set(new NamespacedKey(plugin, "spell_points_total"), PersistentDataType.INTEGER, pointsTotal);
        pdc.set(new NamespacedKey(plugin, "spell_crafter"), PersistentDataType.STRING, player.getName());
        if (template.mythicSkill() != null && !template.mythicSkill().isBlank()) {
            pdc.set(new NamespacedKey(plugin, "spell_mythic_skill"), PersistentDataType.STRING, template.mythicSkill().trim());
        }
        if (template.projectileModelItem() != null && !template.projectileModelItem().isBlank()) {
            pdc.set(new NamespacedKey(plugin, "spell_projectile_model"), PersistentDataType.STRING, normalize(template.projectileModelItem()));
        }
        pdc.set(new NamespacedKey(plugin, "spell_val_damage_multiplier"), PersistentDataType.DOUBLE, template.damageMultiplier());
        pdc.set(new NamespacedKey(plugin, "spell_projectile_scale"), PersistentDataType.DOUBLE, template.projectileScale());

        for (Map.Entry<String, StatDefinition> entry : template.stats().entrySet()) {
            String statKey = entry.getKey();
            int points = Math.max(0, allocatedPoints.getOrDefault(statKey, 0));
            double value = entry.getValue().baseValue() + (entry.getValue().valuePerPoint() * points);
            pdc.set(new NamespacedKey(plugin, "spell_pts_" + statKey), PersistentDataType.INTEGER, points);
            pdc.set(new NamespacedKey(plugin, "spell_val_" + statKey), PersistentDataType.DOUBLE, value);
        }

        item.setItemMeta(meta);
        return item;
    }

    private boolean hasAtLeast(Player player, String itemRef, int amount) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (matchesRef(stack, itemRef)) {
                count += stack.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean consume(Player player, String itemRef, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!matchesRef(stack, itemRef)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= take;
            if (remaining <= 0) {
                player.getInventory().setContents(contents);
                return true;
            }
        }
        player.getInventory().setContents(contents);
        return false;
    }

    private void giveBack(Player player, String itemRef, int amount) {
        ItemStack refund = buildItem(itemRef, amount);
        if (refund == null || refund.getType().isAir()) {
            return;
        }
        var leftovers = player.getInventory().addItem(refund);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private boolean matchesRef(ItemStack item, String ref) {
        if (item == null || item.getType().isAir() || ref == null || ref.isBlank()) {
            return false;
        }
        String normalized = normalize(ref);
        if (normalized.startsWith("oraxen:")) {
            String id = normalized.substring("oraxen:".length());
            return resolver.isOraxenItem(item, id);
        }
        if (normalized.startsWith("material:")) {
            Material material = Material.matchMaterial(normalized.substring("material:".length()).toUpperCase(Locale.ROOT));
            return material != null && item.getType() == material;
        }
        Material directMaterial = Material.matchMaterial(normalized.toUpperCase(Locale.ROOT));
        if (directMaterial != null) {
            return item.getType() == directMaterial;
        }
        return resolver.isOraxenItem(item, normalized);
    }

    private ItemStack buildItem(String ref, int amount) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        String normalized = normalize(ref);
        if (normalized.startsWith("oraxen:")) {
            return resolver.buildOraxenItem(normalized.substring("oraxen:".length()), amount);
        }
        if (normalized.startsWith("material:")) {
            Material material = Material.matchMaterial(normalized.substring("material:".length()).toUpperCase(Locale.ROOT));
            return material == null ? null : new ItemStack(material, amount);
        }
        Material direct = Material.matchMaterial(normalized.toUpperCase(Locale.ROOT));
        if (direct != null) {
            return new ItemStack(direct, amount);
        }
        return resolver.buildOraxenItem(normalized, amount);
    }

    private int resolveSkillLevel(Player player, String skillKey) {
        int direct = skillBridge.getSkillLevel(player, skillKey);
        int best = direct;
        List<String> aliases = skillAliases.get(normalize(skillKey));
        if (aliases != null) {
            for (String alias : aliases) {
                best = Math.max(best, skillBridge.getSkillLevel(player, alias));
            }
        }
        return best;
    }

    private void loadClassBonuses() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.class-bonus-points");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            classBonusPoints.put(normalize(key), section.getInt(key, 0));
        }
    }

    private void loadSkillAliases() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.skill-aliases");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            List<String> values = new ArrayList<>();
            for (String value : section.getStringList(key)) {
                if (value != null && !value.isBlank()) {
                    values.add(normalize(value));
                }
            }
            if (!values.isEmpty()) {
                skillAliases.put(normalize(key), values);
            }
        }
    }

    private void loadSkillBonusRules() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.skill-bonus-points");
        if (section == null) {
            return;
        }
        for (String skillKey : section.getKeys(false)) {
            ConfigurationSection skillSection = section.getConfigurationSection(skillKey);
            if (skillSection == null) {
                continue;
            }
            int everyLevels = Math.max(1, skillSection.getInt("every-levels", 10));
            int points = skillSection.getInt("points", 1);
            skillBonusRules.put(normalize(skillKey), new SkillAliasRule(everyLevels, points));
        }
    }

    private void loadCrystals() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.crystals");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection crystalSection = section.getConfigurationSection(key);
            if (crystalSection == null) {
                continue;
            }
            String itemRef = crystalSection.getString("item", "");
            int points = Math.max(0, crystalSection.getInt("points", 0));
            if (itemRef.isBlank() || points <= 0) {
                continue;
            }
            crystals.put(normalize(key), new CrystalDefinition(normalize(key), itemRef, points));
        }
    }

    private void loadBooks() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.books");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection bookSection = section.getConfigurationSection(key);
            if (bookSection == null) {
                continue;
            }
            String itemRef = bookSection.getString("item", "");
            int points = Math.max(0, bookSection.getInt("bonus-points", 0));
            if (itemRef.isBlank()) {
                continue;
            }
            Set<String> allowedTypes = new LinkedHashSet<>();
            for (String value : bookSection.getStringList("allowed-types")) {
                if (value != null && !value.isBlank()) {
                    allowedTypes.add(normalize(value));
                }
            }
            books.put(normalize(key), new SpellBookDefinition(normalize(key), itemRef, points, allowedTypes));
        }
    }

    private void loadTemplates() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spellcraft.templates");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection templateSection = section.getConfigurationSection(key);
            if (templateSection == null) {
                continue;
            }
            String type = normalize(templateSection.getString("type", ""));
            if (type.isBlank()) {
                continue;
            }
            String displayName = templateSection.getString("display-name", key);
            Set<String> allowedClasses = new LinkedHashSet<>();
            for (String classKey : templateSection.getStringList("allowed-classes")) {
                if (classKey != null && !classKey.isBlank()) {
                    allowedClasses.add(normalize(classKey));
                }
            }

            Map<String, Integer> requiredSkills = new LinkedHashMap<>();
            ConfigurationSection reqSkillsSection = templateSection.getConfigurationSection("required-skills");
            if (reqSkillsSection != null) {
                for (String skill : reqSkillsSection.getKeys(false)) {
                    requiredSkills.put(normalize(skill), Math.max(0, reqSkillsSection.getInt(skill, 0)));
                }
            }

            Map<String, StatDefinition> stats = new LinkedHashMap<>();
            ConfigurationSection statsSection = templateSection.getConfigurationSection("stats");
            if (statsSection != null) {
                for (String statKey : statsSection.getKeys(false)) {
                    ConfigurationSection statSection = statsSection.getConfigurationSection(statKey);
                    if (statSection == null) {
                        continue;
                    }
                    String display = statSection.getString("display-name", statKey);
                    double base = statSection.getDouble("base", 0.0D);
                    double perPoint = statSection.getDouble("per-point", 0.0D);
                    int minPoints = Math.max(0, statSection.getInt("min-points", 0));
                    int maxPoints = Math.max(minPoints, statSection.getInt("max-points", 999));
                    stats.put(normalize(statKey), new StatDefinition(display, base, perPoint, minPoints, maxPoints));
                }
            }
            if (stats.isEmpty()) {
                continue;
            }
            templates.put(
                normalize(key),
                new SpellTemplate(
                    normalize(key),
                    displayName,
                    type,
                    typeToDisplay(type),
                    allowedClasses,
                    requiredSkills,
                    templateSection.getString("mythic-skill", ""),
                    stats,
                    templateSection.getString("projectile-model-item", ""),
                    Math.max(0.1D, templateSection.getDouble("damage-multiplier", 1.0D)),
                    Math.max(0.1D, templateSection.getDouble("projectile-scale", 1.0D))
                )
            );
        }
    }

    private void loadFallbackDefaults() {
        if (crystals.isEmpty()) {
            crystals.put("basic", new CrystalDefinition("basic", "ORAXEN:mana_crystal_basic", 8));
            crystals.put("refined", new CrystalDefinition("refined", "ORAXEN:mana_crystal_refined", 12));
            crystals.put("ancient", new CrystalDefinition("ancient", "ORAXEN:mana_crystal_ancient", 16));
        }
        if (books.isEmpty()) {
            books.put("attack", new SpellBookDefinition("attack", "ORAXEN:spellbook_attack", 2, Set.of("attack_aoe", "attack_projectile")));
            books.put("buff", new SpellBookDefinition("buff", "ORAXEN:spellbook_buff", 2, Set.of("buff")));
        }
        if (templates.isEmpty()) {
            Map<String, StatDefinition> fireStats = new LinkedHashMap<>();
            fireStats.put("damage", new StatDefinition("Danio", 10.0D, 2.0D, 0, 10));
            fireStats.put("area", new StatDefinition("Radio", 2.0D, 0.4D, 0, 8));
            fireStats.put("cast_speed", new StatDefinition("Velocidad de casteo", 1.0D, 0.08D, 0, 8));
            fireStats.put("cooldown", new StatDefinition("Cooldown", 8.0D, -0.5D, 0, 8));
            fireStats.put("mana_cost", new StatDefinition("Costo de mana", 20.0D, -1.0D, 0, 8));
            templates.put("fire_burst", new SpellTemplate(
                "fire_burst",
                "Nova Ignea",
                "attack_aoe",
                "Nova de fuego",
                Set.of("mago"),
                Map.of("magic", 10),
                "",
                fireStats,
                "",
                1.0D,
                1.0D
            ));

            Map<String, StatDefinition> projectileStats = new LinkedHashMap<>();
            projectileStats.put("damage", new StatDefinition("Danio", 9.0D, 1.8D, 0, 10));
            projectileStats.put("projectile_speed", new StatDefinition("Velocidad de proyectil", 34.0D, 2.0D, 0, 10));
            projectileStats.put("projectile_range", new StatDefinition("Alcance", 18.0D, 1.5D, 0, 10));
            projectileStats.put("cast_speed", new StatDefinition("Velocidad de casteo", 1.0D, 0.08D, 0, 8));
            projectileStats.put("cooldown", new StatDefinition("Cooldown", 7.0D, -0.5D, 0, 8));
            projectileStats.put("mana_cost", new StatDefinition("Costo de mana", 18.0D, -1.0D, 0, 8));
            templates.put("arcane_bolt", new SpellTemplate(
                "arcane_bolt",
                "Proyectil Arcano",
                "attack_projectile",
                "Ataque de proyectil",
                Set.of("mago"),
                Map.of("magic", 12),
                "",
                projectileStats,
                "mana_crystal_refined",
                1.0D,
                1.0D
            ));

            Map<String, StatDefinition> buffStats = new LinkedHashMap<>();
            buffStats.put("power", new StatDefinition("Potencia", 1.0D, 0.1D, 0, 10));
            buffStats.put("duration", new StatDefinition("Duracion", 8.0D, 1.5D, 0, 10));
            buffStats.put("cast_speed", new StatDefinition("Velocidad de casteo", 1.0D, 0.08D, 0, 8));
            buffStats.put("cooldown", new StatDefinition("Cooldown", 14.0D, -0.6D, 0, 8));
            buffStats.put("mana_cost", new StatDefinition("Costo de mana", 16.0D, -1.0D, 0, 8));
            templates.put("holy_guard", new SpellTemplate(
                "holy_guard",
                "Guardia Sagrada",
                "buff",
                "Buff",
                Set.of("clerigo", "paladin"),
                Map.of("meditation", 8),
                "",
                buffStats,
                "",
                1.0D,
                1.0D
            ));
        }
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private String typeToDisplay(String type) {
        return switch (type) {
            case "attack_aoe" -> "Ataque en area";
            case "attack_projectile" -> "Ataque de proyectil";
            case "buff" -> "Buff";
            default -> type;
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String colorize(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private record SkillAliasRule(int everyLevels, int points) {
        int pointsFromLevel(int level) {
            if (level <= 0 || points <= 0) {
                return 0;
            }
            return (level / Math.max(1, everyLevels)) * points;
        }
    }

    private record CrystalDefinition(String key, String itemRef, int points) {}

    private record SpellBookDefinition(String key, String itemRef, int bonusPoints, Set<String> allowedTypes) {}

    private record StatDefinition(String displayName, double baseValue, double valuePerPoint, int minPoints, int maxPoints) {}

    private record SpellTemplate(
        String key,
        String displayName,
        String type,
        String typeDisplay,
        Set<String> allowedClasses,
        Map<String, Integer> requiredSkills,
        String mythicSkill,
        Map<String, StatDefinition> stats,
        String projectileModelItem,
        double damageMultiplier,
        double projectileScale
    ) {}
}

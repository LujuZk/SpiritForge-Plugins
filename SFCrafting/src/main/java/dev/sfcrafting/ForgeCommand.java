package dev.sfcrafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ForgeCommand implements CommandExecutor, TabCompleter {

    private final SFCraftingPlugin plugin;
    private final ForgeManager manager;
    private final AuraManager auraManager;
    private final SpellCraftingManager spellCraftingManager;
    private final SpellCastManager spellCastManager;
    private final OraxenItemResolver resolver;
    private final NamespacedKey bowTypeDataKey = new NamespacedKey("customforge", "bow_type");
    private final NamespacedKey woodDataKey = new NamespacedKey("customforge", "wood");
    private final NamespacedKey metalDataKey = new NamespacedKey("customforge", "metal");
    private final NamespacedKey bowDamageDataKey = new NamespacedKey("customforge", "bow_damage");
    private final NamespacedKey bowSpeedDataKey = new NamespacedKey("customforge", "bow_speed");
    private final NamespacedKey bowVelocityDataKey = new NamespacedKey("customforge", "bow_velocity_multiplier");
    private final NamespacedKey bowGravityDataKey = new NamespacedKey("customforge", "arrow_gravity");
    private final NamespacedKey bowWoodRarityDataKey = new NamespacedKey("customforge", "wood_rarity");
    private final NamespacedKey bowMetalRarityDataKey = new NamespacedKey("customforge", "metal_rarity");

    public ForgeCommand(SFCraftingPlugin plugin, ForgeManager manager, AuraManager auraManager, SpellCraftingManager spellCraftingManager, SpellCastManager spellCastManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.auraManager = auraManager;
        this.spellCraftingManager = spellCraftingManager;
        this.spellCastManager = spellCastManager;
        this.resolver = new OraxenItemResolver(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("spellhit")) {
            return handleSpellHit(sender, args);
        }

        if (!sender.hasPermission("sfcrafting.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            if (auraManager != null) {
                auraManager.reloadSettings();
            }
            if (spellCraftingManager != null) {
                spellCraftingManager.reload();
            }
            sender.sendMessage(ChatColor.GREEN + "Config recargada. Aura actualizada.");
            return true;
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("spellcraft")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
                return true;
            }
            if (spellCraftingManager == null || !spellCraftingManager.isEnabled()) {
                sender.sendMessage(ChatColor.RED + "El sistema de spellcraft esta deshabilitado.");
                return true;
            }
            String templateKey = args[1];
            String crystalKey = args[2];
            String bookKey = args[3];
            Map<String, Integer> allocations = parseAllocations(args[4]);
            if (allocations.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Debes asignar stats. Ejemplo: damage=5,area=3,cooldown=2,mana_cost=1");
                return true;
            }

            SpellCraftingManager.CraftResult result = spellCraftingManager.craftSpell(player, templateKey, crystalKey, bookKey, allocations);
            sender.sendMessage(result.message());
            if (!result.success() || result.item() == null || result.item().getType().isAir()) {
                return true;
            }
            var leftovers = player.getInventory().addItem(result.item());
            for (var leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("debugbow")) {
            Player target;
            if (args.length >= 2) {
                target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage(ChatColor.RED + "Debes indicar un jugador: /" + label + " debugbow <jugador>");
                return true;
            }
            ItemStack hand = target.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() != Material.BOW || !hand.hasItemMeta()) {
                sender.sendMessage(ChatColor.RED + "El jugador no tiene un arco valido en la mano principal.");
                return true;
            }
            ItemMeta meta = hand.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String oraxenId = resolver.readOraxenId(hand);
            String bowType = pdc.getOrDefault(bowTypeDataKey, PersistentDataType.STRING, "<none>");
            String wood = pdc.getOrDefault(woodDataKey, PersistentDataType.STRING, "<none>");
            String metal = pdc.getOrDefault(metalDataKey, PersistentDataType.STRING, "<none>");
            int woodRarity = pdc.getOrDefault(bowWoodRarityDataKey, PersistentDataType.INTEGER, -1);
            int metalRarity = pdc.getOrDefault(bowMetalRarityDataKey, PersistentDataType.INTEGER, -1);
            Double pdcDamage = pdc.get(bowDamageDataKey, PersistentDataType.DOUBLE);
            Double pdcSpeed = pdc.get(bowSpeedDataKey, PersistentDataType.DOUBLE);
            Double pdcVelocity = pdc.get(bowVelocityDataKey, PersistentDataType.DOUBLE);
            Double pdcGravity = pdc.get(bowGravityDataKey, PersistentDataType.DOUBLE);
            double attrDamage = sumAttribute(meta, Attribute.GENERIC_ATTACK_DAMAGE);
            sender.sendMessage(ChatColor.GOLD + "Debug arco de " + target.getName() + ":");
            sender.sendMessage(ChatColor.GRAY + "id=" + (oraxenId == null ? "<none>" : oraxenId)
                + " type=" + bowType + " wood=" + wood + "(" + woodRarity + ")"
                + " metal=" + metal + "(" + metalRarity + ")");
            sender.sendMessage(ChatColor.GRAY + "damage=" + fmt(pdcDamage)
                + " | speed=" + fmt(pdcSpeed)
                + " | velocity=" + fmt(pdcVelocity)
                + " | arrow_gravity=" + fmt(pdcGravity));
            sender.sendMessage(ChatColor.DARK_GRAY + "attack_damage_attribute=" + fmt(attrDamage));
            if (pdcSpeed != null && pdcSpeed > 0.0D) {
                double drawSeconds = 3.0D / pdcSpeed;
                sender.sendMessage(ChatColor.DARK_GRAY + "tiempo_carga_completa=" + fmt(drawSeconds) + "s");
            }
            return true;
        }
        if (args.length >= 6 && args[0].equalsIgnoreCase("givebow")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
                return true;
            }
            String bowType = args[1];
            String wood = args[2];
            int woodRarity = parseRarity(args[3]);
            String metal = args[4];
            int metalRarity = parseRarity(args[5]);
            if (woodRarity < 0 || metalRarity < 0) {
                sender.sendMessage(ChatColor.RED + "Rareza invalida. Usa 0-4 o comun/poco_comun/raro/epico/legendario.");
                return true;
            }
            int amount = 1;
            if (args.length >= 7) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[6]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(ChatColor.RED + "Cantidad invalida.");
                    return true;
                }
            }
            Integer finalRarityOverride = null;
            if (args.length >= 8) {
                int parsed = parseRarity(args[7]);
                if (parsed < 0) {
                    sender.sendMessage(ChatColor.RED + "Rareza final invalida.");
                    return true;
                }
                finalRarityOverride = parsed;
            }
            ItemStack bow = manager.buildConfiguredBow(
                bowType,
                wood,
                woodRarity,
                metal,
                metalRarity,
                amount,
                finalRarityOverride
            );
            if (bow == null || bow.getType().isAir()) {
                sender.sendMessage(ChatColor.RED + "No se pudo crear el arco. Revisa tipo/madera/metal (ej: hunter_bow oak iron).");
                return true;
            }
            var leftovers = player.getInventory().addItem(bow);
            for (var leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            int finalRarity = finalRarityOverride == null ? Math.max(woodRarity, metalRarity) : finalRarityOverride;
            sender.sendMessage(ChatColor.GREEN + "Entregado: " + bowType + " " + wood + "(" + woodRarity + ") + "
                + metal + "(" + metalRarity + ") x" + amount + " rareza final " + finalRarity + ".");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " give <oraxen_id|material:id> [cantidad] [rareza]");
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " givebow <tipo> <madera> <rareza_madera> <metal> <rareza_metal> [cantidad] [rareza_final]");
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " spellcraft <plantilla> <cristal> <libro> <stat=puntos,...>");
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " debugbow [jugador]");
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " reload");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
            return true;
        }

        String rawId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Cantidad invalida.");
                return true;
            }
        }
        int rarity = 0;
        if (args.length >= 4) {
            rarity = parseRarity(args[3]);
            if (rarity < 0) {
                sender.sendMessage(ChatColor.RED + "Rareza invalida. Usa 0-4 o comun/poco_comun/raro/epico/legendario.");
                return true;
            }
        }

        ItemStack item = buildItem(rawId, amount);
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "No se encontro el item: " + rawId);
            return true;
        }
        manager.applyRarity(item, rarity);
        var leftovers = player.getInventory().addItem(item);
        for (var leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        sender.sendMessage(ChatColor.GREEN + "Entregado: " + rawId + " x" + amount + " (rareza " + rarity + ").");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.add("give");
            result.add("givebow");
            result.add("spellcraft");
            result.add("debugbow");
            result.add("reload");
            return result;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("spellcraft")) {
            if (spellCraftingManager == null || !spellCraftingManager.isEnabled()) {
                return result;
            }
            if (args.length == 2) {
                result.addAll(spellCraftingManager.getTemplateKeys());
                return result;
            }
            if (args.length == 3) {
                result.addAll(spellCraftingManager.getCrystalKeys());
                return result;
            }
            if (args.length == 4) {
                result.addAll(spellCraftingManager.getBookKeys());
                return result;
            }
            if (args.length == 5) {
                for (String statKey : spellCraftingManager.getStatKeys(args[1])) {
                    result.add(statKey + "=1");
                }
                return result;
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debugbow")) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                result.add(online.getName());
            }
            return result;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("givebow")) {
            if (args.length == 2) {
                result.add("hunter_bow");
                result.add("short_bow");
                result.add("long_bow");
                return result;
            }
            if (args.length == 3) {
                result.add("oak");
                result.add("birch");
                return result;
            }
            if (args.length == 4 || args.length == 6 || args.length == 8) {
                result.add("0");
                result.add("1");
                result.add("2");
                result.add("3");
                result.add("4");
                result.add("comun");
                result.add("poco_comun");
                result.add("raro");
                result.add("epico");
                result.add("legendario");
                return result;
            }
            if (args.length == 5) {
                result.add("copper");
                result.add("iron");
                return result;
            }
            if (args.length == 7) {
                result.add("1");
                return result;
            }
        }
        if (args.length == 4) {
            result.add("0");
            result.add("1");
            result.add("2");
            result.add("3");
            result.add("4");
            result.add("comun");
            result.add("poco_comun");
            result.add("raro");
            result.add("epico");
            result.add("legendario");
            return result;
        }
        return result;
    }

    private int parseRarity(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.matches("\\d+")) {
            int number = Integer.parseInt(value);
            if (number >= 0 && number <= 4) {
                return number;
            }
            return -1;
        }
        return switch (value) {
            case "comun" -> 0;
            case "poco_comun", "pococomun", "poco-comun" -> 1;
            case "raro" -> 2;
            case "epico" -> 3;
            case "legendario" -> 4;
            default -> -1;
        };
    }

    private ItemStack buildItem(String raw, int amount) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("oraxen:")) {
            String id = value.substring("oraxen:".length()).trim();
            return resolver.buildOraxenItem(id, amount);
        }
        if (lower.startsWith("material:")) {
            String id = value.substring("material:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(id);
            return material == null ? null : new ItemStack(material, amount);
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material != null) {
            return new ItemStack(material, amount);
        }
        return resolver.buildOraxenItem(value, amount);
    }

    private double sumAttribute(ItemMeta meta, Attribute attribute) {
        Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
        if (modifiers == null || modifiers.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (AttributeModifier modifier : modifiers) {
            total += modifier.getAmount();
        }
        return total;
    }

    private String fmt(Double value) {
        if (value == null) {
            return "<none>";
        }
        return fmt(value.doubleValue());
    }

    private String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private boolean handleSpellHit(CommandSender sender, String[] args) {
        if (spellCastManager == null) {
            sender.sendMessage(ChatColor.RED + "Spell casting no disponible.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /sfcrafting spellhit <caster_uuid> <target_uuid>");
            return true;
        }
        try {
            UUID casterId = UUID.fromString(args[1]);
            UUID targetId = UUID.fromString(args[2]);
            boolean ok = spellCastManager.applyQueuedSpellHit(casterId, targetId);
            if (!ok) {
                sender.sendMessage(ChatColor.RED + "No se pudo aplicar el impacto.");
            }
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "UUID invalido.");
            return true;
        }
    }

    private Map<String, Integer> parseAllocations(String raw) {
        Map<String, Integer> allocations = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return allocations;
        }
        String[] pairs = raw.split(",");
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=");
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            if (key.isBlank()) {
                continue;
            }
            try {
                int value = Integer.parseInt(parts[1].trim());
                if (value < 0) {
                    continue;
                }
                allocations.put(key, value);
            } catch (NumberFormatException ignored) {
            }
        }
        return allocations;
    }
}



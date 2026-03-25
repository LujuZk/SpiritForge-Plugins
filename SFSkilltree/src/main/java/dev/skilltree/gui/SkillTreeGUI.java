package dev.skilltree.gui;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeGUI {

    private static final String GUI_TITLE = "✦ Árbol de Habilidades ✦";

    // Slots para cada skill en el inventario 3x9 (27 slots)
    private static final int SLOT_MINING = 10;
    private static final int SLOT_FARMING = 12;
    private static final int SLOT_FISHING = 14;
    private static final int SLOT_SWORD = 20;
    private static final int SLOT_AXE = 22;
    private static final int SLOT_BOW = 24;
    private static final int SLOT_TRIDENT = 16;

    private final SkillTreePlugin plugin;

    public SkillTreeGUI(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(GUI_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));

        PlayerSkillData data = plugin.getSkillManager().getData(player);

        // Rellenar fondo con vidrio gris
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        // Colocar cada skill
        inv.setItem(SLOT_MINING, buildSkillItem(SkillType.MINING, data, Material.DIAMOND_PICKAXE));
        inv.setItem(SLOT_FARMING, buildSkillItem(SkillType.FARMING, data, Material.WHEAT));
        inv.setItem(SLOT_FISHING, buildSkillItem(SkillType.FISHING, data, Material.FISHING_ROD));
        inv.setItem(SLOT_SWORD, buildSkillItem(SkillType.WEAPON_SWORD, data, Material.DIAMOND_SWORD));
        inv.setItem(SLOT_AXE, buildSkillItem(SkillType.WEAPON_AXE, data, Material.DIAMOND_AXE));
        inv.setItem(SLOT_BOW, buildSkillItem(SkillType.WEAPON_BOW, data, Material.BOW));
        inv.setItem(SLOT_TRIDENT, buildSkillItem(SkillType.WEAPON_TRIDENT, data, Material.TRIDENT));

        // Guardar y limpiar el inventario del jugador
        plugin.getInventoryManager().saveAndClearInventory(player);

        player.openInventory(inv);
    }

    private ItemStack buildSkillItem(SkillType skill, PlayerSkillData data, Material material) {
        int level = data.getLevel(skill);
        double xp = data.getXP(skill);

        double baseXP = plugin.getConfig().getDouble("xp-per-level", 100);
        double multi = plugin.getConfig().getDouble("xp-multiplier", 1.5);
        double required = data.getXPRequired(skill, baseXP, multi);
        double progress = plugin.getSkillManager().getProgress(
                Bukkit.getPlayer(data.getPlayerUUID()), skill);

        String progressBar = buildProgressBar(progress, 20);
        int maxLevel = plugin.getConfig().getInt("max-level", 50);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("  Nivel: ", NamedTextColor.GRAY)
                .append(Component.text(level, NamedTextColor.YELLOW)));

        if (level < maxLevel) {
            lore.add(Component.text("  XP: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.0f / %.0f", xp, required), NamedTextColor.WHITE)));
            lore.add(Component.text("  " + progressBar, NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("  ¡Nivel MÁXIMO!", NamedTextColor.GOLD));
        }
        lore.add(Component.empty());

        if (skill.isWeapon()) {
            lore.add(Component.text("  » Usá esta arma para ganar XP", NamedTextColor.DARK_AQUA));
        } else {
            lore.add(Component.text("  » Realizá la actividad para ganar XP", NamedTextColor.DARK_AQUA));
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(skill.getDisplayName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String buildProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    public static String getGuiTitle() {
        return GUI_TITLE;
    }

    /**
     * Retorna el SkillType correspondiente al slot clickeado en el menú principal
     */
    public static SkillType getSkillForSlot(int slot) {
        return switch (slot) {
            case SLOT_MINING -> SkillType.MINING;
            case SLOT_FARMING -> SkillType.FARMING;
            case SLOT_FISHING -> SkillType.FISHING;
            case SLOT_SWORD -> SkillType.WEAPON_SWORD;
            case SLOT_AXE -> SkillType.WEAPON_AXE;
            case SLOT_BOW -> SkillType.WEAPON_BOW;
            case SLOT_TRIDENT -> SkillType.WEAPON_TRIDENT;
            default -> null;
        };
    }
}
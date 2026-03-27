package dev.sfcharacter.gui;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.models.CharacterClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ClassSelectGUI {

    public static final String GUI_TITLE = "Elige tu clase";
    private static final int GUI_SIZE = 9;

    public static final NamespacedKey KEY_ACTION = CharacterSelectGUI.KEY_ACTION;
    public static final NamespacedKey KEY_CLASS = new NamespacedKey("sfcharacter", "class_name");

    // Slot positions for each class
    private static final int SLOT_MAGO = 2;
    private static final int SLOT_GUERRERO = 4;
    private static final int SLOT_PICARO = 6;

    private final SFCharacterPlugin plugin;

    public ClassSelectGUI(SFCharacterPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(GUI_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Fill with filler
        ItemStack filler = createFiller();
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler);
        }

        // Back button
        inv.setItem(0, buildBackButton());

        // Class items
        inv.setItem(SLOT_MAGO, buildClassItem(CharacterClass.MAGO));
        inv.setItem(SLOT_GUERRERO, buildClassItem(CharacterClass.GUERRERO));
        inv.setItem(SLOT_PICARO, buildClassItem(CharacterClass.PICARO));

        player.openInventory(inv);
    }

    private ItemStack buildClassItem(CharacterClass clazz) {
        ItemStack item = new ItemStack(clazz.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(clazz.getDisplayName(), clazz.getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(clazz.getDescription(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click para elegir", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "pick_class");
        meta.getPersistentDataContainer().set(KEY_CLASS, PersistentDataType.STRING, clazz.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("← Volver", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "back");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
}

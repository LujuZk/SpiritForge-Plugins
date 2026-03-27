package dev.sfcharacter.gui;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.models.CharacterData;
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

public class CharacterSelectGUI {

    public static final String GUI_TITLE = "Seleccion de Personaje";
    private static final int GUI_SIZE = 9;
    private static final int[] SLOT_POSITIONS = {0, 2, 4, 6, 8};

    public static final NamespacedKey KEY_ACTION = new NamespacedKey("sfcharacter", "action");
    public static final NamespacedKey KEY_SLOT = new NamespacedKey("sfcharacter", "slot");

    private final SFCharacterPlugin plugin;

    public CharacterSelectGUI(SFCharacterPlugin plugin) {
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

        // Place character slots
        List<CharacterData> characters = plugin.getCharacterManager().getCharacters(player.getUniqueId());

        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            int invSlot = SLOT_POSITIONS[i];
            CharacterData data = findBySlot(characters, i);

            if (data == null) {
                inv.setItem(invSlot, buildEmptySlot(i));
            } else {
                inv.setItem(invSlot, buildOccupiedSlot(data));
            }
        }

        player.openInventory(inv);
    }

    private CharacterData findBySlot(List<CharacterData> characters, int slot) {
        for (CharacterData cd : characters) {
            if (cd.slot() == slot) return cd;
        }
        return null;
    }

    private ItemStack buildEmptySlot(int slot) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Slot vacio - Click para crear", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "create");
        meta.getPersistentDataContainer().set(KEY_SLOT, PersistentDataType.INTEGER, slot);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildOccupiedSlot(CharacterData data) {
        ItemStack item = new ItemStack(data.characterClass().getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(data.displayName(), data.characterClass().getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Clase: " + data.characterClass().getDisplayName(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Creado: " + data.createdAt(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click para seleccionar", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "select");
        meta.getPersistentDataContainer().set(KEY_SLOT, PersistentDataType.INTEGER, data.slot());
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

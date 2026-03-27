package dev.sfcharacter.listeners;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.api.CharacterSelectEvent;
import dev.sfcharacter.gui.CharacterSelectGUI;
import dev.sfcharacter.gui.ClassSelectGUI;
import dev.sfcharacter.managers.CharacterManager;
import dev.sfcharacter.models.CharacterClass;
import dev.sfcharacter.models.CharacterData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {

    private final SFCharacterPlugin plugin;

    public GUIListener(SFCharacterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (title.contains(CharacterSelectGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleCharacterSelectClick(player, event.getCurrentItem());
            return;
        }

        if (title.contains(ClassSelectGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleClassSelectClick(player, event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!title.contains(CharacterSelectGUI.GUI_TITLE) && !title.contains(ClassSelectGUI.GUI_TITLE)) {
            return;
        }

        CharacterManager manager = plugin.getCharacterManager();

        // If player has no active character, reopen the GUI after 1 tick
        if (!manager.hasActiveCharacter(player.getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                String currentTitle = PlainTextComponentSerializer.plainText()
                        .serialize(player.getOpenInventory().title());
                if (currentTitle.contains(CharacterSelectGUI.GUI_TITLE)
                        || currentTitle.contains(ClassSelectGUI.GUI_TITLE)) {
                    return;
                }

                if (!manager.hasActiveCharacter(player.getUniqueId())) {
                    plugin.getSelectGUI().open(player);
                }
            });
        }
    }

    // ─── Character Select GUI ───────────────────────────────────────────────

    private void handleCharacterSelectClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();

        String action = pdc.get(CharacterSelectGUI.KEY_ACTION, PersistentDataType.STRING);
        Integer slot = pdc.get(CharacterSelectGUI.KEY_SLOT, PersistentDataType.INTEGER);
        if (action == null || slot == null) return;

        CharacterManager manager = plugin.getCharacterManager();

        switch (action) {
            case "create" -> {
                manager.setPendingSlot(player.getUniqueId(), slot);
                plugin.getClassSelectGUI().open(player);
            }
            case "select" -> {
                CharacterData oldChar = manager.getActiveCharacter(player.getUniqueId());
                manager.setActiveSlot(player.getUniqueId(), slot);
                player.closeInventory();

                // Restore character state (inventory + teleport)
                manager.loadCharacterState(player);

                CharacterData newChar = manager.getActiveCharacter(player.getUniqueId());
                if (newChar != null) {
                    player.sendMessage(Component.text("Personaje seleccionado: ", NamedTextColor.GREEN)
                            .append(Component.text(newChar.displayName(), newChar.characterClass().getColor())));
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                // Fire event for other plugins
                Bukkit.getPluginManager().callEvent(new CharacterSelectEvent(player, oldChar, newChar));
            }
        }
    }

    // ─── Class Select GUI ───────────────────────────────────────────────────

    private void handleClassSelectClick(Player player, ItemStack item) {
        if (!item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();

        String action = pdc.get(ClassSelectGUI.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) return;

        CharacterManager manager = plugin.getCharacterManager();

        switch (action) {
            case "pick_class" -> {
                String className = pdc.get(ClassSelectGUI.KEY_CLASS, PersistentDataType.STRING);
                CharacterClass clazz = CharacterClass.fromName(className);
                if (clazz == null) return;

                int pendingSlot = manager.getPendingSlot(player.getUniqueId());
                if (pendingSlot < 0) return;

                CharacterData data = manager.createCharacter(player.getUniqueId(), pendingSlot, clazz);
                manager.clearPendingSlot(player.getUniqueId());
                player.closeInventory();

                // New character: load state (teleports to world spawn since no saved location)
                manager.loadCharacterState(player);

                player.sendMessage(Component.text("Personaje creado: ", NamedTextColor.GREEN)
                        .append(Component.text(data.displayName(), data.characterClass().getColor())));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                // Fire event
                Bukkit.getPluginManager().callEvent(new CharacterSelectEvent(player, null, data));
            }
            case "back" -> {
                manager.clearPendingSlot(player.getUniqueId());
                plugin.getSelectGUI().open(player);
            }
        }
    }
}

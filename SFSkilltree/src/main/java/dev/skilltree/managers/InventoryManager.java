package dev.skilltree.managers;

import dev.skilltree.SkillTreePlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {

    private final SkillTreePlugin plugin;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    public InventoryManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Guarda el inventario actual del jugador y lo limpia visualmente.
     * Solo funciona si el jugador no tiene ya uno guardado.
     */
    public void saveAndClearInventory(Player player) {
        if (savedInventories.containsKey(player.getUniqueId()))
            return;

        // Clonar el contenido completo del inventario (incluye armor, offhand, etc)
        ItemStack[] content = player.getInventory().getContents();
        ItemStack[] clonedContent = new ItemStack[content.length];
        for (int i = 0; i < content.length; i++) {
            if (content[i] != null) {
                clonedContent[i] = content[i].clone();
            }
        }

        savedInventories.put(player.getUniqueId(), clonedContent);
        player.getInventory().clear();
        player.updateInventory(); // Forzar update visual
    }

    /**
     * Restaura el inventario guardado del jugador.
     */
    public void restoreInventory(Player player) {
        ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved);
            player.updateInventory();
        }
    }

    /**
     * Verifica si el jugador tiene su inventario oculto actualmente.
     */
    public boolean hasSavedInventory(Player player) {
        return savedInventories.containsKey(player.getUniqueId());
    }

    /**
     * Restaura el inventario de todos los jugadores que lo tenían guardado.
     * Util para cuando se desactiva el plugin o se reinicia el server.
     */
    public void restoreAll() {
        for (UUID uuid : savedInventories.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.getInventory().setContents(savedInventories.get(uuid));
                player.updateInventory();
            }
        }
        savedInventories.clear();
    }
}

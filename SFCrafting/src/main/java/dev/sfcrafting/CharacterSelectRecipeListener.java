package dev.sfcrafting;

import dev.sfcharacter.api.CharacterSelectEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener aislado — se carga sólo cuando SFCharacter está disponible, registrado
 * desde SFCraftingPlugin tras verificación.
 */
public final class CharacterSelectRecipeListener implements Listener {

    private final RecipeBookManager manager;

    public CharacterSelectRecipeListener(RecipeBookManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCharacterSelect(CharacterSelectEvent event) {
        Player player = event.getPlayer();
        // Cerrar recetario abierto defensivamente — evita interactuar con el cache del slot viejo
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof RecipeBookHolder) {
            player.closeInventory();
        }
        manager.reloadForActiveSlot(player.getUniqueId());
    }
}

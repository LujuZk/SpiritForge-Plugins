package dev.sfcompass.listeners;

import dev.sfcharacter.api.CharacterSelectEvent;
import dev.sfcompass.managers.CompassManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener aislado — se carga sólo cuando SFCharacter está disponible, registrado
 * desde SFCompassPlugin tras verificación.
 */
public class CharacterSelectCompassListener implements Listener {

    private final CompassManager compassManager;

    public CharacterSelectCompassListener(CompassManager compassManager) {
        this.compassManager = compassManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCharacterSelect(CharacterSelectEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        compassManager.unloadPlayer(uuid);
        compassManager.loadPlayer(uuid);
    }
}

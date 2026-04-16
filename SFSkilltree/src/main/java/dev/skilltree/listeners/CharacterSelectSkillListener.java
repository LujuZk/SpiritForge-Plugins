package dev.skilltree.listeners;

import dev.sfcharacter.api.CharacterSelectEvent;
import dev.skilltree.SkillTreePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener aislado — se carga sólo cuando SFCharacter está disponible, registrado
 * desde SkillTreePlugin tras verificación.
 */
public class CharacterSelectSkillListener implements Listener {

    private final SkillTreePlugin plugin;

    public CharacterSelectSkillListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCharacterSelect(CharacterSelectEvent event) {
        plugin.getSkillManager().reloadForActiveSlot(event.getPlayer());
    }
}

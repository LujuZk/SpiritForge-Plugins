package dev.sfcharacter.api;

import dev.sfcharacter.models.CharacterData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Se dispara después de que un jugador selecciona o cambia de personaje.
 * Otros plugins pueden escuchar este evento para recargar datos per-character.
 */
public class CharacterSelectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final @Nullable CharacterData oldCharacter;
    private final CharacterData newCharacter;

    public CharacterSelectEvent(Player player, @Nullable CharacterData oldCharacter, CharacterData newCharacter) {
        this.player = player;
        this.oldCharacter = oldCharacter;
        this.newCharacter = newCharacter;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * El personaje anterior, o null si es la primera selección de la sesión.
     */
    public @Nullable CharacterData getOldCharacter() {
        return oldCharacter;
    }

    public CharacterData getNewCharacter() {
        return newCharacter;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

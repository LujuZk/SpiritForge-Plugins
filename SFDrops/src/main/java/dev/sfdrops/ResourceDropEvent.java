package dev.sfdrops;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado por SFDrops cuando un bloque custom (ore o log) produce
 * un drop con rareza. Escuchado por SFSkilltree para otorgar XP de minería
 * o tala según el bloque.
 *
 * Rareza: 0=Común, 1=Poco común, 2=Raro, 3=Épico, 4=Legendario
 */
public class ResourceDropEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String blockId;
    private final int rarityLevel;

    public ResourceDropEvent(Player player, String blockId, int rarityLevel) {
        this.player = player;
        this.blockId = blockId;
        this.rarityLevel = rarityLevel;
    }

    /** Jugador que rompió el bloque. */
    public Player getPlayer() { return player; }

    /** ID del bloque roto: Oraxen block ID o Material name en lowercase. Ej: "tin_ore", "oak_log". */
    public String getBlockId() { return blockId; }

    /** Nivel de rareza del drop: 0 (Común) a 4 (Legendario). */
    public int getRarityLevel() { return rarityLevel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

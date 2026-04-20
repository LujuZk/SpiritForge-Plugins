package dev.sfcrafting;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado por SFCrafting cuando un hechizo se craftea exitosamente.
 * Escuchado por SFSkilltree para otorgar XP de spellcrafting.
 */
public class SpellCraftCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String templateKey;
    private final String crystalKey;
    private final int pointsUsed;

    public SpellCraftCompleteEvent(Player player, String templateKey, String crystalKey, int pointsUsed) {
        this.player = player;
        this.templateKey = templateKey;
        this.crystalKey = crystalKey;
        this.pointsUsed = pointsUsed;
    }

    /** Jugador que crafteó el hechizo. */
    public Player getPlayer() { return player; }

    /** Key del template del hechizo crafteado. Ej: "fire_burst", "heal_wave". */
    public String getTemplateKey() { return templateKey; }

    /** Key del cristal usado. Ej: "basic_crystal", "rare_crystal". */
    public String getCrystalKey() { return crystalKey; }

    /** Cantidad de puntos asignados al hechizo. */
    public int getPointsUsed() { return pointsUsed; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

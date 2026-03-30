package dev.sfcrafting;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado por SFCrafting cuando una fundición se completa.
 * Escuchado por SFSkilltree para otorgar XP de herrería.
 *
 * Rareza: 0=Común, 1=Poco común, 2=Raro, 3=Épico, 4=Legendario
 */
public class SmeltCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String recipeId;
    private final int rarityLevel;

    public SmeltCompleteEvent(Player player, String recipeId, int rarityLevel) {
        this.player = player;
        this.recipeId = recipeId;
        this.rarityLevel = rarityLevel;
    }

    /** Jugador que inició la fundición. */
    public Player getPlayer() { return player; }

    /** ID de la receta fundida. Ej: "tin", "copper", "steel", "reheat_tin". */
    public String getRecipeId() { return recipeId; }

    /** Nivel de rareza del output: 0 (Común) a 4 (Legendario). */
    public int getRarityLevel() { return rarityLevel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

package dev.sfcrafting;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado cuando se completa una fundición en el smelter.
 * Permite a otros plugins (ej. SFSkilltree) reaccionar sin dependencia directa.
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

    public Player getPlayer() {
        return player;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public int getRarityLevel() {
        return rarityLevel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

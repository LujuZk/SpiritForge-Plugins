package dev.sfcore.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Se dispara después de que los stats de un jugador cambian (después de reapplyAll).
 * Permite a otros plugins reaccionar a cambios de stats sin polling.
 */
public class StatChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Map<StatType, Double> totals;

    public StatChangeEvent(Player player, Map<StatType, Double> totals) {
        this.player = player;
        this.totals = totals;
    }

    public Player getPlayer() {
        return player;
    }

    public Map<StatType, Double> getTotals() {
        return totals;
    }

    public double getTotal(StatType stat) {
        return totals.getOrDefault(stat, 0.0);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

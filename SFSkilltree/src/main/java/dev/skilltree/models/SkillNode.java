package dev.skilltree.models;

import java.util.List;
import java.util.Set;

/**
 * Representa un nodo dentro del grafo de habilidades.
 * Usa coordenadas de grid (gridX, gridY) en lugar de slots de inventario.
 */
public class SkillNode {

    private final String id;
    private final String name;
    private final String description;
    private final int cost;
    private final int gridX; // columna en el grid (0-8)
    private final int gridY; // fila en el grid (0-5)
    private final String iconId; // ID del icono en icons.yml
    private final List<String> requires;
    private final boolean requiresAll;
    private final List<String> exclusiveWith;
    private final String effectType;
    private final double effectValue;

    public SkillNode(String id, String name, String description, int cost,
                     int gridX, int gridY, String iconId,
                     List<String> requires, boolean requiresAll,
                     List<String> exclusiveWith, String effectType, double effectValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.gridX = gridX;
        this.gridY = gridY;
        this.iconId = iconId;
        this.requires = requires == null ? List.of() : List.copyOf(requires);
        this.requiresAll = requiresAll;
        this.exclusiveWith = exclusiveWith == null ? List.of() : List.copyOf(exclusiveWith);
        this.effectType = effectType;
        this.effectValue = effectValue;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public String getIconId() {
        return iconId;
    }

    public List<String> getRequires() {
        return requires;
    }

    public boolean isRequiresAll() {
        return requiresAll;
    }

    public List<String> getExclusiveWith() {
        return exclusiveWith;
    }

    public String getEffectType() {
        return effectType;
    }

    public double getEffectValue() {
        return effectValue;
    }

    // ─── Lógica ──────────────────────────────────────────────────────────────

    /**
     * Retorna true si este nodo no tiene prerequisitos (es un nodo raíz).
     */
    public boolean isRoot() {
        return requires.isEmpty();
    }

    /**
     * Retorna true si el nodo puede ser desbloqueado según los nodos ya desbloqueados.
     */
    public boolean isUnlockable(Set<String> unlockedNodes) {
        if (requires.isEmpty())
            return true;
        if (requiresAll) {
            return unlockedNodes.containsAll(requires);
        } else {
            return requires.stream().anyMatch(unlockedNodes::contains);
        }
    }

    /**
     * Retorna true si este nodo está bloqueado porque un nodo mutuamente exclusivo ya fue desbloqueado.
     */
    public boolean isBlockedByExclusive(Set<String> unlockedNodes) {
        return exclusiveWith.stream().anyMatch(unlockedNodes::contains);
    }

    /**
     * Calcula el slot de inventario (0-53) basado en las coordenadas del grid.
     * Fórmula: slot = gridY * 9 + gridX
     */
    public int toInventorySlot() {
        return gridY * 9 + gridX;
    }
}

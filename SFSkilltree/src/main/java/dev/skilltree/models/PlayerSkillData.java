package dev.skilltree.models;

import java.util.*;

/**
 * Almacena todos los datos de skills de un jugador en memoria.
 */
public class PlayerSkillData {

    private final UUID playerUUID;
    // skill -> nivel actual
    private final Map<SkillType, Integer> levels = new HashMap<>();
    // skill -> XP actual en el nivel
    private final Map<SkillType, Double> xp = new HashMap<>();
    // skill -> puntos de habilidad disponibles para gastar en el árbol
    private final Map<SkillType, Integer> availablePoints = new HashMap<>();
    // skill -> set de IDs de nodos desbloqueados
    private final Map<SkillType, Set<String>> unlockedNodes = new HashMap<>();

    public PlayerSkillData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        for (SkillType type : SkillType.values()) {
            levels.put(type, 1);
            xp.put(type, 0.0);
            availablePoints.put(type, 0);
            unlockedNodes.put(type, new HashSet<>());
        }
    }

    public UUID getPlayerUUID() { return playerUUID; }

    // ─── Niveles y XP ────────────────────────────────────────────────────────

    public int getLevel(SkillType skill) {
        return levels.getOrDefault(skill, 1);
    }

    public double getXP(SkillType skill) {
        return xp.getOrDefault(skill, 0.0);
    }

    public void setLevel(SkillType skill, int level) {
        levels.put(skill, level);
    }

    public void setXP(SkillType skill, double amount) {
        xp.put(skill, amount);
    }

    public void addXP(SkillType skill, double amount) {
        xp.merge(skill, amount, Double::sum);
    }

    public double getXPRequired(SkillType skill, double baseXP, double multiplier) {
        int level = getLevel(skill);
        return baseXP * Math.pow(multiplier, level - 1);
    }

    // ─── Puntos de habilidad ─────────────────────────────────────────────────

    public int getAvailablePoints(SkillType skill) {
        return availablePoints.getOrDefault(skill, 0);
    }

    public void setAvailablePoints(SkillType skill, int points) {
        availablePoints.put(skill, Math.max(0, points));
    }

    public void addPoints(SkillType skill, int amount) {
        availablePoints.merge(skill, amount, Integer::sum);
    }

    public void spendPoints(SkillType skill, int amount) {
        int current = availablePoints.getOrDefault(skill, 0);
        availablePoints.put(skill, Math.max(0, current - amount));
    }

    // ─── Nodos del árbol ─────────────────────────────────────────────────────

    public Set<String> getUnlockedNodes(SkillType skill) {
        return Collections.unmodifiableSet(
                unlockedNodes.computeIfAbsent(skill, k -> new HashSet<>()));
    }

    public void unlockNode(SkillType skill, String nodeId) {
        unlockedNodes.computeIfAbsent(skill, k -> new HashSet<>()).add(nodeId);
    }

    public void setUnlockedNodes(SkillType skill, Set<String> nodes) {
        unlockedNodes.put(skill, new HashSet<>(nodes));
    }

    public void resetNodes(SkillType skill) {
        unlockedNodes.put(skill, new HashSet<>());
    }
}
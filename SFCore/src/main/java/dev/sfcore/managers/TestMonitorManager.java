package dev.sfcore.managers;

import dev.sfcore.api.StatType;

import java.util.*;

public class TestMonitorManager {

    private final Map<UUID, Set<StatType>> activeTests = new HashMap<>();

    public void enable(UUID uuid, StatType stat) {
        activeTests.computeIfAbsent(uuid, k -> EnumSet.noneOf(StatType.class)).add(stat);
    }

    public void enableAll(UUID uuid) {
        Set<StatType> set = activeTests.computeIfAbsent(uuid, k -> EnumSet.noneOf(StatType.class));
        Collections.addAll(set, StatType.values());
    }

    public void disable(UUID uuid, StatType stat) {
        Set<StatType> set = activeTests.get(uuid);
        if (set != null) set.remove(stat);
    }

    public void disableAll(UUID uuid) {
        activeTests.remove(uuid);
    }

    public boolean isActive(UUID uuid, StatType stat) {
        Set<StatType> set = activeTests.get(uuid);
        return set != null && set.contains(stat);
    }

    public Set<StatType> getActive(UUID uuid) {
        return activeTests.getOrDefault(uuid, Set.of());
    }
}

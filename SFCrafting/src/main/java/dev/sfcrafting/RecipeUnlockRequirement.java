package dev.sfcrafting;

import java.util.List;

public final class RecipeUnlockRequirement {

    private final String skillKey;
    private final int levelRequired;
    private final List<String> requiredDiscoveries;

    public RecipeUnlockRequirement(String skillKey, int levelRequired, List<String> requiredDiscoveries) {
        this.skillKey = skillKey;
        this.levelRequired = levelRequired;
        this.requiredDiscoveries = requiredDiscoveries == null ? List.of() : List.copyOf(requiredDiscoveries);
    }

    public String skillKey() {
        return skillKey;
    }

    public int levelRequired() {
        return levelRequired;
    }

    public List<String> requiredDiscoveries() {
        return requiredDiscoveries;
    }
}

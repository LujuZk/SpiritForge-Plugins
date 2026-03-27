package dev.sfdrops.model;

public enum Rarity {
    COMMON(1),
    UNCOMMON(2),
    RARE(3),
    EPIC(4),
    LEGENDARY(5);

    private final int position;

    Rarity(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
package dev.sfdrops.service;

import dev.sfdrops.model.Rarity;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class RarityRoller {

    private RarityRoller() {}

    public static Rarity roll(Map<Rarity, Double> chances) {
        double r = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0;

        for (Rarity rarity : Rarity.values()) {
            cumulative += chances.getOrDefault(rarity, 0.0);
            if (r <= cumulative) {
                return rarity;
            }
        }

        return Rarity.COMMON;
    }
}
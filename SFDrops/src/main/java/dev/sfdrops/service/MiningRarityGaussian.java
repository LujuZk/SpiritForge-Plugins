package dev.sfdrops.service;

import dev.sfdrops.model.Rarity;

import java.util.EnumMap;
import java.util.Map;

public final class MiningRarityGaussian {

    private MiningRarityGaussian() {}

    public static Map<Rarity, Double> calculate(
            double str,
            double mining,
            double clazz,
            double pickaxe,
            double strMax,
            double miningMax,
            double classMax,
            double pickaxeMax,
            double wStr,
            double wMining,
            double wClass,
            double wPickaxe,
            double curveExponent,
            double width,
            double minimumWeight
    ) {
        double s = clamp(str / strMax, 0, 1);
        double m = clamp(mining / miningMax, 0, 1);
        double c = clamp(clazz / classMax, 0, 1);
        double p = clamp(pickaxe / pickaxeMax, 0, 1);

        double f = (s * wStr) + (m * wMining) + (c * wClass) + (p * wPickaxe);
        f = Math.pow(f, curveExponent);

        double center = 1 + (5 - 1) * f;

        Map<Rarity, Double> weights = new EnumMap<>(Rarity.class);
        double total = 0;

        for (Rarity r : Rarity.values()) {
            double x = r.getPosition();
            double weight = Math.exp(-(Math.pow(x - center, 2)) / width);
            weight += minimumWeight;
            weights.put(r, weight);
            total += weight;
        }

        Map<Rarity, Double> result = new EnumMap<>(Rarity.class);
        for (Rarity r : Rarity.values()) {
            result.put(r, weights.get(r) / total);
        }

        return result;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
package dev.sfcharacter.models;

public record LocationData(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}

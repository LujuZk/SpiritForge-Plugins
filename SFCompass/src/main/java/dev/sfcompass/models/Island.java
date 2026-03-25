package dev.sfcompass.models;

public record Island(
        String id,
        String displayName,
        String worldName,
        int centerX,
        int centerZ,
        int radius,
        int buffer,
        int requiredLevel
) {

    public int outerRadius() {
        return radius + buffer;
    }

    public double distanceTo(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Returns penetration factor for the danger zone.
     * @return -1 if outside danger zone, -2 if inside island (safe zone),
     *         0.0-1.0 penetration factor (1.0 = closest to island center)
     */
    public double penetration(double x, double z) {
        double dist = distanceTo(x, z);
        int outer = outerRadius();

        if (dist >= outer) {
            return -1;
        }
        if (dist <= radius) {
            return -2;
        }
        // In the buffer zone: 0.0 at outer edge, 1.0 at inner edge
        return 1.0 - (dist - radius) / buffer;
    }
}

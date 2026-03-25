package dev.skilltree.models;

/**
 * Todos los tipos de skill disponibles.
 * Los WEAPON_* se ganan usando ese tipo de arma en combate.
 */
public enum SkillType {
    // Habilidades de recolección
    MINING("mining", "⛏ Minería", false),
    FARMING("farming", "🌾 Agricultura", false),
    FISHING("fishing", "🎣 Pesca", false),

    // Habilidades de armas
    WEAPON_SWORD("sword", "⚔ Espada", true),
    WEAPON_BOW("bow", "🏹 Arco", true),
    WEAPON_AXE("axe", "🪓 Hacha", true),
    WEAPON_TRIDENT("trident", "🔱 Tridente", true);

    private final String key;
    private final String displayName;
    private final boolean isWeapon;

    SkillType(String key, String displayName, boolean isWeapon) {
        this.key = key;
        this.displayName = displayName;
        this.isWeapon = isWeapon;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public boolean isWeapon() { return isWeapon; }

    public static SkillType fromKey(String key) {
        for (SkillType type : values()) {
            if (type.key.equalsIgnoreCase(key)) return type;
        }
        return null;
    }
}
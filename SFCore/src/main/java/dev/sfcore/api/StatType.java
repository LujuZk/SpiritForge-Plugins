package dev.sfcore.api;

import org.bukkit.attribute.Attribute;

public enum StatType {
    // ATTRIBUTE-based
    DAMAGE_BONUS("damage_bonus",         Attribute.GENERIC_ATTACK_DAMAGE,  ApplyMode.ADD_SCALAR),
    DAMAGE_REDUCTION("damage_reduction", Attribute.GENERIC_ARMOR,           ApplyMode.ADD_NUMBER),
    MAX_HEALTH("max_health",             Attribute.GENERIC_MAX_HEALTH,      ApplyMode.ADD_NUMBER),
    MOVEMENT_SPEED("movement_speed",     Attribute.GENERIC_MOVEMENT_SPEED,  ApplyMode.ADD_SCALAR),

    // LISTENER-based (implementados)
    LIFESTEAL("lifesteal",       null, ApplyMode.LISTENER),
    MINING_SPEED("mining_speed", null, ApplyMode.LISTENER),

    // Futuros (registrados pero no aplicados aún)
    BLEED("bleed",                           null, ApplyMode.LISTENER),
    STUN("stun",                             null, ApplyMode.LISTENER),
    ARMOR_PIERCE("armor_pierce",             null, ApplyMode.LISTENER),
    EXECUTE("execute",                       null, ApplyMode.LISTENER),
    REGEN_ON_HIT("regen_on_hit",             null, ApplyMode.LISTENER),
    KILL_STREAK_DAMAGE("kill_streak_damage", null, ApplyMode.LISTENER),
    COUNTERATTACK("counterattack",           null, ApplyMode.LISTENER),
    FRENZY("frenzy",                         null, ApplyMode.LISTENER),
    BERSERKER_DAMAGE("berserker_damage",     null, ApplyMode.LISTENER),
    AREA_DAMAGE("area_damage",               null, ApplyMode.LISTENER),
    OPENER_DAMAGE("opener_damage",           null, ApplyMode.LISTENER),
    GLOBAL_MULTIPLIER("global_multiplier",   null, ApplyMode.LISTENER),
    PATH_AMPLIFIER("path_amplifier",         null, ApplyMode.LISTENER),
    VEIN_MINER("vein_miner",                 null, ApplyMode.LISTENER),
    DOUBLE_DROP("double_drop",               null, ApplyMode.LISTENER),
    FORTUNE_BONUS("fortune_bonus",           null, ApplyMode.LISTENER),
    AUTO_PICKUP("auto_pickup",               null, ApplyMode.LISTENER),
    EXPLOSION("explosion",                   null, ApplyMode.LISTENER),
    ORE_DETECTION("ore_detection",           null, ApplyMode.LISTENER),

    // ─── Custom RPG Stats ────────────────────────────────────────────
    STR("str", null, ApplyMode.LISTENER),
    VIT("vit", null, ApplyMode.LISTENER),
    INT("int", null, ApplyMode.LISTENER),
    AGI("agi", null, ApplyMode.LISTENER);

    public enum ApplyMode { ADD_NUMBER, ADD_SCALAR, LISTENER }

    private final String key;
    private final Attribute attribute;
    private final ApplyMode mode;

    StatType(String key, Attribute attribute, ApplyMode mode) {
        this.key = key;
        this.attribute = attribute;
        this.mode = mode;
    }

    public String getKey() { return key; }
    public Attribute getAttribute() { return attribute; }
    public ApplyMode getMode() { return mode; }

    public static StatType fromKey(String key) {
        if (key == null) return null;
        for (StatType t : values()) {
            if (t.key.equals(key)) return t;
        }
        return null;
    }
}

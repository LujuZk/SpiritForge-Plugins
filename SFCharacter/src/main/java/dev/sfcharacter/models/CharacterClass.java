package dev.sfcharacter.models;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum CharacterClass {

    MAGO("Mago", Material.BOOK, "Maestro de las artes arcanas. Usa hechizos devastadores.", NamedTextColor.LIGHT_PURPLE),
    GUERRERO("Guerrero", Material.DIAMOND_SWORD, "Combatiente cuerpo a cuerpo. Fuerte y resistente.", NamedTextColor.RED),
    PICARO("Picaro", Material.IRON_SWORD, "Agil y sigiloso. Ataca desde las sombras.", NamedTextColor.GREEN);

    private final String displayName;
    private final Material material;
    private final String description;
    private final NamedTextColor color;

    CharacterClass(String displayName, Material material, String description, NamedTextColor color) {
        this.displayName = displayName;
        this.material = material;
        this.description = description;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDescription() {
        return description;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public static CharacterClass fromName(String name) {
        if (name == null) return null;
        for (CharacterClass cc : values()) {
            if (cc.name().equalsIgnoreCase(name)) return cc;
        }
        return null;
    }
}

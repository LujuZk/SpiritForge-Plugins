package dev.skilltree.weapons;

import dev.skilltree.models.SkillType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Detecta qué tipo de skill de arma corresponde al item en mano.
 *
 * Orden de detección:
 *   1. Tag PDC  →  para armas custom de otros plugins
 *   2. Material →  para armas vanilla
 *
 * Namespace acordado con el plugin de armas: "spiritforge:weapon_skill"
 */
public class WeaponUtil {

    // Namespace acordado con el plugin de armas custom de tu colega
    // Ambos plugins deben usar exactamente este mismo string
    public static final NamespacedKey WEAPON_SKILL_KEY =
            new NamespacedKey("spiritforge", "weapon_skill");

    private static final Set<Material> SWORDS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    );

    private static final Set<Material> AXES = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    private static final Set<Material> BOWS = EnumSet.of(
            Material.BOW, Material.CROSSBOW
    );

    /**
     * Retorna el SkillType de arma correspondiente al item,
     * o null si no es un arma rastreada.
     */
    public static SkillType getWeaponSkill(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        // Items custom con PDC
        if (item.hasItemMeta()) {
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(WEAPON_SKILL_KEY, PersistentDataType.STRING)) {
                String tag = pdc.get(WEAPON_SKILL_KEY, PersistentDataType.STRING);
                SkillType fromTag = SkillType.fromKey(tag);
                if (fromTag != null) return fromTag;
            }
        }

        // Items vanillas en caso de no encontrar items con tag (para testeos)
        Material mat = item.getType();
        if (SWORDS.contains(mat))    return SkillType.WEAPON_SWORD;
        if (AXES.contains(mat))      return SkillType.WEAPON_AXE;
        if (BOWS.contains(mat))      return SkillType.WEAPON_BOW;
        if (mat == Material.TRIDENT) return SkillType.WEAPON_TRIDENT;

        return null;
    }

    public static boolean isTrackedWeapon(ItemStack item) {
        return getWeaponSkill(item) != null;
    }
}
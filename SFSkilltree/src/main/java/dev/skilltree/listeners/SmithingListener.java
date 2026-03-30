package dev.skilltree.listeners;

import dev.sfcrafting.SmeltCompleteEvent;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Escucha eventos de fundición de SFCrafting para otorgar XP de smithing.
 * XP = materialValue * rarityMultiplier
 */
public class SmithingListener implements Listener {

    private final SkillTreePlugin plugin;

    public SmithingListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmeltComplete(SmeltCompleteEvent event) {
        plugin.getLogger().info("[Smithing] SmeltCompleteEvent recibido: recipeId="
                + event.getRecipeId() + " rarity=" + event.getRarityLevel()
                + " player=" + event.getPlayer().getName());

        ConfigurationSection matValues = plugin.getConfig()
                .getConfigurationSection("skills.smithing.material-values");
        if (matValues == null) {
            plugin.getLogger().warning("[Smithing] No se encontró sección 'skills.smithing.material-values' en config.yml");
            return;
        }

        int materialValue = matValues.getInt(event.getRecipeId(), 0);
        if (materialValue <= 0) {
            plugin.getLogger().info("[Smithing] recipeId '" + event.getRecipeId()
                    + "' no tiene material-value o es 0 → sin XP");
            return;
        }

        double rarityMult = plugin.getConfig()
                .getDouble("skills.smithing.rarity-multipliers." + event.getRarityLevel(), 1.0);

        double xp = materialValue * rarityMult;

        // Debug
        if (plugin.getSkillManager().isDebug(event.getPlayer())) {
            event.getPlayer().sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "[DEBUG] Smithing: " + event.getRecipeId()
                                    + " (rarity " + event.getRarityLevel() + ")"
                                    + " → materialValue=" + materialValue
                                    + " * rarityMult=" + String.format("%.1f", rarityMult)
                                    + " = " + String.format("%.1f", xp) + " XP",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }

        plugin.getSkillManager().addXP(event.getPlayer(), SkillType.SMITHING, xp);
    }
}

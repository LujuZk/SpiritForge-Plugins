package dev.skilltree.listeners;

import dev.sfdrops.ResourceDropEvent;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Escucha ResourceDropEvent de SFDrops para otorgar XP de tala.
 * XP = blockValue × rarityMultiplier
 * Cada tronco y sus multiplicadores se configuran en config.yml bajo skills.woodcutting.
 */
public class WoodcuttingListener implements Listener {

    private final SkillTreePlugin plugin;

    public WoodcuttingListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResourceDrop(ResourceDropEvent event) {
        ConfigurationSection blockValues = plugin.getConfig()
                .getConfigurationSection("skills.woodcutting.block-values");
        if (blockValues == null) return;

        int blockValue = blockValues.getInt(event.getBlockId(), 0);
        if (blockValue <= 0) return;

        double rarityMult = plugin.getConfig()
                .getDouble("skills.woodcutting.rarity-multipliers." + event.getRarityLevel(), 1.0);

        double xp = blockValue * rarityMult;

        plugin.getLogger().info("[Woodcutting] ResourceDropEvent: blockId=" + event.getBlockId()
                + " rarity=" + event.getRarityLevel()
                + " → XP=" + String.format("%.1f", xp));

        // Debug
        if (plugin.getSkillManager().isDebug(event.getPlayer())) {
            event.getPlayer().sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "[DEBUG] Woodcutting: " + event.getBlockId()
                                    + " (rarity " + event.getRarityLevel() + ")"
                                    + " → blockValue=" + blockValue
                                    + " * rarityMult=" + String.format("%.1f", rarityMult)
                                    + " = " + String.format("%.1f", xp) + " XP",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }

        plugin.getSkillManager().addXP(event.getPlayer(), SkillType.WOODCUTTING, xp);
    }
}

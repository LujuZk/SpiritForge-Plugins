package dev.skilltree.listeners;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import java.util.EnumSet;
import java.util.Set;

public class GatheringListener implements Listener {

    private final SkillTreePlugin plugin;

    // Bloques que dan XP de minería
    private static final Set<Material> MINING_BLOCKS = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.OBSIDIAN, Material.NETHERRACK, Material.BASALT
    );

    public GatheringListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Minería ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!MINING_BLOCKS.contains(block.getType())) return;

        double xp = plugin.getConfig().getDouble("skills.mining.xp-per-block", 10.0);
        plugin.getSkillManager().addXP(player, SkillType.MINING, xp);
    }

    // ─── Agricultura ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        double xp = plugin.getConfig().getDouble("skills.farming.xp-per-harvest", 8.0);
        plugin.getSkillManager().addXP(player, SkillType.FARMING, xp);
    }

    // ─── Pesca ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        double xp = plugin.getConfig().getDouble("skills.fishing.xp-per-catch", 15.0);
        plugin.getSkillManager().addXP(player, SkillType.FISHING, xp);
    }
}
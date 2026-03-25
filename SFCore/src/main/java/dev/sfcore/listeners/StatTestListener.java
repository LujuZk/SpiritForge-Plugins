package dev.sfcore.listeners;

import dev.sfcore.api.StatType;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.UUID;

public class StatTestListener implements Listener {

    private final StatManager statManager;
    private final TestMonitorManager testMonitor;

    public StatTestListener(StatManager statManager, TestMonitorManager testMonitor) {
        this.statManager = statManager;
        this.testMonitor = testMonitor;
    }

    // ─── Daño hecho por el jugador ───────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // DAMAGE_BONUS
        if (testMonitor.isActive(uuid, StatType.DAMAGE_BONUS)) {
            double bonus = statManager.getTotal(player, StatType.DAMAGE_BONUS);
            double finalDmg = event.getFinalDamage();
            double baseDmg = bonus > 0 ? finalDmg / (1 + bonus) : finalDmg;
            player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                    .append(Component.text("DAMAGE_BONUS", NamedTextColor.GOLD))
                    .append(Component.text(String.format(" → Base: %.1f | Final: %.1f | Bonus: +%.0f%%",
                            baseDmg, finalDmg, bonus * 100), NamedTextColor.YELLOW)));
        }

        // LIFESTEAL
        if (testMonitor.isActive(uuid, StatType.LIFESTEAL)) {
            double lifesteal = statManager.getTotal(player, StatType.LIFESTEAL);
            if (lifesteal > 0) {
                double heal = event.getFinalDamage() * lifesteal;
                AttributeInstance maxHpInst = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHp = maxHpInst != null ? maxHpInst.getValue() : 20;
                double healCapped = Math.min(heal, maxHp - player.getHealth());
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text("LIFESTEAL", NamedTextColor.GOLD))
                        .append(Component.text(String.format(" → %.0f%% | Heal: %.1f | HP: %.1f/%.1f",
                                lifesteal * 100, healCapped, player.getHealth(), maxHp), NamedTextColor.YELLOW)));
            } else {
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text("LIFESTEAL", NamedTextColor.GOLD))
                        .append(Component.text(" → 0% (sin lifesteal activo)", NamedTextColor.GRAY)));
            }
        }

        // ARMOR_PIERCE
        if (testMonitor.isActive(uuid, StatType.ARMOR_PIERCE)) {
            double val = statManager.getTotal(player, StatType.ARMOR_PIERCE);
            player.sendMessage(testMsg("ARMOR_PIERCE", val));
        }

        // AREA_DAMAGE
        if (testMonitor.isActive(uuid, StatType.AREA_DAMAGE)) {
            double val = statManager.getTotal(player, StatType.AREA_DAMAGE);
            player.sendMessage(testMsg("AREA_DAMAGE", val));
        }

        // OPENER_DAMAGE
        if (testMonitor.isActive(uuid, StatType.OPENER_DAMAGE)) {
            double val = statManager.getTotal(player, StatType.OPENER_DAMAGE);
            player.sendMessage(testMsg("OPENER_DAMAGE", val));
        }

        // BLEED
        if (testMonitor.isActive(uuid, StatType.BLEED)) {
            double val = statManager.getTotal(player, StatType.BLEED);
            player.sendMessage(testMsg("BLEED", val));
        }

        // STUN
        if (testMonitor.isActive(uuid, StatType.STUN)) {
            double val = statManager.getTotal(player, StatType.STUN);
            player.sendMessage(testMsg("STUN", val));
        }

        // EXECUTE
        if (testMonitor.isActive(uuid, StatType.EXECUTE)) {
            double val = statManager.getTotal(player, StatType.EXECUTE);
            player.sendMessage(testMsg("EXECUTE", val));
        }

        // REGEN_ON_HIT
        if (testMonitor.isActive(uuid, StatType.REGEN_ON_HIT)) {
            double val = statManager.getTotal(player, StatType.REGEN_ON_HIT);
            player.sendMessage(testMsg("REGEN_ON_HIT", val));
        }

        // COUNTERATTACK
        if (testMonitor.isActive(uuid, StatType.COUNTERATTACK)) {
            double val = statManager.getTotal(player, StatType.COUNTERATTACK);
            player.sendMessage(testMsg("COUNTERATTACK", val));
        }

        // FRENZY
        if (testMonitor.isActive(uuid, StatType.FRENZY)) {
            double val = statManager.getTotal(player, StatType.FRENZY);
            player.sendMessage(testMsg("FRENZY", val));
        }

        // BERSERKER_DAMAGE
        if (testMonitor.isActive(uuid, StatType.BERSERKER_DAMAGE)) {
            double val = statManager.getTotal(player, StatType.BERSERKER_DAMAGE);
            player.sendMessage(testMsg("BERSERKER_DAMAGE", val));
        }

        // KILL_STREAK_DAMAGE
        if (testMonitor.isActive(uuid, StatType.KILL_STREAK_DAMAGE)) {
            double val = statManager.getTotal(player, StatType.KILL_STREAK_DAMAGE);
            player.sendMessage(testMsg("KILL_STREAK_DAMAGE", val));
        }
    }

    // ─── Daño recibido por el jugador ────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // DAMAGE_REDUCTION
        if (testMonitor.isActive(uuid, StatType.DAMAGE_REDUCTION)) {
            double armor = statManager.getTotal(player, StatType.DAMAGE_REDUCTION);
            double rawDmg = event.getDamage();
            double finalDmg = event.getFinalDamage();
            player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                    .append(Component.text("DAMAGE_REDUCTION", NamedTextColor.GOLD))
                    .append(Component.text(String.format(" → Raw: %.1f | Final: %.1f | Armor bonus: +%.1f",
                            rawDmg, finalDmg, armor), NamedTextColor.YELLOW)));
        }

        // MAX_HEALTH
        if (testMonitor.isActive(uuid, StatType.MAX_HEALTH)) {
            double bonus = statManager.getTotal(player, StatType.MAX_HEALTH);
            AttributeInstance maxHpInst = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHp = maxHpInst != null ? maxHpInst.getValue() : 20;
            player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                    .append(Component.text("MAX_HEALTH", NamedTextColor.GOLD))
                    .append(Component.text(String.format(" → Max HP: %.1f | Bonus: +%.1f | Current: %.1f",
                            maxHp, bonus, player.getHealth()), NamedTextColor.YELLOW)));
        }
    }

    // ─── Minería ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // MINING_SPEED
        if (testMonitor.isActive(uuid, StatType.MINING_SPEED)) {
            double miningSpeed = statManager.getTotal(player, StatType.MINING_SPEED);
            float hardness = event.getBlock().getType().getHardness();
            String blockName = event.getBlock().getType().name();

            if (hardness <= 0) {
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text("MINING_SPEED", NamedTextColor.GOLD))
                        .append(Component.text(" → " + blockName + " | Instantáneo", NamedTextColor.YELLOW)));
            } else {
                double ticksBase = Math.ceil(hardness * 30);
                double secsBase = ticksBase / 20.0;

                if (miningSpeed <= 0) {
                    player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                            .append(Component.text("MINING_SPEED", NamedTextColor.GOLD))
                            .append(Component.text(String.format(" → %s (dur: %.1f) | Sin bonus: ~%.2fs",
                                    blockName, hardness, secsBase), NamedTextColor.YELLOW)));
                } else {
                    int hasteLevel = (int) Math.floor(miningSpeed);
                    double hasteMultiplier = 1.0 + 0.2 * hasteLevel;
                    double ticksWith = Math.ceil(ticksBase / hasteMultiplier);
                    double secsWith = ticksWith / 20.0;
                    double pctFaster = (1.0 - secsWith / secsBase) * 100.0;
                    String hasteName = "Haste " + toRoman(hasteLevel);

                    player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                            .append(Component.text("MINING_SPEED", NamedTextColor.GOLD))
                            .append(Component.text(String.format(" → %s (dur: %.1f) | Sin bonus: ~%.2fs | Con %s: ~%.2fs | -%.1f%%",
                                    blockName, hardness, secsBase, hasteName, secsWith, pctFaster), NamedTextColor.GREEN)));
                }
            }
        }

        // DOUBLE_DROP
        if (testMonitor.isActive(uuid, StatType.DOUBLE_DROP)) {
            double val = statManager.getTotal(player, StatType.DOUBLE_DROP);
            player.sendMessage(testMsg("DOUBLE_DROP", val));
        }

        // FORTUNE_BONUS
        if (testMonitor.isActive(uuid, StatType.FORTUNE_BONUS)) {
            double val = statManager.getTotal(player, StatType.FORTUNE_BONUS);
            player.sendMessage(testMsg("FORTUNE_BONUS", val));
        }

        // VEIN_MINER
        if (testMonitor.isActive(uuid, StatType.VEIN_MINER)) {
            double val = statManager.getTotal(player, StatType.VEIN_MINER);
            player.sendMessage(testMsg("VEIN_MINER", val));
        }

        // AUTO_PICKUP
        if (testMonitor.isActive(uuid, StatType.AUTO_PICKUP)) {
            double val = statManager.getTotal(player, StatType.AUTO_PICKUP);
            player.sendMessage(testMsg("AUTO_PICKUP", val));
        }

        // ORE_DETECTION
        if (testMonitor.isActive(uuid, StatType.ORE_DETECTION)) {
            double val = statManager.getTotal(player, StatType.ORE_DETECTION);
            player.sendMessage(testMsg("ORE_DETECTION", val));
        }

        // EXPLOSION
        if (testMonitor.isActive(uuid, StatType.EXPLOSION)) {
            double val = statManager.getTotal(player, StatType.EXPLOSION);
            player.sendMessage(testMsg("EXPLOSION", val));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private Component testMsg(String statName, double value) {
        return Component.text("[TEST] ", NamedTextColor.GRAY)
                .append(Component.text(statName, NamedTextColor.GOLD))
                .append(Component.text(String.format(" → Value: %.2f", value), NamedTextColor.YELLOW));
    }

    private String toRoman(int n) {
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens      = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones      = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        if (n <= 0 || n >= 4000) return String.valueOf(n);
        return thousands[n / 1000] + hundreds[(n % 1000) / 100] + tens[(n % 100) / 10] + ones[n % 10];
    }
}

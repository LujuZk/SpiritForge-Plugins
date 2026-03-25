package dev.sfcore.listeners;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatStatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        double lifesteal = SFCoreAPI.get().getTotal(player, StatType.LIFESTEAL);
        if (lifesteal <= 0) return;

        double heal = event.getFinalDamage() * lifesteal;
        var maxHealthInst = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthInst == null) return;

        double maxHp = maxHealthInst.getValue();
        double healCapped = Math.min(heal, maxHp - player.getHealth());
        if (healCapped > 0) player.setHealth(player.getHealth() + healCapped);
    }
}

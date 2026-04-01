package dev.sfcombat;

import org.bukkit.plugin.java.JavaPlugin;

public final class SFCombatPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new BowStatsListener(this), this);
        getLogger().info("SFCombat habilitado.");
    }
}

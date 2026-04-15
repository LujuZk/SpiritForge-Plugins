package dev.sfcore;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.commands.SFCoreCommand;
import dev.sfcore.database.StatDatabase;
import dev.sfcore.listeners.CombatStatListener;
import dev.sfcore.listeners.MagicStaffListener;
import dev.sfcore.listeners.PlayerConnectionListener;
import dev.sfcore.listeners.StatTestListener;
import dev.sfcore.managers.ManaManager;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import dev.sfcore.placeholders.SFCorePlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class SFCorePlugin extends JavaPlugin {

    private StatDatabase db;
    private StatManager statManager;
    private ManaManager manaManager;
    private MagicStaffListener magicStaffListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        db = new StatDatabase(getDataFolder());
        statManager = new StatManager(db);
        manaManager = new ManaManager(this, db, statManager);
        SFCoreAPI.init(statManager, manaManager);

        var testMonitor = new TestMonitorManager();
        magicStaffListener = new MagicStaffListener(this, statManager, getConfig().getConfigurationSection("magic-staves"));

        var coreCommand = new SFCoreCommand(statManager, testMonitor, manaManager);
        getCommand("sfcore").setExecutor(coreCommand);
        getCommand("sfcore").setTabCompleter(coreCommand);

        // Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(statManager, testMonitor, manaManager), this);
        pm.registerEvents(new CombatStatListener(this, getConfig()), this);
        pm.registerEvents(new StatTestListener(statManager, testMonitor), this);
        pm.registerEvents(magicStaffListener, this);

        if (manaManager != null && manaManager.isEnabled()) {
            getServer().getScheduler().runTaskTimer(this, manaManager::tickRegen, 20L, 20L);
        }
        if (magicStaffListener != null) {
            long period = magicStaffListener.getRefreshIntervalTicks();
            getServer().getScheduler().runTaskTimer(this, magicStaffListener::syncOnlinePlayers, period, period);
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SFCorePlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().info("PlaceholderAPI not found, skipping SFCore placeholders.");
        }

        getLogger().info("SFCore enabled - stats and mana API ready.");
    }

    @Override
    public void onDisable() {
        if (statManager != null) {
            statManager.saveAll();
        }
        if (manaManager != null) {
            for (var player : getServer().getOnlinePlayers()) {
                manaManager.saveAndUnload(player.getUniqueId());
            }
        }
        SFCoreAPI.shutdown();
        if (db != null) {
            db.close();
        }
        getLogger().info("SFCore disabled.");
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public ManaManager getManaManager() {
        return manaManager;
    }
}

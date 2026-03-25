package dev.sfcore;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.commands.SFCoreCommand;
import dev.sfcore.database.StatDatabase;
import dev.sfcore.listeners.CombatStatListener;
import dev.sfcore.listeners.PlayerConnectionListener;
import dev.sfcore.listeners.StatTestListener;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SFCorePlugin extends JavaPlugin {

    private StatDatabase db;
    private StatManager statManager;

    @Override
    public void onEnable() {
        db = new StatDatabase(getDataFolder());
        statManager = new StatManager(db);
        SFCoreAPI.init(statManager);

        var testMonitor = new TestMonitorManager();

        // Comandos
        var coreCommand = new SFCoreCommand(statManager, testMonitor);
        getCommand("sfcore").setExecutor(coreCommand);
        getCommand("sfcore").setTabCompleter(coreCommand);

        // Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(statManager, testMonitor), this);
        pm.registerEvents(new CombatStatListener(), this);
        pm.registerEvents(new StatTestListener(statManager, testMonitor), this);

        getLogger().info("SFCore enabled — stats API ready.");
    }

    @Override
    public void onDisable() {
        if (statManager != null) statManager.saveAll();
        SFCoreAPI.shutdown();
        if (db != null) db.close();
        getLogger().info("SFCore disabled.");
    }
}

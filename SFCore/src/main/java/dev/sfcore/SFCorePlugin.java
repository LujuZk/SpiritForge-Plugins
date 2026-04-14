package dev.sfcore;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.commands.SFCoreCommand;
import dev.sfcore.database.SFDatabase;
import dev.sfcore.database.SFDatabaseFactory;
import dev.sfcore.database.StatDatabase;
import dev.sfcore.listeners.CombatStatListener;
import dev.sfcore.listeners.PlayerConnectionListener;
import dev.sfcore.listeners.StatTestListener;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class SFCorePlugin extends JavaPlugin {

    private SFDatabaseFactory databaseFactory;
    private StatManager statManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigurationSection dbConfig = getConfig().getConfigurationSection("database");
        if (dbConfig == null) {
            getLogger().severe("Falta sección 'database' en config.yml — SFCore no se puede iniciar.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            databaseFactory = new SFDatabaseFactory(dbConfig, getDataFolder());
        } catch (RuntimeException e) {
            getLogger().severe("No se pudo inicializar la base de datos: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SFDatabase coreDb = databaseFactory.get("sfcore");
        StatDatabase db = new StatDatabase(coreDb);

        statManager = new StatManager(db);
        SFCoreAPI.init(statManager, databaseFactory);

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
        if (databaseFactory != null) databaseFactory.closeAll();
        getLogger().info("SFCore disabled.");
    }
}

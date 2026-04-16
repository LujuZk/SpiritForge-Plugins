package dev.sfcore;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.commands.SFCoreCommand;
import dev.sfcore.database.AsyncDatabaseExecutor;
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
    private AsyncDatabaseExecutor asyncExecutor;
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

        int asyncThreads = dbConfig.getInt("async-threads", 2);
        asyncExecutor = new AsyncDatabaseExecutor(this, asyncThreads);

        SFDatabase coreDb = databaseFactory.get("sfcore");
        StatDatabase db = new StatDatabase(coreDb);

        statManager = new StatManager(db);
        SFCoreAPI.init(statManager, databaseFactory, asyncExecutor);

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

        var sfCharacter = pm.getPlugin("SFCharacter");
        if (sfCharacter != null && sfCharacter.isEnabled()) {
            registerCharacterSelectListener();
        } else if (sfCharacter != null) {
            pm.registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent ev) {
                    if (ev.getPlugin().getName().equals("SFCharacter")) {
                        dev.sfcore.util.CharacterSlotResolver.invalidate();
                        registerCharacterSelectListener();
                    }
                }
            }, this);
            getLogger().info("SFCharacter presente — listener per-character se registrará tras su enable.");
        } else {
            getLogger().info("SFCharacter no detectado — stats operarán en single-slot mode (slot 0).");
        }

        getLogger().info("SFCore enabled — stats API ready.");
    }

    @SuppressWarnings("unchecked")
    private void registerCharacterSelectListener() {
        try {
            Class<? extends org.bukkit.event.Event> eventClass =
                    (Class<? extends org.bukkit.event.Event>)
                    Class.forName("dev.sfcharacter.api.CharacterSelectEvent");
            java.lang.reflect.Method getPlayer = eventClass.getMethod("getPlayer");
            getServer().getPluginManager().registerEvent(
                    eventClass,
                    new org.bukkit.event.Listener() {},
                    org.bukkit.event.EventPriority.MONITOR,
                    (listener, event) -> {
                        try {
                            org.bukkit.entity.Player player =
                                    (org.bukkit.entity.Player) getPlayer.invoke(event);
                            statManager.reloadForActiveSlot(player);
                        } catch (Exception e) {
                            getLogger().warning("Error en CharacterSelect reload: " + e.getMessage());
                        }
                    },
                    this
            );
            getLogger().info("SFCharacter detectado — per-character scoping de stats activado.");
        } catch (Exception e) {
            getLogger().warning("No se pudo registrar listener de CharacterSelectEvent: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (statManager != null) statManager.saveAll();
        SFCoreAPI.shutdown();
        if (asyncExecutor != null) asyncExecutor.shutdown();
        if (databaseFactory != null) databaseFactory.closeAll();
        getLogger().info("SFCore disabled.");
    }
}

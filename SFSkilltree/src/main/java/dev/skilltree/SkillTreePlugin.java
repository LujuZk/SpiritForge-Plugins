package dev.skilltree;

import dev.skilltree.commands.SkillCommand;
import dev.skilltree.commands.SkillAdminCommand;
import dev.skilltree.database.DatabaseManager;
import dev.skilltree.listeners.CombatListener;
import dev.skilltree.listeners.GatheringListener;
import dev.skilltree.listeners.GUIListener;
import dev.skilltree.managers.SkillManager;
import dev.skilltree.managers.SkillPointManager;
import dev.skilltree.managers.TreeManager;
import dev.skilltree.managers.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SkillTreePlugin extends JavaPlugin {

    private static SkillTreePlugin instance;
    private DatabaseManager databaseManager;
    private SkillManager skillManager;
    private TreeManager treeManager;
    private SkillPointManager skillPointManager;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Copiar archivos de recursos por defecto si no existen
        saveResource("icons.yml", false);
        saveSkillTreeResources();

        // Inicializar base de datos
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Inicializar managers
        skillManager      = new SkillManager(this);
        treeManager       = new TreeManager(this);
        skillPointManager = new SkillPointManager(this);
        inventoryManager  = new InventoryManager(this);

        // Cargar árboles desde trees/*.yml e icons.yml
        treeManager.loadTrees();

        // Registrar comandos
        getCommand("skills").setExecutor(new SkillCommand(this));
        getCommand("skillsadmin").setExecutor(new SkillAdminCommand(this));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GatheringListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("SkillTreePlugin habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        if (inventoryManager != null) inventoryManager.restoreAll();
        if (skillManager != null)     skillManager.saveAll();
        if (databaseManager != null)  databaseManager.close();
        getLogger().info("SkillTreePlugin deshabilitado.");
    }

    public static SkillTreePlugin getInstance()       { return instance; }
    public DatabaseManager getDatabaseManager()       { return databaseManager; }
    public SkillManager getSkillManager()             { return skillManager; }
    public TreeManager getTreeManager()               { return treeManager; }
    public SkillPointManager getSkillPointManager()   { return skillPointManager; }
    public InventoryManager getInventoryManager()     { return inventoryManager; }

    private void saveIfPresent(String path) {
        if (getResource(path) != null) {
            saveResource(path, false);
        }
    }

    private void saveSkillTreeResources() {
        try (JarFile jar = new JarFile(getFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("skills/") && name.endsWith(".yml")) {
                    saveIfPresent(name);
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error al extraer árboles de habilidades por defecto: " + e.getMessage());
        }
    }
}
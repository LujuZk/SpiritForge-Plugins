package dev.skilltree;

import dev.sfcore.api.SFCoreAPI;
import dev.skilltree.commands.SkillCommand;
import dev.skilltree.commands.SkillAdminCommand;
import dev.skilltree.database.DatabaseManager;
import dev.skilltree.listeners.CombatListener;
import dev.skilltree.listeners.GatheringListener;
import dev.skilltree.listeners.GUIListener;
import dev.skilltree.listeners.MiningListener;
import dev.skilltree.listeners.SmithingListener;
import dev.skilltree.listeners.WoodcuttingListener;
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

        // Inicializar base de datos (via SFCore)
        var sfCorePlugin = getServer().getPluginManager().getPlugin("SFCore");
        if (sfCorePlugin == null || !sfCorePlugin.isEnabled()) {
            getLogger().severe("SFCore no está habilitado — SFSkilltree no puede iniciarse.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        databaseManager = new DatabaseManager(this, SFCoreAPI.get().getDatabase("skilltree"));

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

        // Listener de GUI (siempre activo)
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Per-character scoping condicional a SFCharacter
        var sfCharacter = getServer().getPluginManager().getPlugin("SFCharacter");
        if (sfCharacter != null && sfCharacter.isEnabled()) {
            getServer().getPluginManager().registerEvents(
                    new dev.skilltree.listeners.CharacterSelectSkillListener(this), this);
            getLogger().info("SFCharacter detectado — per-character scoping de skills activado.");
        } else {
            getLogger().info("SFCharacter no detectado — skills en single-slot mode (slot 0).");
        }

        // Listeners de combate y recolección vanilla — deshabilitados temporalmente
        // getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        // getServer().getPluginManager().registerEvents(new GatheringListener(this), this);

        // Smithing XP — condicional a SFCrafting
        if (getServer().getPluginManager().getPlugin("SFCrafting") != null) {
            getServer().getPluginManager().registerEvents(new SmithingListener(this), this);
            getLogger().info("SFCrafting detectado — XP de Herrería habilitada.");
        }

        // Mining + Woodcutting XP — condicional a SFDrops
        if (getServer().getPluginManager().getPlugin("SFDrops") != null) {
            getServer().getPluginManager().registerEvents(new MiningListener(this), this);
            getServer().getPluginManager().registerEvents(new WoodcuttingListener(this), this);
            getLogger().info("SFDrops detectado — XP de Minería y Tala habilitadas.");
        }

        getLogger().info("SkillTreePlugin habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        if (inventoryManager != null) inventoryManager.restoreAll();
        if (skillManager != null)     skillManager.saveAll();
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

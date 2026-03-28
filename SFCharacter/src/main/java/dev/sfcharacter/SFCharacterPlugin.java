package dev.sfcharacter;

import dev.sfcharacter.api.SFCharacterAPI;
import dev.sfcharacter.commands.CharacterCommand;
import dev.sfcharacter.database.CharacterDatabase;
import dev.sfcharacter.gui.CharacterSelectGUI;
import dev.sfcharacter.gui.ClassSelectGUI;
import dev.sfcharacter.listeners.GUIListener;
import dev.sfcharacter.listeners.PlayerConnectionListener;
import dev.sfcharacter.listeners.SelectionProtectionListener;
import dev.sfcharacter.managers.CharacterManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SFCharacterPlugin extends JavaPlugin {

    private CharacterDatabase database;
    private CharacterManager characterManager;
    private CharacterSelectGUI selectGUI;
    private ClassSelectGUI classSelectGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Database
        String dbFile = getConfig().getString("database.file", "characters.db");
        database = new CharacterDatabase(getDataFolder(), dbFile);

        // Managers
        characterManager = new CharacterManager(this, database);

        // API
        SFCharacterAPI.init(characterManager);

        // GUIs
        selectGUI = new CharacterSelectGUI(this);
        classSelectGUI = new ClassSelectGUI(this);

        // Commands
        CharacterCommand cmd = new CharacterCommand(this);
        getCommand("character").setExecutor(cmd);
        getCommand("character").setTabCompleter(cmd);

        // Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new GUIListener(this), this);
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new SelectionProtectionListener(this), this);

        getLogger().info("SFCharacter enabled");
    }

    @Override
    public void onDisable() {
        // Save all online players' character states
        for (Player player : getServer().getOnlinePlayers()) {
            if (characterManager.hasActiveCharacter(player.getUniqueId())
                    && !characterManager.isInCharacterSelection(player.getUniqueId())) {
                characterManager.saveCharacterState(player);
            }
        }

        SFCharacterAPI.shutdown();
        if (database != null) database.close();
        getLogger().info("SFCharacter disabled");
    }

    public CharacterManager getCharacterManager() {
        return characterManager;
    }

    public CharacterSelectGUI getSelectGUI() {
        return selectGUI;
    }

    public ClassSelectGUI getClassSelectGUI() {
        return classSelectGUI;
    }
}

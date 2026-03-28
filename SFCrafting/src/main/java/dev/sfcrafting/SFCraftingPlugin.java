package dev.sfcrafting;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.plugin.java.JavaPlugin;

public final class SFCraftingPlugin extends JavaPlugin {

    private ForgeManager forgeManager;
    private AuraManager auraManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        sanitizeConfigFile();
        reloadConfig();
        forgeManager = new ForgeManager(this);
        auraManager = new AuraManager(this);
        auraManager.start();

        ForgeCommand command = new ForgeCommand(this, forgeManager, auraManager);
        var pluginCommand = getCommand("sfcrafting");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new ForgeListener(forgeManager), this);
        getServer().getPluginManager().registerEvents(new MoldRecipeListener(this, forgeManager), this);
        getServer().getPluginManager().registerEvents(new AuraListener(auraManager), this);
        startCoolingTask();
        startWaterQuenchTask();
        if (getConfig().getBoolean("forge.enable-pack-fixer", false)) {
            new OraxenPackFixer(this, 75).schedule();
        }
        getLogger().info("SFCrafting habilitado.");
    }

    @Override
    public void onDisable() {
        if (auraManager != null) {
            auraManager.shutdown();
        }
        if (forgeManager != null) {
            forgeManager.shutdown();
        }
    }

    private void sanitizeConfigFile() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            return;
        }
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String cleaned = raw.replace("`r`n", "\r\n")
                .replace("`n", "\n")
                .replace("`r", "\r");
            if (!raw.equals(cleaned)) {
                Files.writeString(file.toPath(), cleaned, StandardCharsets.UTF_8);
                getLogger().warning("Config.yml contenia literales `r`n y fue sanitizado.");
            }
        } catch (IOException e) {
            getLogger().warning("No se pudo sanitizar config.yml: " + e.getMessage());
        }
    }

    private void startCoolingTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (forgeManager == null) {
                return;
            }
            for (var player : getServer().getOnlinePlayers()) {
                forgeManager.hotItemManager().maybeCoolPlayer(player.getInventory());
            }
            for (var inventory : forgeManager.getForgeInventories()) {
                forgeManager.hotItemManager().maybeCoolInventory(inventory);
            }
        }, 40L, 40L);
    }

    private void startWaterQuenchTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (forgeManager == null) {
                return;
            }
            for (var world : getServer().getWorlds()) {
                for (var item : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
                    forgeManager.hotItemManager().coolIfInWater(item);
                }
            }
        }, 20L, 20L);
    }
}



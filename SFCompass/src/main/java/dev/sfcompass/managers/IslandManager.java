package dev.sfcompass.managers;

import dev.sfcompass.models.Island;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class IslandManager {

    private static final Logger log = Logger.getLogger("SFCompass");
    private final Map<String, Island> islands = new LinkedHashMap<>();

    public IslandManager(FileConfiguration config) {
        loadIslands(config);
    }

    private void loadIslands(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("islands");
        if (section == null) {
            log.warning("[SFCompass] No islands section found in config.yml");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection islandSec = section.getConfigurationSection(id);
            if (islandSec == null) continue;

            Island island = new Island(
                    id,
                    islandSec.getString("display-name", id),
                    islandSec.getString("world", "world"),
                    islandSec.getInt("center.x", 0),
                    islandSec.getInt("center.z", 0),
                    islandSec.getInt("radius", 100),
                    islandSec.getInt("buffer", 50),
                    islandSec.getInt("required-level", 1)
            );
            islands.put(id, island);
            log.info("[SFCompass] Loaded island: " + id + " (" + island.displayName() + ")");
        }
    }

    public Island getIsland(String id) {
        return islands.get(id);
    }

    public Collection<Island> getAllIslands() {
        return islands.values();
    }

    public Collection<String> getIslandIds() {
        return islands.keySet();
    }
}

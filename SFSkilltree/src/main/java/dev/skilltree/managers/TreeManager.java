package dev.skilltree.managers;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.IconDefinition;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga y almacena los grafos de habilidades desde skills/<skill>/tree.yml y el
 * registro de iconos desde icons.yml.
 */

public class TreeManager {

    private final SkillTreePlugin plugin;
    private final Map<SkillType, SkillGraph> graphs = new EnumMap<>(SkillType.class);
    private final Map<String, IconDefinition> icons = new HashMap<>();

    public TreeManager(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carga todos los grafos desde skills/<skill>/tree.yml y el registro de iconos
     * desde icons.yml.
     */

    public void loadTrees() {
        graphs.clear();
        icons.clear();

        // Cargar iconos primero
        loadIcons();

        // Cargar grafos modulares: plugins/SFSkilltree/skills/*/tree.yml
        File skillsDir = new File(plugin.getDataFolder(), "skills");
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
            plugin.getLogger().info("Creada carpeta skills/, agrega carpetas modulares para cada árbol");
            return;
        }

        File[] skillDirs = skillsDir.listFiles(File::isDirectory);
        if (skillDirs == null || skillDirs.length == 0) {
            plugin.getLogger().warning("No se encontraron carpetas modulares en skills/");
            return;
        }

        for (File dir : skillDirs) {
            File treeFile = new File(dir, "tree.yml");
            if (treeFile.exists()) {
                loadTreeFile(treeFile);
            } else {
                plugin.getLogger().warning("Carpeta modular '" + dir.getName() + "' no contiene un tree.yml");
            }
        }
    }

    /**
     * Carga el registro de iconos desde icons.yml.
     */
    private void loadIcons() {
        File iconsFile = new File(plugin.getDataFolder(), "icons.yml");
        if (!iconsFile.exists()) {
            plugin.saveResource("icons.yml", false);
            plugin.getLogger().info("Creado icons.yml por defecto");
        }

        FileConfiguration iconsConfig = YamlConfiguration.loadConfiguration(iconsFile);
        ConfigurationSection iconsSection = iconsConfig.getConfigurationSection("icons");
        if (iconsSection == null) {
            plugin.getLogger().warning("No se encontró sección 'icons' en icons.yml");
            return;
        }

        for (String iconId : iconsSection.getKeys(false)) {
            ConfigurationSection iconSec = iconsSection.getConfigurationSection(iconId);
            if (iconSec == null)
                continue;

            String oraxenId = iconSec.getString("oraxen-id");
            if (oraxenId != null) {
                icons.put(iconId, new IconDefinition(oraxenId));
            }
        }

        plugin.getLogger().info("Iconos cargados: " + icons.size());
    }

    /**
     * Carga un archivo de árbol individual (tree.yml).
     */
    private void loadTreeFile(File treeFile) {
        FileConfiguration treeConfig = YamlConfiguration.loadConfiguration(treeFile);

        String id = treeConfig.getString("id");
        String displayName = treeConfig.getString("display-name", id);
        String skillTypeStr = treeConfig.getString("skill-type");

        if (id == null || skillTypeStr == null) {
            plugin.getLogger().warning("Archivo de árbol inválido (falta 'id' o 'skill-type'): " + treeFile.getName());
            return;
        }

        SkillType skillType = SkillType.fromKey(skillTypeStr);
        if (skillType == null) {
            plugin.getLogger().warning("SkillType desconocido en " + treeFile.getName() + ": " + skillTypeStr);
            return;
        }

        SkillGraph graph = new SkillGraph(id, displayName, skillType);
        graph.loadFromConfig(treeConfig);

        graphs.put(skillType, graph);
        plugin.getLogger().info("Árbol cargado: " + id + " (" + graph.getNodes().size() + " nodos, "
                + graph.getAllEdges().size() + " edges)");
    }

    /**
     * Retorna el grafo de habilidades para un skill dado.
     */
    public SkillGraph getTree(SkillType skill) {
        return graphs.get(skill);
    }

    /**
     * Retorna true si existe un grafo para el skill dado.
     */
    public boolean hasTree(SkillType skill) {
        return graphs.containsKey(skill);
    }

    /**
     * Retorna la definición de un icono por su ID.
     */
    public IconDefinition getIcon(String iconId) {
        return icons.get(iconId);
    }

    /**
     * Retorna el icono por defecto si el iconId no existe.
     */
    public IconDefinition getIconOrDefault(String iconId) {
        IconDefinition icon = icons.get(iconId);
        if (icon != null)
            return icon;
        // Fallback a icono por defecto
        return icons.getOrDefault("node_locked", new IconDefinition("st_node_locked"));
    }
}
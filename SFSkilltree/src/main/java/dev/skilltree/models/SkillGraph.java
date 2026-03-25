package dev.skilltree.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Grafo dirigido acíclico (DAG) que representa el árbol de habilidades de un skill.
 * Reemplaza SkillTree con una estructura más flexible que soporta múltiples padres/hijos.
 */
public class SkillGraph {

    private final String id;
    private final String displayName;
    private final SkillType skillType;
    
    // id -> nodo
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    
    // from -> lista de to (edges del grafo)
    private final Map<String, List<String>> edges = new HashMap<>();
    
    // to -> lista de from (edges inversos, para encontrar padres)
    private final Map<String, List<String>> reverseEdges = new HashMap<>();

    // Grid cells exported from the editor (exact positions)
    private final List<GridCell> gridCells = new ArrayList<>();

    // Path mappings (which connector cells belong to which edge)
    private final List<PathMapping> pathMappings = new ArrayList<>();

    public SkillGraph(String id, String displayName, SkillType skillType) {
        this.id = id;
        this.displayName = displayName;
        this.skillType = skillType;
    }

    /**
     * Carga el grafo desde una sección de configuración YAML.
     */
    public void loadFromConfig(ConfigurationSection section) {
        if (section == null)
            return;

        // Cargar nodos
        ConfigurationSection nodesSection = section.getConfigurationSection("nodes");
        if (nodesSection != null) {
            for (String nodeId : nodesSection.getKeys(false)) {
                ConfigurationSection nodeSec = nodesSection.getConfigurationSection(nodeId);
                if (nodeSec == null)
                    continue;

                String name = nodeSec.getString("name", nodeId);
                String description = nodeSec.getString("description", "");
                int cost = nodeSec.getInt("cost", 1);
                int gridX = nodeSec.getInt("grid-x", 0);
                int gridY = nodeSec.getInt("grid-y", 0);
                String iconId = nodeSec.getString("icon", "node_locked");
                boolean reqAll = nodeSec.getBoolean("requires-all", false);
                String effectType = nodeSec.getString("effect-type", "none");
                double effectValue = nodeSec.getDouble("effect-value", 0.0);

                List<String> requires = nodeSec.getStringList("requires");
                List<String> exclusiveWith = nodeSec.getStringList("exclusive-with");

                SkillNode node = new SkillNode(nodeId, name, description, cost,
                        gridX, gridY, iconId, requires, reqAll, exclusiveWith, effectType, effectValue);

                nodes.put(nodeId, node);
            }
        }

        // Cargar edges
        List<Map<?, ?>> edgesList = section.getMapList("edges");
        for (Map<?, ?> edgeMap : edgesList) {
            Object fromObj = edgeMap.get("from");
            Object toObj = edgeMap.get("to");
            if (fromObj == null || toObj == null)
                continue;

            String from = fromObj.toString();
            String to = toObj.toString();

            // Validar que ambos nodos existan
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
                continue;
            }

            edges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            reverseEdges.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
        }

        // Cargar grid (posiciones de celdas del editor)
        List<Map<?, ?>> gridList = section.getMapList("grid");
        for (Map<?, ?> pageEntry : gridList) {
            Object pageObj = pageEntry.get("page");
            int page = pageObj instanceof Number ? ((Number) pageObj).intValue() : 0;

            Object cellsObj = pageEntry.get("cells");
            if (cellsObj instanceof List<?> cellsList) {
                for (Object cellObj : cellsList) {
                    if (!(cellObj instanceof Map<?, ?> cellMap)) continue;

                    int col = cellMap.get("col") instanceof Number n ? n.intValue() : 0;
                    int row = cellMap.get("row") instanceof Number n ? n.intValue() : 0;
                    Object typeObj = cellMap.get("type");
                    String type = typeObj != null ? String.valueOf(typeObj) : "";
                    String cellId;
                    if ("node".equals(type)) {
                        Object nid = cellMap.get("node-id");
                        cellId = nid != null ? String.valueOf(nid) : "";
                    } else {
                        Object cid = cellMap.get("connector-id");
                        cellId = cid != null ? String.valueOf(cid) : "";
                    }
                    gridCells.add(new GridCell(page, col, row, type, cellId));
                }
            }
        }

        // Cargar paths (mapeo conector-a-edge)
        List<Map<?, ?>> pathsList = section.getMapList("paths");
        for (Map<?, ?> pathMap : pathsList) {
            Object fromObj2 = pathMap.get("from");
            Object toObj2 = pathMap.get("to");
            String from = fromObj2 != null ? String.valueOf(fromObj2) : "";
            String to = toObj2 != null ? String.valueOf(toObj2) : "";
            Object cellsObj = pathMap.get("cells");
            List<String> cells = new ArrayList<>();
            if (cellsObj instanceof List<?> cellsList) {
                for (Object c : cellsList) {
                    cells.add(String.valueOf(c));
                }
            }
            pathMappings.add(new PathMapping(from, to, cells));
        }
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public Map<String, SkillNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public SkillNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Retorna los nodos hijos directos de un nodo dado.
     */
    public List<String> getChildren(String nodeId) {
        return edges.getOrDefault(nodeId, List.of());
    }

    /**
     * Retorna los nodos padres directos de un nodo dado.
     */
    public List<String> getParents(String nodeId) {
        return reverseEdges.getOrDefault(nodeId, List.of());
    }

    /**
     * Retorna todos los edges del grafo como una lista de pares (from, to).
     */
    public List<Edge> getAllEdges() {
        List<Edge> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                result.add(new Edge(from, to));
            }
        }
        return result;
    }

    /**
     * Retorna el estado de un nodo para un jugador dado.
     */
    public NodeState getNodeState(String nodeId, Set<String> unlockedNodes) {
        SkillNode node = nodes.get(nodeId);
        if (node == null)
            return NodeState.LOCKED;

        if (unlockedNodes.contains(nodeId))
            return NodeState.UNLOCKED;
        
        if (node.isBlockedByExclusive(unlockedNodes))
            return NodeState.EXCLUSIVE_BLOCKED;
        
        if (node.isUnlockable(unlockedNodes))
            return NodeState.AVAILABLE;
        
        return NodeState.LOCKED;
    }

    /**
     * Retorna todos los nodos ordenados por posición en el grid (gridY, luego gridX).
     */
    public List<SkillNode> getNodesSorted() {
        return nodes.values().stream()
                .sorted(Comparator.comparingInt(SkillNode::getGridY)
                        .thenComparingInt(SkillNode::getGridX))
                .toList();
    }

    /**
     * Representa un edge (arista) del grafo.
     */
    public static class Edge {
        private final String from;
        private final String to;

        public Edge(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }
    }

    // ─── Grid cell from editor ──────────────────────────────────────────────

    public record GridCell(int page, int col, int row, String type, String id) {
    }

    public List<GridCell> getGridCells() {
        return Collections.unmodifiableList(gridCells);
    }

    public boolean hasGridData() {
        return !gridCells.isEmpty();
    }

    // ─── Path mapping (connector cells per edge) ────────────────────────────

    public record PathMapping(String from, String to, List<String> connectorCells) {
    }

    public List<PathMapping> getPathMappings() {
        return Collections.unmodifiableList(pathMappings);
    }
}

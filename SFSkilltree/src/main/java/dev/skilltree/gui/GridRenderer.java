package dev.skilltree.gui;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.IconDefinition;
import dev.skilltree.models.NodeState;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renderiza un SkillGraph en una GUI de 6x9 (54 slots).
 * Filas 0-4 para árbol, fila 5 reservada para navegación.
 *
 * Si el grafo tiene datos de grid del editor, los usa directamente (1:1).
 * Si no, usa un layout automático basado en topología (DAG) como fallback.
 */
public class GridRenderer {

    private static final int INVENTORY_COLS = 9;
    private static final int TREE_ROWS = 5;

    private final SkillTreePlugin plugin;

    public GridRenderer(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    public int calculateTotalPages(SkillGraph graph) {
        if (graph.hasGridData()) {
            int maxPage = 0;
            for (SkillGraph.GridCell cell : graph.getGridCells()) {
                maxPage = Math.max(maxPage, cell.page());
            }
            return maxPage + 1;
        }
        // Fallback: todo en 1 página
        return 1;
    }

    public void renderPage(Inventory inventory,
                           SkillGraph graph,
                           Set<String> unlockedNodes,
                           int page,
                           int availablePoints) {

        Set<String> unlocked = unlockedNodes == null ? Collections.emptySet() : unlockedNodes;

        clearTreeArea(inventory);
        clearNavigationRow(inventory);

        if (graph.hasGridData()) {
            renderFromGrid(inventory, graph, unlocked, page, availablePoints);
        } else {
            renderFallback(inventory, graph, unlocked, availablePoints);
        }
    }

    // ─── Grid-based rendering (editor 1:1) ──────────────────────────────────

    private void renderFromGrid(Inventory inventory,
                                SkillGraph graph,
                                Set<String> unlocked,
                                int page,
                                int availablePoints) {

        // Build a set of connector cells that should be "on"
        Set<String> onConnectorCells = buildOnConnectorCells(graph, unlocked);

        for (SkillGraph.GridCell cell : graph.getGridCells()) {
            if (cell.page() != page) continue;
            if (cell.row() < 0 || cell.row() >= TREE_ROWS) continue;
            if (cell.col() < 0 || cell.col() >= INVENTORY_COLS) continue;

            int slot = cell.row() * INVENTORY_COLS + cell.col();

            if ("node".equals(cell.type())) {
                SkillNode node = graph.getNode(cell.id());
                if (node == null) continue;
                NodeState state = graph.getNodeState(node.getId(), unlocked);
                inventory.setItem(slot, createNodeItem(node, state, availablePoints));
            } else if ("connector".equals(cell.type())) {
                String connectorBase = cell.id();
                String cellKey = cell.col() + "," + cell.row();
                boolean on = onConnectorCells.contains(cellKey);
                inventory.setItem(slot, createConnectorItem(connectorBase, on));
            }
        }
    }

    /**
     * Determina qué celdas de conectores deben estar encendidas (on).
     * Un conector se enciende si ambos nodos del path al que pertenece están desbloqueados.
     */
    private Set<String> buildOnConnectorCells(SkillGraph graph, Set<String> unlocked) {
        Set<String> onCells = new HashSet<>();

        for (SkillGraph.PathMapping path : graph.getPathMappings()) {
            boolean bothUnlocked = unlocked.contains(path.from()) && unlocked.contains(path.to());
            if (bothUnlocked) {
                onCells.addAll(path.connectorCells());
            }
        }

        return onCells;
    }

    // ─── Fallback rendering (no grid data → simple list) ────────────────────

    private void renderFallback(Inventory inventory,
                                SkillGraph graph,
                                Set<String> unlocked,
                                int availablePoints) {
        // Simple fallback: place nodes in order, left to right, top to bottom
        List<SkillNode> sorted = graph.getNodesSorted();
        int slot = 0;
        for (SkillNode node : sorted) {
            if (slot >= TREE_ROWS * INVENTORY_COLS) break;
            NodeState state = graph.getNodeState(node.getId(), unlocked);
            inventory.setItem(slot, createNodeItem(node, state, availablePoints));
            slot++;
        }
    }

    // ─── Item creation ──────────────────────────────────────────────────────

    private void clearTreeArea(Inventory inventory) {
        for (int row = 0; row < TREE_ROWS; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                inventory.setItem(row * INVENTORY_COLS + col, null);
            }
        }
    }

    private void clearNavigationRow(Inventory inventory) {
        for (int slot = 45; slot <= 53; slot++) {
            inventory.setItem(slot, null);
        }
    }

    private ItemStack createNodeItem(SkillNode node, NodeState state, int availablePoints) {
        String baseIconId = node.getIconId();
        boolean useSkillIcon = baseIconId != null && !baseIconId.isBlank() && !baseIconId.startsWith("node_");

        String iconId;
        if (useSkillIcon) {
            iconId = switch (state) {
                case UNLOCKED -> baseIconId + "_unlocked";
                case AVAILABLE -> availablePoints >= node.getCost()
                        ? baseIconId + "_available"
                        : baseIconId + "_locked";
                case EXCLUSIVE_BLOCKED -> baseIconId + "_exclusive";
                case LOCKED -> baseIconId + "_locked";
            };
        } else {
            iconId = switch (state) {
                case UNLOCKED -> "node_unlocked";
                case AVAILABLE -> availablePoints >= node.getCost() ? "node_available" : "node_locked";
                case EXCLUSIVE_BLOCKED -> "node_exclusive";
                case LOCKED -> "node_locked";
            };
        }

        IconDefinition icon = plugin.getTreeManager().getIconOrDefault(iconId);

        ItemStack item = icon.buildItem();
        ItemMeta meta = item.getItemMeta();

        NamedTextColor nameColor = switch (state) {
            case UNLOCKED -> NamedTextColor.GREEN;
            case AVAILABLE -> NamedTextColor.YELLOW;
            case LOCKED -> NamedTextColor.DARK_GRAY;
            case EXCLUSIVE_BLOCKED -> NamedTextColor.DARK_RED;
        };

        meta.displayName(Component.text(node.getName(), nameColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(node.getDescription(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Costo: " + node.getCost() + " punto" + (node.getCost() == 1 ? "" : "s"),
                NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        if (!node.getRequires().isEmpty()) {
            String requiresLabel = node.isRequiresAll() ? "Requiere todos: " : "Requiere uno de: ";
            lore.add(Component.text(requiresLabel + String.join(", ", node.getRequires()), NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        }

        String stateLabel = switch (state) {
            case UNLOCKED -> "Estado: Desbloqueado";
            case AVAILABLE -> availablePoints >= node.getCost()
                    ? "Estado: Disponible (click para desbloquear)"
                    : "Estado: Puntos insuficientes (" + availablePoints + "/" + node.getCost() + ")";
            case EXCLUSIVE_BLOCKED -> "Estado: Bloqueado por exclusividad";
            case LOCKED -> "Estado: Prerequisitos no cumplidos";
        };
        lore.add(Component.text(stateLabel, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        meta.getPersistentDataContainer().set(
                new NamespacedKey("skilltree", "node_id"),
                PersistentDataType.STRING,
                node.getId());

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConnectorItem(String connectorBase, boolean on) {
        String iconId = connectorBase + (on ? "_on" : "_off");
        IconDefinition icon = plugin.getTreeManager().getIconOrDefault(iconId);

        ItemStack item = icon.buildItem();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}

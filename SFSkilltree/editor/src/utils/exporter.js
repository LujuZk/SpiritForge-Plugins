import jsyaml from 'js-yaml';
import { GRID_COLS } from './constants';

export function exportToYaml(treeCtx, paths = []) {
    const { id, displayName, skillType, cells, edges } = treeCtx;

    // Group cells by page
    const cellsByPage = {};
    Object.entries(cells).forEach(([key, content]) => {
        const parts = key.split(",");
        if (parts.length === 3) {
            const page = parseInt(parts[0], 10);
            const col = parseInt(parts[1], 10);
            const row = parseInt(parts[2], 10);
            if (!cellsByPage[page]) cellsByPage[page] = [];
            cellsByPage[page].push({ col, row, content });
        }
    });

    const yamlData = {
        id,
        "display-name": displayName,
        "skill-type": skillType,
        "max-tier": Math.max(...Object.keys(cellsByPage).map(Number), 0) + 1,
        nodes: {},
        cells: []
    };

    // Include paths in export
    if (paths.length > 0) {
        yamlData.paths = paths.map(p => ({
            id: p.id,
            page: p.page,
            nodes: p.nodes.map(n => ({
                page: n.page,
                col: n.col,
                row: n.row,
                "node-id": n.nodeId
            }))
        }));
    }

    // Process cells for each page
    Object.entries(cellsByPage).forEach(([pageStr, pageCells]) => {
        const page = parseInt(pageStr, 10);

        pageCells.forEach(({ col, row, content }) => {
            if (content.type === 'node') {
                const nodeObj = {
                    name: content.name,
                    description: content.description,
                    cost: content.cost,
                    icon: content.iconId,
                    tier: page + 1,
                    "gui-slot": page * 54 + row * 9 + col,
                    requires: content.requires && content.requires.length > 0 ? content.requires : [],
                    "requires-all": content.requiresAll || false,
                    "exclusive-with": content.exclusiveWith && content.exclusiveWith.length > 0 ? content.exclusiveWith : [],
                    "effect-type": content.effectType,
                    "effect-value": content.effectValue
                };

                yamlData.nodes[content.nodeId] = nodeObj;

                yamlData.cells.push({
                    "gui-slot": page * 54 + row * 9 + col,
                    type: "node",
                    "node-id": content.nodeId
                });
            } else if (content.type === 'connector') {
                const connectorCellObj = {
                    "gui-slot": page * 54 + row * 9 + col,
                    type: "connector",
                    connector: content.connectorId
                };

                const dependsOn = inferConnectorDependencies(col, row, cells, page);
                if (dependsOn.length > 0) {
                    connectorCellObj["depends-on"] = dependsOn;
                }

                yamlData.cells.push(connectorCellObj);
            }
        });
    });

    const edgeSet = new Set();
    Object.entries(yamlData.nodes).forEach(([nodeId, nodeObj]) => {
        const reqs = Array.isArray(nodeObj.requires) ? nodeObj.requires : [];
        reqs.forEach(req => {
            if (!yamlData.nodes[req]) return;
            edgeSet.add(`${req}::${nodeId}`);
        });
    });
    if (edgeSet.size > 0) {
        yamlData.edges = Array.from(edgeSet).map(pair => {
            const [from, to] = pair.split('::');
            return { from, to };
        });
    }

    // Convert to YAML
    const yamlString = jsyaml.dump(yamlData, {
        quotingType: '"',
        forceQuotes: false
    });

    // Trigger download
    const blob = new Blob([yamlString], { type: "text/yaml;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${id}_skilltree.yml`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

function inferConnectorDependencies(col, row, cells, page) {
    const dependencies = [];

    // Scan left
    for (let x = col - 1; x >= 0; x--) {
        const c = cells[`${page},${x},${row}`];
        if (c) {
            if (c.type === 'node') {
                dependencies.push(c.nodeId);
            }
            if (c.type === 'node') break;
        }
    }

    // Scan right
    for (let x = col + 1; x < GRID_COLS; x++) {
        const c = cells[`${page},${x},${row}`];
        if (c) {
            if (c.type === 'node') {
                dependencies.push(c.nodeId);
            }
            if (c.type === 'node') break;
        }
    }

    return dependencies;
}

const ensureArray = (val) => Array.isArray(val) ? val : (val ? [String(val)] : []);

export function importFromYaml(yamlString) {
    const data = jsyaml.load(yamlString);
    if (!data || !data.id) throw new Error("Invalid YML format");

    const newContext = {
        id: data.id,
        displayName: data["display-name"] || data.id,
        skillType: data["skill-type"] || "SWORD",
        cells: {},
        edges: data.edges ? data.edges.map(e => ({ from: e.from, to: e.to })) : [],
        availableAssets: { nodes: [], connectors: [] }
    };

    // Restore paths if present
    const paths = data.paths ? data.paths.map(p => ({
        id: p.id,
        page: p.page,
        nodes: p.nodes.map(n => ({
            page: n.page,
            col: n.col,
            row: n.row,
            nodeId: n["node-id"]
        })),
        edges: []
    })) : [];

    const maxTier = data["max-tier"] || 1;
    const nodeMap = data.nodes || {};

    if (data.cells && data.cells.length > 0) {
        // New format with explicit cells array
        data.cells.forEach(cell => {
            const slot = cell["gui-slot"];
            const page = Math.floor(slot / 54);
            const relativeSlot = slot % 54;
            const col = relativeSlot % 9;
            const row = Math.floor(relativeSlot / 9);

            const key = `${page},${col},${row}`;

            if (cell.type === "node") {
                const nodeId = cell["node-id"];
                const nodeData = nodeMap[nodeId];
                if (nodeData) {
                    newContext.cells[key] = {
                        type: "node",
                        nodeId: nodeId,
                        name: nodeData.name || "",
                        description: nodeData.description || "",
                        cost: nodeData.cost || 1,
                        iconId: nodeData.icon || 'node_available',
                        effectType: nodeData["effect-type"] || "none",
                        effectValue: nodeData["effect-value"] || 0,
                        requires: ensureArray(nodeData.requires),
                        requiresAll: nodeData["requires-all"] || false,
                        exclusiveWith: ensureArray(nodeData["exclusive-with"])
                    };
                }
            } else if (cell.type === "connector") {
                newContext.cells[key] = {
                    type: "connector",
                    connectorId: cell.connector || 'line_h_on'
                };
            }
        });
    } else {
        // Old format (only nodes, using grid-x/grid-y or gui-slot)
        Object.entries(nodeMap).forEach(([nodeId, nodeData]) => {
            let page = 0;
            let col = 0;
            let row = 0;

            if (nodeData["gui-slot"] !== undefined) {
                const slot = nodeData["gui-slot"];
                page = Math.floor(slot / 54);
                const relativeSlot = slot % 54;
                col = relativeSlot % 9;
                row = Math.floor(relativeSlot / 9);
            } else {
                col = nodeData["grid-x"] || 0;
                row = nodeData["grid-y"] || 0;
                page = (nodeData.tier || 1) - 1;
            }

            const key = `${page},${col},${row}`;
            newContext.cells[key] = {
                type: "node",
                nodeId: nodeId,
                name: nodeData.name || "",
                description: nodeData.description || "",
                cost: nodeData.cost || 1,
                iconId: nodeData.icon || 'node_available',
                effectType: nodeData["effect-type"] || "damage_bonus",
                effectValue: nodeData["effect-value"] || 0,
                requires: ensureArray(nodeData.requires),
                requiresAll: nodeData["requires-all"] || false,
                exclusiveWith: ensureArray(nodeData["exclusive-with"])
            };
        });

        // Auto-generate connectors for old YMLs based on edges
        if (data.edges) {
            data.edges.forEach(edge => {
                const pNodeKey = Object.keys(newContext.cells).find(k => newContext.cells[k].nodeId === edge.from);
                const cNodeKey = Object.keys(newContext.cells).find(k => newContext.cells[k].nodeId === edge.to);

                if (pNodeKey && cNodeKey) {
                    const [pPage, pCol, pRow] = pNodeKey.split(',').map(Number);
                    const [cPage, cCol, cRow] = cNodeKey.split(',').map(Number);

                    if (pPage === cPage && pRow === cRow) {
                        // Same row, fill horizontally
                        const start = Math.min(pCol, cCol) + 1;
                        const end = Math.max(pCol, cCol);
                        for (let x = start; x < end; x++) {
                            const key = `${pPage},${x},${pRow}`;
                            if (!newContext.cells[key]) {
                                newContext.cells[key] = { type: "connector", connectorId: 'line_h_on' };
                            }
                        }
                    } else if (pPage === cPage && pCol === cCol) {
                        // Same col, fill vertically
                        const start = Math.min(pRow, cRow) + 1;
                        const end = Math.max(pRow, cRow);
                        for (let y = start; y < end; y++) {
                            const key = `${pPage},${pCol},${y}`;
                            if (!newContext.cells[key]) {
                                newContext.cells[key] = { type: "connector", connectorId: 'line_v_on' };
                            }
                        }
                    } else if (pPage === cPage) {
                        // Diagonal? Usually we just put an arrow_cross or try to find a path. For now, place a simple connector at the halfway point if empty
                        const midCol = Math.floor((pCol + cCol) / 2);
                        const midRow = Math.floor((pRow + cRow) / 2);
                        const key = `${pPage},${midCol},${midRow}`;
                        if (!newContext.cells[key]) {
                            newContext.cells[key] = { type: "connector", connectorId: 'arrow_cross' };
                        }
                    }
                }
            });
        }
    }

    return { newContext, maxTier, paths };
}

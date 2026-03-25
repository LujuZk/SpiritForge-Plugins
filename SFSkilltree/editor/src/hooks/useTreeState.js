import { useState, useCallback, useEffect } from 'react';
import jsyaml from 'js-yaml';
import { GRID_COLS, GRID_ROWS } from '../utils/constants';

const initialState = {
    id: "sword",
    displayName: "⚔ Espada",
    skillType: "SWORD",
    cells: {}, // key: "page,col,row"
    edges: [], // edges are global across pages
    availableAssets: { nodes: [], connectors: [] },
    customIcons: {}, // ID -> { 'oraxen-id': 'st_*' }
    customIconPngs: {}, // ID -> data URL (png)
    customConnectorPngs: {}, // baseId -> data URL (png off)
    importedIconsYaml: false,
    connectorMapping: {
        horizontal: 'connector_h',
        diagUp: 'connector_diag_up',
        diagDown: 'connector_diag_down'
    },
    importedConfigYaml: null
};

let nodeCounter = 1;
export function useTreeState() {
    const [treeContext, setTreeContext] = useState(initialState);
    const [selectedCellFormat, setSelectedCellFormat] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);

    // Path building state
    const [shiftPath, setShiftPath] = useState([]);
    const [paths, setPaths] = useState([]);

    // Debug logging helper
    const log = (msg, data) => {
        console.log(`[useTreeState] ${msg}`, data || '');
    };

    // Load connectors from editor assets (public/assets/connectors)
    useEffect(() => {
        fetch('/api/assets')
            .then(res => res.json())
            .then(data => {
                if (!data?.connectors) return;
                const normalizedConnectors = normalizeConnectorIds(data.connectors || []);
                setTreeContext(prev => ({
                    ...prev,
                    availableAssets: {
                        ...prev.availableAssets,
                        connectors: Array.from(new Set([...(prev.availableAssets?.connectors || []), ...normalizedConnectors]))
                    }
                }));
            })
            .catch(err => console.error('Failed to fetch assets:', err));
    }, []);

    const normalizeConnectorIds = (ids) => {
        const bases = new Set();
        ids.forEach(raw => {
            let id = String(raw || '').replace('.png', '');
            id = id.replace(/_(on|off)$/i, '');
            if (id) bases.add(id);
        });
        return Array.from(bases);
    };

    const goToPage = (page) => {
        if (page < 0 || page >= totalPages) return;
        setSelectedCellFormat(null);
        setCurrentPage(page);
    };

    const changeTotalPages = (count) => {
        const newTotal = Math.max(1, parseInt(count) || 1);
        setTotalPages(newTotal);
        if (currentPage >= newTotal) setCurrentPage(newTotal - 1);
    };

    const updateTreeMetadata = (field, value) => {
        setTreeContext(prev => ({ ...prev, [field]: value }));
    };

    const getCellContent = (col, row) => {
        return treeContext.cells[`${currentPage},${col},${row}`] || null;
    };

    const setCellContent = (col, row, content) => {
        setTreeContext(prev => {
            const key = `${currentPage},${col},${row}`;
            const newCells = { ...prev.cells };
            if (content === null) delete newCells[key];
            else newCells[key] = content;
            return { ...prev, cells: newCells };
        });
    };

    const placeNewNode = (col, row, iconId = '') => {
        const nodeId = `node_${col}_${row}_${nodeCounter++}`;
        setCellContent(col, row, {
            type: "node", nodeId,
            name: "", description: "", cost: 1,
            iconId: iconId,
            iconPng: null,
            effectType: "none", effectValue: 0,
            requires: [], requiresAll: false, exclusiveWith: []
        });
    };

    const placeConnector = (col, row, connectorId, connectorPngOverride = null) => {
        const connectorPng = connectorPngOverride || treeContext.customConnectorPngs?.[connectorId] || null;
        setCellContent(col, row, { type: "connector", connectorId, connectorPng });
    };

    const updateNodeProperties = (page, col, row, updates) => {
        setTreeContext(prev => {
            const key = `${page},${col},${row}`;
            const cell = prev.cells[key];
            if (!cell || cell.type !== 'node') return prev;
            return { ...prev, cells: { ...prev.cells, [key]: { ...cell, ...updates } } };
        });
    };

    const updateCellProperties = (page, col, row, updates) => {
        setTreeContext(prev => {
            const key = `${page},${col},${row}`;
            const cell = prev.cells[key];
            if (!cell) return prev;
            return { ...prev, cells: { ...prev.cells, [key]: { ...cell, ...updates } } };
        });
    };

    const registerCustomIcon = (iconId, dataUrl) => {
        if (!iconId || !dataUrl) return;
        setTreeContext(prev => {
            const nodes = prev.availableAssets?.nodes || [];
            const nextNodes = nodes.includes(iconId) ? nodes : [...nodes, iconId];
            return {
                ...prev,
                availableAssets: {
                    ...prev.availableAssets,
                    nodes: nextNodes
                },
                customIconPngs: {
                    ...(prev.customIconPngs || {}),
                    [iconId]: dataUrl
                }
            };
        });
    };

    const registerCustomConnector = (baseId, dataUrl) => {
        if (!baseId || !dataUrl) return;
        setTreeContext(prev => {
            const connectors = prev.availableAssets?.connectors || [];
            const nextConnectors = connectors.includes(baseId) ? connectors : [...connectors, baseId];
            return {
                ...prev,
                availableAssets: {
                    ...prev.availableAssets,
                    connectors: nextConnectors
                },
                customConnectorPngs: {
                    ...(prev.customConnectorPngs || {}),
                    [baseId]: dataUrl
                }
            };
        });
    };

    const moveCell = (fromCol, fromRow, toCol, toRow) => {
        if (fromCol === toCol && fromRow === toRow) return;
        setTreeContext(prev => {
            const fromKey = `${currentPage},${fromCol},${fromRow}`;
            const toKey = `${currentPage},${toCol},${toRow}`;
            const newCells = { ...prev.cells };
            const fromContent = newCells[fromKey];
            const toContent = newCells[toKey];
            if (fromContent) newCells[toKey] = fromContent; else delete newCells[toKey];
            if (toContent) newCells[fromKey] = toContent; else delete newCells[fromKey];
            return { ...prev, cells: newCells };
        });
    };

    const removeCell = (col, row) => {
        setCellContent(col, row, null);
        if (selectedCellFormat === `${currentPage},${col},${row}`) setSelectedCellFormat(null);
    };

    const addEdge = (fromNodeId, toNodeId) => {
        setTreeContext(prev => {
            if (prev.edges.some(e => e.from === fromNodeId && e.to === toNodeId)) return prev;
            return { ...prev, edges: [...prev.edges, { from: fromNodeId, to: toNodeId }] };
        });
    };

    const removeEdge = (fromNodeId, toNodeId) => {
        setTreeContext(prev => ({
            ...prev,
            edges: prev.edges.filter(e => !(e.from === fromNodeId && e.to === toNodeId))
        }));
    };

    const toggleExclusiveWith = (page, col, row, targetNodeId) => {
        setTreeContext(prev => {
            const key = `${page},${col},${row}`;
            const cell = prev.cells[key];
            if (!cell || cell.type !== 'node') return prev;

            const currentNodeId = cell.nodeId;
            const currentExclusive = cell.exclusiveWith || [];
            const isRemoving = currentExclusive.includes(targetNodeId);

            const newCurrentExclusive = isRemoving
                ? currentExclusive.filter(id => id !== targetNodeId)
                : [...currentExclusive, targetNodeId];

            const newCells = { ...prev.cells, [key]: { ...cell, exclusiveWith: newCurrentExclusive } };

            // Update target node bidirectionally
            const targetKey = Object.keys(prev.cells).find(k => prev.cells[k]?.nodeId === targetNodeId);
            if (targetKey) {
                const targetCell = prev.cells[targetKey];
                const targetExclusive = targetCell.exclusiveWith || [];
                const newTargetExclusive = isRemoving
                    ? targetExclusive.filter(id => id !== currentNodeId)
                    : (targetExclusive.includes(currentNodeId) ? targetExclusive : [...targetExclusive, currentNodeId]);
                newCells[targetKey] = { ...targetCell, exclusiveWith: newTargetExclusive };
            }

            return { ...prev, cells: newCells };
        });
    };

    // ─── ICONS IMPORT ─────────────────────────────────────────────────────────

    const importIcons = (yamlString) => {
        try {
            const data = jsyaml.load(yamlString);
            if (data && data.icons) {
                setTreeContext(prev => {
                    const mergedIcons = { ...prev.customIcons, ...data.icons };
                    const incomingIds = Object.keys(data.icons || {});
                    const nodeIds = incomingIds.filter(id => (
                        !id.startsWith('connector_') &&
                        !id.startsWith('node_') &&
                        !id.startsWith('page_') &&
                        !id.startsWith('nav_') &&
                        id !== 'transparent'
                    ));
                    const connectorIds = normalizeConnectorIds(incomingIds.filter(id => id.startsWith('connector_')));
                    const mergedNodes = Array.from(new Set([...(prev.availableAssets?.nodes || []), ...nodeIds]));
                    const mergedConnectors = Array.from(new Set([...(prev.availableAssets?.connectors || []), ...connectorIds]));
                    return {
                        ...prev,
                        customIcons: mergedIcons,
                        importedIconsYaml: true,
                        availableAssets: {
                            ...prev.availableAssets,
                            nodes: mergedNodes,
                            connectors: mergedConnectors
                        }
                    };
                });
                log(`Imported ${Object.keys(data.icons).length} icons`);
                return true;
            }
        } catch (e) {
            console.error("Failed to parse icons.yml", e);
        }
        return false;
    };

    const importConfig = (yamlString) => {
        try {
            const data = jsyaml.load(yamlString);
            const connectors = data?.connectors || {};
            const mapping = {
                horizontal: connectors.horizontal || connectors.h || 'connector_h',
                diagUp: connectors.diag_up || connectors.diagUp || 'connector_diag_up',
                diagDown: connectors.diag_down || connectors.diagDown || 'connector_diag_down'
            };
            setTreeContext(prev => ({
                ...prev,
                connectorMapping: mapping,
                importedConfigYaml: yamlString
            }));
            return true;
        } catch (e) {
            console.error("Failed to parse config.yml", e);
        }
        return false;
    };

    // ─── PATH BUILDING (STATE-DRIVEN) ──────────────────────────────────────────

    /**
     * Internal: check if a cell can be added to the path.
     * Rules:
     * - Must start with a node.
     * - Max 2 nodes total.
     * - Must be adjacent (4-dir).
     * - End node must come after at least one connector.
     * - Connectors can't be reused across paths.
     */
    const _checkAdjacency = (currentPath, page, col, row, content) => {
        const key = `${page},${col},${row}`;

        if (!content) return { valid: false, reason: 'No content' };
        if (currentPath.some(p => p.key === key)) return { valid: false, reason: 'Already in path' };

        const nodeCount = currentPath.filter(p => p.type === 'node').length;
        if (nodeCount >= 2) return { valid: false, reason: 'Already has 2 nodes' };

        if (currentPath.length === 0) {
            return content.type === 'node' ? { valid: true } : { valid: false, reason: 'First cell must be node' };
        }

        const last = currentPath[currentPath.length - 1];
        if (last.page !== page) return { valid: false, reason: 'Different page' };

        // No adjacency restriction: allow diagonal or jumps

        if (content.type === 'node') {
            if (last.type !== 'connector') {
                return { valid: false, reason: 'Node must follow connector' };
            }
        } else if (content.type === 'connector') {
            if (nodeCount === 0) return { valid: false, reason: 'Connector before first node' };
        }

        // Connector reuse check
        if (content.type === 'connector') {
            const isUsed = paths.some(p => (p.cells || []).some(c => c.page === page && c.col === col && c.row === row));
            if (isUsed) return { valid: false, reason: 'Connector already used in another path' };
        }

        return { valid: true };
    };

    const canAddToPath = (page, col, row, content) => {
        return _checkAdjacency(shiftPath, page, col, row, content).valid;
    };

    const addToShiftPath = useCallback((page, col, row, content) => {
        setShiftPath(prev => {
            const validation = _checkAdjacency(prev, page, col, row, content);
            if (!validation.valid) {
                log(`Cell ${col},${row} rejected: ${validation.reason}`);
                return prev;
            }

            const newItem = {
                page, col, row, key: `${page},${col},${row}`,
                type: content.type,
                nodeId: content.nodeId || null,
                connectorId: content.connectorId || null
            };
            const newPath = [...prev, newItem];
            const nodeCount = newPath.filter(p => p.type === 'node').length;

            log(`Added ${content.type} at ${col},${row}. Path length: ${newPath.length}`);

            if (nodeCount === 2) {
                log('Path complete! Finalizing...');
                queueMicrotask(() => _commitPath(newPath));
                return newPath;
            }

            return newPath;
        });
    }, [paths]);

    const _commitPath = useCallback((pathCells) => {
        const nodePts = pathCells.filter(p => p.type === 'node' && p.nodeId);
        if (nodePts.length < 2) {
            setShiftPath([]);
            return;
        }

        const fromNode = nodePts[0];
        const toNode = nodePts[1];

        setTreeContext(prev => {
            const edgeExists = prev.edges.some(e => e.from === fromNode.nodeId && e.to === toNode.nodeId);
            const newEdges = edgeExists ? prev.edges : [...prev.edges, { from: fromNode.nodeId, to: toNode.nodeId }];

            const toKey = Object.keys(prev.cells).find(k => prev.cells[k]?.nodeId === toNode.nodeId);
            let newCells = prev.cells;
            if (toKey) {
                const toCell = prev.cells[toKey];
                const currentReqs = toCell.requires || [];
                if (!currentReqs.includes(fromNode.nodeId)) {
                    newCells = { ...prev.cells, [toKey]: { ...toCell, requires: [...currentReqs, fromNode.nodeId] } };
                }
            }
            return { ...prev, edges: newEdges, cells: newCells };
        });

        setPaths(prev => [
            ...prev,
            {
                id: `path_${Date.now()}`,
                page: fromNode.page,
                cells: pathCells.map(c => ({ page: c.page, col: c.col, row: c.row, nodeId: c.nodeId || null })),
                nodes: nodePts.map(n => ({ page: n.page, col: n.col, row: n.row, nodeId: n.nodeId })),
                edges: [{ from: fromNode.nodeId, to: toNode.nodeId }]
            }
        ]);

        setShiftPath([]);
    }, []);

    const finalizeCurrentPath = () => {
        setShiftPath(prev => {
            _commitPath(prev);
            return [];
        });
    };

    const clearShiftPath = () => {
        setShiftPath([]);
    };

    const loadPaths = (newPaths) => {
        const sanitized = newPaths.map(p => {
            if (p.cells) return p;
            return { ...p, cells: p.nodes || [] };
        });
        setPaths(sanitized);
    };

    return {
        treeContext, setTreeContext,
        selectedCellFormat, setSelectedCellFormat,
        updateTreeMetadata, getCellContent,
        placeNewNode, placeConnector, moveCell, removeCell,
        updateNodeProperties, updateCellProperties, addEdge, removeEdge, toggleExclusiveWith,
        currentPage, totalPages, goToPage, changeTotalPages,
        // Icons import
        importIcons, importConfig, registerCustomIcon, registerCustomConnector,
        // Path building
        shiftPath, paths, loadPaths,
        canAddToPath, addToShiftPath, finalizeCurrentPath, clearShiftPath
    };
}

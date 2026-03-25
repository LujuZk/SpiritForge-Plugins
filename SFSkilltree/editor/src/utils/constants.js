export const GRID_COLS = 9;
export const GRID_ROWS = 6;
export const TOTAL_CELLS = GRID_COLS * GRID_ROWS;

export const CONNECTORS = [
    { id: "connector_h", label: "──" },
    { id: "connector_v", label: "│" },
    { id: "connector_elbow_tr", label: "╚═" },
    { id: "connector_elbow_br", label: "╔═" },
    { id: "connector_elbow_tl", label: "═╝" },
    { id: "connector_elbow_bl", label: "═╗" },
    { id: "connector_split_2_right", label: "═<2" },
    { id: "connector_split_2_left", label: "2>═" },
    { id: "connector_split_3_right", label: "═<3" },
    { id: "connector_split_3_left", label: "3>═" },
    { id: "connector_diag_down", label: "╲" },
    { id: "connector_diag_up", label: "╱" }
];

export const SKILL_TYPES = [
    "SWORD", "AXE", "MINING", "FARMING", "FISHING", "BOW", "TRIDENT"
];

// Helper to check if a cell is reserved (last row for hotbar)
export const isReservedRow = (row) => row === 5;

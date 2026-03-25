import JSZip from 'jszip';
import jsyaml from 'js-yaml';

// ─── Base icons (mirrors plugin's icons.yml exactly) ────────────────────────
// Format: icon name → { 'oraxen-id': 'st_*' }
const BASE_ICONS = {
    // Node States
    node_locked:   { 'oraxen-id': 'st_node_locked' },
    node_available: { 'oraxen-id': 'st_node_available' },
    node_unlocked:  { 'oraxen-id': 'st_node_unlocked' },
    node_exclusive: { 'oraxen-id': 'st_node_exclusive' },

    // Connectors ON
    connector_h_on:             { 'oraxen-id': 'st_connector_h_on' },
    connector_v_on:             { 'oraxen-id': 'st_connector_v_on' },
    connector_diag_down_on:     { 'oraxen-id': 'st_connector_diag_down_on' },
    connector_diag_up_on:       { 'oraxen-id': 'st_connector_diag_up_on' },
    connector_v_half_top_on:    { 'oraxen-id': 'st_connector_v_half_top_on' },
    connector_v_half_bottom_on: { 'oraxen-id': 'st_connector_v_half_bottom_on' },

    // Connectors OFF
    connector_h_off:             { 'oraxen-id': 'st_connector_h_off' },
    connector_v_off:             { 'oraxen-id': 'st_connector_v_off' },
    connector_diag_down_off:     { 'oraxen-id': 'st_connector_diag_down_off' },
    connector_diag_up_off:       { 'oraxen-id': 'st_connector_diag_up_off' },
    connector_v_half_top_off:    { 'oraxen-id': 'st_connector_v_half_top_off' },
    connector_v_half_bottom_off: { 'oraxen-id': 'st_connector_v_half_bottom_off' },

    // Elbows ON
    elbow_left_down_on:  { 'oraxen-id': 'st_elbow_left_down_on' },
    elbow_left_up_on:    { 'oraxen-id': 'st_elbow_left_up_on' },
    elbow_right_down_on: { 'oraxen-id': 'st_elbow_right_down_on' },
    elbow_right_up_on:   { 'oraxen-id': 'st_elbow_right_up_on' },

    // Elbows OFF
    elbow_left_down_off:  { 'oraxen-id': 'st_elbow_left_down_off' },
    elbow_left_up_off:    { 'oraxen-id': 'st_elbow_left_up_off' },
    elbow_right_down_off: { 'oraxen-id': 'st_elbow_right_down_off' },
    elbow_right_up_off:   { 'oraxen-id': 'st_elbow_right_up_off' },

    // Pages
    page_0: { 'oraxen-id': 'st_page_0' },
    page_1: { 'oraxen-id': 'st_page_1' },
    page_2: { 'oraxen-id': 'st_page_2' },
    page_3: { 'oraxen-id': 'st_page_3' },
    page_4: { 'oraxen-id': 'st_page_4' },
    page_5: { 'oraxen-id': 'st_page_5' },
    page_6: { 'oraxen-id': 'st_page_6' },
    page_7: { 'oraxen-id': 'st_page_7' },
    page_8: { 'oraxen-id': 'st_page_8' },
    page_9: { 'oraxen-id': 'st_page_9' },

    // Navigation
    nav_prev:          { 'oraxen-id': 'st_nav_prev' },
    nav_prev_disabled: { 'oraxen-id': 'st_nav_prev_disabled' },
    nav_next:          { 'oraxen-id': 'st_nav_next' },
    nav_next_disabled: { 'oraxen-id': 'st_nav_next_disabled' },

    // Misc
    transparent: { 'oraxen-id': 'st_transparent' },
};

// ─── Oraxen items base YAML (embedded from Server/plugins/Oraxen/items/skilltree.yml) ─
// Custom skill/connector items are concatenated at the end when present.
const BASE_ORAXEN_ITEMS_YAML = `# ═══════════════════════════════════════════════════════════════════════
# SFSkilltree — Oraxen item definitions
# Prefix: st_ to avoid ID collisions with other plugins
# ═══════════════════════════════════════════════════════════════════════

# ─── Node States (single-layer, generate_model: true) ─────────────────

st_node_available:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/node_available.png

st_node_locked:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/node_locked.png

st_node_unlocked:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/node_unlocked.png

st_node_exclusive:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/node_exclusive_blocked.png

# ─── Connectors ON (single-layer, generate_model: true) ──────────────

st_connector_h_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_h.png

st_connector_v_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_v.png

st_connector_diag_down_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_diag_down.png

st_connector_diag_up_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_diag_up.png

st_elbow_left_down_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/elbow_left_down.png

st_elbow_left_up_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/elbow_left_up.png

st_elbow_right_down_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/elbow_right_down.png

st_elbow_right_up_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/elbow_right_up.png

# ─── Connectors OFF (single-layer, generate_model: true) ─────────────

st_connector_h_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_h.png

st_connector_v_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_v.png

st_connector_diag_down_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_diag_down.png

st_connector_diag_up_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_diag_up.png

st_elbow_left_down_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/elbow_left_down.png

st_elbow_left_up_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/elbow_left_up.png

st_elbow_right_down_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/elbow_right_down.png

st_elbow_right_up_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/elbow_right_up.png

# ─── Connector half variants (no textures yet — placeholders) ────────

st_connector_v_half_bottom_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_v.png

st_connector_v_half_top_on:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/on/connector_v.png

st_connector_v_half_bottom_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_v.png

st_connector_v_half_top_off:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/connectors/off/connector_v.png

# ─── Pages (use arrow textures as placeholders) ──────────────────────

st_page_0:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_page_1:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_page_2:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_page_3:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_page_4:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_page_5:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_page_6:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_page_7:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_page_8:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_page_9:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

# ─── Navigation ──────────────────────────────────────────────────────

st_nav_prev:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_nav_prev_disabled:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_left.png

st_nav_next:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

st_nav_next_disabled:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/arrow_right.png

# ─── Misc ────────────────────────────────────────────────────────────

st_transparent:
  material: PAPER
  excludeFromInventory: true
  Pack:
    generate_model: true
    parent_model: item/generated
    textures:
      - skilltree/node_locked.png
`;

// ─── Oraxen pack paths (always the same regardless of mode) ─────────────────
const ORAXEN_ITEMS_PATH    = 'Server/plugins/Oraxen/items/skilltree.yml';
const ORAXEN_TEXTURES_ROOT = 'Server/plugins/Oraxen/pack/textures/';
const ORAXEN_MODELS_ROOT   = 'Server/plugins/Oraxen/pack/models/';

function buildIconsYaml(icons) {
    const keys = Object.keys(icons || {});
    const groups = [
        { title: 'Node States',    match: k => k.startsWith('node_') },
        { title: 'Connectors (ON)',  match: k => k.startsWith('connector_') && k.endsWith('_on') },
        { title: 'Connectors (OFF)', match: k => k.startsWith('connector_') && k.endsWith('_off') },
        { title: 'Elbows (ON)',      match: k => k.startsWith('elbow_') && k.endsWith('_on') },
        { title: 'Elbows (OFF)',     match: k => k.startsWith('elbow_') && k.endsWith('_off') },
        {
            title: 'Skill Icons',
            match: k => !k.startsWith('node_') && !k.startsWith('connector_') &&
                        !k.startsWith('elbow_') && !k.startsWith('page_') &&
                        !k.startsWith('nav_') && k !== 'transparent'
        },
        { title: 'Pages',      match: k => k.startsWith('page_') },
        { title: 'Navigation', match: k => k.startsWith('nav_') },
        { title: 'Misc',       match: k => k === 'transparent' },
    ];

    const lines = ['icons:', ''];

    groups.forEach(group => {
        const groupKeys = keys.filter(group.match).sort();
        if (groupKeys.length === 0) return;
        lines.push(`  # ${group.title}`);
        groupKeys.forEach(key => {
            const entry = icons[key] || {};
            lines.push(`  ${key}:`);
            lines.push(`    oraxen-id: ${entry['oraxen-id']}`);
        });
        lines.push('');
    });

    return lines.join('\n').trimEnd() + '\n';
}

export async function exportToZip(treeCtx, options = {}) {
    const zip = new JSZip();

    const sanitizeId = (id) => String(id || '').toLowerCase().replace(/[^a-z0-9_]/g, '_');

    const skillName = sanitizeId(treeCtx.id || 'skill');
    const mode = options.mode || 'plugin';

    const pathsByMode = {
        plugin: {
            icons:      'icons.yml',
            tree:       `skills/${skillName}/tree.yml`,
            config:     'config.yml',
            readmeMode: 'plugin'
        },
        repo: {
            icons:      'src/main/resources/icons.yml',
            tree:       `src/main/resources/skills/${skillName}/tree.yml`,
            config:     'src/main/resources/config.yml',
            readmeMode: 'repo'
        }
    };

    const paths = pathsByMode[mode] || pathsByMode.plugin;

    // ── 1. Build tree.yml ─────────────────────────────────────────────
    const yamlData = buildTreeYaml(treeCtx);

    // ── 2. Resolve icons: merge customIcons (imported) into base set ──
    const mergedIcons = { ...BASE_ICONS, ...(treeCtx.customIcons || {}) };

    const allCells = Object.values(treeCtx.cells);
    const allNodes = allCells.filter(c => c.type === 'node');
    const allConnectors = allCells.filter(c => c.type === 'connector');
    const customIconPngs = treeCtx.customIconPngs || {};
    const customConnectorPngs = treeCtx.customConnectorPngs || {};

    const toBase64Png = (dataUrlOrBase64) => {
        if (!dataUrlOrBase64) return null;
        const str = String(dataUrlOrBase64);
        if (str.startsWith('data:')) {
            const parts = str.split(',');
            return parts.length > 1 ? parts[1] : null;
        }
        return str;
    };

    const blobToBase64 = (blob) => new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            const result = String(reader.result || '');
            const parts = result.split(',');
            resolve(parts.length > 1 ? parts[1] : null);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    });

    const fetchSkillIconBase64 = async (iconId) => {
        if (!iconId) return null;
        try {
            const res = await fetch(`/assets/skills/${skillName}/${iconId}.png`);
            if (res.ok) {
                const blob = await res.blob();
                return await blobToBase64(blob);
            }
            const resFlat = await fetch(`/assets/skills/${iconId}.png`);
            if (!resFlat.ok) return null;
            const blobFlat = await resFlat.blob();
            return await blobToBase64(blobFlat);
        } catch {
            return null;
        }
    };

    const tintToFluorYellow = async (base64Png) => {
        if (!base64Png) return null;
        const dataUrl = `data:image/png;base64,${base64Png}`;
        const img = new Image();
        const loaded = new Promise((resolve) => {
            img.onload = () => resolve(true);
            img.onerror = () => resolve(false);
        });
        img.src = dataUrl;
        const ok = await loaded;
        if (!ok) return null;

        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        ctx.globalCompositeOperation = 'source-atop';
        ctx.fillStyle = '#f6ff00';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.globalCompositeOperation = 'source-over';
        try {
            const tinted = canvas.toDataURL('image/png');
            return tinted.split(',')[1];
        } catch {
            return null;
        }
    };

    const fetchConnectorBase64 = async (baseId) => {
        if (!baseId) return null;
        try {
            const res = await fetch(`/assets/connectors/${baseId}.png`);
            if (!res.ok) return null;
            const blob = await res.blob();
            return await blobToBase64(blob);
        } catch {
            return null;
        }
    };

    const NODE_STATE_VARIANTS = [
        { suffix: 'unlocked',  frame: 'node_unlocked' },
        { suffix: 'available', frame: 'node_available' },
        { suffix: 'locked',    frame: 'node_locked' },
        { suffix: 'exclusive', frame: 'node_exclusive_blocked' }
    ];

    // Oraxen YAML extra entries (appended after BASE_ORAXEN_ITEMS_YAML)
    const oraxenExtraLines = [];
    let hasNewIcons = false;

    // ── 3. Process custom skill icon PNGs ─────────────────────────────
    const baseIconIds = new Set();
    const baseIconPngs = {};

    Object.entries(customIconPngs).forEach(([rawId, dataUrl]) => {
        const iconId = sanitizeId(rawId);
        if (!iconId || iconId.startsWith('node_')) return;
        baseIconIds.add(iconId);
        if (dataUrl && !baseIconPngs[iconId]) baseIconPngs[iconId] = dataUrl;
    });

    for (const node of allNodes) {
        let baseIconId = sanitizeId(node.iconId);
        if (!baseIconId || baseIconId.startsWith('node_')) continue;

        baseIconIds.add(baseIconId);

        const iconPngData = node.iconPng || customIconPngs[node.iconId];
        if (iconPngData && !baseIconPngs[baseIconId]) baseIconPngs[baseIconId] = iconPngData;

        if (yamlData.nodes[node.nodeId]) yamlData.nodes[node.nodeId].icon = baseIconId;
    }

    for (const baseIconId of baseIconIds) {
        // Skip icons that are already fully defined in BASE_ICONS (no new pack data needed)
        if (BASE_ICONS[baseIconId] && !baseIconPngs[baseIconId]) continue;

        const iconPngData = baseIconPngs[baseIconId];
        let base64Png = iconPngData ? toBase64Png(iconPngData) : null;
        if (!base64Png) base64Png = await fetchSkillIconBase64(baseIconId);

        const hasSkillTexture = !!base64Png;
        if (hasSkillTexture) {
            const texPath = `${ORAXEN_TEXTURES_ROOT}skilltree/skills/${skillName}/${baseIconId}.png`;
            zip.file(texPath, base64Png, { base64: true });
        }

        for (const variant of NODE_STATE_VARIANTS) {
            const stateIconId = `${baseIconId}_${variant.suffix}`;

            // Skip if already in BASE_ICONS (e.g. sword_basic_available already defined)
            if (BASE_ICONS[stateIconId]) continue;

            const modelJson = {
                parent: 'item/generated',
                textures: {}
            };
            if (hasSkillTexture) {
                modelJson.textures.layer0 = `skilltree/skills/${skillName}/${baseIconId}`;
                modelJson.textures.layer1 = `skilltree/${variant.frame}`;
            } else {
                modelJson.textures.layer0 = `skilltree/${variant.frame}`;
            }

            const modelPath = `${ORAXEN_MODELS_ROOT}skilltree/skills/${skillName}/${stateIconId}.json`;
            zip.file(modelPath, JSON.stringify(modelJson, null, 2));

            // Add to mergedIcons for icons.yml
            if (!mergedIcons[stateIconId]) {
                mergedIcons[stateIconId] = { 'oraxen-id': `st_${stateIconId}` };
                hasNewIcons = true;
            }

            // Add Oraxen item entry
            oraxenExtraLines.push(
                `st_${stateIconId}:`,
                `  material: PAPER`,
                `  excludeFromInventory: true`,
                `  Pack:`,
                `    generate_model: false`,
                `    model: skilltree/skills/${skillName}/${stateIconId}`,
                ``
            );
        }
    }

    // ── 4. Process custom connector PNGs ──────────────────────────────
    const connectorBases = new Set([
        ...(treeCtx.availableAssets?.connectors || []),
        ...Object.keys(customConnectorPngs)
    ]);
    for (const conn of allConnectors) {
        const raw = conn.connectorId;
        if (!raw) continue;
        const base = String(raw).replace(/_(on|off)$/i, '');
        connectorBases.add(base);
    }

    for (const rawBase of connectorBases) {
        const base = sanitizeId(rawBase);
        const offId = `${base}_off`;
        const onId  = `${base}_on`;

        // Skip connectors already fully defined in BASE_ICONS
        if (BASE_ICONS[offId] && BASE_ICONS[onId] && !customConnectorPngs[rawBase]) continue;

        const offPngData = customConnectorPngs[rawBase];
        let offBase64 = toBase64Png(offPngData);
        if (!offBase64) offBase64 = await fetchConnectorBase64(base);
        if (!offBase64) continue;

        const tinted = await tintToFluorYellow(offBase64);
        const onBase64 = tinted || offBase64;

        zip.file(`${ORAXEN_TEXTURES_ROOT}skilltree/connectors/off/${base}.png`, offBase64, { base64: true });
        if (onBase64) {
            zip.file(`${ORAXEN_TEXTURES_ROOT}skilltree/connectors/on/${base}.png`, onBase64, { base64: true });
        }

        const offModel = {
            parent: 'item/generated',
            textures: { layer0: `skilltree/connectors/off/${base}` }
        };
        const onModel = {
            parent: 'item/generated',
            textures: { layer0: `skilltree/connectors/on/${base}` }
        };
        zip.file(`${ORAXEN_MODELS_ROOT}skilltree/connectors/off/${base}.json`, JSON.stringify(offModel, null, 2));
        zip.file(`${ORAXEN_MODELS_ROOT}skilltree/connectors/on/${base}.json`, JSON.stringify(onModel, null, 2));

        if (!mergedIcons[offId]) {
            mergedIcons[offId] = { 'oraxen-id': `st_${offId}` };
            hasNewIcons = true;
        }
        if (!mergedIcons[onId]) {
            mergedIcons[onId] = { 'oraxen-id': `st_${onId}` };
            hasNewIcons = true;
        }

        oraxenExtraLines.push(
            `st_${offId}:`,
            `  material: PAPER`,
            `  excludeFromInventory: true`,
            `  Pack:`,
            `    generate_model: false`,
            `    model: skilltree/connectors/off/${base}`,
            ``,
            `st_${onId}:`,
            `  material: PAPER`,
            `  excludeFromInventory: true`,
            `  Pack:`,
            `    generate_model: false`,
            `    model: skilltree/connectors/on/${base}`,
            ``
        );
    }

    // ── 5. Generate Server/plugins/Oraxen/items/skilltree.yml ─────────
    let oraxenItemsYaml = BASE_ORAXEN_ITEMS_YAML;
    if (oraxenExtraLines.length > 0) {
        oraxenItemsYaml += '\n# ─── Custom items (generated by editor) ─────────────────────────────\n\n';
        oraxenItemsYaml += oraxenExtraLines.join('\n');
    }
    zip.file(ORAXEN_ITEMS_PATH, oraxenItemsYaml);

    // ── 6. Export icons.yml (base + new) ──────────────────────────────
    const iconsYaml = buildIconsYaml(mergedIcons);
    zip.file(paths.icons, iconsYaml);

    // ── 7. Export config.yml if imported ─────────────────────────────
    if (treeCtx.importedConfigYaml) {
        const updatedConfig = updateConfigYaml(treeCtx.importedConfigYaml, treeCtx);
        if (updatedConfig) zip.file(paths.config, updatedConfig);
    }

    // ── 8. Export tree.yml ────────────────────────────────────────────
    const finalYamlString = jsyaml.dump(yamlData, { quotingType: '"', forceQuotes: false });
    zip.file(paths.tree, finalYamlString);

    // ── 9. README ─────────────────────────────────────────────────────
    zip.file('README.txt', getReadmeContent(skillName, hasNewIcons, paths.readmeMode));

    // ── 10. Download ──────────────────────────────────────────────────
    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${skillName}_project.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

// ─── Build the YAML object that the plugin actually reads ────────────────────
function buildTreeYaml(treeCtx) {
    const { id, displayName, skillType, cells, edges } = treeCtx;

    const yamlData = {
        id,
        'display-name': displayName,
        'skill-type': skillType.toLowerCase(),
        nodes: {},
        grid: [],
        paths: []
    };

    // ── Export nodes ──────────────────────────────────────────────────
    Object.entries(cells).forEach(([key, content]) => {
        if (content.type !== 'node') return;

        const nodeId = content.nodeId;
        const nodeObj = {
            name: content.name,
            description: content.description,
            cost: content.cost,
            requires: content.requires || [],
            'requires-all': content.requiresAll || false,
            'exclusive-with': content.exclusiveWith || [],
            'effect-type': content.effectType,
            'effect-value': content.effectValue
        };
        if (content.iconId) nodeObj.icon = content.iconId;
        yamlData.nodes[nodeId] = nodeObj;
    });

    // ── Export edges ──────────────────────────────────────────────────
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

    // ── Export grid ───────────────────────────────────────────────────
    const pageMap = {};
    Object.entries(cells).forEach(([key, content]) => {
        const parts = key.split(',');
        if (parts.length !== 3) return;
        const [page, col, row] = parts.map(Number);

        const cellEntry = { col, row };
        if (content.type === 'node') {
            cellEntry.type = 'node';
            cellEntry['node-id'] = content.nodeId;
        } else if (content.type === 'connector') {
            cellEntry.type = 'connector';
            cellEntry['connector-id'] = String(content.connectorId || '').replace(/_(on|off)$/i, '');
        } else {
            return;
        }

        if (!pageMap[page]) pageMap[page] = [];
        pageMap[page].push(cellEntry);
    });

    Object.keys(pageMap).sort((a, b) => a - b).forEach(page => {
        yamlData.grid.push({ page: Number(page), cells: pageMap[page] });
    });

    // ── Export paths ──────────────────────────────────────────────────
    const pathsData = treeCtx._paths || [];
    for (const path of pathsData) {
        if (!path.cells || path.cells.length === 0) continue;
        const nodes = (path.cells || []).filter(c => c.nodeId);
        if (nodes.length < 2) continue;
        const connectorCells = (path.cells || []).filter(c => !c.nodeId);
        yamlData.paths.push({
            from: nodes[0].nodeId,
            to: nodes[nodes.length - 1].nodeId,
            cells: connectorCells.map(c => `${c.col},${c.row}`)
        });
    }

    return yamlData;
}

// ─── README ──────────────────────────────────────────────────────────────────
function getReadmeContent(skillName, hasNewIcons, mode = 'plugin') {
    const iconNote = hasNewIcons
        ? `   - Server/plugins/Oraxen/items/skilltree.yml  <- REEMPLAZAR el existente (incluye items nuevos)
   - Server/plugins/Oraxen/pack/textures/        <- copiar carpetas (fusionar si ya existe)
   - Server/plugins/Oraxen/pack/models/          <- copiar carpetas (fusionar si ya existe)`
        : `   - Server/plugins/Oraxen/items/skilltree.yml  <- REEMPLAZAR el existente`;

    if (mode === 'repo') {
        return `SFSkilltree - Proyecto exportado: ${skillName}
============================================

INSTRUCCIONES DE INSTALACION (REPO):

1. Descomprimi el ZIP en la RAIZ del proyecto.

   Archivos que se copiaran:
   - src/main/resources/skills/${skillName}/tree.yml
   - src/main/resources/icons.yml
${iconNote}

2. Para conservar iconos existentes, importalos en el editor antes de exportar.

3. Compila el plugin (./gradlew build).

4. Copia el JAR al servidor y reinicia.
   Luego aplica los cambios de Oraxen: /oraxen reload all

El layout en el juego sera identico al del editor.
`;
    }

    return `SFSkilltree - Proyecto exportado: ${skillName}
============================================

INSTRUCCIONES DE INSTALACION:

1. Descomprimi los archivos del plugin dentro de:
   plugins/SFSkilltree/

   Archivos que se copiaran:
   - skills/${skillName}/tree.yml  <- arbol de habilidades
   - icons.yml                     <- registro completo de iconos

2. Copia la carpeta Server/ a la RAIZ del servidor:
${iconNote}

3. Para conservar iconos existentes, importalos en el editor antes de exportar.

4. Reinicia el servidor o usa /oraxen reload all.
   Luego usa /reload (o reinicio completo) para el plugin.

El layout en el juego sera identico al del editor.
`;
}

function updateConfigYaml(rawYaml, treeCtx) {
    if (!rawYaml) return null;
    let data;
    try {
        data = jsyaml.load(rawYaml) || {};
    } catch {
        return null;
    }

    const id = String(treeCtx.id || '').toLowerCase().trim();
    const displayName = treeCtx.displayName || treeCtx.id || id;
    if (!id) return rawYaml;

    const weaponTypes = new Set(['SWORD', 'AXE', 'BOW', 'TRIDENT']);
    const sectionKey = weaponTypes.has(String(treeCtx.skillType || '').toUpperCase())
        ? 'weapon-skills'
        : 'skills';

    if (!data[sectionKey]) data[sectionKey] = {};
    if (!data[sectionKey][id]) data[sectionKey][id] = {};
    data[sectionKey][id]['display-name'] = displayName;

    return jsyaml.dump(data, { quotingType: '"', forceQuotes: false });
}

function createPlaceholderImage(text) {
    const canvas = document.createElement('canvas');
    canvas.width = 64;
    canvas.height = 64;
    const ctx = canvas.getContext('2d');

    ctx.fillStyle = '#4a3b2c';
    ctx.fillRect(0, 0, 64, 64);

    ctx.strokeStyle = '#c8a03c';
    ctx.lineWidth = 4;
    ctx.strokeRect(2, 2, 60, 60);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 12px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text.substring(0, 4).toUpperCase(), 32, 32);

    return canvas.toDataURL('image/png');
}

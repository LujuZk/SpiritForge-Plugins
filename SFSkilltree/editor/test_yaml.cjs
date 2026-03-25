const fs = require('fs');
const jsyaml = require('js-yaml');

// Paste the minimal logic to verify how the cell translates
const yamlStr = fs.readFileSync('../src/main/resources/skills/sword/tree.yml', 'utf8');
const data = jsyaml.load(yamlStr);

const newContext = { cells: {} };
const nodeMap = data.nodes || {};

Object.entries(nodeMap).forEach(([nodeId, nodeData]) => {
    let page = 0, col = 0, row = 0;
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

    // Simulate what exporter does
    const effectType = nodeData["effect-type"] || "damage_bonus";
    console.log(`Node: ${nodeId} -> Key: ${key}, effectType: ${effectType}, is command? ${effectType === 'command'}`);

    newContext.cells[key] = {
        type: "node",
        nodeId: nodeId,
        effectType: effectType
    };
});

// Assume I clicked 4,1 on page 0 -> 0,4,1
console.log('Cell at 0,4,1:', newContext.cells['0,4,1']);

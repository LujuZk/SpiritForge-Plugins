# SFSkilltree Editor

A web-based visual editor for creating and managing skill tree configurations for the SFSkilltree Minecraft plugin.

## Overview

The editor provides a drag-and-drop interface for building skill trees visually. It runs as a React + Vite web application and exports YAML configurations compatible with the Minecraft plugin.

## Features

- **Visual Editor**: Drag nodes and connectors onto a grid
- **Multi-Page Support**: Create trees with multiple pages (tiers)
- **Asset Browser**: Browse available node icons and connector textures
- **Property Editor**: Configure node details (name, description, cost, effects)
- **YAML Export**: Generate ready-to-use config files
- **Real-time Preview**: See exactly how your tree will look

## Getting Started

### Installation

```bash
cd editor
npm install
```

### Development

```bash
npm run dev
```

The editor runs at `http://localhost:5173/SFSkilltree/`

### Production Build

```bash
npm run build
```

Output in `dist/` folder.

## Usage

### Interface Layout

```
┌─────────────────────────────────────────────────────────────┐
│ Toolbar: Tree ID, Display Name, Type, Pages, Export       │
├────────┬─────────────────────────────────────┬────────────┤
│        │                                     │            │
│Toolbox │          Grid Canvas                │ Properties │
│        │                                     │   Panel    │
│ Nodes  │     9x6 draggable grid             │  (select   │
│        │                                     │   node)    │
│ Connect│                                     │            │
│        │                                     │            │
├────────┴─────────────────────────────────────┴────────────┤
│ Pagination: [←] [1] [2] [3] [4] [5] [→]                  │
└─────────────────────────────────────────────────────────────┘
```

### Adding Elements

1. **Nodes**: Drag from "Nodos Disponibles" section in Toolbox
2. **Connectors**: Drag from "Conectores" section
3. **Position**: Drop onto any grid cell

### Connecting Nodes

1. Click first node (selected = highlighted)
2. Hold `Shift` and click second node
3. An edge is created between them

### Node Properties

Click a node to edit:
- **Name**: Display name
- **Description**: Tooltip text
- **Cost**: Point cost
- **Icon**: Visual icon
- **Effect Type**: What this node does
- **Effect Value**: Numeric bonus
- **Requires**: Prerequisites
- **Exclusive With**: Blocked alternatives

### Pagination

- Use number input in toolbar to set total pages
- Navigate with ← → buttons or click page numbers

### Export

Click "Export YML" to download the configuration file. Place it in `trees/` folder on your Minecraft server.

## Asset System

### Node Icons

Place PNG files in:
```
editor/public/SFSkilltree/assets/nodes/
```

Filename (without .png) becomes the node ID.

### Connectors

Place PNG files in:
```
editor/public/SFSkilltree/assets/connectors/
```

Connector states:
- `_on.png` - Active/connected
- `_off.png` - Inactive/unconnected

## Configuration Files

### Exported YAML Structure

```yaml
display-name: "⚔ Espada"
skill-type: "SWORD"
max-tier: 3

nodes:
  sword_1a:
    name: "Corte Básico"
    description: "+5% damage with sword"
    cost: 1
    tier: 1
    gui-slot: 19
    arrows:
      20: 2004    # slot: customModelData
    requires: []
    exclusive-with: []
    effect-type: damage_bonus
    effect-value: 0.05
```

## Technology Stack

- **React 18**: UI framework
- **Vite**: Build tool and dev server
- **@dnd-kit**: Drag and drop functionality
- **CSS Variables**: Theming system

## Project Structure

```
editor/
├── public/
│   └── SFSkilltree/
│       └── assets/        # Images for nodes/connectors
├── src/
│   ├── components/        # React components
│   │   ├── Grid.jsx      # Main editor grid
│   │   ├── Toolbox.jsx   # Draggable items
│   │   ├── Toolbar.jsx    # Top controls
│   │   └── ...
│   ├── hooks/
│   │   └── useTreeState.js   # State management
│   ├── utils/
│   │   ├── constants.js      # Grid size, connectors
│   │   └── exporter.js       # YAML export
│   └── App.jsx           # Root component
├── vite.config.js         # Vite configuration
└── package.json
```

## Integration with Minecraft Plugin

1. Export your tree from the editor
2. Place the YAML file in `plugins/SFSkilltree/trees/`
3. Add icons to `SFResourcePack` if using custom ones
4. Restart or reload the plugin

## Customization

### Theme Colors

Edit `src/index.css` to change colors:
```css
:root {
  --bg-dark: #1a1209;
  --bg-panel: #241a10;
  --border-highlight: #c8a03c;
  --text-gold: #c8a03c;
  /* ... */
}
```

### Grid Size

Modify `src/utils/constants.js`:
```javascript
export const GRID_COLS = 9;
export const GRID_ROWS = 6;
```

## Troubleshooting

### Images Not Loading
- Check browser console for 404 errors
- Verify images are in correct folders
- Ensure folder names match (nodes, connectors)

### Drag Not Working
- Ensure click is on the element
- Check browser console for JavaScript errors

### Export Invalid
- Validate YAML syntax
- Check required fields are present

## License

MIT License

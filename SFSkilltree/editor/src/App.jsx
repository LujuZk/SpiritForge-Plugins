import React, { useState } from 'react';
import { DndContext, pointerWithin, DragOverlay } from '@dnd-kit/core';
import { useTreeState } from './hooks/useTreeState';
import { exportToYaml } from './utils/exporter';

// Skeleton imports - we will create these next
import Toolbar from './components/Toolbar';
import Toolbox from './components/Toolbox';
import Grid from './components/Grid';
import NodePropertiesPanel from './components/NodePropertiesPanel';
import DragOverlayContent from './components/DragOverlayContent';

function App() {
  const treeState = useTreeState();
  const [activeData, setActiveData] = useState(null);

  const handleDragStart = (event) => {
    const { active } = event;
    setActiveData(active.data.current);
  };

  const handleDragCancel = () => {
    setActiveData(null);
  };

  const handleDragEnd = (event) => {
    setActiveData(null);
    const { active, over } = event;

    // Default drag handlers logic placeholder (we'll implement this fully later)
    if (!over) return;

    // Parse drop target
    if (over.id.startsWith('cell-')) {
      const parts = over.id.replace('cell-', '').split(',');
      const currentPage = parseInt(parts[0], 10);
      const col = parseInt(parts[1], 10);
      const row = parseInt(parts[2], 10);

      // If dragged from toolbox
      if (active.data.current?.fromToolbox) {
        const type = active.data.current.type;
        if (type === 'node') {
          const iconId = active.data.current.iconId ?? '';
          treeState.placeNewNode(col, row, iconId);
        } else if (type === 'connector') {
          const connectorId = active.id.replace('toolbox-', '');
          const connectorPng = active.data.current.connectorPng || null;
          treeState.placeConnector(col, row, connectorId, connectorPng);
        }
      }
      // If dragged from grid (move/swap)
      else if (active.id.toString().startsWith('cell-')) {
        const fromParts = active.id.replace('cell-', '').split(',');
        const fromCol = parseInt(fromParts[1], 10);
        const fromRow = parseInt(fromParts[2], 10);
        treeState.moveCell(fromCol, fromRow, col, row);
      }
    }
  };

  return (
    <div className="app-container">
      <Toolbar
        treeState={treeState}
        onExport={() => exportToYaml(treeState.treeContext, treeState.paths)}
      />

      <DndContext
        collisionDetection={pointerWithin}
        onDragStart={handleDragStart}
        onDragCancel={handleDragCancel}
        onDragEnd={handleDragEnd}
      >
        <div className="main-content">
          <Toolbox treeState={treeState} />

          <div className="center-workspace">
            <Grid treeState={treeState} />
          </div>

          <NodePropertiesPanel treeState={treeState} />
        </div>

        <DragOverlay dropAnimation={null}>
          <DragOverlayContent activeData={activeData} />
        </DragOverlay>
      </DndContext>
    </div>
  );
}

export default App;

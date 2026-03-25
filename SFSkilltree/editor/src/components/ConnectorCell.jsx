import React from 'react';
import { useDraggable } from '@dnd-kit/core';

function ConnectorCell({ id, content, isSelected, onClick, onContextMenu }) {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id,
        data: { type: 'connector', content },
        activationConstraint: {
            distance: 8
        }
    });

    const baseId = String(content.connectorId || '').replace(/_(on|off)$/i, '');
    const offUrl = `/assets/connectors/${baseId}.png`;
    const legacyOffUrl = `/assets/connectors/${baseId}_off.png`;
    const baseUrl = `/assets/connectors/${baseId}.png`;
    const imageUrl = content.connectorPng
        ? `url("${content.connectorPng}")`
        : `url("${offUrl}"), url("${legacyOffUrl}"), url("${baseUrl}")`;

    const style = {
        width: '100%',
        height: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        cursor: 'grab',
        backgroundColor: 'transparent',
        border: isSelected ? '1px dashed var(--border-selected)' : 'none',
        opacity: isDragging ? 0.3 : 1,
        backgroundImage: imageUrl,
        backgroundSize: 'contain',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
    };

    const handleClick = (e) => {
        e.stopPropagation();
        if (onClick) onClick(e);
    };

    return (
        <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            style={style}
            onClick={handleClick}
            onContextMenu={onContextMenu}
            title={content.connectorId}
        />
    );
}

export default ConnectorCell;

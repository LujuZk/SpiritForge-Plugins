import React from 'react';
import { useDraggable } from '@dnd-kit/core';

function NodeCell({ id, content, isSelected, isExclusive, onClick, onContextMenu, skillId }) {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id,
        data: { type: 'node', content, skillId },
        activationConstraint: {
            distance: 8
        }
    });

    const safeSkillId = (skillId || 'skill').toLowerCase();
    const skillUrl = content.iconPng
        ? `url("${content.iconPng}")`
        : (content.iconId
            ? `url("/assets/skills/${safeSkillId}/${content.iconId}.png"), url("/assets/skills/${content.iconId}.png"), url("/assets/nodes/${content.iconId}.png")`
            : null);
    const frameUrl = `url("/assets/nodes/node_locked.png")`;
    const backgroundImage = skillUrl ? `${frameUrl}, ${skillUrl}` : frameUrl;

    const style = {
        width: '100%',
        height: '100%',
        backgroundColor: 'transparent',
        border: `2px solid ${isSelected ? 'var(--border-selected)' : 'var(--border-highlight)'}`,
        borderRadius: '8px',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        cursor: 'grab',
        position: 'relative',
        overflow: 'hidden',
        opacity: isDragging ? 0.3 : 1,
        backgroundImage: backgroundImage,
        backgroundSize: 'contain',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
    };

    const handleClick = (e) => {
        e.stopPropagation();
        if (onClick) onClick(e);
    };

    const hasExclusives = (content.exclusiveWith || []).length > 0;

    return (
        <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            style={style}
            onClick={handleClick}
            onContextMenu={onContextMenu}
            title={content.name || content.nodeId}
        >
            {hasExclusives && (
                <div style={{
                    position: 'absolute', bottom: '2px', right: '2px',
                    width: '12px', height: '12px',
                    backgroundColor: '#cc2222',
                    borderRadius: '50%',
                    border: '1px solid #ff6666',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '8px', color: '#fff', fontWeight: 'bold',
                    pointerEvents: 'none', zIndex: 5,
                    lineHeight: 1,
                }}>
                    ✕
                </div>
            )}
        </div>
    );
}

export default NodeCell;

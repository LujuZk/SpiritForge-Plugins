import React from 'react';

function DragOverlayContent({ activeData }) {
    if (!activeData) return null;

    if (activeData.type === 'node') {
        const iconId = activeData.iconId || activeData.content?.iconId;
        const iconPng = activeData.iconPng || activeData.content?.iconPng;
        const skillId = (activeData.skillId || activeData.content?.skillId || 'skill').toLowerCase();
        const skillUrl = iconPng
            ? `url("${iconPng}")`
            : (iconId
                ? `url("/assets/skills/${skillId}/${iconId}.png"), url("/assets/skills/${iconId}.png"), url("/assets/nodes/${iconId}.png")`
                : null);
        const frameUrl = `url("/assets/nodes/node_locked.png")`;
        const imageUrl = skillUrl ? `${frameUrl}, ${skillUrl}` : frameUrl;

        return (
            <div style={{
                width: 'var(--grid-cell-size)',
                height: 'var(--grid-cell-size)',
                backgroundColor: 'transparent',
                border: '2px solid var(--border-selected)',
                borderRadius: '8px',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                cursor: 'grabbing',
                position: 'relative',
                boxShadow: '0 8px 16px rgba(0,0,0,0.6)',
                backgroundImage: imageUrl,
                backgroundSize: 'contain',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
            }}>
            </div>
        );
    }

    if (activeData.type === 'connector') {
        const connectorId = activeData.connectorId || activeData.content?.connectorId;
        const connectorPng = activeData.connectorPng || activeData.content?.connectorPng;
        const baseId = String(connectorId || '').replace(/_(on|off)$/i, '');
        const offUrl = `/assets/connectors/${baseId}.png`;
        const legacyOffUrl = `/assets/connectors/${baseId}_off.png`;
        const baseUrl = `/assets/connectors/${baseId}.png`;
        const imageUrl = connectorPng
            ? `url("${connectorPng}")`
            : `url("${offUrl}"), url("${legacyOffUrl}"), url("${baseUrl}")`;

        return (
            <div style={{
                width: 'var(--grid-cell-size)',
                height: 'var(--grid-cell-size)',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                cursor: 'grabbing',
                backgroundColor: 'transparent',
                border: '1px dashed var(--border-highlight)',
                boxShadow: '0 8px 16px rgba(0,0,0,0.6)',
                backgroundImage: imageUrl,
                backgroundSize: 'contain',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat'
            }}>
            </div>
        );
    }

    return null;
}

export default DragOverlayContent;

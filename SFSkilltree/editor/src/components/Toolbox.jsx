import React from 'react';
import { useDraggable } from '@dnd-kit/core';

function ToolboxNodeItem({ iconId, imageUrl, skillId, isBase }) {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id: `toolbox-node-${iconId}`,
        data: { type: 'node', fromToolbox: true, iconId, skillId }
    });

    const safeSkillId = (skillId || 'skill').toLowerCase();
    const skillUrl = isBase
        ? null
            : (imageUrl
            ? `url("${imageUrl}")`
            : `url("/assets/skills/${safeSkillId}/${iconId}.png"), url("/assets/skills/${iconId}.png"), url("/assets/nodes/${iconId}.png")`);
    const frameUrl = `url("/assets/nodes/node_locked.png")`;
    const finalImageUrl = skillUrl ? `${frameUrl}, ${skillUrl}` : frameUrl;

    return (
        <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            title={iconId}
            style={{
                opacity: isDragging ? 0.3 : 1,
                width: 'var(--grid-cell-size)',
                height: 'var(--grid-cell-size)',
                backgroundColor: 'transparent',
                border: '2px solid var(--border-highlight)',
                borderRadius: '8px',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                cursor: 'grab',
                backgroundImage: finalImageUrl,
                backgroundSize: 'contain',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
                overflow: 'hidden'
            }}
        />
    );
}

function ToolboxConnectorItem({ connectorId, imageUrl }) {
    const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
        id: `toolbox-${connectorId}`,
        data: { type: 'connector', fromToolbox: true, connectorId, connectorPng: imageUrl || null }
    });

    const baseId = String(connectorId || '').replace(/_(on|off)$/i, '');
    const offUrl = `/assets/connectors/${baseId}.png`;
    const legacyOffUrl = `/assets/connectors/${baseId}_off.png`;
    const baseUrl = `/assets/connectors/${baseId}.png`;
    const finalImageUrl = imageUrl
        ? `url("${imageUrl}")`
        : `url("${offUrl}"), url("${legacyOffUrl}"), url("${baseUrl}")`;

    return (
        <div
            ref={setNodeRef}
            {...listeners}
            {...attributes}
            style={{
                opacity: isDragging ? 0.3 : 1,
                width: '50px',
                height: '50px',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                cursor: 'grab',
                backgroundColor: 'transparent',
                border: '1px dashed var(--border-color)',
                borderRadius: '4px',
                backgroundImage: finalImageUrl,
                backgroundSize: 'contain',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat'
            }}
            title={connectorId}
        />
    );
}

function Toolbox({ treeState }) {
    const { availableAssets, customIconPngs, customConnectorPngs, id: skillId } = treeState.treeContext;
    const { registerCustomConnector } = treeState;

    // Get connectors from assets - remove .png extension only
    const connectorIds = (availableAssets?.connectors && availableAssets.connectors.length > 0)
        ? availableAssets.connectors.map(c => c.replace('.png', ''))
        : [];

    return (
        <div className="panel" style={{
            width: '320px',
            display: 'flex',
            flexDirection: 'column',
            borderRight: '1px solid var(--border-color)',
            overflowY: 'auto'
        }}>
            <div style={{ padding: '16px', borderBottom: '1px solid var(--border-color)' }}>
                <h3 className="text-gold" style={{ marginBottom: '16px' }}>Nodos Disponibles</h3>
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    gap: '8px',
                    justifyItems: 'center'
                }}>
                    <ToolboxNodeItem
                        key="node-off"
                        iconId=""
                        imageUrl={null}
                        skillId={skillId}
                        isBase
                    />
                </div>
            </div>

            <div style={{ padding: '16px', flex: 1 }}>
                <h3 className="text-gold" style={{ marginBottom: '16px' }}>Conectores</h3>
                {!treeState.treeContext.importedIconsYaml && (
                    <div style={{ fontSize: '12px', color: 'var(--text-dim)', marginBottom: '8px' }}>
                        ImportÃ¡ icons.yml para cargar los conectores.
                    </div>
                )}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    gap: '12px',
                    justifyItems: 'center'
                }}>
                    {connectorIds.map(id => (
                        <ToolboxConnectorItem
                            key={id}
                            connectorId={id}
                            imageUrl={customConnectorPngs?.[id]}
                        />
                    ))}
                </div>

                <div style={{ marginTop: '16px', fontSize: '12px', color: 'var(--text-dim)' }}>
                    Agregar conector (PNG OFF):
                </div>
                <input
                    type="file"
                    accept="image/png"
                    onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (!file) return;
                        
                        const baseId = (file.name || '').replace(/\.png$/i, '').trim();
                        if (!baseId) return;
                        
                        const reader = new FileReader();
                        reader.onload = (event) => {
                            registerCustomConnector(baseId, event.target.result);
                        };
                        reader.readAsDataURL(file);
                        e.target.value = '';
                    }}
                />
            </div>
        </div>
    );
}

export default Toolbox;



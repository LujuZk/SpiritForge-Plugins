import React from 'react';

function NodePropertiesPanel({ treeState }) {
    const { treeContext, selectedCellFormat, updateNodeProperties, updateCellProperties, registerCustomIcon, removeCell, removeEdge, toggleExclusiveWith, currentPage } = treeState;

    if (!selectedCellFormat) return null;

    const parts = selectedCellFormat.split(',');
    if (parts.length !== 3) return null;

    const [page, col, row] = parts.map(Number);
    const content = treeContext.cells[`${page},${col},${row}`];
    if (!content) return null;

    const isNode = content.type === 'node';
    const isConnector = content.type === 'connector';
    if (!isNode && !isConnector) return null;

    const sourceEdges = isNode ? treeContext.edges.filter(e => e.from === content.nodeId) : [];
    const targetEdges = isNode ? treeContext.edges.filter(e => e.to === content.nodeId) : [];

    const handleChange = (field, value) => {
        updateNodeProperties(page, col, row, { [field]: value });
    };

    const handleArrayChange = (field, value) => {
        const arr = value.split(',').map(s => s.trim()).filter(Boolean);
        handleChange(field, arr);
    };

    const safeSkillId = (treeContext.id || 'skill').toLowerCase();
    const iconIds = (treeContext.availableAssets?.nodes || []).filter(Boolean).sort();
    const frameUrl = `url("/assets/nodes/node_locked.png")`;

    const getSkillIconLayer = (iconId) => {
        if (!iconId) return null;
        const customPng = treeContext.customIconPngs?.[iconId];
        if (customPng) return `url("${customPng}")`;
        return `url("/assets/skills/${safeSkillId}/${iconId}.png"), url("/assets/skills/${iconId}.png"), url("/assets/nodes/${iconId}.png")`;
    };

    const handleIconUpload = (file) => {
        if (!file) return;
        
        const fileName = file.name || '';
        const baseId = fileName.replace(/\.png$/i, '').trim();
        if (!baseId) return;
        
        const reader = new FileReader();
        reader.onload = (event) => {
            const dataUrl = event.target.result;
            updateNodeProperties(page, col, row, { iconId: baseId, iconPng: dataUrl });
            registerCustomIcon(baseId, dataUrl);
        };
        reader.readAsDataURL(file);
    };

    return (
        <div className="panel" style={{
            width: '320px',
            display: 'flex',
            flexDirection: 'column',
            borderLeft: '1px solid var(--border-color)',
            overflowY: 'auto'
        }}>
            <div style={{ padding: '16px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 className="text-gold">{isNode ? 'Node Properties' : 'Connector Properties'}</h3>
                <span className="mono text-dim">[Page {page + 1}, {col}, {row}]</span>
            </div>

            <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px', flex: 1 }}>

                {isNode && (
                    <>
                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Node ID</label>
                            <input
                                className="input"
                                value={content.nodeId}
                                onChange={e => handleChange('nodeId', e.target.value)}
                                disabled
                            />
                            <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '4px' }}>
                                ID is auto-generated based on initial position.
                            </div>
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Name</label>
                            <input
                                className="input"
                                value={content.name}
                                onChange={e => handleChange('name', e.target.value)}
                            />
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Description</label>
                            <textarea
                                className="input"
                                rows={3}
                                value={content.description}
                                onChange={e => handleChange('description', e.target.value)}
                                style={{ resize: 'vertical' }}
                            />
                        </div>

                        <div style={{ display: 'flex', gap: '8px' }}>
                            <div style={{ flex: 1 }}>
                                <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Cost</label>
                                <input
                                    type="number"
                                    className="input"
                                    value={content.cost}
                                    onChange={e => handleChange('cost', Number(e.target.value))}
                                />
                            </div>
                            <div style={{ flex: 1 }}>
                                <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Icon ID</label>
                                <select
                                    className="input"
                                    value={content.iconId || ''}
                                    onChange={e => updateNodeProperties(page, col, row, { iconId: e.target.value, iconPng: null })}
                                >
                                    <option value="">-- Default --</option>
                                    {(treeContext.availableAssets?.nodes || []).map(icon => (
                                        <option key={icon} value={icon}>{icon}</option>
                                    ))}
                                    {!((treeContext.availableAssets?.nodes || []).includes(content.iconId)) && content.iconId && (
                                        <option value={content.iconId}>{content.iconId}</option>
                                    )}
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '6px' }}>Skill Icons</label>
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(5, 1fr)',
                                gap: '6px'
                            }}>
                                <button
                                    className="button"
                                    onClick={() => updateNodeProperties(page, col, row, { iconId: '', iconPng: null })}
                                    style={{
                                        width: '48px',
                                        height: '48px',
                                        padding: 0,
                                        backgroundColor: content.iconId ? 'var(--bg-panel-light)' : 'var(--border-highlight)',
                                        borderRadius: '6px',
                                        border: content.iconId ? '1px solid var(--border-color)' : '2px solid var(--text-gold)'
                                    }}
                                    title="Sin icono"
                                >
                                    Off
                                </button>
                                {iconIds.map(iconId => {
                                    const skillLayer = getSkillIconLayer(iconId);
                                    const bg = skillLayer ? `${frameUrl}, ${skillLayer}` : frameUrl;
                                    const isActive = content.iconId === iconId && !content.iconPng;
                                    return (
                                        <button
                                            key={iconId}
                                            className="button"
                                            onClick={() => updateNodeProperties(page, col, row, { iconId, iconPng: null })}
                                            title={iconId}
                                            style={{
                                                width: '48px',
                                                height: '48px',
                                                padding: 0,
                                                backgroundColor: isActive ? 'var(--border-highlight)' : 'var(--bg-panel-light)',
                                                borderRadius: '6px',
                                                border: isActive ? '2px solid var(--text-gold)' : '1px solid var(--border-color)',
                                                backgroundImage: bg,
                                                backgroundSize: 'contain',
                                                backgroundPosition: 'center',
                                                backgroundRepeat: 'no-repeat'
                                            }}
                                        />
                                    );
                                })}
                            </div>
                            <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '6px' }}>
                                Para ver miniaturas, guardá los PNG en editor/public/assets/skills/{safeSkillId} o editor/public/assets/skills/[iconId].png.
                            </div>
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '6px' }}>Skill Icon (PNG)</label>
                            <input
                                type="file"
                                accept="image/png"
                                onChange={e => handleIconUpload(e.target.files?.[0])}
                            />
                            {content.iconPng && (
                                <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <img src={content.iconPng} alt="icon preview" style={{ width: '32px', height: '32px', border: '1px solid var(--border-color)' }} />
                                    <button
                                        className="button"
                                        onClick={() => handleChange('iconPng', null)}
                                        style={{ backgroundColor: 'var(--bg-panel-light)' }}
                                    >
                                        Remove Custom Icon
                                    </button>
                                </div>
                            )}
                            <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '6px' }}>
                                El PNG se asocia por nombre de archivo (ID).
                            </div>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <div>
                                <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Effect Type</label>
                                <select
                                    className="input"
                                    style={{ width: '100%', cursor: 'pointer' }}
                                    value={content.effectType}
                                    onChange={e => handleChange('effectType', e.target.value)}
                                >
                                    <option value="none">— Ninguno —</option>
                                    <optgroup label="Atributos">
                                        <option value="damage_bonus">damage_bonus — Daño extra (%)</option>
                                        <option value="damage_reduction">damage_reduction — Reducción de daño</option>
                                        <option value="max_health">max_health — Salud máxima</option>
                                        <option value="movement_speed">movement_speed — Velocidad (%)</option>
                                    </optgroup>
                                    <optgroup label="Listeners (activos)">
                                        <option value="lifesteal">lifesteal — Robo de vida (%)</option>
                                        <option value="mining_speed">mining_speed — Velocidad minería (Haste lvl)</option>
                                    </optgroup>
                                    <optgroup label="Listeners (pendientes)">
                                        <option value="bleed">bleed</option>
                                        <option value="stun">stun</option>
                                        <option value="armor_pierce">armor_pierce</option>
                                        <option value="execute">execute</option>
                                        <option value="regen_on_hit">regen_on_hit</option>
                                        <option value="kill_streak_damage">kill_streak_damage</option>
                                        <option value="counterattack">counterattack</option>
                                        <option value="frenzy">frenzy</option>
                                        <option value="berserker_damage">berserker_damage</option>
                                        <option value="area_damage">area_damage</option>
                                        <option value="opener_damage">opener_damage</option>
                                        <option value="global_multiplier">global_multiplier</option>
                                        <option value="path_amplifier">path_amplifier</option>
                                        <option value="vein_miner">vein_miner</option>
                                        <option value="double_drop">double_drop</option>
                                        <option value="fortune_bonus">fortune_bonus</option>
                                        <option value="auto_pickup">auto_pickup</option>
                                        <option value="explosion">explosion</option>
                                        <option value="ore_detection">ore_detection</option>
                                    </optgroup>
                                </select>
                            </div>

                            <div>
                                <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>
                                    Effect Value
                                </label>
                                <input
                                    type="number"
                                    step="0.01"
                                    className="input"
                                    style={{ width: '100%' }}
                                    value={Number(content.effectValue) || 0}
                                    onChange={e => handleChange('effectValue', Number(e.target.value))}
                                    disabled={content.effectType === 'none'}
                                />
                                <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '4px' }}>
                                    {content.effectType === 'damage_bonus' || content.effectType === 'movement_speed'
                                        ? 'Escalar multiplicativo (ej: 0.10 = +10%)'
                                        : content.effectType === 'none'
                                        ? 'Sin efecto'
                                        : 'Valor absoluto (ej: 2.0)'}
                                </div>
                            </div>
                        </div>

                        <div style={{ borderTop: '1px solid var(--border-color)', margin: '8px 0' }} />

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Requires (comma sep)</label>
                            <input
                                className="input"
                                value={Array.isArray(content.requires) ? content.requires.join(', ') : (content.requires || '')}
                                onChange={e => handleArrayChange('requires', e.target.value)}
                            />
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '-8px' }}>
                            <input
                                type="checkbox"
                                checked={!!content.requiresAll}
                                onChange={e => handleChange('requiresAll', e.target.checked)}
                                id="reqAll"
                            />
                            <label htmlFor="reqAll" className="text-dim">Requires All</label>
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '6px' }}>Exclusive With</label>
                            {(() => {
                                const otherNodes = Object.values(treeContext.cells)
                                    .filter(c => c.type === 'node' && c.nodeId !== content.nodeId)
                                    .sort((a, b) => (a.name || a.nodeId).localeCompare(b.name || b.nodeId));
                                if (otherNodes.length === 0) {
                                    return (
                                        <div style={{ fontSize: '12px', color: 'var(--text-dim)', fontStyle: 'italic' }}>
                                            No hay otros nodos en la grilla.
                                        </div>
                                    );
                                }
                                return (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', maxHeight: '160px', overflowY: 'auto' }}>
                                        {otherNodes.map(node => {
                                            const isChecked = (content.exclusiveWith || []).includes(node.nodeId);
                                            return (
                                                <label key={node.nodeId} style={{
                                                    display: 'flex', alignItems: 'center', gap: '8px',
                                                    padding: '5px 8px', borderRadius: '4px', cursor: 'pointer',
                                                    backgroundColor: isChecked ? 'rgba(255,68,68,0.1)' : 'var(--bg-panel-light)',
                                                    border: isChecked ? '1px solid rgba(255,68,68,0.45)' : '1px solid var(--border-color)',
                                                    fontSize: '12px'
                                                }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={isChecked}
                                                        onChange={() => toggleExclusiveWith(page, col, row, node.nodeId)}
                                                    />
                                                    <span style={{ color: isChecked ? '#ff7777' : 'var(--text-main)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                        {node.name || node.nodeId}
                                                    </span>
                                                    <span style={{ color: 'var(--text-dim)', fontSize: '10px', flexShrink: 0 }}>
                                                        {node.nodeId}
                                                    </span>
                                                </label>
                                            );
                                        })}
                                    </div>
                                );
                            })()}
                        </div>

                        <div style={{ borderTop: '1px solid var(--border-color)', margin: '8px 0' }} />

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '8px' }}>Logical Edges</label>
                            {sourceEdges.length === 0 && targetEdges.length === 0 && (
                                <div style={{ fontSize: '12px', color: 'var(--text-dim)', fontStyle: 'italic' }}>
                                    No edges. Shift+click another node to connect.
                                </div>
                            )}

                            {sourceEdges.map(edge => (
                                <div key={`s-${edge.to}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: 'var(--bg-panel-light)', padding: '4px 8px', marginBottom: '4px', borderRadius: '4px', fontSize: '12px' }}>
                                    <span>-> to: {edge.to}</span>
                                    <button
                                        onClick={() => removeEdge(edge.from, edge.to)}
                                        style={{ background: 'none', border: 'none', color: '#ff5555', cursor: 'pointer' }}>
                                        x
                                    </button>
                                </div>
                            ))}

                            {targetEdges.map(edge => (
                                <div key={`t-${edge.from}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: 'var(--bg-panel-light)', padding: '4px 8px', marginBottom: '4px', borderRadius: '4px', fontSize: '12px' }}>
                                    <span>&lt;- from: {edge.from}</span>
                                    <button
                                        onClick={() => removeEdge(edge.from, edge.to)}
                                        style={{ background: 'none', border: 'none', color: '#ff5555', cursor: 'pointer' }}>
                                        x
                                    </button>
                                </div>
                            ))}
                        </div>
                    </>
                )}

                {isConnector && (
                    <>
                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Icon ID</label>
                            <select
                                className="input"
                                value={content.connectorId}
                                onChange={e => {
                                    const baseId = e.target.value;
                                    const connectorPng = treeContext.customConnectorPngs?.[baseId] || null;
                                    updateCellProperties(page, col, row, { connectorId: baseId, connectorPng });
                                }}
                            >
                                {((treeContext.availableAssets && treeContext.availableAssets.connectors) || []).map(icon => (
                                    <option key={icon} value={icon}>{icon}</option>
                                ))}
                                {!((treeContext.availableAssets && treeContext.availableAssets.connectors) || []).includes(content.connectorId) && (
                                    <option value={content.connectorId}>{content.connectorId}</option>
                                )}
                            </select>
                            <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '4px' }}>
                                Se listan conectores base (OFF). El ON se genera al exportar.
                            </div>
                        </div>

                        <div>
                            <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Logical Dependencies</label>
                            <div style={{ fontSize: '12px', color: 'var(--text-dim)', fontStyle: 'italic' }}>
                                Dependencies are auto-detected by scanning the horizontal axis during export.
                            </div>
                        </div>
                    </>
                )}

            </div>

            <div style={{ padding: '16px', borderTop: '1px solid var(--border-color)' }}>
                <button
                    className="button danger"
                    style={{ width: '100%' }}
                    onClick={() => removeCell(page, col, row)}
                >
                    Delete {isNode ? 'Node' : 'Connector'}
                </button>
            </div>
        </div>
    );
}

export default NodePropertiesPanel;




import React, { useRef } from 'react';
import { SKILL_TYPES } from '../utils/constants';
import { importFromYaml } from '../utils/exporter';
import { exportToZip } from '../utils/zipExporter';

function Toolbar({ treeState, onExport }) {
    const { treeContext, updateTreeMetadata, setTreeContext, totalPages, changeTotalPages, loadPaths, paths } = treeState;
    const fileInputRef = useRef(null);
    const iconsInputRef = useRef(null);
    const configInputRef = useRef(null);

    const handleImport = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event) => {
            try {
                const yamlString = event.target.result;
                if (file.name === 'icons.yml') {
                    const success = treeState.importIcons(yamlString);
                    if (success) alert("Icons.yml importado correctamente");
                } else if (file.name === 'config.yml') {
                    const success = treeState.importConfig(yamlString);
                    if (success) alert("Config.yml importado correctamente");
                } else {
                    const { newContext, maxTier, paths } = importFromYaml(yamlString);
                    setTreeContext(prev => ({
                        ...newContext,
                        availableAssets: prev.availableAssets,
                        customIcons: prev.customIcons,
                        customIconPngs: prev.customIconPngs,
                        customConnectorPngs: prev.customConnectorPngs,
                        importedIconsYaml: prev.importedIconsYaml,
                        connectorMapping: prev.connectorMapping,
                        importedConfigYaml: prev.importedConfigYaml
                    }));
                    changeTotalPages(maxTier);
                    if (paths && paths.length > 0) loadPaths(paths);
                }
            } catch (err) {
                alert("Error importing file: " + err.message);
            }
        };
        reader.readAsText(file);
        e.target.value = null;
    };

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 24px',
            backgroundColor: 'var(--bg-panel)',
            borderBottom: '1px solid var(--border-color)'
        }}>
            <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                <div style={{ fontWeight: 'bold', fontSize: '18px', color: 'var(--text-gold)' }}>
                    SF Skill Tree Editor
                </div>

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <label className="text-dim">ID:</label>
                    <input
                        type="text"
                        className="input"
                        value={treeContext.id}
                        onChange={(e) => updateTreeMetadata('id', e.target.value)}
                        style={{ width: '120px' }}
                    />
                </div>

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <label className="text-dim">Display Name:</label>
                    <input
                        type="text"
                        className="input"
                        value={treeContext.displayName}
                        onChange={(e) => updateTreeMetadata('displayName', e.target.value)}
                        style={{ width: '160px' }}
                    />
                </div>

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <label className="text-dim">Type:</label>
                    <select
                        className="select"
                        value={treeContext.skillType}
                        onChange={(e) => updateTreeMetadata('skillType', e.target.value)}
                    >
                        {SKILL_TYPES.map(type => (
                            <option key={type} value={type}>{type}</option>
                        ))}
                    </select>
                </div>

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <label className="text-dim">Páginas:</label>
                    <input
                        type="number"
                        className="input"
                        value={totalPages}
                        onChange={(e) => changeTotalPages(e.target.value)}
                        min="1"
                        style={{ width: '60px' }}
                    />
                </div>
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
                <input
                    type="file"
                    accept=".yml,.yaml"
                    style={{ display: 'none' }}
                    ref={fileInputRef}
                    onChange={handleImport}
                />
                <button className="button" onClick={() => fileInputRef.current.click()} style={{ backgroundColor: 'var(--bg-panel-light)' }}>
                    Import Tree.yml
                </button>
                <input
                    type="file"
                    accept=".yml,.yaml"
                    style={{ display: 'none' }}
                    ref={iconsInputRef}
                    onChange={handleImport}
                />
                <button className="button" onClick={() => iconsInputRef.current.click()} style={{ backgroundColor: '#4a3b2c', color: 'var(--text-gold)' }}>
                    Import Icons.yml
                </button>
                <input
                    type="file"
                    accept=".yml,.yaml"
                    style={{ display: 'none' }}
                    ref={configInputRef}
                    onChange={handleImport}
                />
                <button className="button" onClick={() => configInputRef.current.click()} style={{ backgroundColor: '#3a2a18', color: 'var(--text-gold)' }}>
                    Import Config.yml
                </button>
                <button className="button" onClick={onExport} style={{ backgroundColor: 'var(--border-highlight)' }}>
                    Export YML
                </button>
                <button className="button" onClick={() => exportToZip({ ...treeContext, _paths: paths }, { mode: 'repo' })} style={{ backgroundColor: '#2e8b57' }}>
                    Export ZIP (Repo)
                </button>
            </div>
        </div>
    );
}

export default Toolbar;

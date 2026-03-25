const fs = require('fs');

const panelPath = 'src/components/NodePropertiesPanel.jsx';
let content = fs.readFileSync(panelPath, 'utf8');

// Match from "<div style={{ display: 'flex', gap: '8px' }}>" to the end of the div of Effect Value
const regex = /<div style=\{\{ display: 'flex', gap: '8px' \}\}>\s*<div style=\{\{ flex: 1 \}\}>\s*<label className="text-dim" style=\{\{ display: 'block', marginBottom: '4px' \}\}>Effect Type<\/label>\s*<input\s*className="input"\s*value=\{content\.effectType\}\s*onChange=\{e => handleChange\('effectType', e\.target\.value\)\}\s*\/>\s*<\/div>\s*<div style=\{\{ flex: 1 \}\}>\s*<label className="text-dim" style=\{\{ display: 'block', marginBottom: '4px' \}\}>Effect Value<\/label>\s*<input\s*type="number"\s*step="0\.01"\s*className="input"\s*value=\{content\.effectValue\}\s*onChange=\{e => handleChange\('effectValue', Number\(e\.target\.value\)\)\}\s*\/>\s*<\/div>\s*<\/div>/g;

const replaceStr = `<div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <div>
                                <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Effect Type</label>
                                <select
                                    className="input"
                                    style={{ width: '100%', cursor: 'pointer' }}
                                    value={content.effectType}
                                    onChange={e => handleChange('effectType', e.target.value)}
                                >
                                    <option value="damage_bonus">Daño Extra</option>
                                    <option value="speed_bonus">Velocidad</option>
                                    <option value="health_bonus">Salud Máxima</option>
                                    <option value="command">Ejecutar Comando</option>
                                </select>
                            </div>
                            
                            {content.effectType === 'command' ? (
                                <div>
                                    <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>Comando a ejecutar</label>
                                    <textarea
                                        className="input"
                                        rows={2}
                                        placeholder="/give %player% diamond 1"
                                        value={typeof content.effectValue === 'string' ? content.effectValue : ''}
                                        onChange={e => handleChange('effectValue', e.target.value)}
                                        style={{ resize: 'vertical' }}
                                    />
                                    <div style={{ fontSize: '10px', color: 'var(--text-dim)', marginTop: '4px' }}>
                                        Use %player% para el nombre del jugador.
                                    </div>
                                </div>
                            ) : (
                                <div>
                                    <label className="text-dim" style={{ display: 'block', marginBottom: '4px' }}>
                                        {content.effectType === 'damage_bonus' ? 'Aumento % (ej: 0.10)' : 'Valor (ej: 2.0)'}
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="input"
                                        style={{ width: '100%' }}
                                        value={Number(content.effectValue) || 0}
                                        onChange={e => handleChange('effectValue', Number(e.target.value))}
                                    />
                                </div>
                            )}
                        </div>`;

if (regex.test(content)) {
    console.log("Found match, replacing...");
    content = content.replace(regex, replaceStr);
    fs.writeFileSync(panelPath, content, 'utf8');
    console.log("Done.");
} else {
    console.log("No match found!");
}

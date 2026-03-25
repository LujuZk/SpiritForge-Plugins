const fs = require('fs');
const jsyaml = require('js-yaml');
const { importFromYaml } = require('./src/utils/exporter.js');

try {
    const yamlString = fs.readFileSync('../src/main/resources/skills/sword/tree.yml', 'utf8');
    const result = importFromYaml(yamlString);
    console.log("Import Result:", JSON.stringify(result, null, 2));
} catch (e) {
    console.error(e);
}

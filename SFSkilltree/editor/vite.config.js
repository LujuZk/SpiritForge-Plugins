import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'fs-extra';
import path from 'path';

// Plugin to serve available assets during development
const assetScannerPlugin = () => ({
  name: 'asset-scanner',
  configureServer(server) {
    server.middlewares.use('/api/assets', async (req, res) => {
      try {
        const publicDir = path.resolve(__dirname, 'public/assets');
        const nodesDir = path.join(publicDir, 'nodes');
        const connectorsDir = path.join(publicDir, 'connectors');

        // Ensure directories exist
        await fs.ensureDir(nodesDir);
        await fs.ensureDir(connectorsDir);

        const readDirWithoutExt = async (dir) => {
          const files = await fs.readdir(dir);
          return files
            .filter(f => f.endsWith('.png'))
            .map(f => f.replace('.png', ''));
        };

        const nodes = await readDirWithoutExt(nodesDir);
        const connectors = await readDirWithoutExt(connectorsDir);

        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({ nodes, connectors }));
      } catch (err) {
        res.statusCode = 500;
        res.end(JSON.stringify({ error: err.message }));
      }
    });
  }
});

// https://vite.dev/config/
export default defineConfig({
  base: '/',
  plugins: [react(), assetScannerPlugin()],
})

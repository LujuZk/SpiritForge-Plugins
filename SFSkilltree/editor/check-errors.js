const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch();
    const page = await browser.newPage();

    page.on('console', msg => {
        if (msg.type() === 'error') {
            console.log(`BROWSER ERROR: ${msg.text()}`);
        }
    });

    page.on('pageerror', exception => {
        console.log(`UNCAUGHT EXCEPTION: ${exception}`);
    });

    try {
        await page.goto('http://localhost:5173/SFSkilltree/');
        // Wait a bit for React to try to mount
        await page.waitForTimeout(2000);
    } catch (e) {
        console.error("Failed to load page: ", e);
    } finally {
        await browser.close();
    }
})();

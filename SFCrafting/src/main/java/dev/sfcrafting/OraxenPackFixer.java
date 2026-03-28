package dev.sfcrafting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class OraxenPackFixer {

    private final JavaPlugin plugin;
    private final int packFormat;

    public OraxenPackFixer(JavaPlugin plugin, int packFormat) {
        this.plugin = plugin;
        this.packFormat = packFormat;
    }

    public void schedule() {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::fixNow, 100L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::fixNow, 6000L, 6000L);
    }

    private void fixNow() {
        Plugin oraxen = plugin.getServer().getPluginManager().getPlugin("Oraxen");
        if (oraxen == null) {
            return;
        }
        File packDir = new File(oraxen.getDataFolder(), "pack");
        if (!packDir.isDirectory()) {
            return;
        }
        String mcmetaJson = "{\"pack\":{\"pack_format\":" + packFormat + ",\"description\":\"Oraxen\"}}";
        File mcmeta = new File(packDir, "pack.mcmeta");
        try {
            Files.writeString(mcmeta.toPath(), mcmetaJson, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo actualizar pack.mcmeta de Oraxen: " + ex.getMessage());
            return;
        }
        File zip = new File(packDir, "pack.zip");
        if (!zip.isFile()) {
            return;
        }
        Path tempZip;
        try {
            tempZip = Files.createTempFile(packDir.toPath(), "pack", ".tmp.zip");
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo crear zip temporal: " + ex.getMessage());
            return;
        }
        boolean replaced = false;
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempZip.toFile())))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if ("pack.mcmeta".equals(name)) {
                    ZipEntry newEntry = new ZipEntry(name);
                    out.putNextEntry(newEntry);
                    out.write(mcmetaJson.getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                    replaced = true;
                } else {
                    out.putNextEntry(new ZipEntry(name));
                    in.transferTo(out);
                    out.closeEntry();
                }
                in.closeEntry();
            }
            if (!replaced) {
                out.putNextEntry(new ZipEntry("pack.mcmeta"));
                out.write(mcmetaJson.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo actualizar pack.zip de Oraxen: " + ex.getMessage());
            try {
                Files.deleteIfExists(tempZip);
            } catch (IOException ignored) {
            }
            return;
        }
        try {
            Files.move(tempZip, zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo reemplazar pack.zip de Oraxen: " + ex.getMessage());
        }
    }
}


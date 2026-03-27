package dev.sfdrops;

import dev.sfdrops.listener.OreDropListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SFDropsPlugin extends JavaPlugin {

    private final Set<UUID> debugPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new OreDropListener(this), this);
        getLogger().info("SFDrops enabled.");
    }

    public boolean isDebug(Player player) {
        return debugPlayers.contains(player.getUniqueId()) || getConfig().getBoolean("debugGlobal", false);
    }

    public boolean toggleDebug(Player player) {
        UUID id = player.getUniqueId();
        if (debugPlayers.contains(id)) {
            debugPlayers.remove(id);
            return false;
        }
        debugPlayers.add(id);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sfdrops")) return false;

        getLogger().info("/" + label + " ejecutado por " + sender.getName() + " args=" + String.join(",", args));

        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sfdrops.admin")) {
                sender.sendMessage("[SFDrops] No tienes permiso para reload.");
                return true;
            }
            reloadConfig();
            sender.sendMessage("[SFDrops] Config recargada.");
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("[SFDrops] Este comando solo puede usarlo un jugador.");
                return true;
            }
            boolean enabled = toggleDebug(player);
            sender.sendMessage(enabled ? "[SFDrops] Debug activado." : "[SFDrops] Debug desactivado.");
            getLogger().info("Debug para " + player.getName() + " = " + enabled);
            return true;
        }

        sender.sendMessage("[SFDrops] Uso: /" + label + " <reload|debug>");
        return true;
    }
}

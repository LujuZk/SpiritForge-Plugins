package dev.sfcompass.commands;

import dev.sfcompass.SFCompassPlugin;
import dev.sfcompass.managers.CompassManager;
import dev.sfcompass.managers.IslandManager;
import dev.sfcompass.models.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CompassCommand implements CommandExecutor, TabCompleter {

    private final SFCompassPlugin plugin;
    private final CompassManager compassManager;
    private final IslandManager islandManager;

    public CompassCommand(SFCompassPlugin plugin) {
        this.plugin = plugin;
        this.compassManager = plugin.getCompassManager();
        this.islandManager = plugin.getIslandManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            return handleShowLevel(sender);
        }

        String action = args[0].toLowerCase();
        return switch (action) {
            case "give" -> handleGive(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "info" -> handleInfo(sender, args);
            case "point" -> handlePoint(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleShowLevel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("sfcompass.use")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }
        int level = compassManager.getLevel(player.getUniqueId());
        sender.sendMessage(Component.text("Tu nivel de brújula: " + level, NamedTextColor.AQUA));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sfcompass.admin")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /compass give <jugador>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }
        target.getInventory().addItem(compassManager.createCompassItem());
        sender.sendMessage(Component.text("Brújula entregada a " + target.getName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sfcompass.admin")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /compass setlevel <jugador> <nivel>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }
        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1) {
                sender.sendMessage(Component.text("El nivel debe ser >= 1.", NamedTextColor.RED));
                return true;
            }
            compassManager.setLevel(target.getUniqueId(), level);
            sender.sendMessage(Component.text(
                    "Nivel de " + target.getName() + " establecido a " + level, NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Nivel inválido.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sfcompass.admin")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /compass info <jugador>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }
        int level = compassManager.getLevel(target.getUniqueId());
        sender.sendMessage(Component.text(
                "Nivel de brújula de " + target.getName() + ": " + level, NamedTextColor.AQUA));
        return true;
    }

    private boolean handlePoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sfcompass.admin")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /compass point <jugador> <isla>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }
        Island island = islandManager.getIsland(args[2]);
        if (island == null) {
            sender.sendMessage(Component.text("Isla no encontrada: " + args[2], NamedTextColor.RED));
            return true;
        }
        compassManager.pointCompassTo(target, island);
        sender.sendMessage(Component.text(
                "Brújula de " + target.getName() + " apuntando a " + island.displayName(), NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== SFCompass ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/compass - Ver tu nivel", NamedTextColor.YELLOW));
        if (sender.hasPermission("sfcompass.admin")) {
            sender.sendMessage(Component.text("/compass give <jugador>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/compass setlevel <jugador> <nivel>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/compass info <jugador>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/compass point <jugador> <isla>", NamedTextColor.YELLOW));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("sfcompass.admin")) {
                subs.addAll(List.of("give", "setlevel", "info", "point"));
            }
            String prefix = args[0].toLowerCase();
            for (String sub : subs) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
            return completions;
        }

        if (!sender.hasPermission("sfcompass.admin")) return completions;

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("point")) {
            String prefix = args[2].toLowerCase();
            for (String id : islandManager.getIslandIds()) {
                if (id.toLowerCase().startsWith(prefix)) completions.add(id);
            }
            return completions;
        }

        return completions;
    }
}

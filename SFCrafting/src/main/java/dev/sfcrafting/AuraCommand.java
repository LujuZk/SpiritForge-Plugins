package dev.sfcrafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AuraCommand implements CommandExecutor, TabCompleter {

    private final AuraManager auraManager;

    public AuraCommand(AuraManager auraManager) {
        this.auraManager = auraManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
            return true;
        }

        String action = args.length == 0 ? "toggle" : args[0].trim().toLowerCase(Locale.ROOT);
        return switch (action) {
            case "on" -> {
                if (auraManager.isActive(player)) {
                    sender.sendMessage(ChatColor.YELLOW + "El aura ya esta activa.");
                    yield true;
                }
                boolean ok = auraManager.activate(player);
                sender.sendMessage(ok
                    ? ChatColor.AQUA + "Aura activada."
                    : ChatColor.RED + "No se pudo activar el aura (item visual no encontrado).");
                yield true;
            }
            case "off" -> {
                if (!auraManager.isActive(player)) {
                    sender.sendMessage(ChatColor.YELLOW + "El aura ya estaba desactivada.");
                    yield true;
                }
                auraManager.deactivate(player);
                sender.sendMessage(ChatColor.AQUA + "Aura desactivada.");
                yield true;
            }
            case "toggle" -> {
                boolean enabled = auraManager.toggle(player);
                sender.sendMessage(enabled
                    ? ChatColor.AQUA + "Aura activada."
                    : ChatColor.AQUA + "Aura desactivada.");
                yield true;
            }
            case "status" -> {
                sender.sendMessage(auraManager.isActive(player)
                    ? ChatColor.GREEN + "Aura activa."
                    : ChatColor.YELLOW + "Aura inactiva.");
                yield true;
            }
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <on|off|toggle|status>");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("on");
            options.add("off");
            options.add("toggle");
            options.add("status");
        }
        return options;
    }
}


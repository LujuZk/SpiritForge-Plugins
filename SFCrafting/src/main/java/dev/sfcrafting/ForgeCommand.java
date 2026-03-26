package dev.sfcrafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ForgeCommand implements CommandExecutor, TabCompleter {

    private final SFCraftingPlugin plugin;
    private final ForgeManager manager;
    private final AuraManager auraManager;
    private final OraxenItemResolver resolver;

    public ForgeCommand(SFCraftingPlugin plugin, ForgeManager manager, AuraManager auraManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.auraManager = auraManager;
        this.resolver = new OraxenItemResolver(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcrafting.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            if (auraManager != null) {
                auraManager.reloadSettings();
            }
            sender.sendMessage(ChatColor.GREEN + "Config recargada. Aura actualizada.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " give <oraxen_id|material:id> [cantidad] [rareza]");
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " reload");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede usarlo un jugador.");
            return true;
        }

        String rawId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Cantidad invalida.");
                return true;
            }
        }
        int rarity = 0;
        if (args.length >= 4) {
            rarity = parseRarity(args[3]);
            if (rarity < 0) {
                sender.sendMessage(ChatColor.RED + "Rareza invalida. Usa 0-4 o comun/poco_comun/raro/epico/legendario.");
                return true;
            }
        }

        ItemStack item = buildItem(rawId, amount);
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.RED + "No se encontro el item: " + rawId);
            return true;
        }
        manager.applyRarity(item, rarity);
        var leftovers = player.getInventory().addItem(item);
        for (var leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        sender.sendMessage(ChatColor.GREEN + "Entregado: " + rawId + " x" + amount + " (rareza " + rarity + ").");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.add("give");
            result.add("reload");
            return result;
        }
        if (args.length == 4) {
            result.add("0");
            result.add("1");
            result.add("2");
            result.add("3");
            result.add("4");
            result.add("comun");
            result.add("poco_comun");
            result.add("raro");
            result.add("epico");
            result.add("legendario");
            return result;
        }
        return result;
    }

    private int parseRarity(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.matches("\\d+")) {
            int number = Integer.parseInt(value);
            if (number >= 0 && number <= 4) {
                return number;
            }
            return -1;
        }
        return switch (value) {
            case "comun" -> 0;
            case "poco_comun", "pococomun", "poco-comun" -> 1;
            case "raro" -> 2;
            case "epico" -> 3;
            case "legendario" -> 4;
            default -> -1;
        };
    }

    private ItemStack buildItem(String raw, int amount) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("oraxen:")) {
            String id = value.substring("oraxen:".length()).trim();
            return resolver.buildOraxenItem(id, amount);
        }
        if (lower.startsWith("material:")) {
            String id = value.substring("material:".length()).trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(id);
            return material == null ? null : new ItemStack(material, amount);
        }
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material != null) {
            return new ItemStack(material, amount);
        }
        return resolver.buildOraxenItem(value, amount);
    }
}



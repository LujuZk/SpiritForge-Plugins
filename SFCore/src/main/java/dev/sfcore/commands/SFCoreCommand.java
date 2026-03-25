package dev.sfcore.commands;

import dev.sfcore.api.SFCoreAPI;
import dev.sfcore.api.StatType;
import dev.sfcore.managers.StatManager;
import dev.sfcore.managers.TestMonitorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class SFCoreCommand implements CommandExecutor, TabCompleter {

    private final StatManager statManager;
    private final TestMonitorManager testMonitor;

    // Aliases para stats comunes
    private static final Map<String, StatType> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("damage", StatType.DAMAGE_BONUS);
        ALIASES.put("dmg", StatType.DAMAGE_BONUS);
        ALIASES.put("armor", StatType.DAMAGE_REDUCTION);
        ALIASES.put("health", StatType.MAX_HEALTH);
        ALIASES.put("hp", StatType.MAX_HEALTH);
        ALIASES.put("speed", StatType.MOVEMENT_SPEED);
        ALIASES.put("mining", StatType.MINING_SPEED);
        ALIASES.put("ls", StatType.LIFESTEAL);
    }

    public SFCoreCommand(StatManager statManager, TestMonitorManager testMonitor) {
        this.statManager = statManager;
        this.testMonitor = testMonitor;
    }

    // ─── Command Executor ────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("sfcore.admin")) {
            player.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "test" -> handleTest(player, args);
            case "stats" -> handleStats(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    // ─── Test Subcommand ─────────────────────────────────────────────

    private void handleTest(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Uso: /sfcore test <stat|all> <on|off|add> [value]", NamedTextColor.YELLOW));
            return;
        }

        String statArg = args[1].toLowerCase();
        String action = args[2].toLowerCase();

        // /sfcore test all on/off
        if (statArg.equals("all")) {
            handleTestAll(player, action);
            return;
        }

        // Resolver stat type (por key o alias)
        StatType stat = resolveStatType(statArg);
        if (stat == null) {
            player.sendMessage(Component.text("Stat desconocido: " + statArg, NamedTextColor.RED));
            return;
        }

        switch (action) {
            case "on" -> {
                testMonitor.enable(player.getUniqueId(), stat);
                double total = statManager.getTotal(player, stat);
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text(stat.getKey(), NamedTextColor.GOLD))
                        .append(Component.text(" monitor activado", NamedTextColor.GREEN))
                        .append(Component.text(" | Valor actual: " + String.format("%.2f", total), NamedTextColor.YELLOW)));
            }
            case "off" -> {
                testMonitor.disable(player.getUniqueId(), stat);
                // Quitar bonus de test si existe
                statManager.removeBonus(player, "test:" + stat.getKey());
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text(stat.getKey(), NamedTextColor.GOLD))
                        .append(Component.text(" monitor desactivado + bonus test removido", NamedTextColor.RED)));
            }
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Uso: /sfcore test <stat> add <value>", NamedTextColor.YELLOW));
                    return;
                }
                try {
                    double value = Double.parseDouble(args[3]);
                    statManager.addBonus(player, "test:" + stat.getKey(), stat, value);
                    player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                            .append(Component.text(stat.getKey(), NamedTextColor.GOLD))
                            .append(Component.text(" bonus añadido: " + String.format("%.2f", value), NamedTextColor.GREEN))
                            .append(Component.text(" (source: test:" + stat.getKey() + ")", NamedTextColor.GRAY)));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Valor inválido: " + args[3], NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Acción inválida. Usa: on, off, add", NamedTextColor.RED));
        }
    }

    private void handleTestAll(Player player, String action) {
        switch (action) {
            case "on" -> {
                testMonitor.enableAll(player.getUniqueId());
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text("Todos los monitors activados", NamedTextColor.GREEN)));
            }
            case "off" -> {
                testMonitor.disableAll(player.getUniqueId());
                statManager.clearSource(player, "test:");
                player.sendMessage(Component.text("[TEST] ", NamedTextColor.GRAY)
                        .append(Component.text("Todos los monitors desactivados + bonuses test removidos", NamedTextColor.RED)));
            }
            default -> player.sendMessage(Component.text("Para 'all' solo se puede usar on/off", NamedTextColor.RED));
        }
    }

    // ─── Stats Subcommand ────────────────────────────────────────────

    private void handleStats(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
                return;
            }
        }

        Map<StatType, Double> totals = statManager.getAllTotals(target);

        player.sendMessage(Component.text("═══ Stats de " + target.getName() + " ═══", NamedTextColor.GOLD));

        for (StatType stat : StatType.values()) {
            double value = totals.getOrDefault(stat, 0.0);
            NamedTextColor valueColor = value > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            player.sendMessage(Component.text("  " + stat.getKey(), NamedTextColor.YELLOW)
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.2f", value), valueColor)));
        }
    }

    // ─── Help ────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("═══ SFCore Comandos ═══", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  /sfcore test <stat|all> on", NamedTextColor.YELLOW)
                .append(Component.text(" — Activa monitor de stat", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /sfcore test <stat|all> off", NamedTextColor.YELLOW)
                .append(Component.text(" — Desactiva monitor + quita bonus test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /sfcore test <stat> add <value>", NamedTextColor.YELLOW)
                .append(Component.text(" — Agrega bonus de test", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /sfcore stats [player]", NamedTextColor.YELLOW)
                .append(Component.text(" — Muestra todos los stats", NamedTextColor.GRAY)));
    }

    // ─── Tab Completer ───────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("test", "stats"), completions);
        } else if (args[0].equalsIgnoreCase("test")) {
            if (args.length == 2) {
                List<String> options = new ArrayList<>();
                options.add("all");
                for (StatType stat : StatType.values()) {
                    options.add(stat.getKey());
                }
                options.addAll(ALIASES.keySet());
                StringUtil.copyPartialMatches(args[1], options, completions);
            } else if (args.length == 3) {
                if (args[1].equalsIgnoreCase("all")) {
                    StringUtil.copyPartialMatches(args[2], List.of("on", "off"), completions);
                } else {
                    StringUtil.copyPartialMatches(args[2], List.of("on", "off", "add"), completions);
                }
            } else if (args.length == 4 && args[2].equalsIgnoreCase("add")) {
                StringUtil.copyPartialMatches(args[3], List.of("0.1", "0.5", "1", "5", "10"), completions);
            }
        } else if (args[0].equalsIgnoreCase("stats") && args.length == 2) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        }

        Collections.sort(completions);
        return completions;
    }

    // ─── Utilities ───────────────────────────────────────────────────

    private StatType resolveStatType(String input) {
        // Primero intentar key exacto
        StatType stat = StatType.fromKey(input);
        if (stat != null) return stat;
        // Luego intentar alias
        return ALIASES.get(input.toLowerCase());
    }
}

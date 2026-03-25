package dev.skilltree.commands;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillAdminCommand implements CommandExecutor {

    private final SkillTreePlugin plugin;

    public SkillAdminCommand(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("skilltree.admin")) {
            sender.sendMessage(Component.text("Sin permiso.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return true;
        }

        switch (action) {
            case "reset" -> {
                plugin.getSkillManager().resetPlayer(target.getUniqueId());
                // Recargar datos frescos
                plugin.getSkillManager().loadPlayer(target);
                sender.sendMessage(Component.text(
                        "Skills de " + target.getName() + " reseteados.", NamedTextColor.GREEN));
                target.sendMessage(Component.text(
                        "Un admin reseteó tus skills.", NamedTextColor.YELLOW));
            }

            case "give" -> {
                // /skillsadmin give <jugador> <skill> <cantidad>
                if (args.length < 4) { sendHelp(sender); return true; }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    sender.sendMessage(Component.text("Skill inválido: " + args[2], NamedTextColor.RED));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[3]);
                    plugin.getSkillManager().addXP(target, skill, amount);
                    sender.sendMessage(Component.text(
                            "Dado " + amount + " XP de " + skill.getDisplayName()
                                    + " a " + target.getName(), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Cantidad inválida.", NamedTextColor.RED));
                }
            }

            case "info" -> {
                PlayerSkillData data = plugin.getSkillManager().getData(target);
                sender.sendMessage(Component.text(
                        "=== Skills de " + target.getName() + " ===", NamedTextColor.GOLD));
                for (SkillType type : SkillType.values()) {
                    sender.sendMessage(Component.text(
                            type.getDisplayName() + " → Nv " + data.getLevel(type)
                                    + " | XP: " + String.format("%.1f", data.getXP(type)),
                            NamedTextColor.YELLOW));
                }
            }
            case "resettree" -> {
                // /skillsadmin resettree <jugador> <skill>
                if (args.length < 3) { sendHelp(sender); return true; }
                SkillType skill = SkillType.fromKey(args[2]);
                if (skill == null) {
                    sender.sendMessage(Component.text("Skill inválido: " + args[2], NamedTextColor.RED));
                    return true;
                }
                plugin.getSkillPointManager().resetTree(target, skill);
                sender.sendMessage(Component.text(
                        "Árbol de " + skill.getDisplayName() + " reseteado para " + target.getName(),
                        NamedTextColor.GREEN));
                target.sendMessage(Component.text(
                        "Tu árbol de " + skill.getDisplayName() + " fue reseteado.",
                        NamedTextColor.YELLOW));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Uso:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/skillsadmin reset <jugador>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/skillsadmin give <jugador> <skill> <cantidad>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/skillsadmin info <jugador>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Skills válidos: mining, farming, fishing, sword, axe, bow, trident", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/skillsadmin resettree <jugador> <skill>", NamedTextColor.YELLOW));
    }
}
package dev.skilltree.commands;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.gui.SkillTreeGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillCommand implements CommandExecutor {

    private final SkillTreePlugin plugin;
    private final SkillTreeGUI gui;

    public SkillCommand(SkillTreePlugin plugin) {
        this.plugin = plugin;
        this.gui = new SkillTreeGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar este comando.");
            return true;
        }

        if (!player.hasPermission("skilltree.use")) {
            player.sendMessage(Component.text(
                    plugin.getConfig().getString("messages.no-permission",
                            "No tenés permiso."), NamedTextColor.RED));
            return true;
        }

        gui.open(player);
        return true;
    }
}
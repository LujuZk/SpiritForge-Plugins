package dev.sfcharacter.commands;

import dev.sfcharacter.SFCharacterPlugin;
import dev.sfcharacter.managers.CharacterManager;
import dev.sfcharacter.models.CharacterData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CharacterCommand implements CommandExecutor, TabCompleter {

    private final SFCharacterPlugin plugin;

    public CharacterCommand(SFCharacterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores.", NamedTextColor.RED));
            return true;
        }

        CharacterManager manager = plugin.getCharacterManager();
        String subcommand = args.length > 0 ? args[0].toLowerCase() : "open";

        switch (subcommand) {
            case "open", "switch" -> {
                if (manager.isInCharacterSelection(player.getUniqueId())) {
                    plugin.getSelectGUI().open(player);
                    return true;
                }

                // Save current state if player has an active character
                if (manager.hasActiveCharacter(player.getUniqueId())) {
                    manager.saveCharacterState(player);
                }

                manager.clearAndSendToLobby(player);
                plugin.getSelectGUI().open(player);
                player.sendMessage(Component.text("Selecciona un personaje.", NamedTextColor.GOLD));
                return true;
            }
            case "info" -> {
                CharacterData active = manager.getActiveCharacter(player.getUniqueId());
                if (active == null) {
                    player.sendMessage(Component.text("No tenes un personaje activo.", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Personaje activo: ", NamedTextColor.GRAY)
                            .append(Component.text(active.displayName(), active.characterClass().getColor())));
                    player.sendMessage(Component.text("Clase: " + active.characterClass().getDisplayName(), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Slot: " + (active.slot() + 1), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Creado: " + active.createdAt(), NamedTextColor.DARK_GRAY));
                }
                return true;
            }
            default -> {
                player.sendMessage(Component.text("Uso: /character [open|info|switch]", NamedTextColor.YELLOW));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String sub : List.of("open", "info", "switch")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return List.of();
    }
}

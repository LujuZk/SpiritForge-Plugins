package dev.sfcore.placeholders;

import dev.sfcore.SFCorePlugin;
import dev.sfcore.api.StatType;
import dev.sfcore.managers.ManaManager;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SFCorePlaceholderExpansion extends PlaceholderExpansion {

    private final SFCorePlugin plugin;

    public SFCorePlaceholderExpansion(SFCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sfcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LujuZk";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);
        ManaManager manaManager = plugin.getManaManager();

        return switch (key) {
            case "health", "vida" -> format1(player.getHealth());
            case "max_health", "maxhealth", "vida_maxima", "vida_max", "max_vida" -> format1(getMaxHealth(player));
            case "mana" -> format1(manaManager != null ? manaManager.getMana(player) : 0.0D);
            case "max_mana", "mana_maxima", "mana_maximo", "mana_max" ->
                format1(manaManager != null ? manaManager.getMaxMana(player) : 0.0D);
            case "magic_damage", "magicdamage", "dano_magico" ->
                format1(plugin.getStatManager() != null ? plugin.getStatManager().getTotal(player, StatType.MAGIC_DAMAGE) : 0.0D);
            default -> null;
        };
    }

    private static double getMaxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0D;
    }

    private static String format1(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}

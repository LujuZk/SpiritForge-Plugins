package dev.skilltree.listeners;

import dev.sfcrafting.SpellCraftCompleteEvent;
import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Escucha eventos de crafteo de hechizos de SFCrafting para otorgar XP de spellcrafting.
 * XP = templateValue × crystalMultiplier
 * Cada template y cristal se configuran en config.yml bajo skills.spellcrafting.
 */
public class SpellcraftingListener implements Listener {

    private final SkillTreePlugin plugin;

    public SpellcraftingListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpellCraftComplete(SpellCraftCompleteEvent event) {
        plugin.getLogger().info("[Spellcrafting] SpellCraftCompleteEvent recibido: template="
                + event.getTemplateKey() + " crystal=" + event.getCrystalKey()
                + " points=" + event.getPointsUsed()
                + " player=" + event.getPlayer().getName());

        ConfigurationSection templateValues = plugin.getConfig()
                .getConfigurationSection("skills.spellcrafting.template-values");
        if (templateValues == null) {
            plugin.getLogger().warning("[Spellcrafting] No se encontró sección 'skills.spellcrafting.template-values' en config.yml");
            return;
        }

        int baseValue = templateValues.getInt(event.getTemplateKey(), 0);
        if (baseValue <= 0) {
            plugin.getLogger().info("[Spellcrafting] template '" + event.getTemplateKey()
                    + "' no tiene template-value o es 0 → sin XP");
            return;
        }

        double crystalMult = plugin.getConfig()
                .getDouble("skills.spellcrafting.crystal-multipliers." + event.getCrystalKey(), 1.0);

        double xp = baseValue * crystalMult;

        if (plugin.getSkillManager().isDebug(event.getPlayer())) {
            event.getPlayer().sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "[DEBUG] Spellcrafting: " + event.getTemplateKey()
                                    + " (crystal " + event.getCrystalKey() + ")"
                                    + " → templateValue=" + baseValue
                                    + " * crystalMult=" + String.format("%.1f", crystalMult)
                                    + " = " + String.format("%.1f", xp) + " XP",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }

        plugin.getSkillManager().addXP(event.getPlayer(), SkillType.SPELLCRAFTING, xp);
    }
}

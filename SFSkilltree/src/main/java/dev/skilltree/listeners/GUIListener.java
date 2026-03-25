package dev.skilltree.listeners;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.gui.SkillTreeDetailGUI;
import dev.skilltree.gui.SkillTreeGUI;
import dev.skilltree.managers.SkillPointManager;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {

    private final SkillTreePlugin plugin;
    private final SkillTreeGUI mainGui;
    private final SkillTreeDetailGUI detailGui;

    public GUIListener(SkillTreePlugin plugin) {
        this.plugin = plugin;
        this.mainGui = new SkillTreeGUI(plugin);
        this.detailGui = new SkillTreeDetailGUI(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getSkillManager().loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getInventoryManager().restoreInventory(event.getPlayer());
        plugin.getSkillManager().saveAndUnload(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(org.bukkit.event.player.PlayerKickEvent event) {
        plugin.getInventoryManager().restoreInventory(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        // Solo restaurar si realmente cerró un menú nuestro y tenía inventario guardado
        if (plugin.getInventoryManager().hasSavedInventory(player)) {
            // Verificar si el inventario que cerró es el menú (por el título u otra
            // propiedad)
            // Como esto se dispara justo ANTES de cambiar/abrir otra UI, a veces trae
            // problemas si cambian de pag.
            // Una opción mejor es dejar que `saveAndClear` solo actúe si NO tiene guardado,
            // pero siempre que cierre restauramos el inventario. De esta forma,
            // navegar entre páginas (que abre y cierra guis rápidamente) no duplica / rompe
            // el inventory.

            // Para ser 100% seguros y evitar restaurar ítems mientras navega de Página 1 a
            // Página 2,
            // Bukkit ejecuta InventoryCloseEvent ANTES de abrir el próximo al usar
            // player.openInventory,
            // entonces, si se lo restauramos de inmediato aquí, lo va a volver a ver.
            // Para eso, scheduleamos la restauración 1 tick después y chequeamos si tiene
            // otro abierto.

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Chequear qué tiene abierto el jugador AHORA (después del cierre o cambio de
                // inventario)
                org.bukkit.inventory.InventoryView currentView = player.getOpenInventory();
                if (currentView != null) {
                    String title = PlainTextComponentSerializer.plainText().serialize(currentView.title());
                    // Si _todavía_ está en alguna de las GUIs del árbol, NO le restauramos el
                    // inventario aún
                    // ya que está navegando.
                    if (title.contains("Árbol")) {
                        return;
                    }
                }

                // Si cerró por completo nuestras interfaces, restauramos
                plugin.getInventoryManager().restoreInventory(player);
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getCurrentItem() == null)
            return;

        String title = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        // ─── Menú principal ───────────────────────────────────────────────────
        if (title.contains("Árbol de Habilidades")) {
            event.setCancelled(true);
            handleMainGuiClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        // ─── GUI de árbol detallado ───────────────────────────────────────────
        if (title.contains("— Árbol")) {
            event.setCancelled(true);
            handleDetailGuiClick(player, event.getSlot(), event.getCurrentItem(), title);
        }
    }

    // ─── Menú principal: click en un skill abre su árbol ─────────────────────

    private void handleMainGuiClick(Player player, int slot, ItemStack item) {
        // Los slots de skills en el menú principal (definidos en SkillTreeGUI)
        SkillType skill = SkillTreeGUI.getSkillForSlot(slot);
        if (skill == null)
            return;
        if (!plugin.getTreeManager().hasTree(skill)) {
            player.sendMessage(Component.text(
                    "Este skill no tiene árbol configurado aún.", NamedTextColor.YELLOW));
            return;
        }
        detailGui.open(player, skill);
    }

    // ─── GUI de árbol: navegación y desbloqueo de nodos ──────────────────────

    private void handleDetailGuiClick(Player player, int slot, ItemStack item, String title) {
        // Recuperar skill y página actual del PDC del jugador
        NamespacedKey pageKey = new NamespacedKey(plugin, "tree_page");
        NamespacedKey skillKey = new NamespacedKey(plugin, "tree_skill");
        var pdc = player.getPersistentDataContainer();

        Integer page = pdc.get(pageKey, PersistentDataType.INTEGER);
        String skillKeyStr = pdc.get(skillKey, PersistentDataType.STRING);
        if (page == null || skillKeyStr == null)
            return;

        SkillType skill = SkillType.fromKey(skillKeyStr);
        if (skill == null)
            return;

        // ─── Navegación de páginas (target_page en el item) ───────────────────
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "target_page"),
                PersistentDataType.INTEGER)) {

            int targetPage = item.getItemMeta().getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "target_page"),
                    PersistentDataType.INTEGER);

            detailGui.open(player, skill, targetPage);
            return;
        }

        // Botón ← volver (inventario del jugador, identificado por acción en PDC)
        if (item.hasItemMeta() && SkillTreeDetailGUI.ACTION_BACK_MAIN.equals(
                item.getItemMeta().getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "gui_action"),
                        PersistentDataType.STRING))) {
            mainGui.open(player);
            return;
        }

        // Click en un nodo: intentar desbloquearlo
        if (item.hasItemMeta()) {
            var itemPdc = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey nodeKey = new NamespacedKey("skilltree", "node_id");
            String nodeId = itemPdc.get(nodeKey, PersistentDataType.STRING);
            if (nodeId == null)
                return;

            SkillPointManager.UnlockResult result = plugin.getSkillPointManager().tryUnlock(player, skill, nodeId);

            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(Component.text("✔ Nodo desbloqueado!", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(),
                            org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                    detailGui.open(player, skill, page); // refrescar
                }
                case NOT_ENOUGH_POINTS ->
                    player.sendMessage(Component.text("✖ No tenés suficientes puntos.", NamedTextColor.RED));
                case PREREQUISITES_NOT_MET ->
                    player.sendMessage(Component.text("✖ Prerequisitos no cumplidos.", NamedTextColor.RED));
                case BLOCKED_BY_EXCLUSIVE ->
                    player.sendMessage(Component.text("✖ Ya elegiste otro camino en este nivel.", NamedTextColor.RED));
                case ALREADY_UNLOCKED ->
                    player.sendMessage(Component.text("Ya tenés este nodo.", NamedTextColor.YELLOW));
                default -> {
                }
            }
        }
    }
}

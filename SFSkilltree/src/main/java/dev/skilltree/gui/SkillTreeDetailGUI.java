package dev.skilltree.gui;

import dev.skilltree.SkillTreePlugin;
import dev.skilltree.models.IconDefinition;
import dev.skilltree.models.PlayerSkillData;
import dev.skilltree.models.SkillGraph;
import dev.skilltree.models.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GUI del árbol de habilidades de un skill.
 * Usa un grid continuo de 6×9 (54 slots) renderizado por GridRenderer.
 *
 * Fila 0-4  → árbol (nodos + conectores)
 * Fila 5    → navegación (volver, flechas, número de página)
 */
public class SkillTreeDetailGUI {

    public static final String ACTION_BACK_MAIN = "back_main";
    private static final int PLAYER_SLOT_BACK = 9; // primer slot del inventario (no hotbar)
    private static final int PLAYER_SLOT_INFO = 22; // centro de los 27 slots del inventario
    private static final int HOTBAR_SLOT_PREV = 2;
    private static final int HOTBAR_SLOT_PAGE_START = 3; // 3, 4, 5
    private static final int HOTBAR_SLOT_PAGE_END = 5;
    private static final int HOTBAR_SLOT_NEXT = 6;

    private final SkillTreePlugin plugin;
    private final GridRenderer gridRenderer;

    public SkillTreeDetailGUI(SkillTreePlugin plugin) {
        this.plugin = plugin;
        this.gridRenderer = new GridRenderer(plugin);
    }

    public void open(Player player, SkillType skill, int page) {
        SkillGraph graph = plugin.getTreeManager().getTree(skill);
        if (graph == null) {
            player.sendMessage(Component.text("Este skill no tiene árbol configurado.", NamedTextColor.RED));
            return;
        }

        int totalPages = gridRenderer.calculateTotalPages(graph);
        if (totalPages <= 0)
            totalPages = 1;
        page = Math.max(0, Math.min(page, totalPages - 1));

        PlayerSkillData data = plugin.getSkillManager().getData(player);
        Set<String> unlocked = data.getUnlockedNodes(skill);
        int points = data.getAvailablePoints(skill);

        // \uE00A desplaza -48px para centrar la textura de 256px en la GUI de 176px
        // ꐟ (U+A41F) = glifo Oraxen st_background (256×256, ascent 15)
        // \uE00B desplaza -158px para traer el texto al centro
        Component title = Component.text("\uE00Aꐟ\uE00B", NamedTextColor.WHITE)
                .append(Component.text(graph.getDisplayName() + " — Árbol",
                        NamedTextColor.GOLD, TextDecoration.BOLD));

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Renderizar la página actual del grafo
        gridRenderer.renderPage(inv, graph, unlocked, page, points);

        // ─── Hotbar de navegación ────────────────────────────────────────────
        // Guardar y limpiar inventario ANTES de poner los botones
        plugin.getInventoryManager().saveAndClearInventory(player);
        renderNavigationBar(player, inv, page, totalPages, points);

        player.openInventory(inv);

        // Persistir skill y página actual en el PDC del jugador
        var pdc = player.getPersistentDataContainer();
        pdc.set(new org.bukkit.NamespacedKey(plugin, "tree_skill"),
                org.bukkit.persistence.PersistentDataType.STRING, skill.getKey());
        pdc.set(new org.bukkit.NamespacedKey(plugin, "tree_page"),
                org.bukkit.persistence.PersistentDataType.INTEGER, page);
    }

    public void open(Player player, SkillType skill) {
        open(player, skill, 0);
    }

    // ─── Construcción de ítems ────────────────────────────────────────────────

    private ItemStack buildPointsInfo(int points) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Puntos disponibles: " + points,
                NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("  Ganás 1 punto por nivel subido", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void renderNavigationBar(Player player, Inventory inv, int page, int totalPages, int points) {
        for (int slot = 45; slot <= 53; slot++) {
            inv.setItem(slot, null);
        }

        org.bukkit.inventory.PlayerInventory pInv = player.getInventory();

        // Hotbar: 0,1,7,8 vacíos visualmente con filler.
        ItemStack hotbarFiller = createHotbarFiller();
        pInv.setItem(0, hotbarFiller.clone());
        pInv.setItem(1, hotbarFiller.clone());
        pInv.setItem(7, hotbarFiller.clone());
        pInv.setItem(8, hotbarFiller.clone());

        // Botón volver al menú principal en primer slot del inventario del jugador.
        pInv.setItem(PLAYER_SLOT_BACK, makeBackButton());

        // Estrella de puntos en el centro del inventario del jugador.
        pInv.setItem(PLAYER_SLOT_INFO, buildPointsInfo(points));

        // Flecha anterior en hotbar.
        boolean hasPrev = page > 0;
        ItemStack prev = makeIconButton(hasPrev ? "nav_prev" : "nav_prev_disabled",
                "◄", hasPrev ? page - 1 : null);
        pInv.setItem(HOTBAR_SLOT_PREV, prev);

        // Flecha siguiente en hotbar.
        boolean hasNext = page < totalPages - 1;
        ItemStack next = makeIconButton(hasNext ? "nav_next" : "nav_next_disabled",
                "►", hasNext ? page + 1 : null);
        pInv.setItem(HOTBAR_SLOT_NEXT, next);

        // Números de página (1-based)
        int pageNumber = page + 1;
        String pageStr = Integer.toString(pageNumber);
        int len = pageStr.length();
        int startSlot = HOTBAR_SLOT_PAGE_START + (3 - len) / 2; // slots 3-5

        // Limpiar slots de página con filler
        for (int slot = HOTBAR_SLOT_PAGE_START; slot <= HOTBAR_SLOT_PAGE_END; slot++) {
            pInv.setItem(slot, hotbarFiller.clone());
        }

        for (int i = 0; i < len && startSlot + i <= HOTBAR_SLOT_PAGE_END; i++) {
            char ch = pageStr.charAt(i);
            String iconId = "page_" + ch;
            ItemStack digit = makeIconButton(iconId, String.valueOf(ch), null);
            pInv.setItem(startSlot + i, digit);
        }
    }

    private ItemStack makeBackButton() {
        ItemStack item = makeTextButton("← Volver al menú", Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "gui_action"),
                org.bukkit.persistence.PersistentDataType.STRING,
                ACTION_BACK_MAIN);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeTextButton(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHotbarFiller() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(fillerMeta);
        return filler;
    }

    private ItemStack makeIconButton(String iconId, String fallbackName, Integer targetPage) {
        IconDefinition icon = plugin.getTreeManager().getIcon(iconId);
        ItemStack item;
        ItemMeta meta;

        if (icon != null) {
            item = icon.buildItem();
            meta = item.getItemMeta();
        } else {
            item = new ItemStack(Material.PAPER);
            meta = item.getItemMeta();
        }

        meta.displayName(Component.text(fallbackName, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        if (targetPage != null) {
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "target_page"),
                    org.bukkit.persistence.PersistentDataType.INTEGER,
                    targetPage);
        }

        item.setItemMeta(meta);
        return item;
    }

}

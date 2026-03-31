package dev.sfcrafting;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RecipeBookGUI {

    static final int SIZE = 54;
    static final int RECIPES_PER_PAGE = 28;
    static final int SLOT_PREV = 45;
    static final int SLOT_TAB_SMELTER = 48;
    static final int SLOT_TAB_ANVIL = 49;
    static final int SLOT_NEXT = 53;

    private static final int[] RECIPE_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private RecipeBookGUI() {}

    public static void open(Player player, RecipeBookManager manager, ForgeManager forgeManager,
                            SkillBridge skillBridge, ForgeState.StationType category, int page) {
        List<ForgeRecipe> recipes = category == ForgeState.StationType.SMELTER
                ? manager.getSmelterRecipes()
                : manager.getAnvilRecipes();

        int totalPages = Math.max(1, (int) Math.ceil(recipes.size() / (double) RECIPES_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        RecipeBookHolder holder = new RecipeBookHolder(player.getUniqueId(), category, page);
        String title = ChatColor.DARK_GRAY + "Recetario - "
                + (category == ForgeState.StationType.SMELTER ? "Fundicion" : "Yunque")
                + " (" + (page + 1) + "/" + totalPages + ")";
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        fillBorder(inventory);
        fillRecipes(inventory, recipes, page, player, manager, forgeManager, skillBridge);
        fillNavigation(inventory, page, totalPages, category);

        player.openInventory(inventory);
    }

    private static void fillBorder(Inventory inventory) {
        ItemStack glass = buildGlass();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
        }
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9 + 7, glass);
            inventory.setItem(row * 9 + 8, glass);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }
    }

    private static void fillRecipes(Inventory inventory, List<ForgeRecipe> recipes, int page,
                                     Player player, RecipeBookManager manager,
                                     ForgeManager forgeManager, SkillBridge skillBridge) {
        int start = page * RECIPES_PER_PAGE;
        int end = Math.min(start + RECIPES_PER_PAGE, recipes.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= RECIPE_SLOTS.length) break;

            ForgeRecipe recipe = recipes.get(i);
            boolean unlocked = manager.isRecipeUnlocked(player, recipe);

            ItemStack display;
            if (unlocked) {
                display = buildUnlockedItem(recipe, forgeManager);
            } else {
                display = buildLockedItem(recipe, player, manager, skillBridge);
            }
            inventory.setItem(RECIPE_SLOTS[slotIndex], display);
        }
    }

    private static void fillNavigation(Inventory inventory, int page, int totalPages,
                                        ForgeState.StationType category) {
        if (page > 0) {
            inventory.setItem(SLOT_PREV, buildNavButton(Material.ARROW, ChatColor.YELLOW + "Pagina anterior"));
        }
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, buildNavButton(Material.ARROW, ChatColor.YELLOW + "Pagina siguiente"));
        }

        boolean isSmelter = category == ForgeState.StationType.SMELTER;
        inventory.setItem(SLOT_TAB_SMELTER, buildTabButton(Material.FURNACE,
                ChatColor.GOLD + "Fundicion",
                isSmelter));
        inventory.setItem(SLOT_TAB_ANVIL, buildTabButton(Material.ANVIL,
                ChatColor.GOLD + "Yunque",
                !isSmelter));
    }

    private static ItemStack buildUnlockedItem(ForgeRecipe recipe, ForgeManager forgeManager) {
        ItemStack output = forgeManager.buildRecipeOutput(recipe);
        if (output == null) {
            output = new ItemStack(Material.PAPER, 1);
        }
        output = output.clone();
        ItemMeta meta = output.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            lore.add("");
            lore.add(ChatColor.GRAY + "Ingredientes:");
            appendIngredient(lore, recipe.inputA(), recipe.inputAAmount());
            if (recipe.inputB() != null) {
                appendIngredient(lore, recipe.inputB(), recipe.inputBAmount());
            }
            if (recipe.inputC() != null) {
                appendIngredient(lore, recipe.inputC(), recipe.inputCAmount());
            }
            lore.add("");
            lore.add(ChatColor.GRAY + "Tiempo: " + ChatColor.WHITE + String.format("%.1fs", recipe.cookTimeTicks() / 20.0));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            output.setItemMeta(meta);
        }
        return output;
    }

    private static ItemStack buildLockedItem(ForgeRecipe recipe, Player player,
                                              RecipeBookManager manager, SkillBridge skillBridge) {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Receta Bloqueada");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID: " + recipe.id());
            lore.add("");

            RecipeUnlockRequirement req = recipe.unlockRequirement();
            if (req != null) {
                lore.add(ChatColor.GRAY + "Requisitos:");

                if (req.skillKey() != null && req.levelRequired() > 0) {
                    int playerLevel = skillBridge.getSkillLevel(player, req.skillKey());
                    String skillName = skillBridge.getSkillDisplayName(req.skillKey());
                    boolean met = playerLevel >= req.levelRequired();
                    String prefix = met ? (ChatColor.GREEN + "  \u2713 ") : (ChatColor.RED + "  \u2717 ");
                    lore.add(prefix + skillName + " nivel " + req.levelRequired()
                            + (met ? "" : ChatColor.GRAY + " (actual: " + playerLevel + ")"));
                }

                for (String materialId : req.requiredDiscoveries()) {
                    boolean discovered = manager.hasDiscovered(player.getUniqueId(), materialId);
                    String prefix = discovered ? (ChatColor.GREEN + "  \u2713 ") : (ChatColor.RED + "  \u2717 ");
                    lore.add(prefix + materialId + (discovered ? " descubierto" : " no descubierto"));
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void appendIngredient(List<String> lore, ForgeIngredient ingredient, int amount) {
        String id = ingredient.displayId();
        lore.add(ChatColor.WHITE + "  - " + id + (amount > 1 ? " x" + amount : ""));
    }

    private static ItemStack buildGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack buildNavButton(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildTabButton(Material material, String name, boolean selected) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            if (selected) {
                lore.add(ChatColor.GREEN + "Seleccionado");
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(ChatColor.GRAY + "Click para ver");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

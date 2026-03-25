package dev.skilltree.models;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Define un icono que puede ser usado por nodos o conectores.
 * Almacena un ID de Oraxen y delega la creación del ItemStack a Oraxen.
 */
public class IconDefinition {
    private final String oraxenId;

    public IconDefinition(String oraxenId) {
        this.oraxenId = oraxenId;
    }

    public String getOraxenId() {
        return oraxenId;
    }

    /**
     * Construye un ItemStack usando Oraxen.
     * Si el item no existe en Oraxen, retorna PAPER sin textura como fallback visible.
     */
    public ItemStack buildItem() {
        ItemBuilder builder = OraxenItems.getItemById(oraxenId);
        if (builder == null) {
            return new ItemStack(Material.PAPER);
        }
        return builder.build();
    }
}

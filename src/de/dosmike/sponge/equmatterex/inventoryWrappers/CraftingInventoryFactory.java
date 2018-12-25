package de.dosmike.sponge.equmatterex.inventoryWrappers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.common.item.inventory.util.InventoryUtil;

/**
 * For proper construction of Inventories it is necessary to require the Sponge Implementation.
 * After fighting with fabrics and lenses for hours (non have any code comments and I still don't
 * fully understand how they're built relying upon each other) if figured that no full
 * CraftingInventory was required for my purpose, but a properly built CraftingGridInventory.
 * So only this functionality remained.
 *
 * With this provider class all dependencies to SpongeCommon and MCP are encapsulated and the rest
 * of the code stays sane and safe.
 **/
public class CraftingInventoryFactory {

    /**
     * Creates a Minecraft InventoryCrafting and uses SpongeCommon to convert it to a
     * CraftingGridInventoryAdapter that is compatible to SpongeAPI as CraftingGrifInventory.
     * @return An empty CraftingGridInventory that can be used for recipe probing.
     */
    public static CraftingGridInventory buildGrid() {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            public boolean canInteractWith(EntityPlayer playerIn) {
                return false;
            }
        }, 3, 3);
        return InventoryUtil.toSpongeInventory(inv);
    }

}

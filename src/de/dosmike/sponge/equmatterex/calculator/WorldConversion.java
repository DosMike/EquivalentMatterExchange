package de.dosmike.sponge.equmatterex.calculator;

import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.Recipe;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/** template for storing world conversions */
public class WorldConversion implements Recipe {

    static int counter=0;
    List<ItemStackSnapshot> in = new LinkedList<>();
    ItemStackSnapshot out;
    String id, name;
    public WorldConversion(ItemStackSnapshot result, Collection<ItemStackSnapshot> cost) {
        in.addAll(cost);
        out = result;
        id = "eqmatterex:worldconversion"+counter;
        name = "World Conversion "+(counter++);
    }

    public List<ItemStackSnapshot> getRequiredInputs() {
        return in;
    }

    @Override
    public ItemStackSnapshot getExemplaryResult() {
        return out;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}

package de.dosmike.sponge.equmatterex.util;

import de.dosmike.sponge.equmatterex.EquivalentMatter;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class ForgeHelper {

    public static final String FURNACE = "minecraft:furnace";
    public static final String LIT_FURNACE = "minecraft:lit_furnace";
    public static final String CRAFTING_TABLE = "minecraft:crafting_table";
    public static final String CHEST = "minecraft:chest";
    public static final String DAYLIGHT_DETECTOR = "minecraft:daylight_detector";
    public static final String DAYLIGHT_DETECTOR_INVERTED = "minecraft:daylight_detector_inverted";

    public static boolean isOfType(String type, BlockType test) {
        if (type == null) return test == null; else if (test == null) return false;
        String testId = test.getId();
        EquivalentMatter.l("Comparing %s and %s", type, testId);
        return type.equals(testId);
    }

    public static boolean isOfType(String type, BlockState test) {
        if (type == null) return test == null; else if (test == null) return false;
        return isOfType(type, test.getType());
    }

    public static boolean isOfType(String type, BlockSnapshot test) {
        if (type == null) return test == null; else if (test == null) return false;
        return isOfType(type, test.getState().getType());
    }

    public static boolean isOfType(String type, Location<World> test) {
        if (type == null) return test == null; else if (test == null) return false;
        return isOfType(type, test.getBlockType());
    }


}

package de.dosmike.sponge.equmatterex.emcDevices;

import de.dosmike.sponge.equmatterex.TabletView;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;

public class TransmutationTable extends Device {

    public TransmutationTable(Location<World> baseBlockLocation) {
        super(baseBlockLocation, Type.TRANSMUTATION_TABLE);
    }

    public static void openTableFor(Player player) {
        new TabletView(player);
    }

    @Override
    void tick() {

    }

    @Override
    protected BigInteger offerEMC(BigInteger amount) {
        return amount;
    }

    @Override
    protected BigInteger pollEMC(BigInteger amount) {
        return BigInteger.ZERO;
    }

    @Override
    protected BigInteger peekEMC(BigInteger amount) {
        return BigInteger.ZERO;
    }

    @Override
    protected BigInteger getEMC() {
        return BigInteger.ZERO;
    }

    @Override
    protected void setEMC(BigInteger emcStored) {

    }

    /** item frame not required for validity, only for producing stuff */
    public static boolean validateStructure(Location<World> location) {
        BlockType typeAbove = location.getRelative(Direction.UP).getBlockType();
        if (location.getBlockType().equals(BlockTypes.CRAFTING_TABLE) &&
                typeAbove.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
                typeAbove.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            return true;
        }
        return false;
    }
}

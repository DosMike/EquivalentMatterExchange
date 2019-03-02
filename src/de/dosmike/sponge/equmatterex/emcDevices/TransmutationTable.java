package de.dosmike.sponge.equmatterex.emcDevices;

import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.ItemTypeEx;
import de.dosmike.sponge.equmatterex.TabletView;
import de.dosmike.sponge.equmatterex.util.ForgeHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TransmutationTable extends Device {

    private static Set<ItemTypeEx> listItemTypes = new HashSet<>();
    private static boolean blacklistItemTypes = true;

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
        if (ForgeHelper.isOfType(ForgeHelper.CRAFTING_TABLE, location) &&
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, typeAbove) ||
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, typeAbove)) {
            return true;
        }
        return false;
    }

    public static void loadItemTypeBlacklist(Collection<String> listItemTypes, boolean blacklistItemTypes) {
        TransmutationTable.listItemTypes.clear();
        if (listItemTypes != null)
            for (String s : listItemTypes) {
                Optional<ItemTypeEx> type = ItemTypeEx.valueOf(s);
                if (!type.isPresent())
                    EquivalentMatter.w("Invalid Item Type (Condenser listItemType) %s", s);
                else
                    TransmutationTable.listItemTypes.add(type.get());
            }
        TransmutationTable.blacklistItemTypes = blacklistItemTypes;
    }

    public static boolean canLearn(ItemTypeEx type) {
        if (blacklistItemTypes)
            return !listItemTypes.contains(type);
        else
            return listItemTypes.contains(type);
    }
}

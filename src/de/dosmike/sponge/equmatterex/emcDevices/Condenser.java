package de.dosmike.sponge.equmatterex.emcDevices;

import de.dosmike.sponge.equmatterex.calculator.Calculator;
import de.dosmike.sponge.equmatterex.ItemFrameUtils;
import de.dosmike.sponge.equmatterex.ItemTypeEx;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.Optional;

public class Condenser extends Device {

    public Condenser(Location<World> baseBlockLocation) {
        super(baseBlockLocation, Type.CONDENSER);
    }

    private BigInteger emcStore = BigInteger.ZERO;
    private ItemStackSnapshot targeted=null;
    private BigInteger emcDelta = BigInteger.ZERO;
    private BigInteger emcTarget = BigInteger.ZERO;

    private int secondTimer = 15; //every 20 ticks is a second
    boolean isTier2 = false, placementFailed=true;

    @Override
    void tick() {
        if (++secondTimer >= 20) {
            secondTimer = 0;

            if (baseLocation.getRelative(Direction.UP).getBlockType().equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED))
                isTier2 = true;

            Optional<ItemFrame> item = ItemFrameUtils.getItemFramFrom(baseLocation, baseLocation.getBlock().get(Keys.DIRECTION).orElse(Direction.NONE));
            if (item.isPresent()) {
                Optional<ItemStackSnapshot> stack = item.get().get(Keys.REPRESENTED_ITEM);
                targeted = stack.orElse(null);
                if (targeted!=null) emcTarget = Calculator.getValueFor(targeted).orElse(BigInteger.ZERO);
                else emcTarget = BigInteger.ZERO;
            } else {
//                EquivalentMatter.l("No target");
                targeted = null;
            }
        }
        if (targeted==null || emcTarget.compareTo(BigInteger.ZERO)<=0) return;

        TileEntityCarrier chest = (TileEntityCarrier) baseLocation.getTileEntity().get();
        if (baseLocation.getBlockType().equals(BlockTypes.CHEST)) {
            ItemTypeEx tt = ItemTypeEx.of(targeted);
            Optional<ItemStack> stack = chest.getInventory()
                    .query(QueryOperationTypes.ITEM_STACK_CUSTOM.of((s)->{
                            ItemTypeEx st = ItemTypeEx.of(s);
                            return (!st.equals(tt)) &&
                            Calculator.getValueFor(st).isPresent();
                    })).poll(isTier2?64:1);
            if (stack.isPresent()) {
                emcDelta = Calculator.getValueFor(stack.get()).get();
                emcStore = emcStore.add(emcDelta);
            }
        }
        if (emcStore.compareTo(emcTarget)>=0) {
            ItemStack insert = ItemStack.builder().fromSnapshot(targeted).quantity(1).build();
            chest.getInventory().offer(insert);
            if (insert.isEmpty()) { //was accepted
                emcStore = emcStore.subtract(emcTarget);
                placementFailed = false;
            } else {
                placementFailed = true;
            }
        }
    }

    @Override
    Text getHoloText() {
        if (emcTarget.compareTo(BigInteger.ZERO)<=0)
            return Text.of(TextColors.RED, "<NO TARGET> ", TextColors.RESET, emcStore.toString(), TextColors.YELLOW, "EMC");
        else {
            double done = emcStore.multiply(BigInteger.valueOf(10000)).divide(emcTarget).doubleValue()/100.0;
            return Text.of(targeted.getType().getName(), " ", TextColors.BLUE, String.format("%.2f%% ", done), TextColors.RESET,
                    emcStore.toString(), "/",
                    emcTarget.toString(), TextColors.YELLOW, "EMC");
        }
    }

    /** item frame not required for validity, only for producing stuff */
    public static boolean validateStructure(Location<World> location) {
        BlockType typeAbove = location.getRelative(Direction.UP).getBlockType();
        if (location.getBlockType().equals(BlockTypes.CHEST) &&
            typeAbove.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
            typeAbove.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
//            EquivalentMatter.l("is Condenser!");
            return true;
        }
        return false;
    }

    @Override
    protected BigInteger offerEMC(BigInteger amount) {
        if (targeted == null) {
            return amount;
        } else {
            if (emcStore.compareTo(emcTarget)>=0 && placementFailed) {
                return amount;
            } else {
                emcStore = emcStore.add(amount);
                return BigInteger.ZERO;
            }
        }
    }

    @Override
    protected BigInteger pollEMC(BigInteger amount) {
        if (amount.compareTo(emcStore)>0) {
            BigInteger ret = emcStore;
            emcStore = BigInteger.ZERO;
            return ret;
        } else {
            emcStore = emcStore.subtract(amount);
            return amount;
        }
    }

    @Override
    protected BigInteger peekEMC(BigInteger amount) {
        if (amount.compareTo(emcStore)>0) {
            return emcStore;
        } else {
            return amount;
        }
    }

    @Override
    protected BigInteger getEMC() {
        return emcStore;
    }

    @Override
    protected void setEMC(BigInteger emcStored) {
        emcStore = emcStored;
    }
}

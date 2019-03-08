package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.ItemFrameUtils;
import de.dosmike.sponge.equmatterex.ItemTypeEx;
import de.dosmike.sponge.equmatterex.calculator.Calculator;
import de.dosmike.sponge.equmatterex.util.ForgeHelper;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Condenser extends Device {

    public Condenser(Location<World> baseBlockLocation) {
        super(baseBlockLocation, Type.CONDENSER);
    }

    private BigInteger emcStore = BigInteger.ZERO;
    private ItemStackSnapshot targeted=null;
    private ItemTypeEx targetType;
    private BigInteger emcDelta = BigInteger.ZERO;
    private BigInteger emcTarget = BigInteger.ZERO;

    private int secondTimer = 15; //every 20 ticks is a second
    boolean isTier2 = false, placementFailed=true;

    private static Set<ItemTypeEx> listItemTypes = new HashSet<>();
    private static boolean blacklistItemTypes = true;
    private static Set<ItemTypeEx> listIgnoreNBT = new HashSet<>();
    private static boolean blacklistIgnoreNBT = true;
    private static double efficiency = 1.0;

    private static final ParticleEffect angryPFX = ParticleEffect.builder()
            .type(ParticleTypes.ANGRY_VILLAGER)
            .quantity(3)
            .velocity(new Vector3d(0,0.1,0))
            .build();

    @Override
    void tick() {
        if (++secondTimer >= 20) {
            secondTimer = 0;

            if (ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, baseLocation.getRelative(Direction.UP)))
                isTier2 = true;

            Optional<ItemFrame> item = ItemFrameUtils.getItemFramFrom(baseLocation, baseLocation.getBlock().get(Keys.DIRECTION).orElse(Direction.NONE));
            if (item.isPresent()) {
                Optional<ItemStackSnapshot> stack = item.get().get(Keys.REPRESENTED_ITEM);
                targeted = stack.orElse(null);
                targetType = targeted == null ? null : ItemTypeEx.of(targeted);
                emcTarget = BigInteger.ZERO;
                if (targeted!=null) {
                    if (canDuplicate(targetType))
                        emcTarget = Calculator.getValueFor(targeted).orElse(BigInteger.ZERO);
                    else {
                        Location<World> at = item.get().getLocation();
                        at.getExtent().spawnParticles(
                                angryPFX, at.getPosition()
                        );
                        at.getExtent().playSound(
                                SoundTypes.ENTITY_VILLAGER_NO,
                                SoundCategories.BLOCK,
                                at.getPosition(),
                                1.0
                        );
                    }
                }
            } else {
//                EquivalentMatter.l("No target");
                targeted = null;
            }
        }
        if (targeted==null || emcTarget.compareTo(BigInteger.ZERO)<=0 || !canDuplicate(targetType)) return;

        TileEntityCarrier chest = (TileEntityCarrier) baseLocation.getTileEntity().get();
        if (ForgeHelper.isOfType(ForgeHelper.CHEST, baseLocation)) {
            Optional<ItemStack> stack = chest.getInventory()
                    .query(QueryOperationTypes.ITEM_STACK_CUSTOM.of((s)->{
                            ItemTypeEx st = ItemTypeEx.of(s);
                            return (!st.equals(targetType)) &&
                            Calculator.getValueFor(st).isPresent();
                    })).poll(isTier2?64:1);
            if (stack.isPresent()) {
                emcDelta = BigDecimal.valueOf(efficiency).multiply(
                        new BigDecimal(Calculator.getValueFor(stack.get()).get())
                ).toBigInteger();
                emcStore = emcStore.add(emcDelta);
            }
        }
        if (emcStore.compareTo(emcTarget)>=0) {
            boolean fullCopy = duplicateWithNBT(targetType);
            ItemStack insert;
            if (fullCopy)
                insert = ItemStack.builder().fromSnapshot(targeted).quantity(1).build();
            else
                insert = targetType.itemStack();
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
        if (ForgeHelper.isOfType(ForgeHelper.CHEST, location) &&
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, typeAbove) ||
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, typeAbove)) {
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


    public static void loadNBTblacklist(Collection<String> listItemTypes, boolean blacklistItemTypes) {
        Condenser.listIgnoreNBT.clear();
        if (listItemTypes != null)
            for (String s : listItemTypes) {
                Optional<ItemTypeEx> type = ItemTypeEx.valueOf(s);
                if (!type.isPresent())
                    EquivalentMatter.w("Invalid Item Type (Condenser listDuplicateNBT) %s", s);
                else
                    Condenser.listIgnoreNBT.add(type.get());
            }
        Condenser.blacklistIgnoreNBT = blacklistItemTypes;
    }

    /** returns whether the condenser should duplicate these items with or without
     * nbt tags. depends on device.conf */
    public static boolean duplicateWithNBT(ItemTypeEx type) {
        if (blacklistIgnoreNBT)
            return !listIgnoreNBT.contains(type);
        else
            return listIgnoreNBT.contains(type);
    }

    public static void loadItemTypeBlacklist(Collection<String> listItemTypes, boolean blacklistItemTypes) {
        Condenser.listItemTypes.clear();
        if (listItemTypes != null)
            for (String s : listItemTypes) {
                Optional<ItemTypeEx> type = ItemTypeEx.valueOf(s);
                if (!type.isPresent())
                    EquivalentMatter.w("Invalid Item Type (Condenser listItemType) %s", s);
                else
                    Condenser.listItemTypes.add(type.get());
            }
        Condenser.blacklistItemTypes = blacklistItemTypes;
    }

    public static boolean canDuplicate(ItemTypeEx type) {
        if (blacklistItemTypes)
            return !listItemTypes.contains(type);
        else
            return listItemTypes.contains(type);
    }

    public static double getEfficiency() {
        return efficiency;
    }
    public static void setEfficiency(double efficiency) {
        Condenser.efficiency = Math.max(0.0, Math.min(1.0, efficiency));
    }

}

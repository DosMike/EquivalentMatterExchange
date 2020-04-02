package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.equmatterex.util.ForgeHelper;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.block.GroundLuminanceProperty;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Collector extends Device {

    public Collector(Location<World> baseBlockLocation) {
        super(baseBlockLocation, Type.COLLECTOR);
    }

    private long emcStore = 0, emcMaxStore = 10_000;
    private double emcDelta = 0.0;
    private double emcCollect = 0.0;
    private static BigInteger maxOutput = BigInteger.valueOf(4);

    private int secondTimer = 15; //every 20 ticks is a second
    private double cachedLightLevel = 0;
    private static final Set<Direction> around = new HashSet<>();
    static {
        around.add(Direction.NORTH);
        around.add(Direction.EAST);
        around.add(Direction.SOUTH);
        around.add(Direction.WEST);
    }
    private boolean isTier2 = false, storageLimit=false;

    @Override
    void tick() {
        if (++secondTimer >= 20) {
            secondTimer = 0;

            if (ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, baseLocation.getRelative(Direction.UP)))
                isTier2 = true;

            cachedLightLevel = baseLocation.getRelative(Direction.UP)
                    .getProperty(GroundLuminanceProperty.class)
                    .map(AbstractProperty::getValue).orElse(0.0);

            //empty furnace
            baseLocation.getTileEntity().map(te->(TileEntityCarrier)te).ifPresent(tec->{
                AtomicBoolean removedThings = new AtomicBoolean(false);
                tec.getInventory().slots().forEach(slot->{
                    Optional<ItemStack> fuel = slot.poll();
                    if (fuel.isPresent()) {
                        Item item = (Item)baseLocation.getExtent().createEntity(EntityTypes.ITEM,
                                baseLocation.getPosition().add( .5,1.1,.5 ));
                        item.offer(Keys.REPRESENTED_ITEM, fuel.get().createSnapshot());
                        item.setVelocity(new Vector3d(0,.1,0));
                        baseLocation.getExtent().spawnEntity(item);

                        removedThings.set(true);
                    }
                });
                if (removedThings.get())
                    baseLocation.getExtent().playSound(SoundTypes.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategories.BLOCK, baseLocation.getPosition(), 1.0, 1.0);
            });

            //update gain
            if (baseLocation.getExtent().getDimension().getType().equals(DimensionTypes.NETHER)) {
                emcDelta = 4;
            } else {
                emcDelta = 0.25 + (3.75*cachedLightLevel/14.0);
            }
            emcDelta/=isTier2?2:20; //20 tps, delta above is per second

        }

        emcCollect += emcDelta;
        if (emcCollect > 1) {
            emcStore += (long)emcCollect; //throw decimals
            emcCollect = emcCollect%1; //keep decimals
        }
        if (emcStore > emcMaxStore) {
            emcStore = emcMaxStore;
            storageLimit = true;
        } else {
            storageLimit = false;
        }
        //spread emc to adjacent devices unless collector
        List<Device> devices = around.stream()
                .map(dir->baseLocation.getRelative(dir))
                .map(DeviceRegistry::findDevice)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(d->!(d instanceof Collector))
                .collect(Collectors.toList());
        if (devices.size()>0 && emcStore>=devices.size()) {
            BigInteger perDevice;
            if (isTier2) {
                perDevice = BigInteger.valueOf(emcStore / devices.size());
                //maximum output rate
                if (perDevice.compareTo(maxOutput) > 0) perDevice = maxOutput;
            } else {
                perDevice = BigInteger.ONE;
            }

            BigInteger remain = BigInteger.ZERO;
            for (Device device : devices) {
                remain = remain.add(device.offerEMC(perDevice));
            }
            emcStore = emcStore - perDevice.longValue()*devices.size() + remain.longValue();
        }
    }

    @Override
    Text getHoloText() {
        return Text.of(storageLimit?TextColors.GOLD:TextColors.GREEN, String.format("+%.2f ", emcDelta*20), TextColors.BLUE, String.format("%.2f%% ", (double)emcStore*100.0/emcMaxStore), TextColors.RESET, emcStore, "/", emcMaxStore, TextColors.YELLOW, "EMC");
    }

    /** item frame not required for validity, only for producing stuff */
    public static boolean validateStructure(Location<World> location) {
        BlockType typeAbove = location.getRelative(Direction.UP).getBlockType();
        BlockType type = location.getBlockType();
        if ((ForgeHelper.isOfType(ForgeHelper.FURNACE, type) ||
            ForgeHelper.isOfType(ForgeHelper.LIT_FURNACE, type)) &&
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, typeAbove) ||
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, typeAbove)) {
            return true;
        }
        return false;
    }

    @Override
    protected BigInteger offerEMC(BigInteger amount) {
        return amount;
    }

    @Override
    protected BigInteger pollEMC(BigInteger amount) {
        BigInteger contains = BigInteger.valueOf(emcStore);
        if (amount.compareTo(contains)>0) {
            emcStore = 0L;
            return contains;
        } else {
            emcStore = contains.subtract(amount).longValue();
            return amount;
        }
    }

    @Override
    protected BigInteger peekEMC(BigInteger amount) {
        BigInteger contains = BigInteger.valueOf(emcStore);
        if (amount.compareTo(contains)>0) {
            return contains;
        } else {
            return amount;
        }
    }

    @Override
    protected BigInteger getEMC() {
        return BigInteger.valueOf(emcStore);
    }

    @Override
    protected void setEMC(BigInteger emcStored) {
        emcStore = emcStored.longValue();
    }
}

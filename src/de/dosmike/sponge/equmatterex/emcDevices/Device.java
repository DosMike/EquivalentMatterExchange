package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.impl.DeviceOwnerDataImpl;
import de.dosmike.sponge.equmatterex.customNBT.impl.EMCStoreDataImpl;
import de.dosmike.sponge.equmatterex.customNBT.impl.HoloVisibleDataImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.UUID;

public abstract class Device {

    public enum Type {
        COLLECTOR(true, true),
        CONDENSER(true, true),
        TRANSMUTATION_TABLE(false, false);

        private final boolean holo, upgradeable;
        public boolean hasHologram() {
            return holo;
        }
        public boolean isUpgradeable() {
            return upgradeable;
        }
        Type(boolean hasHologram, boolean isUpgradeable) {
            holo = hasHologram;
            upgradeable = isUpgradeable;
        }

        @Override
        public String toString() {
            return name().charAt(0)+name().toLowerCase().substring(1);
        }
    }

    protected Location<World> baseLocation;
    public Location<World> getLocation() {
        return baseLocation;
    }
    private Type type;
    public Type getType() {
        return type;
    }
    private UUID owner=null;
    public UUID getOwner() {
        return owner;
    }
    //cache for armorstand entities, so holo() does not need to search every time
    private UUID holoEntityIDCache = null;
    private Vector3d holoPositionCache;
    private static final float holoScanDistance = 0.4f;
    protected static final Vector3d holoBaseOffset = new Vector3d(0.5, 1.1, 0.5);

    public Device(Location<World> baseBlockLocation, Type type) {
        this.baseLocation = baseBlockLocation;
        this.type = type;
        this.holoPositionCache = this.baseLocation.getPosition().add(holoBaseOffset);
//        this.owner = owner;
    }
    /** for loading purposes */
    public void setOwner(UUID owner) {
        this.owner = owner;
    }
    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(owner); //null safe (in case the owner property was lost or never set)
    }
    protected boolean hideText = false;
    /** returns wether this device currently displays holograms */
    public boolean isHoloVisible() {
        return !hideText;
    }
    /** has no effect if the device does not have a holo to begin with */
    public void setHoloVisible(boolean visible) {
        if (!type.hasHologram()) return;
        hideText =! visible;
        if (hideText) {
            ArmorStand stand = null;
            if (holoEntityIDCache != null) stand = (ArmorStand) baseLocation.getExtent().getEntity(holoEntityIDCache)
                    .filter(e -> e instanceof ArmorStand).orElse(null);
            if (stand == null) stand = (ArmorStand)baseLocation.getExtent().getNearbyEntities(holoPositionCache,holoScanDistance)
                    .stream()
                    .filter(e -> e instanceof ArmorStand)
                    .findAny().orElse(null);
            if (stand != null) stand.remove();
        }
    }

    abstract void tick();
    /** needs to be overwritten if type specifies */
    Text getHoloText() {
        if (type.hasHologram())
            throw new NotImplementedException("Type "+type.name().toLowerCase()+" requires getHoloText() to be implemented");
        return null;
    }

    /**
     * with this function the device can be filled with emc.
     * any emc that exceeds the storage capacity will be returned.
     */
    protected abstract BigInteger offerEMC(BigInteger amount);
    /**
     * tries to take the specified emc from this device.
     * returns the actual available emc in case more was specified.
     */
    protected abstract BigInteger pollEMC(BigInteger amount);
    /**
     * same as pollEMC but without actually removing the emc.
     */
    protected abstract BigInteger peekEMC(BigInteger amount);
    /**
     * returns the amount of emc currently in this device
     */
    protected abstract BigInteger getEMC();
    /**
     * used during loading to restore the saved amount of EMC
     */
    protected abstract void setEMC(BigInteger emcStored);

    /** updates the blocks holo, if necessary */
    final void holo() {
        if (type.hasHologram() && !hideText) {
            ArmorStand stand = null;
            if (holoEntityIDCache != null) {
                stand = (ArmorStand) baseLocation.getExtent().getEntity(holoEntityIDCache)
                        .filter(e -> e instanceof ArmorStand).orElse(null);
            }
            //check if armorstand is absent
            if (stand == null) {
                //scan for an existing armorstand
                stand = baseLocation.getExtent().getNearbyEntities(holoPositionCache,holoScanDistance)
                        .stream()
                        .filter(e -> e instanceof ArmorStand)
                        .findAny()
                        .map(e -> (ArmorStand) e)
                        .orElseGet(() -> { //if there's none, create one
                            ArmorStand createdStand = (ArmorStand) baseLocation.getExtent().createEntity(EntityTypes.ARMOR_STAND, holoPositionCache);
                            createdStand.offer(Keys.HAS_GRAVITY, false);
                            createdStand.offer(Keys.AI_ENABLED, false);
                            createdStand.offer(Keys.ARMOR_STAND_HAS_ARMS, false);
                            createdStand.offer(Keys.ARMOR_STAND_HAS_BASE_PLATE, false);
                            createdStand.offer(Keys.ARMOR_STAND_MARKER, true);
                            createdStand.offer(Keys.INVISIBLE, true);
                            createdStand.offer(Keys.CUSTOM_NAME_VISIBLE, true);
                            baseLocation.spawnEntity(createdStand);
                            Task.builder().delayTicks(1).execute(()->{
                                if (!createdStand.isRemoved() && createdStand.isLoaded() && baseLocation.getExtent().isLoaded())
                                    createdStand.setLocation(baseLocation.getExtent().getLocation(holoPositionCache));
                            });
                            return createdStand;
                        });
                holoEntityIDCache = stand.getUniqueId();
            }
            stand.offer(Keys.DISPLAY_NAME, getHoloText());
        }
    }

    public static boolean validate(Location<World> location, Device device) {
        if (device instanceof Condenser) {
            return Condenser.validateStructure(location);
        } else if (device instanceof Collector) {
            return Collector.validateStructure(location);
        } else if (device instanceof TransmutationTable) {
            return TransmutationTable.validateStructure(location);
        }
        return false;
    }

    public void safeNBT() {
        //store custom data
        TileEntity te = baseLocation.getRelative(Direction.UP).getTileEntity().get();
        if (!te.offer(new EMCStoreDataImpl(getEMC())).isSuccessful())
            EquivalentMatter.w("Was not able to safe %s", CustomNBT.EMC.getName());
        if (type.hasHologram()) {
            if (!te.offer(new HoloVisibleDataImpl(isHoloVisible())).isSuccessful())
                EquivalentMatter.w("Was not able to safe %s", CustomNBT.HOLO_VISIBLE.getName());
        }
        if (owner != null) {
            if (!te.offer(new DeviceOwnerDataImpl(getOwner())).isSuccessful())
                EquivalentMatter.w("Was not able to safe %s", CustomNBT.DEVICE_OWNER.getName());
        } else if (te.getKeys().contains(CustomNBT.DEVICE_OWNER)) {
            te.remove(CustomNBT.DEVICE_OWNER);
        }
    }
    public void loadNBT() {
        //restore saved values
        TileEntity te = baseLocation.getRelative(Direction.UP).getTileEntity().get();
        te.get(CustomNBT.EMC).ifPresent(this::setEMC);
        hideText = !te.get(CustomNBT.HOLO_VISIBLE).orElse(true);
        setOwner(te.get(CustomNBT.DEVICE_OWNER).orElse(null));
    }

}

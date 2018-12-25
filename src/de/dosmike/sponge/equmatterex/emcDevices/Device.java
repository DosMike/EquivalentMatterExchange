package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.equmatterex.customNBT.impl.HoloVisibleDataImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;

public abstract class Device {

    public enum Type {
        COLLECTOR(true),
        CONDENSOR(true),
        TRANSMUTATION_TABLE(false);

        private final boolean holo;
        public boolean hasHologram() {
            return holo;
        }
        Type(boolean hasHologram) {
            holo = hasHologram;
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
//    private UUID owner;

    public Device(Location<World> baseBlockLocation, Type type) {
        this.baseLocation = baseBlockLocation;
        this.type = type;
//        this.owner = owner;
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
            baseLocation.getExtent().getNearbyEntities(baseLocation.getPosition().add(holoBaseOffset), 0.1)
                    .stream()
                    .filter(e -> e instanceof ArmorStand)
                    .findAny()
                    .ifPresent(Entity::remove);
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

    protected static final Vector3d holoBaseOffset = new Vector3d(0.5, 1.1, 0.5);
    /** updates the blocks holo, if necessary */
    final void holo() {
        if (type.hasHologram() && !hideText) {
            ArmorStand stand = baseLocation.getExtent().getNearbyEntities(baseLocation.getPosition().add(holoBaseOffset), 0.1)
                    .stream()
                    .filter(e -> e instanceof ArmorStand)
                    .findAny()
                    .map(e -> (ArmorStand) e)
                    .orElseGet(() -> {
                        ArmorStand createdStand = (ArmorStand) baseLocation.getExtent().createEntity(EntityTypes.ARMOR_STAND, baseLocation.getPosition().add(holoBaseOffset));
                        createdStand.offer(Keys.HAS_GRAVITY, false);
                        createdStand.offer(Keys.AI_ENABLED, false);
                        createdStand.offer(Keys.ARMOR_STAND_HAS_ARMS, false);
                        createdStand.offer(Keys.ARMOR_STAND_HAS_BASE_PLATE, false);
                        createdStand.offer(Keys.ARMOR_STAND_MARKER, true);
                        createdStand.offer(Keys.INVISIBLE, true);
                        createdStand.offer(Keys.CUSTOM_NAME_VISIBLE, true);
                        baseLocation.spawnEntity(createdStand);
                        return createdStand;
                    });
            stand.offer(Keys.DISPLAY_NAME, getHoloText());
        }
    }

    public static boolean validate(Location<World> location, Device device) {
        if (device instanceof Condenser) {
            return Condenser.validateStructure(location);
        } else if (device instanceof Collector) {
            return Collector.validateStructure(location);
        }
        return false;
    }
}

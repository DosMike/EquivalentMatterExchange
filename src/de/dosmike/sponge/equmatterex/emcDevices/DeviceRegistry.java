package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3i;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.impl.EMCStoreDataImpl;
import de.dosmike.sponge.equmatterex.customNBT.impl.HoloVisibleDataImpl;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceRegistry {

    private static Map<Location<World>, Device> deviceMap = new HashMap<>();
    private static final Object deviceLock = new Object();

    /** build devices from main block at chunk load */
    public static void onLoadChunk(Chunk chunk) {
        synchronized (deviceLock) {
            Vector3i min = chunk.getBlockMin(), max = chunk.getBlockMax();
            for (int y = min.getY(); y <= max.getY(); y++)
                for (int z = min.getZ(); z <= max.getZ(); z++)
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        BlockState inspect = chunk.getBlock(x, y, z);
                        Location<World> globalLocation = chunk.getWorld().getLocation(x, y, z);
                        Optional<Device> device = Optional.empty();
                        if (inspect.getType().equals(BlockTypes.CHEST))
                            device = tryBuild(Device.Type.CONDENSOR, globalLocation);
                        else if (inspect.getType().equals(BlockTypes.FURNACE) ||
                                inspect.getType().equals(BlockTypes.LIT_FURNACE))
                            device = tryBuild(Device.Type.COLLECTOR, globalLocation);

                        if (device.isPresent()) {
                            deviceMap.put(globalLocation, device.get());
                            //restore saved values
                            final Device fDevice = device.get();
                            globalLocation.getRelative(Direction.UP).getTileEntity().get()
                                    .get(CustomNBT.EMC).ifPresent(fDevice::setEMC);
                            globalLocation.getRelative(Direction.UP).getTileEntity().get()
                                    .get(CustomNBT.HOLO_VISIBLE).ifPresent(visible->fDevice.hideText=!visible);
                        }
                    }
        }
    }

    public static void onUnloadChunk(Chunk chunk) {
        synchronized (deviceLock) {
            Vector3i min = chunk.getBlockMin(), max = chunk.getBlockMax();
            List<Location<World>> devices = deviceMap.entrySet().stream()
                    .filter((k)->{
                        Location<World> at = k.getKey();
                        //check if device is in unloading chunk
                        return at.getExtent().getUniqueId().equals(chunk.getWorld().getUniqueId()) &&
                        at.getBlockX()>=min.getX() && at.getBlockX()<=max.getX() &&
                        at.getBlockY()>=min.getY() && at.getBlockY()<=max.getY() &&
                        at.getBlockZ()>=min.getZ() && at.getBlockZ()<=max.getZ();
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            for (Location<World> target : devices) {
                deviceMap.remove(target);
            }
        }
    }

    /**
     * tries to build device from base block location, faster for use in chunk loading
     * note: not using validate to speed improvements
     */
    public static Optional<Device> tryBuild(Device.Type deviceType, Location<World> loc){
        BlockType type = loc.getRelative(Direction.UP).getBlockType();
        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            if (deviceType == Device.Type.CONDENSOR) {
//                EquivalentMatter.l("is Condenser!");
                return Optional.of(new Condenser(loc));
            } else if (deviceType == Device.Type.COLLECTOR) {
//                EquivalentMatter.l("is Collector!");
                return Optional.of(new Collector(loc));
            }
        }
        return Optional.empty();
    }
    /**
     * tries to build device from base block location, faster for use in chunk loading
     * note: not using validate to speed improvements
     * @param loc the block that was placed
     * @param blockAtLoc the block to assume at loc (since events may make this unfetchable from location)
     */
    public static Optional<Device> tryPlaceDevice(Location<World> loc, BlockState blockAtLoc) {
        Location<World> base = loc;
        BlockType type = blockAtLoc.getType();
        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR)  ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            base = loc.getRelative(Direction.DOWN);
        }
        BlockType bt = base.getBlockType();
        Optional<Device> device;
        if (bt.equals(BlockTypes.CHEST)) {
            device = tryBuild(Device.Type.CONDENSOR, base);
        } else if (bt.equals(BlockTypes.FURNACE) ||
                    bt.equals(BlockTypes.LIT_FURNACE)) {
            device = tryBuild(Device.Type.COLLECTOR, base);
        } else {
            device = Optional.empty();
        }
        if (device.isPresent())
            synchronized (deviceLock) {
                deviceMap.put(base, device.get());
            }
        return device;
    }
    /**
     * removes a device if any is found at this lcoation
     * note: not using validate to speed improvements
     * @param loc the block that was broken
     * @param blockAtLoc the block to assume at loc (since events may make this unfetchable from location)
     * @return the device that used to be there
     */
    public static Optional<Device> tryBreakDevice(Location<World> loc, BlockState blockAtLoc) {
        Location<World> base = loc;
        BlockType type = blockAtLoc.getType();
        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            base = loc.getRelative(Direction.DOWN);
        }
        return unregisterDevice(base);
    }
    

    /** ticks all currently loaded devices and DELETES unloaded ones */
    public static void deviceTick() {
        synchronized (deviceLock) {
            try {
//                EquivalentMatter.l("There are %d devices", deviceMap.size());
                Set<Location<World>> broken = new HashSet<>();
                deviceMap.forEach((k, v) -> {
                    if (!Device.validate(k, v))
                        broken.add(k);
                });
                for (Location<World> b : broken) {
                    Device d = deviceMap.remove(b);
                    EquivalentMatter.l("Removed invalid device %s at %d %d %d", d.getClass().getSimpleName(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
                }
                Collection<Device> devices = new HashSet<>(deviceMap.values());
                devices.forEach(d -> {
                    try {
                        d.tick();
                        d.holo();
                        //store custom data
                        TileEntity te = d.baseLocation.getRelative(Direction.UP).getTileEntity().get();
                        te.offer(new EMCStoreDataImpl(d.getEMC()));
                        te.offer(new HoloVisibleDataImpl(d.isHoloVisible()));
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void unregisterDevice(Device device) {
        synchronized (deviceLock) {
            Location<World> at = device.getLocation();
            if (device.getType().hasHologram()) {
                at.getExtent().getNearbyEntities(at.getPosition().add(Device.holoBaseOffset), 0.1)
                        .stream()
                        .filter(e -> e instanceof ArmorStand)
                        .findAny()
                        .ifPresent(Entity::remove);
            }
            deviceMap.remove(at);
        }
    }
    public static Optional<Device> unregisterDevice(Location<World> loc) {
        synchronized (deviceLock) {
            Optional<Device> device = Optional.ofNullable(deviceMap.get(loc));
            if (device.isPresent()) {
                if (device.get().getType().hasHologram()) {
                    loc.getExtent().getNearbyEntities(loc.getPosition().add(Device.holoBaseOffset), 0.1)
                            .stream()
                            .filter(e -> e instanceof ArmorStand)
                            .findAny()
                            .ifPresent(Entity::remove);
                }
                deviceMap.remove(loc);
            }
            return device;
        }
    }

    public static Optional<Device> findDevice(Location<World> loc) {
        synchronized (deviceLock) {
            return Optional.ofNullable(deviceMap.get(loc));
        }
    }

}

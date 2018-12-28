package de.dosmike.sponge.equmatterex.emcDevices;

import com.flowpowered.math.vector.Vector3i;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.DaylightDetector;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class DeviceRegistry {

    private static Map<Location<World>, Device> deviceMap = new HashMap<>();
    private static final Object deviceLock = new Object();

    private static Map<Class<? extends Device>, DevicePermission> permissions = new HashMap<>();
    static {
        permissions.put(Collector.class, new DevicePermission(Device.Type.COLLECTOR, false, false));
        permissions.put(Condenser.class, new DevicePermission(Device.Type.CONDENSER, false, false));
        permissions.put(TransmutationTable.class, new DevicePermission(Device.Type.TRANSMUTATION_TABLE, false, false));
    }
    public static DevicePermission getPermissions(Device device) {
        Class<? extends Device> clz = device.getClass();
        return permissions.get(clz);
    }
    public static DevicePermission getPermissions(Class<? extends Device> device) {
        return permissions.get(device);
    }

    /** build devices from main block at chunk load */
    public static void onLoadChunk(Chunk chunk) {
        synchronized (deviceLock) {
            //should be faster than scanning blockwise
            chunk.getTileEntities(te->te instanceof DaylightDetector)
                    .forEach(te->loadDeviceFromLocation(te.getLocation())
            );
        }
    }
    private static void loadDeviceFromLocation(Location<World> location) {
        Location<World> globalLocation = location.getRelative(Direction.DOWN);
        BlockState inspect = globalLocation.getBlock();
        Optional<Device> device = Optional.empty();
        if (inspect.getType().equals(BlockTypes.CHEST))
            device = tryBuild(Device.Type.CONDENSER, globalLocation);
        else if (inspect.getType().equals(BlockTypes.FURNACE) ||
                inspect.getType().equals(BlockTypes.LIT_FURNACE))
            device = tryBuild(Device.Type.COLLECTOR, globalLocation);
        else if (inspect.getType().equals(BlockTypes.CRAFTING_TABLE))
            device = tryBuild(Device.Type.TRANSMUTATION_TABLE, globalLocation);

        if (device.isPresent()) {
            device.get().loadNBT();
            //store device as loaded in map
            deviceMap.put(globalLocation, device.get());
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
                Device device = deviceMap.remove(target);
                device.safeNBT();

                if (device.getType().hasHologram()) {
                    chunk.getNearbyEntities(device.baseLocation.getPosition().add(Device.holoBaseOffset), 0.1)
                            .stream()
                            .filter(e -> e instanceof ArmorStand)
                            .findAny()
                            .ifPresent(Entity::remove);
                }
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
            if (deviceType == Device.Type.CONDENSER) {
                return Optional.of(new Condenser(loc));
            } else if (deviceType == Device.Type.COLLECTOR) {
                return Optional.of(new Collector(loc));
            } else if (deviceType == Device.Type.TRANSMUTATION_TABLE)
                return Optional.of(new TransmutationTable(loc));
        }
        return Optional.empty();
    }
    /**
     * tries to build device from base block location, faster for use in chunk loading
     * note: not using validate to speed improvements
     * @param loc the block that was placed
     * @param blockAtLoc the block to assume at loc (since events may make this unfetchable from location)
     */
    public static Optional<Device> tryPlaceDevice(@Nullable Player player, Location<World> loc, BlockState blockAtLoc) {
        Location<World> base = loc;
        BlockType type = blockAtLoc.getType();
        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR)  ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            base = loc.getRelative(Direction.DOWN);
        }
        BlockType bt = base.getBlockType();
        Optional<Device> device;
        if (bt.equals(BlockTypes.CHEST)) {
            if (player != null && !permissions.get(Condenser.class).hasPermissionCreate(player)) {
                device = Optional.empty();
                player.sendMessage(Text.of(TextColors.RED, "You are not allowed to build a Condenser"));
            } else
                device = tryBuild(Device.Type.CONDENSER, base);
        } else if (bt.equals(BlockTypes.FURNACE) ||
                    bt.equals(BlockTypes.LIT_FURNACE)) {
            if (player != null && !permissions.get(Collector.class).hasPermissionCreate(player)) {
                device = Optional.empty();
                player.sendMessage(Text.of(TextColors.RED, "You are not allowed to build a Collector"));
            } else
                device = tryBuild(Device.Type.COLLECTOR, base);
        } else if (bt.equals(BlockTypes.CRAFTING_TABLE)) {
            if (player != null && !permissions.get(TransmutationTable.class).hasPermissionCreate(player)) {
                device = Optional.empty();
                player.sendMessage(Text.of(TextColors.RED, "You are not allowed to build a Transmutation Table"));
            } else
                device = tryBuild(Device.Type.TRANSMUTATION_TABLE, base);
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
    public static Optional<Device> tryBreakDevice(@Nullable Player player, Location<World> loc, BlockState blockAtLoc) {
        Location<World> base = loc;
        BlockType type = blockAtLoc.getType();
        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            base = loc.getRelative(Direction.DOWN);
        }
        Optional<Device> target = findDevice(base);
        if (!target.isPresent()) return Optional.empty();
        Device device = target.get();
        if (player != null) {
            if (!device.isOwner(player) && !getPermissions(device).hasPermissionInteract(player)) {
                player.sendMessage(Text.of(TextColors.RED, "You don't have permission to do that"));
                return Optional.empty();
            }
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

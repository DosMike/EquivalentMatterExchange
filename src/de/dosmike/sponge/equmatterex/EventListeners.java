package de.dosmike.sponge.equmatterex;

import de.dosmike.sponge.equmatterex.emcDevices.Collector;
import de.dosmike.sponge.equmatterex.emcDevices.Condenser;
import de.dosmike.sponge.equmatterex.emcDevices.Device;
import de.dosmike.sponge.equmatterex.emcDevices.DeviceRegistry;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EventListeners {

    @Listener
    public void onLoadChunk(LoadChunkEvent event) {
        DeviceRegistry.onLoadChunk(event.getTargetChunk());
    }

    @Listener
    public void onUnoadChunk(UnloadChunkEvent event) {
        DeviceRegistry.onUnloadChunk(event.getTargetChunk());
    }

    @Listener
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        if (event.isCancelled()) return;
        itPlace: for (Transaction<BlockSnapshot> tranny : event.getTransactions()) {
            if (!tranny.isValid()||
                !tranny.getOriginal().getState().getType().equals(BlockTypes.AIR)||
                !tranny.getFinal().getLocation().isPresent()) continue;
            //prevent double-chesting devices
            if (tranny.getFinal().getState().getType().equals(BlockTypes.CHEST)) {
                Set<Direction> around = new HashSet<>();
                around.add(Direction.NORTH);
                around.add(Direction.EAST);
                around.add(Direction.SOUTH);
                around.add(Direction.WEST);
                for (Direction dir : around) {
                    Location<World> rel = tranny.getFinal().getLocation().get().getRelative(dir);
                    if (rel.getBlockType().equals(BlockTypes.CHEST) &&
                        DeviceRegistry.findDevice(rel).isPresent()) {
                        tranny.setValid(false); //prevent placing
                        continue itPlace; //don't create device
                    }
                }
            }

            Optional<Device> device = tranny.getOriginal().getLocation()
                    .flatMap(loc -> DeviceRegistry.tryPlaceDevice(loc, tranny.getFinal().getState()));
        }
    }
    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        if (event.isCancelled()) return;
        for (Transaction<BlockSnapshot> tranny : event.getTransactions()) {
            if (!tranny.isValid()) continue;
            Optional<Device> device = tranny.getOriginal().getLocation()
                    .flatMap(loc -> DeviceRegistry.tryBreakDevice(loc, tranny.getOriginal().getState()));
        }
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Secondary event) {
        Optional<Location<World>> block = event.getTargetBlock().getLocation();
        if (!block.isPresent() ||
            event.getTargetBlock().equals(BlockSnapshot.NONE)) return;
        BlockType type = event.getTargetBlock().getState().getType();

        Player player = event.getCause().first(Player.class).orElse(null);
        if (player == null) return;
        boolean isPlaceBlock = false;
        boolean isPlayerSneaking = player.get(Keys.IS_SNEAKING).orElse(false);
        Optional<ItemStack> handItem = player.getItemInHand(event.getHandType());
        if (handItem.isPresent())
            if (handItem
                .flatMap(item->item.getType().getBlock())
                .isPresent() &&
                !handItem.get().isEmpty() &&
                !handItem.get().getType().equals(ItemTypes.AIR) &&
                isPlayerSneaking)
            isPlaceBlock = true;

        if (type.equals(BlockTypes.DAYLIGHT_DETECTOR) ||
            type.equals(BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            Optional<Device> device = DeviceRegistry.findDevice(block.get().getRelative(Direction.DOWN));
            if (device.isPresent())
                if (!isPlaceBlock) {
                    if (type.equals(BlockTypes.DAYLIGHT_DETECTOR) &&
                        handItem.orElse(ItemStack.empty()).getType().equals(ItemTypes.END_CRYSTAL)) {
                        player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class))
                                .query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.END_CRYSTAL))
                                .poll(1);
                    } else {
                        if (isPlayerSneaking && handItem.orElse(ItemStack.empty()).getType().equals(ItemTypes.AIR)) {
                            device.get().setHoloVisible(!device.get().isHoloVisible());
                        }
                        event.setCancelled(true);
                    }
                }
        } else {
            Device device = DeviceRegistry.findDevice(block.get()).orElse(null);
            if (device instanceof Collector) {
                if (!isPlaceBlock)
                    event.setCancelled(true);
            } else if (device instanceof Condenser) {

            }
        }
    }

}

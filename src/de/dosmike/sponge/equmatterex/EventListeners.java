package de.dosmike.sponge.equmatterex;

import de.dosmike.sponge.equmatterex.emcDevices.*;
import de.dosmike.sponge.equmatterex.util.ForgeHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EventListeners {

    private static Set<Direction> around = new HashSet<>();
    static {
        around.add(Direction.NORTH);
        around.add(Direction.EAST);
        around.add(Direction.SOUTH);
        around.add(Direction.WEST);
    }

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
        Optional<Player> player = event.getCause().first(Player.class);
        if (event.isCancelled()) return;
        itPlace: for (Transaction<BlockSnapshot> tranny : event.getTransactions()) {
            if (!tranny.isValid()||
                !tranny.getOriginal().getState().getType().equals(BlockTypes.AIR)||
                !tranny.getFinal().getLocation().isPresent()) continue;
            //prevent double-chesting devices
            if (ForgeHelper.isOfType(ForgeHelper.CHEST, tranny.getFinal())) {
                for (Direction dir : around) {
                    Location<World> rel = tranny.getFinal().getLocation().get().getRelative(dir);
                    if (ForgeHelper.isOfType(ForgeHelper.CHEST, rel) &&
                        DeviceRegistry.findDevice(rel).isPresent()) {
                        tranny.setValid(false); //prevent placing
                        continue itPlace; //don't create device
                    }
                }
            } else if (ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, tranny.getFinal())) {
                Location<World> loc = tranny.getFinal().getLocation().orElse(null);
                if (loc != null) {
                    loc = loc.getRelative(Direction.DOWN);
                    if (ForgeHelper.isOfType(ForgeHelper.CHEST, loc)) //chest below
                        for (Direction dir : around) {
                            if (ForgeHelper.isOfType(ForgeHelper.CHEST, loc.getRelative(dir))){ //there's a chest somewhere around -> double chest
                                tranny.setValid(false); //prevent placing
                                continue itPlace; //don't create device
                            }
                        }
                }
            }

            Optional<Device> device = tranny.getOriginal().getLocation()
                    .flatMap(loc -> DeviceRegistry.tryPlaceDevice(player.orElse(null), loc, tranny.getFinal().getState()));
            device.ifPresent(d->{
                d.setOwner(player.map(Player::getUniqueId).orElse(null));
                d.safeNBT();
            });
        }
    }
    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        if (event.isCancelled()) return;
        Player player = event.getCause().first(Player.class).orElse(null);
        for (Transaction<BlockSnapshot> tranny : event.getTransactions()) {
            if (!tranny.isValid()) continue;
            Optional<Device> device = tranny.getOriginal().getLocation()
                    .flatMap(loc -> DeviceRegistry.tryBreakDevice(player, loc, tranny.getOriginal().getState()));
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

        if (ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, type) ||
            ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR_INVERTED, type)) {
            Optional<Device> device = DeviceRegistry.findDevice(block.get().getRelative(Direction.DOWN));
            if (device.isPresent())
                if (!device.get().isOwner(player) && !DeviceRegistry.getPermissions(device.get()).hasPermissionCreate(player))
                    event.setCancelled(true);
                else if (!isPlaceBlock) {
                    if (device.get().getType().isUpgradeable() && //check if upgradeable
                        ForgeHelper.isOfType(ForgeHelper.DAYLIGHT_DETECTOR, type) && //not yet upgraded
                        handItem.orElse(ItemStack.empty()).getType().equals(ItemTypes.END_CRYSTAL)) { //upgrade item used
                        player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class))
                                .query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.END_CRYSTAL))
                                .poll(1); //remove upgrade item
                        //don't block interaction, turning the block inverted (counts as upgraded)
                    } else {
                        if (isPlayerSneaking && handItem.orElse(ItemStack.empty()).getType().equals(ItemTypes.AIR)) {
                            device.get().setHoloVisible(!device.get().isHoloVisible());
                            device.get().safeNBT();
                        }
                        event.setCancelled(true);
                    }
                }
        } else { //blocking default interactions
            Device device = DeviceRegistry.findDevice(block.get()).orElse(null);
            if (device instanceof Collector) {
                if (!isPlaceBlock)
                    event.setCancelled(true);
            } else if (device instanceof Condenser) {
                /**/
            } else if (device instanceof TransmutationTable) {
                if (!isPlaceBlock) {
                    event.setCancelled(true);
                    if (player != null) {
                        if (DeviceRegistry.getPermissions(device).hasPermissionInteract(player))
                            TransmutationTable.openTableFor(player);
                        else
                            player.sendMessage(Text.of(TextColors.RED, "You may not use this Transmutation Table"));
                    }
                }
            }
        }
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        EMCAccount.loadFromFile(event.getTargetEntity());
    }

    @Listener
    public void onPlayerPart(ClientConnectionEvent.Disconnect event) {
        EMCAccount.saveToFile(event.getTargetEntity());
    }

    @Listener
    public void onServerShutdown(GameStoppingEvent event) {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            EMCAccount.saveToFile(player);
        }
    }

}

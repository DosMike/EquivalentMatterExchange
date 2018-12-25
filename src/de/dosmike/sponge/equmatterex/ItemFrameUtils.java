package de.dosmike.sponge.equmatterex;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class ItemFrameUtils {
    public static Optional<ItemFrame> getItemFramFrom(Location<World> attached, Direction fromWall) {
        Vector3d expected = attached.getPosition().add(.5,.5,.5).add(fromWall.asOffset().mul(.5));
        return attached.getExtent().getNearbyEntities(expected, 0.1).stream()
                .filter(e->e instanceof ItemFrame)
                .findAny()
                .map(e->(ItemFrame)e);
    }
}

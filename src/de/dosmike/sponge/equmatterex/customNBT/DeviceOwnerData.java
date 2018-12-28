package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.UUID;

/**
 * As per Sponge Docs custom data can only hold int, String and double "and such".
 * As a matter of fact it seems that the way I'm implementing it all other primitives
 * (although being store-able) can not be written into world/chunk data as persistent
 * data and thus end up in the failedMaipulator data-List.
 *
 * That's why this actually boolean value is stored as int
 *
 * @see <a href="https://docs.spongepowered.org/stable/en/plugin/data/custom/serialization.html">Sponge Docs - Data API - Serializing Custom Data</a>
 */
public interface DeviceOwnerData extends DataManipulator<DeviceOwnerData, ImmutableDeviceOwnerData> {

    Value<UUID> deviceOwner();

}

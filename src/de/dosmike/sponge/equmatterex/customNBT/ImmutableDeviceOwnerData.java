package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.UUID;

public interface ImmutableDeviceOwnerData extends ImmutableDataManipulator<ImmutableDeviceOwnerData, DeviceOwnerData> {

    ImmutableValue<UUID> deviceOwner();

}

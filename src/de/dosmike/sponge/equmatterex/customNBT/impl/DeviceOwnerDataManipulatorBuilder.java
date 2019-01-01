package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.DeviceOwnerData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableDeviceOwnerData;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class DeviceOwnerDataManipulatorBuilder extends AbstractDataBuilder<DeviceOwnerData> implements DataManipulatorBuilder<DeviceOwnerData, ImmutableDeviceOwnerData> {

    public DeviceOwnerDataManipulatorBuilder() {
        super(DeviceOwnerData.class, 1);
    }

    @Override
    public DeviceOwnerDataImpl create() {
        return new DeviceOwnerDataImpl();
    }

    @Override
    public Optional<DeviceOwnerData> createFrom(DataHolder dataHolder) {
        return create().fill(dataHolder, MergeFunction.IGNORE_ALL);
    }

    @Override
    public Optional<DeviceOwnerData> buildContent(DataView container) throws InvalidDataException {
        return create().from(container);
    }
}

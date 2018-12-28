package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.UUID;

public class ImmutableDeviceOwnerDataImpl extends AbstractImmutableSingleData<UUID, ImmutableDeviceOwnerData, DeviceOwnerData> implements ImmutableDeviceOwnerData {

    public ImmutableDeviceOwnerDataImpl() {
        this(new UUID(0L,0L));
    }

    public ImmutableDeviceOwnerDataImpl(UUID value) {
        super(value, CustomNBT.DEVICE_OWNER);
    }

    @Override
    public ImmutableValue<UUID> deviceOwner() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.DEVICE_OWNER, getValue()).asImmutable();
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.DEVICE_OWNER.getQuery(), this.value);
    }

    @Override
    public DeviceOwnerData asMutable() {
        return new DeviceOwnerDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    protected ImmutableValue<?> getValueGetter() {
        return deviceOwner();
    }
}

package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.DeviceOwnerData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableDeviceOwnerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;
import java.util.UUID;

public class DeviceOwnerDataImpl extends AbstractSingleData<UUID, DeviceOwnerData, ImmutableDeviceOwnerData> implements DeviceOwnerData {

    public DeviceOwnerDataImpl() {
        this(new UUID (0L, 0L));
    }

    public DeviceOwnerDataImpl(UUID value) {
        super(value, CustomNBT.DEVICE_OWNER);
    }

    @Override
    public Value<UUID> deviceOwner() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.DEVICE_OWNER, getValue());
    }

    @Override
    public ImmutableDeviceOwnerDataImpl asImmutable() {
        return new ImmutableDeviceOwnerDataImpl(getValue());
    }

    @Override
    public Optional<DeviceOwnerData> fill(DataHolder dataHolder, MergeFunction overlap) {
        DeviceOwnerData merged = overlap.merge(this, dataHolder.get(DeviceOwnerData.class).orElse(null));
        setValue(merged.deviceOwner().get());
        return Optional.of(this);
    }

    @Override
    public Optional<DeviceOwnerData> from(DataContainer container) {
        return from((DataView)container);
    }

    public Optional<DeviceOwnerData> from(DataView container) {
        if (container.contains(CustomNBT.DEVICE_OWNER)) {
            UUID value = container.getObject(CustomNBT.DEVICE_OWNER.getQuery(), UUID.class).get();
            return Optional.of(setValue(value));
        }

        return Optional.empty();
    }

    @Override
    public DeviceOwnerData copy() {
        return new DeviceOwnerDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    protected Value<?> getValueGetter() {
        return deviceOwner();
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.DEVICE_OWNER.getQuery(), getValue());
    }
}

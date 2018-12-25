package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.HoloVisibleData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableHoloVisibleData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

public class ImmutableHoloVisibleDataImpl extends AbstractImmutableSingleData<Boolean, ImmutableHoloVisibleData, HoloVisibleData> implements ImmutableHoloVisibleData {

    public ImmutableHoloVisibleDataImpl() {
        this(true);
    }

    public ImmutableHoloVisibleDataImpl(boolean value) {
        super(value, CustomNBT.HOLO_VISIBLE);
    }

    @Override
    public ImmutableValue<Boolean> holoVisible() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.HOLO_VISIBLE, getValue()).asImmutable();
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.HOLO_VISIBLE.getQuery(), this.value);
    }

    @Override
    public HoloVisibleData asMutable() {
        return new HoloVisibleDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    protected ImmutableValue<?> getValueGetter() {
        return holoVisible();
    }
}

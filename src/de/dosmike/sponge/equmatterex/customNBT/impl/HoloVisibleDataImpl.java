package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.HoloVisibleData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableHoloVisibleData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;

public class HoloVisibleDataImpl extends AbstractSingleData<Boolean, HoloVisibleData, ImmutableHoloVisibleData> implements HoloVisibleData {

    public HoloVisibleDataImpl() {
        this(true);
    }

    public HoloVisibleDataImpl(boolean value) {
        super(value, CustomNBT.HOLO_VISIBLE);
    }

    @Override
    public Value<Boolean> holoVisible() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.HOLO_VISIBLE, getValue());
    }

    @Override
    public ImmutableHoloVisibleData asImmutable() {
        return new ImmutableHoloVisibleDataImpl(getValue());
    }

    @Override
    public Optional<HoloVisibleData> fill(DataHolder dataHolder, MergeFunction overlap) {
        HoloVisibleData merged = overlap.merge(this, dataHolder.get(HoloVisibleData.class).orElse(null));
        setValue(merged.holoVisible().get());
        return Optional.of(this);
    }

    @Override
    public Optional<HoloVisibleData> from(DataContainer container) {
        return from((DataView)container);
    }

    public Optional<HoloVisibleData> from(DataView container) {
        if (container.contains(CustomNBT.HOLO_VISIBLE)) {
            Boolean value = container.getBoolean(CustomNBT.HOLO_VISIBLE.getQuery()).get();
            return Optional.of(setValue(value));
        }

        return Optional.empty();
    }

    @Override
    public HoloVisibleData copy() {
        return new HoloVisibleDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    protected Value<?> getValueGetter() {
        return holoVisible();
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.HOLO_VISIBLE.getQuery(), getValue());
    }
}

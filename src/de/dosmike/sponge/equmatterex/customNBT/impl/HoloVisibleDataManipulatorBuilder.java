package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.HoloVisibleData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableHoloVisibleData;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class HoloVisibleDataManipulatorBuilder extends AbstractDataBuilder<HoloVisibleData> implements DataManipulatorBuilder<HoloVisibleData, ImmutableHoloVisibleData> {

    public HoloVisibleDataManipulatorBuilder() {
        super(HoloVisibleData.class, 1);
    }

    @Override
    public HoloVisibleData create() {
        return new HoloVisibleDataImpl();
    }

    @Override
    public Optional<HoloVisibleData> createFrom(DataHolder dataHolder) {
        return create().fill(dataHolder, MergeFunction.IGNORE_ALL);
    }

    @Override
    public Optional<HoloVisibleData> buildContent(DataView container) throws InvalidDataException {
        return create().from(container.getContainer());
    }
}

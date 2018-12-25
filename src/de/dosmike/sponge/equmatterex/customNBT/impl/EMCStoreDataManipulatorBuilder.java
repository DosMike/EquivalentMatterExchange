package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.EMCStoreData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableEMCStoreData;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class EMCStoreDataManipulatorBuilder extends AbstractDataBuilder<EMCStoreData> implements DataManipulatorBuilder<EMCStoreData, ImmutableEMCStoreData> {

    public EMCStoreDataManipulatorBuilder() {
        super(EMCStoreData.class, 1);
    }

    @Override
    public EMCStoreDataImpl create() {
        return new EMCStoreDataImpl();
    }

    @Override
    public Optional<EMCStoreData> createFrom(DataHolder dataHolder) {
        return create().fill(dataHolder, MergeFunction.IGNORE_ALL);
    }

    @Override
    public Optional<EMCStoreData> buildContent(DataView container) throws InvalidDataException {
        return create().from(container);
    }
}

package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.EMCStoreData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableEMCStoreData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.math.BigInteger;
import java.util.Optional;

public class EMCStoreDataImpl extends AbstractSingleData<BigInteger, EMCStoreData, ImmutableEMCStoreData> implements EMCStoreData {

    public EMCStoreDataImpl() {
        this(BigInteger.ZERO);
    }

    public EMCStoreDataImpl(BigInteger emcValue) {
        super(emcValue, CustomNBT.EMC);
    }

    @Override
    public Optional<EMCStoreData> fill(DataHolder dataHolder, MergeFunction overlap) {
        EMCStoreData merged = overlap.merge(this, dataHolder.get(EMCStoreData.class).orElse(null));
        setValue(merged.emcValue().get());
        return Optional.of(this);
    }

    @Override
    public Optional<EMCStoreData> from(DataContainer container) {
        return from((DataView)container);
    }

    public Optional<EMCStoreData> from(DataView container) {
        if (container.contains(CustomNBT.EMC)) {
            BigInteger value = new BigInteger(container.getString(CustomNBT.EMC.getQuery()).get(), 16);
            return Optional.of(setValue(value));
        }

        return Optional.empty();
    }

    @Override
    public EMCStoreData copy() {
        return new EMCStoreDataImpl(getValue());
    }

    @Override
    public ImmutableEMCStoreData asImmutable() {
        return new ImmutableEMCStoreDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.EMC.getQuery(), getValue().toString(16));
    }

    @Override
    public Value<BigInteger> emcValue() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.EMC, getValue());
    }

    @Override
    protected Value<?> getValueGetter() {
        return emcValue();
    }



}

package de.dosmike.sponge.equmatterex.customNBT.impl;

import de.dosmike.sponge.equmatterex.customNBT.CustomNBT;
import de.dosmike.sponge.equmatterex.customNBT.EMCStoreData;
import de.dosmike.sponge.equmatterex.customNBT.ImmutableEMCStoreData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.math.BigInteger;

public class ImmutableEMCStoreDataImpl extends AbstractImmutableSingleData<BigInteger, ImmutableEMCStoreData, EMCStoreData> implements ImmutableEMCStoreData {

    public ImmutableEMCStoreDataImpl() {
        this(BigInteger.ZERO);
    }

    public ImmutableEMCStoreDataImpl(BigInteger value) {
        super(value, CustomNBT.EMC);
    }

    @Override
    public EMCStoreData asMutable() {
        return new EMCStoreDataImpl(this.value);
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(CustomNBT.EMC.getQuery(), this.value.toString(16));
    }

    @Override
    public ImmutableValue<BigInteger> emcValue() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(CustomNBT.EMC, getValue()).asImmutable();
    }

    @Override
    protected ImmutableValue<?> getValueGetter() {
        return emcValue();
    }

    @Override
    public int getContentVersion() {
        return 1;
    }
}

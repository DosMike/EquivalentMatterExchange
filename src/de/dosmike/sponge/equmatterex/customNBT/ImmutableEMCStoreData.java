package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.math.BigInteger;

public interface ImmutableEMCStoreData extends ImmutableDataManipulator<ImmutableEMCStoreData, EMCStoreData> {

    ImmutableValue<BigInteger> emcValue();

}

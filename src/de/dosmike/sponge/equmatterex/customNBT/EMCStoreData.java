package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.value.mutable.Value;

import java.math.BigInteger;

public interface EMCStoreData extends DataManipulator<EMCStoreData, ImmutableEMCStoreData> {

    Value<BigInteger> emcValue();

}

package de.dosmike.sponge.equmatterex.calculator;

import de.dosmike.sponge.equmatterex.ItemTypeEx;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

public interface ItemValueMapper<T> extends Callable<Map<ItemTypeEx, BigInteger>> {

//    abstract int changes();
    Map<ItemTypeEx, BigInteger> compute(Collection<T> recipes);

}

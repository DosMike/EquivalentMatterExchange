package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

public interface ImmutableHoloVisibleData extends ImmutableDataManipulator<ImmutableHoloVisibleData, HoloVisibleData> {

    ImmutableValue<Boolean> holoVisible();

}

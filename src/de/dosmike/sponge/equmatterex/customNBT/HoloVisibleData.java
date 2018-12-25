package de.dosmike.sponge.equmatterex.customNBT;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.value.mutable.Value;

public interface HoloVisibleData extends DataManipulator<HoloVisibleData, ImmutableHoloVisibleData> {

    Value<Boolean> holoVisible();

}

package de.dosmike.sponge.equmatterex.customNBT;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.customNBT.impl.*;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

import java.math.BigInteger;
import java.util.UUID;

public class CustomNBT {

    public static Key<Value<BigInteger>> EMC = DummyObjectProvider.createExtendedFor(Key.class, "EMC");
    public static Key<Value<Boolean>> HOLO_VISIBLE = DummyObjectProvider.createExtendedFor(Key.class, "HOLO_VISIBLE");
    public static Key<Value<UUID>> DEVICE_OWNER = DummyObjectProvider.createExtendedFor(Key.class, "DEVICE_OWNER");

//    private PluginContainer container = EquivalentMatter.getInstance().getContainer();
//    private DataRegistration<EMCStoreData, ImmutableEMCStoreData> EMCSTORE_DATA_REGISTRATION;
//    private DataRegistration<HoloVisibleData, ImmutableHoloVisibleData> HOLOVIS_DATA_REGISTRATION;

    @Listener
    public void onKeyRegistration(GameRegistryEvent.Register<Key<?>> event) {
        EquivalentMatter.l("Registering Custom Keys");

        EMC = Key.builder()
                .type(new TypeToken<Value<BigInteger>>() {})
                .id("emc")
                .name("EMC Store")
                .query(DataQuery.of("EMC"))
                .build();
        event.register(EMC);

        HOLO_VISIBLE = Key.builder()
                .type(new TypeToken<Value<Boolean>>() {})
                .id("holovis")
                .name("Hologram Visible")
                .query(DataQuery.of("HoloVisible"))
                .build();
        event.register(HOLO_VISIBLE);

        DEVICE_OWNER = Key.builder()
                .type(new TypeToken<Value<UUID>>() {})
                .id("deviceowner")
                .name("Device Owner")
                .query(DataQuery.of("DeviceOwner"))
                .build();
        event.register(DEVICE_OWNER);
    }

    @Listener
    public void onDataRegistration(GameRegistryEvent.Register<DataRegistration<?, ?>> event) {
        EquivalentMatter.l("Registering Custom Data");
        /*this.EMCSTORE_DATA_REGISTRATION =*/ DataRegistration.builder()
                .dataClass(EMCStoreData.class)
                .immutableClass(ImmutableEMCStoreData.class)
                .dataImplementation(EMCStoreDataImpl.class)
                .immutableImplementation(ImmutableEMCStoreDataImpl.class)
                .builder(new EMCStoreDataManipulatorBuilder())
                .name("Stored EMC")
                .id("emcvaluereg")
                .build();
        /*this.HOLOVIS_DATA_REGISTRATION =*/ DataRegistration.builder()
                .dataClass(HoloVisibleData.class)
                .immutableClass(ImmutableHoloVisibleData.class)
                .dataImplementation(HoloVisibleDataImpl.class)
                .immutableImplementation(ImmutableHoloVisibleDataImpl.class)
                .builder(new HoloVisibleDataManipulatorBuilder())
                .name("Visible Hologram")
                .id("holovisreg")
                .build();
        /*this.DEVICE_OWNER_REGISTRATION =*/ DataRegistration.builder()
                .dataClass(DeviceOwnerData.class)
                .immutableClass(ImmutableDeviceOwnerData.class)
                .dataImplementation(DeviceOwnerDataImpl.class)
                .immutableImplementation(ImmutableDeviceOwnerDataImpl.class)
                .builder(new DeviceOwnerDataManipulatorBuilder())
                .name("Device Owner")
                .id("deviceownerreg")
                .build();
    }

}

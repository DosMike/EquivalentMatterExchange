package de.dosmike.sponge.equmatterex.emcDevices;

import org.spongepowered.api.command.CommandSource;

public class DevicePermission {

    private boolean permissiveCreation, permissiveInteraction;
//    int optionMaxDevices = -1;
    private String deviceName;
    public DevicePermission(Device.Type type, boolean requireBuildPermission, boolean requireSharedAccessPermission) {
        deviceName = type.name().toLowerCase();
        permissiveCreation = requireBuildPermission;
        permissiveInteraction = requireSharedAccessPermission;
    }

    /** @return true if the source is allowed to create such a device */
    public boolean hasPermissionCreate(CommandSource source) {
        return !permissiveCreation || source.hasPermission("equmatterex.device."+deviceName+".create");
    }
    /** @return true if the source is allowed to interact with a device they did not build. this includes breaking */
    public boolean hasPermissionInteract(CommandSource source) {
        return !permissiveInteraction || source.hasPermission("equmatterex.device."+deviceName+".sharedaccess");
    }

    public void setPermissionCreate(boolean value) throws IllegalAccessException {
        boolean permitted=false;
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (e.getClassName().contains("EquivalentMatter"))
                permitted = true;
        }
        if (!permitted) throw new IllegalAccessException();
        permissiveCreation = value;
    }
    public void setPermissionAccess(boolean value) throws IllegalAccessException {
        boolean permitted=false;
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (e.getClassName().contains("EquivalentMatter"))
                permitted = true;
        }
        if (!permitted) throw new IllegalAccessException();
        permissiveInteraction = value;
    }

}

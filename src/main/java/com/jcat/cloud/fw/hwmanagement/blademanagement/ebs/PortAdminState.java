package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

/**
 * represents a common PortAdminState usable for DMX and BSP in their respective NetonfLib
 *
 */
public enum PortAdminState {
    ENABLED, DISABLED;

    /**
     * gets the Enum used in the DMX library
     * com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.Transport.Bridge.Port.AdminState
     *
     * @return Enum - AdminState.Enum of bridge port used in DMX functions
     */
    public com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.Transport.Bridge.Port.AdminState.Enum getDmxAdminStateEnum() {
        switch (this) {
        case ENABLED:
            return com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.Transport.Bridge.Port.AdminState.ENABLED;
        case DISABLED:
            return com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.Transport.Bridge.Port.AdminState.DISABLED;

        }
        return null;
    }

    /**
     * gets the Enum used in the BSP library
     * bspEricssonComTop.ManagedElementDocument.ManagedElement.Transport.Bridge.BridgePort.AdminState
     *
     * @return Enum - AdminState.Enum of bridge port used in BSP functions
     */
    public bspEricssonDMXCTransportLibrary.AdminState.Enum getBspAdminStateEnum() {
        switch (this) {
        case ENABLED:
            return bspEricssonDMXCTransportLibrary.AdminState.ENABLED;
        case DISABLED:
            return bspEricssonDMXCTransportLibrary.AdminState.DISABLED;
        }
        return null;
    }
}

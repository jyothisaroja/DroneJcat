package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Ethernet Switch Port Fault
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-23 initial version
 *
 */
public class EthernetSwitchPortFault extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, EthernetSwitchPortFault> {

        private Builder() {
            event = new EthernetSwitchPortFault();
        }

        public Builder equipment(String equipment) {
            event.addMoiArgs(LogEntryMoiType.EQUIPMENT, equipment);
            return this;
        }

        public Builder ethernetPort(int ethernetPort) {
            event.addMoiArgs(LogEntryMoiType.ETHERNET_PORT, "" + ethernetPort);
            return this;
        }

        @Override
        public Builder getThis() {
            return this;
        }

        public Builder region(String region) {
            event.addMoiArgs(LogEntryMoiType.REGION, region);
            return this;
        }

        public Builder topOfRackSwith(String topOfRackSwitch) {
            event.addMoiArgs(LogEntryMoiType.TOP_OF_RACK_SWITCH, topOfRackSwitch);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031684;
    private static int timeoutInSeconds = 300;

    private EthernetSwitchPortFault() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

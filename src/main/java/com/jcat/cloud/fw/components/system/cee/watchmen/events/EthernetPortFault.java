package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Ethernet Port Fault
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-22 initial version
 * @author zpralak 2017-11-01 added method node
 *
 */
public class EthernetPortFault extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, EthernetPortFault> {
        private Builder() {
            event = new EthernetPortFault();
        }

        public Builder aggr(String aggr) {
            event.addMoiArgs(LogEntryMoiType.AGGR, aggr);
            return this;
        }

        public Builder ceeFunction(String ceeFunction) {
            event.addMoiArgs(LogEntryMoiType.CEE_FUNCTION, ceeFunction);
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

        public Builder host(String host) {
            event.addMoiArgs(LogEntryMoiType.HOST, host);
            return this;
        }

        public Builder network(String network) {
            event.addMoiArgs(LogEntryMoiType.NETWORK, network);
            return this;
        }

        public Builder node(String node) {
            event.addMoiArgs(LogEntryMoiType.NODE, node);
            return this;
        }

        public Builder region(String region) {
            event.addMoiArgs(LogEntryMoiType.REGION, region);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031681;

    private static int timeoutInSeconds = 300;

    protected EthernetPortFault() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

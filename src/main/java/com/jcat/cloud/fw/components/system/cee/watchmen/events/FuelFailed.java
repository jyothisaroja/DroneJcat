package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP event for Fuel Failed
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
public class FuelFailed extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, FuelFailed> {
        private Builder() {
            event = new FuelFailed();
        }

        public Builder ceeFunction(String ceeFunction) {
            event.addMoiArgs(LogEntryMoiType.CEE_FUNCTION, ceeFunction);
            return this;
        }

        public Builder ctrlDomain(String ctrlDomain) {
            event.addMoiArgs(LogEntryMoiType.CTRL_DOMAIN, ctrlDomain);
            return this;
        }

        public Builder fuelId(String fuelId) {
            event.addMoiArgs(LogEntryMoiType.FUEL, fuelId);
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
    }

    public static int Major = 193;
    public static int Minor = 2031706;
    private static int timeoutInSeconds = 300;

    protected FuelFailed() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

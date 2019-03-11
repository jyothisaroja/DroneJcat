package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP event for Power Supply Failure
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05.23 initial version
 *
 */
public class PowerSupplyFailure extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, PowerSupplyFailure> {
        private Builder() {
            event = new PowerSupplyFailure();
        }

        public Builder equipment(String equipment) {
            event.addMoiArgs(LogEntryMoiType.EQUIPMENT, equipment);
            return this;
        }

        @Override
        public Builder getThis() {
            return this;
        }

        public Builder powerSupply(String powerSupply) {
            event.addMoiArgs(LogEntryMoiType.POWER_SUPPLY, powerSupply);
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
    public static int Minor = 2031686;
    private static int timeoutInSeconds = 300;

    protected PowerSupplyFailure() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

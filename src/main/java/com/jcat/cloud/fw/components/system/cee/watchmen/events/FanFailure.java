package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Fan Failure
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
public class FanFailure extends EcsSnmpEvent {

    public static class Builder extends EcsSnmpEvent.Builder<Builder, FanFailure> {
        private Builder() {
            event = new FanFailure();
        }

        public Builder equipment(String equipment) {
            event.addMoiArgs(LogEntryMoiType.EQUIPMENT, equipment);
            return this;
        }

        public Builder fan(String fan) {
            event.addMoiArgs(LogEntryMoiType.FAN, fan);
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
    public static int Minor = 2031685;
    private static int timeoutInSeconds = 300;

    protected FanFailure() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

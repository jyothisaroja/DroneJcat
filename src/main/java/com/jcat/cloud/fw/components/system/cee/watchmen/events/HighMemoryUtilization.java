package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP event for High Memory Utilization
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-22 initial version
 *
 */
public class HighMemoryUtilization extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, HighMemoryUtilization> {
        private Builder() {
            event = new HighMemoryUtilization();
        }

        public Builder equipment(String equipment) {
            event.addMoiArgs(LogEntryMoiType.EQUIPMENT, equipment);
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

        public Builder serverBlade(String serverBlade) {
            event.addMoiArgs(LogEntryMoiType.SERVER_BLADE, serverBlade);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031689;
    private static int timeoutInSeconds = 300;

    protected HighMemoryUtilization() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

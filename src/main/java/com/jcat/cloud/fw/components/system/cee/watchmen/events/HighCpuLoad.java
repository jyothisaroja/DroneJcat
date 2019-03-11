package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP event for High CPU load
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
public class HighCpuLoad extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, HighCpuLoad> {
        private Builder() {
            event = new HighCpuLoad();
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
    public static int Minor = 2031688;
    private static int timeoutInSeconds = 300;

    protected HighCpuLoad() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Compute Host Failed
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
public class ComputeHostFailed extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, ComputeHostFailed> {
        private Builder() {
            event = new ComputeHostFailed();
        }

        public Builder ceeFunction(String ceeFunction) {
            event.addMoiArgs(LogEntryMoiType.CEE_FUNCTION, ceeFunction);
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

        public Builder region(String region) {
            event.addMoiArgs(LogEntryMoiType.REGION, region);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031678;
    private static int timeoutInSeconds = 300;

    private ComputeHostFailed() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

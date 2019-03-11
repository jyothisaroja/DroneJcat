package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP event for VM unavailable.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-26 initial version
 *
 */
public class VmUnavailable extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, VmUnavailable> {
        private Builder() {
            event = new VmUnavailable();
        }

        public Builder ceeFunction(String ceeFunction) {
            event.addMoiArgs(LogEntryMoiType.CEE_FUNCTION, ceeFunction);
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

        public Builder tenant(String tenant) {
            event.addMoiArgs(LogEntryMoiType.TENANT, tenant);
            return this;
        }

        public Builder vmId(String vmId) {
            event.addMoiArgs(LogEntryMoiType.VM_ID, vmId);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031702;
    private static int timeoutInSeconds = 300;

    protected VmUnavailable() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

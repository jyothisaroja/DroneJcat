package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Cic Restarted
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-24 initial version
 *
 */
public class CicRestarted extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, CicRestarted> {
        private Builder() {
            event = new CicRestarted();
        }

        public Builder ceeFunction(String ceeFunction) {
            event.addMoiArgs(LogEntryMoiType.CEE_FUNCTION, ceeFunction);
            return this;
        }

        public Builder cic(String cic) {
            event.addMoiArgs(LogEntryMoiType.CIC, cic);
            return this;
        }

        public Builder ctrlDomain(String ctrlDomain) {
            event.addMoiArgs(LogEntryMoiType.CTRL_DOMAIN, ctrlDomain);
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
    public static int Minor = 2031701;
    private static int timeoutInSeconds = 300;

    protected CicRestarted() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}

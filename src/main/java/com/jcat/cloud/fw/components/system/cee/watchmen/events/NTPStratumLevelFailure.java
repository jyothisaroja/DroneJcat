package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

public class NTPStratumLevelFailure extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, NTPStratumLevelFailure> {
        private Builder() {
            event = new NTPStratumLevelFailure();
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

        public Builder upstreamNTPServerConnnection(String upstreamNTPServerConnection) {
            event.addMoiArgs(LogEntryMoiType.UPSTREAM_NTPSERVER_CONNECTION, upstreamNTPServerConnection);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031708;
    private static int timeoutInSeconds = 300;

    protected NTPStratumLevelFailure() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }

}

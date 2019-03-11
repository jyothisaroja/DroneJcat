package com.jcat.cloud.fw.components.system.cee.watchmen.events;

import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;

/**
 * SNMP Event for Centralized Storage Alert
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-25 initial version
 *
 */
public class CentralizedStorageAlert extends EcsSnmpEvent {
    public static class Builder extends EcsSnmpEvent.Builder<Builder, CentralizedStorageAlert> {

        private Builder() {
            event = new CentralizedStorageAlert();
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

        public Builder storageSystemName(String storageSystemName) {
            event.addMoiArgs(LogEntryMoiType.STORAGE_SYSTEM_NAME, storageSystemName);
            return this;
        }
    }

    public static int Major = 193;
    public static int Minor = 2031704;
    private static int timeoutInSeconds = 300;

    private CentralizedStorageAlert() {
        super(Minor, Major, timeoutInSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }
}
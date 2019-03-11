package com.jcat.cloud.fw.components.system.cee.openstack.nova;

import java.util.Date;

import org.openstack4j.model.compute.ext.Service;

/**
 * <p>Class represents NOVA service
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2015-03-17- initial version
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 *
 */
public class EcsNovaService {

    public enum Binary {
        COMPUTE("nova-compute"), CONDUCTOR("nova-conductor"), SCHEDULER("nova-scheduler"), CONSOLEAUTH(
                "nova-consoleauth");
        private String mValue;

        Binary(String value) {
            mValue = value;
        }

        public String value() {
            return mValue;
        }
    }

    /**
     * The state of a Nova service entity
     */
    public enum State {
        DOWN, UNRECOGNIZED, UP;

        public static State forValue(String value) {
            if (value != null) {
                for (State s : State.values()) {
                    if (s.name().equalsIgnoreCase(value)) {
                        return s;
                    }
                }
            }
            return State.UNRECOGNIZED;
        }
    }

    /**
     * The status of a Nova service entity
     */
    public enum Status {
        DISABLED, ENABLED, UNRECOGNIZED;

        public static Status forValue(String value) {
            if (value != null) {
                for (Status s : Status.values()) {
                    if (s.name().equalsIgnoreCase(value)) {
                        return s;
                    }
                }
            }
            return Status.UNRECOGNIZED;
        }
    }

    private Service mService;

    protected EcsNovaService(Service service) {
        mService = service;
    }

    public String getBinary() {
        return mService.getBinary();
    }

    public String getDisabledReason() {
        return mService.getDisabledReason();
    }

    public String getHost() {
        return mService.getHost();
    }

    public String getId() {
        return mService.getId();
    }

    public State getState() {
        return State.forValue(mService.getState().toString());
    }

    public Status getStatus() {
        return Status.forValue(mService.getStatus().toString());
    }

    public Date getUpdatedAt() {
        return mService.getUpdatedAt();
    }

    public String getZone() {
        return mService.getZone();
    }

    @Override
    public String toString() {
        return mService.toString();
    }
}

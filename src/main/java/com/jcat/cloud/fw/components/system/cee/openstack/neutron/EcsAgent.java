package com.jcat.cloud.fw.components.system.cee.openstack.neutron;

import java.util.Date;

import org.openstack4j.model.network.Agent;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * <p>Class represents Neutron Agent
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2015-03-20 initial version
 *
 */
public class EcsAgent {

    public enum Type {
        DHCP, L3, OPEN_VSWITCH, METADATA, UNRECOGNIZED;

        @JsonCreator
        public static Type forValue(String value) {
            if (value != null) {
                for (Type s : Type.values()) {
                    if (s.name().equalsIgnoreCase(value)) {
                        return s;
                    }
                }
            }
            return Type.UNRECOGNIZED;
        }
    }

    private Agent mAgent;

    public EcsAgent(Agent agent) {
        mAgent = agent;
    }

    public boolean getAdminStateUp() {
        return mAgent.getAdminStateUp();
    }

    public Type getAgentType() {
        return Type.forValue(mAgent.getAgentType().toString());
    }

    public boolean getAlive() {
        return mAgent.getAlive();
    }

    public String getBinary() {
        return mAgent.getBinary();
    }

    public Date getCreatedAt() {
        return mAgent.getCreatedAt();
    }

    public String getDescription() {
        return mAgent.getDescription();
    }

    public Date getHeartbeatTimestamp() {
        return mAgent.getHeartbeatTimestamp();
    }

    public String getHost() {
        return mAgent.getHost();
    }

    public String getId() {
        return mAgent.getId();
    }

    public Date getStartedAt() {
        return mAgent.getStartedAt();
    }

    public String getTopic() {
        return mAgent.getTopic();
    }

    @Override
    public String toString() {
        return mAgent.toString();
    }
}

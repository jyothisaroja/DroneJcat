package com.jcat.cloud.fw.components.model.network;

import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.openstack4j.model.network.builder.PortBuilder;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**
 * Class which represents a port
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2014-11-14 initial version
 * @author ethssce 2014-11-20 add trunkport support
 * @author ethssce 2014-11-24 builder uses createName for generating names
 * @author ethssce 2014-11-27 added name option
 */
public class EcsPort extends EcsComponent {

    public static class Builder {
        private PortBuilder mPortBuilder;
        private boolean mIsAdminStateUpSet;

        Builder(String name, String networkId) {
            mPortBuilder = Builders.port().name(name).networkId(networkId);
        }

        public Builder adminStateUp(boolean adminStateUp) {
            mPortBuilder = mPortBuilder.adminState(adminStateUp);
            return this;
        }

        public EcsPort build() {
            // if the user does not set admin state, by default we set it to up
            if (!mIsAdminStateUpSet) {
                mPortBuilder = mPortBuilder.adminState(true);
            }
            EcsPort ecsPort = new EcsPort(mPortBuilder.build());
            return ecsPort;
        }

        public Builder fixedIP(String subnetId) {
            mPortBuilder = mPortBuilder.fixedIp(null, subnetId);
            return this;
        }

        public Builder fixedIP(String address, String subnetId) {
            mPortBuilder = mPortBuilder.fixedIp(address, subnetId);
            return this;
        }

        public Builder macAddress(String macAddress) {
            mPortBuilder = mPortBuilder.macAddress(macAddress);
            return this;
        }

        public Builder name(String name) {
            mPortBuilder = mPortBuilder.name(name);
            return this;
        }

        public Builder securityGroups(String groupName) {
            mPortBuilder = mPortBuilder.securityGroup(groupName);
            return this;
        }

        public Builder status(Status status) {
            mPortBuilder = mPortBuilder.state(State.valueOf(status.toString()));
            return this;
        }

        public Builder tenantId(String tenantId) {
            mPortBuilder = mPortBuilder.tenantId(tenantId);
            return this;
        }
    }

    public enum Status {
        ACTIVE, DOWN, BUILD, ERROR, UNRECOGNIZED;
    }

    public enum TrunkPortType {
        TRUNK("trunk"), SUBPORT("subport");

        private String value;

        private TrunkPortType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final Port mPort;

    private EcsPort(Port port) {
        mPort = port;
    }

    public static Builder builder(String networkId) {
        return new Builder(ControllerUtil.createName(), networkId);
    }

    public Port get() {
        return mPort;
    }

    public String getName() {
        return mPort.getName();
    }
}

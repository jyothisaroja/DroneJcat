package com.jcat.cloud.fw.components.model.network;

import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.builder.NetworkBuilder;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**
 * Class which represents a network
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-11-19 initial version
 * @author eedelk 2015-01-25 added deletionLevel
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2017-01-05 added method getId
 */
public class EcsNetwork extends EcsComponent {

    public static class Builder {

        private NetworkBuilder mNetworkBuilder;
        private DeletionLevel mDeletionLevel = DeletionLevel.TEST_CASE;
        private boolean isAdminStateUpSet;

        private Builder(String networkName) {
            mNetworkBuilder = Builders.network().name(networkName);
        }

        public Builder adminStateUp(boolean adminStateUp) {
            isAdminStateUpSet = true;
            mNetworkBuilder = mNetworkBuilder.adminStateUp(adminStateUp);
            return this;
        }

        public EcsNetwork build() {
            if (!isAdminStateUpSet) {
                // if the user does not provide admin state, by default it will be set to true
                mNetworkBuilder = mNetworkBuilder.adminStateUp(true);
            }
            return new EcsNetwork(mNetworkBuilder.build(), mDeletionLevel);
        }

        public Builder name(String name) {
            mNetworkBuilder = mNetworkBuilder.name(name);
            return this;
        }

        public Builder routerExternal(boolean routerExternal) {
            mNetworkBuilder = mNetworkBuilder.isRouterExternal(routerExternal);
            return this;
        }

        public Builder setDeletionLevel(DeletionLevel deletionLevel) {
            mDeletionLevel = deletionLevel;
            return this;
        }

        public Builder tenantId(String tenantId) {
            mNetworkBuilder = mNetworkBuilder.tenantId(tenantId);
            return this;
        }
    }

    public enum Status {
        ACTIVE, DOWN, BUILD, ERROR, UNRECOGNIZED;
    }

    private final Network mNetwork;
    private final DeletionLevel mDeletionLevel;

    /**
     * @param network
     * @param deletionLevel
     */
    private EcsNetwork(Network network, DeletionLevel deletionLevel) {
        mNetwork = network;
        mDeletionLevel = deletionLevel;
    }

    public static Builder builder() {
        return new Builder(ControllerUtil.createName());
    }

    public Network get() {
        return mNetwork;
    }

    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    public String getId() {
        return mNetwork.getId();
    }

    public String getName() {
        return mNetwork.getName();
    }
}

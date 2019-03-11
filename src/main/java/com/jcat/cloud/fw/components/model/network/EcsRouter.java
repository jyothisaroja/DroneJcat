package com.jcat.cloud.fw.components.model.network;

import org.openstack4j.api.Builders;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.builder.RouterBuilder;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**
 * Class which represents a router
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2014- initial version
 * @author ethssce 2014-11-24 builder uses createName for generating names
 * @author ethssce 2014-11-27 added name option
 * @author zdagjyo 2017-02-02 added method getTenantId
 */

public class EcsRouter extends EcsComponent {

    public static class Builder {
        private RouterBuilder mRouterBuilder;
        private boolean mIsAdminStateUpSet;

        Builder(String name) {
            mRouterBuilder = Builders.router().name(name);
        }

        /**
         * Sets the admin state
         *
         * @param isAdminStateUp - boolean
         * @return this
         */
        public Builder adminState(boolean isAdminStateUp) {
            mIsAdminStateUpSet = true;
            mRouterBuilder = mRouterBuilder.adminStateUp(isAdminStateUp);
            return this;
        }

        public EcsRouter build() {
            if (!mIsAdminStateUpSet) {
                // if the user does not set admin state, by default we set it to up
                mRouterBuilder.adminStateUp(true);
            }
            return new EcsRouter(mRouterBuilder.build());
        }

        /**
         *
         * Sets an external gateway for this router. The provided network id must be an external network.
         *
         * @param externalNetworkId - String - Id of the external network to set as external gateway for this router.
         * @return this
         */
        public Builder externalGateway(String externalNetworkId) {
            mRouterBuilder = mRouterBuilder.externalGateway(externalNetworkId);
            return this;
        }

        /**
         *
         * @param name - String - name of the router
         * @return this Builder
         */
        public Builder name(String name) {
            mRouterBuilder = mRouterBuilder.name(name);
            return this;
        }

        /**
         *
         * Sets a specific tenant that this router should be created for.
         *
         * @param tenantId - String - id of specific tenant
         * @return this
         */
        public Builder tenantId(String tenantId) {
            mRouterBuilder = mRouterBuilder.tenantId(tenantId);
            return this;
        }
    }

    private final Router mRouter;

    private EcsRouter(Router router) {
        mRouter = router;
    }

    public static Builder builder() {
        return new Builder(ControllerUtil.createName());
    }

    public Router get() {
        return mRouter;
    }

    public String getName() {
        return mRouter.getName();
    }

    public String getNetworkIdOfExternalGateway() {
        ExternalGateway externalGateway = mRouter.getExternalGatewayInfo();
        if (externalGateway == null) {
            return null;
        } else {
            return externalGateway.getNetworkId();
        }
    }

    public String getTenantId() {
        return mRouter.getTenantId();
    }
}

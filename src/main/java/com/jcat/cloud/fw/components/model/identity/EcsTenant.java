package com.jcat.cloud.fw.components.model.identity;

import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v2.Tenant;
import org.openstack4j.model.identity.v2.builder.TenantBuilder;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**
 * Class which collects parameters to build a VM
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce 2015-04-30 initial version with builder and updater
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 */
public final class EcsTenant extends EcsComponent {

    /**
     *
     * Builder class for the tenant
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2015
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * see changelog on top of EcsTenant class
     */
    public static class EcsTenantBuilder {
        // class member declaration
        private TenantBuilder mTenantBuilder;

        protected EcsTenantBuilder() {
            String name = ControllerUtil.createName();
            // maximum-length for tenantName is 64 characters
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            mTenantBuilder = Builders.identityV2().tenant().name(name);
        }

        protected EcsTenantBuilder(String name) {
            // maximum-length for tenantName is 64 characters
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            mTenantBuilder = Builders.identityV2().tenant().name(name);
        }

        public EcsTenantBuilder(String id, boolean check) {
            // check exists to distinguish those EcsTenantBuilders with one string parameter
            mTenantBuilder = Builders.identityV2().tenant().id(id);
        }

        public EcsTenant build() {
            return new EcsTenant(mTenantBuilder.build());
        }

        public EcsTenantBuilder description(String description) {
            mTenantBuilder = mTenantBuilder.description(description);
            return this;
        }

        public EcsTenantBuilder enabled(boolean enabled) {
            mTenantBuilder = mTenantBuilder.enabled(enabled);
            return this;
        }

        public EcsTenantBuilder name(String name) {
            mTenantBuilder = mTenantBuilder.name(name);
            return this;
        }
    }

    // member variables for EcsTenant
    private Tenant mTenant;

    /**
     * SHALL NOT BE USED IN TEST CASE DIRECTLY!
     * will just be used in KeystoneController and EcsTenant class to create EcsTenant instance
     *
     * @param tenant
     */
    public EcsTenant(Tenant tenant) {
        mTenant = tenant;
    }

    public static EcsTenantBuilder builder() {
        return new EcsTenantBuilder();
    }

    public static EcsTenantBuilder builder(String name) {
        return new EcsTenantBuilder(name);
    }

    public static EcsTenantBuilder updateBuilder(String id) {
        return new EcsTenantBuilder(id, true);
    }

    /**
     *
     * @return Tenant - Openstack4j instance for tenant
     */
    public Tenant get() {
        return mTenant;
    }

    /**
     *
     * @return String - description
     */
    public String getDescription() {
        return mTenant.getDescription();
    }

    /**
     *
     * @return String - Id
     */
    public String getId() {
        return mTenant.getId();
    }

    /**
     *
     * @return String - name
     */
    public String getName() {
        return mTenant.getName();
    }

    /**
     *
     * @return boolean - enabled
     */
    public boolean isEnabled() {
        return mTenant.isEnabled();
    }

    /**
     *
     * @param tenant
     */
    public void set(Tenant tenant) {
        mTenant = tenant;
    }

    @Override
    public String toString() {
        return String.format("Tenant: %s with ID: %s (description: %s , enabled: %s)", getName(), getId(),
                getDescription(), isEnabled());
    }

}

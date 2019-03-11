package com.jcat.cloud.fw.infrastructure.os4j;

import java.util.Date;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.internal.OSClientSession.OSClientSessionV3;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionTarget;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionType;
import com.jcat.cloud.fw.components.model.identity.EcsCredentials;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.resources.OpenStackResource;

/**
 * OpenStack4jEcs, is entry point to ECS OpenStack,
 * Just @Inject this where you need it and use the Client
 * to talk to OpenStack.
 *
 * When you need to construct objects for use with the API,
 * use the provided builders, such as,
 *
 * Builders.volume().description("lol volym").bootable(false).name("test").size(2).build();
 *
 *
 * When you need to wait for certain conditions, such as for volume
 * status to become available, use our own LoopHelper.
 *
 * See more at openstack4j web page
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eantcvi - Anto Cviti, initial
 * @author ethssce - 2014-08-14 - added OpenStackResource as member, added Javadocs
 * @author eqinann - 2014-10-08 removed unnecessary methods, updated names to ECS naming convention
 * @author ezhgyin - 2014-10-28 add getClientForTenant
 * @author eqinann - 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo - 2018-06-27 - modified keystone version to v3 to support CEE8
 */
public class OpenStack4jEcs {
    private static EcsCredentials mCurrentCredentials;
    private static Date mTokenExpiryDate;
    private final Logger mLogger = Logger.getLogger(OpenStack4jEcs.class.getName());
    private OSFactoryEcs mOsFactoryEcs;
    private OSClientSessionV3 mOSClientSession;
    private final TestConfiguration mTestConfiguration;

    private final OpenStackResource mOpenStackConfiguration;
    private Facing mCurrentPerspective;
    private EcsCredentials mAdminCredentials;
    private boolean forceAuth = false;

    /**
     * Injectable interface
     *
     * @param testConfiguration
     * @param openStackResource
     */
    @Inject
    public OpenStack4jEcs(final TestConfiguration testConfiguration, final OpenStackResource openStackResource) {
        mLogger.info("create OpenStack4jEcsLib");
        mTestConfiguration = testConfiguration;
        mOpenStackConfiguration = openStackResource;
        String adminTenant = mOpenStackConfiguration.getTenant();
        String adminUser = mOpenStackConfiguration.getUser();
        String adminPassword = mOpenStackConfiguration.getPassword();
        mAdminCredentials = new EcsCredentials(adminUser, adminPassword, Identifier.byName("Default"),
                Identifier.byName("admin"));
        mCurrentCredentials = mAdminCredentials;
        mOsFactoryEcs = new OSFactoryEcs(mOpenStackConfiguration.getEndpoint(), adminTenant, adminUser, adminPassword,
                mTestConfiguration.getOsServiceUrlHandlingMode(), mOpenStackConfiguration.getIpPublic());
        initialize();
    }

    private void authenticate(Facing perspective, EcsCredentials ecsCredentials) {
        Identifier domain = ecsCredentials.getDomain();
        Identifier project = ecsCredentials.getProject();
        String user = ecsCredentials.getUser();
        String password = ecsCredentials.getPassword();
        mLogger.debug(
                String.format("Authentiate OSClient with domain:%s, user:%s, password:%s.", domain, user, password));
        try {
            mOSClientSession = mOsFactoryEcs.authenticate(perspective, user, password, domain, project);
            mTokenExpiryDate = mOSClientSession.getToken().getExpires();
        } catch (Exception ex) {
            EcsConnectionException exception = new EcsConnectionException(EcsConnectionType.REST,
                    EcsConnectionTarget.OPENSTACK, mOpenStackConfiguration.getIpPublic());
            ex.printStackTrace();
            exception.setStackTrace(new StackTraceElement[0]);
            throw exception;
        }
    }

    /**
     * Initialize openstack4j with PUBLIC perspective.
     */
    private void initialize() {
        authenticate(Facing.PUBLIC, mCurrentCredentials);
        mCurrentPerspective = Facing.PUBLIC;
    }

    /**
     * Return true if current authentication is using the default admin tenant and admin user
     */
    public boolean currentCredentialsIsAdmin() {
        return mCurrentCredentials.equals(mAdminCredentials);
    }

    /**
     * Disable force authenticate.
     */
    public OpenStack4jEcs disableForceAuthenticate() {
        forceAuth = false;
        return this;
    }

    /**
     * Enable force authenticate.
     */
    public OpenStack4jEcs enableForceAuthenticate() {
        forceAuth = true;
        return this;
    }

    /**
     * Get an OSClient with specific perspective
     * @param perspective - Facing - the perspective of the URL
     * @return OSClient - the OpenStack client session
     */
    public OSClientV3 getClient(Facing perspective) {
        return getClient(perspective, mCurrentCredentials);
    }

    /**
     * Get an OSClient with specific perspective and user provided credentials
     *
     * @param perspective
     * @param ecsCredentials
     * @return OSClient - the OpenStack client session
     */
    public OSClientV3 getClient(Facing perspective, EcsCredentials ecsCredentials) {
        Identifier domain = ecsCredentials.getDomain();
        String user = ecsCredentials.getUser();
        // compare current date with 2 minutes less than the token expiry date and reauthenticate if token expires
        Date tokenExpiryDate = new Date(mTokenExpiryDate.getTime() - 120 * 1000);
        if (new Date().after(tokenExpiryDate) || forceAuth) {
            authenticate(perspective, ecsCredentials);
            mCurrentPerspective = perspective;
            mCurrentCredentials = ecsCredentials;
        } else if (!mCurrentPerspective.equals(perspective) || !mCurrentCredentials.equals(ecsCredentials)) {
            // get a client session with different perspective
            authenticate(perspective, ecsCredentials);
            mCurrentPerspective = perspective;
            mCurrentCredentials = ecsCredentials;
        } else {
            mLogger.debug(String.format("Return OSClient with perspective:%s for domain:%s, user:%s ", perspective,
                    domain, user));
        }
        return mOSClientSession;
    }

    /**
     * Returns OSClient session for admin user
     */
    public OSClientV3 getClientForAdminUser(Facing perspective) {
        if (mCurrentCredentials.equals(mAdminCredentials)) {
            return mOSClientSession;
        } else {
            return getClient(perspective, mAdminCredentials);
        }
    }

    /**
     * Get {@link OpenStackResource}
     */
    public OpenStackResource getConfiguration() {
        return mOpenStackConfiguration;
    }

    public EcsCredentials getCurrentCredentials() {
        return mCurrentCredentials;
    }

    /**
     * Get status of force authentication.
     *
     * @return the forceAuth
     */
    public boolean isForceAuth() {
        return forceAuth;
    }
}

package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Guice provider for {@link OpenStackResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-01-28 Initial version
 * @author emulign 2013-02-19 Added TASS provider logic and {@link #mTestConfiguration}
 * @author esauali 2013-03-29 Renamed
 * @author ezhgyin 2013-10-09 Update the way of getting resource according to the new TASS adapter
 *
 */
public class OpenStackResourceProvider implements Provider<OpenStackResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenStackResource get() {
        return (OpenStackResource) mCfa.get(OpenStackResource.class, OpenStackResource.OPENSTACK);
    };
}

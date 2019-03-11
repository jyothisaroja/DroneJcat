package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Provider for {@link EmcResource}
 *
 * @author esauali 2013-11-18 Initial version
 *
 */
public class EmcResourceProvider implements Provider<EmcResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public EmcResource get() {
        return (EmcResource) mCfa.get(EmcResource.class, EmcResource.RESOURCE_ID);
    };

}

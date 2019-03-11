package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Guice provider for {@link OpenStackResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eolnans 2014-11-13 Initial version
 */
public class FuelResourceProvider implements Provider<FuelResource> {
    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public FuelResource get() {
        return (FuelResource) mCfa.get(FuelResource.class, FuelResource.FUEL);
    }

}

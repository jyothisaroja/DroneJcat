package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Guice provider for {@link EcmResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ewubnhn 2013-07-01 Initial version
 * @author ezhgyin 2013-10-09 Update the way of getting resource according to the new TASS adapter
 *
 */
public class EcmResourceProvider implements Provider<EcmResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public EcmResource get() {
        return (EcmResource) mCfa.get(EcmResource.class, EcmResource.ECM);
    };
}

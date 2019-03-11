package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Guice provider for {@link DmxResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-01-25 initial version
 * @author emulign 2013-02-20 Added TASS provider logic and {@link #mTestConfiguration}
 * @author ezhgyin 2013-10-09 Update the way of getting resource according to the new TASS adapter
 *
 */
public class DmxResourceProvider implements Provider<DmxResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public DmxResource get() {
        return mCfa.get(DmxResource.class, DmxResource.DMX);
    };
}

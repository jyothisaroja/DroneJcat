package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Guice provider for {@link LxcResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-11-18 initial version
 *
 */
public class LxcResourceProvider implements Provider<LxcResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public LxcResource get() {
        LxcResource lxc = null;
        if (mCfa.contains(LxcResource.RESOURCE_ID)) {
            lxc = mCfa.get(LxcResource.class, LxcResource.RESOURCE_ID);
        }
        return lxc;
    };
}

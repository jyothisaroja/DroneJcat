package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;

/**
 * Provider for {@link SerialConsolesResource}
 *
 * @author esauali 2013-11-18 Initial version
 *
 */
public class SerialConsoleResourceProvider implements Provider<SerialConsolesResource> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public SerialConsolesResource get() {
        return (SerialConsolesResource) mCfa.get(SerialConsolesResource.class, SerialConsolesResource.RESOURCE_ID);
    };

}

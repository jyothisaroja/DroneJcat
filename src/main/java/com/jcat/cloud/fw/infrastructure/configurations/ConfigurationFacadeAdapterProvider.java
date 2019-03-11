package com.jcat.cloud.fw.infrastructure.configurations;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.ericsson.commonlibrary.cf.xml.adapter.Adapter;
import com.ericsson.commonlibrary.cf.xml.adapter.AdapterBuilder;
import com.ericsson.tass.adapter.TassAdapter;
import com.google.inject.Provider;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.ResourceAdapter;

/**
 * Guice provider for {@link ConfigurationFacadeAdapter}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ewubnhn 2013-04-10 Initial version
 * @author ezhgyin 2013-10-09 updated with the new TASS adapter
 * @author emulign 2013-10-17 updated with unique base url
 * @author ethssce 2014-08-08 added createEmptyTassAdapter() method to make unit testing possible, annotated it deprecated to show that it shall not be used
 *
 */
public class ConfigurationFacadeAdapterProvider implements Provider<ConfigurationFacadeAdapter> {

    /**
     * Provider containing test configuration properties
     */
    private TestConfiguration mTestConfiguration;

    /**
     * Constructor accepting (normally injected) {@link TestConfiguration}
     *
     * @param testConfig {@link TestConfiguration} Test configuration data
     */
    @Inject
    public ConfigurationFacadeAdapterProvider(TestConfiguration testConfig) {
        mTestConfiguration = testConfig;
    }

    /**
     * protected: this method shall never be called from outside the ConfigurationFacadeAdapterProvider class, except for test purposes
     *
     * @param baseUrl
     * @param node
     * @param userName
     * @return TassAdapter - returns an empty TassAdapter
     */
    protected TassAdapter createEmptyTassAdapter(String baseUrl, String node, String userName) {
        return new TassAdapter(baseUrl, node, userName) {
        };
    }

    /**
     * Selects a {@link ConfigurationFacadeAdapter} based on the {@link ResourceAdapter} type used.
     *
     * {@inheritDoc}
     */
    @Override
    public ConfigurationFacadeAdapter get() {
        ConfigurationFacadeAdapter configurationFacadeAdapter = null;

        if (mTestConfiguration.getResourceAdapter().equals(ResourceAdapter.TASS)) {
            configurationFacadeAdapter = createEmptyTassAdapter(mTestConfiguration.getBaseUrl(),
                    mTestConfiguration.getNode(), mTestConfiguration.getUserName());
        } else if (mTestConfiguration.getResourceAdapter().equals(ResourceAdapter.FILE_XML)) {
            AdapterBuilder ab = Adapter.newBuilder();
            ab.addPath(mTestConfiguration.getResourcePath());
            configurationFacadeAdapter = ab.build();
        }
        return configurationFacadeAdapter;
    }
}

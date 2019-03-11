package com.jcat.cloud.fw.infrastructure.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfigurationProviderJcat;

/**
 * Guice module for building properties and configuration classes
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-13-06 Initial version
 * @author esauali 2013-03-29 Change to bind TestConfiguration to a static instance in listener
 */
public class PropertiesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TestConfiguration.class).toProvider(TestConfigurationProviderJcat.class).in(Singleton.class);
    }
}

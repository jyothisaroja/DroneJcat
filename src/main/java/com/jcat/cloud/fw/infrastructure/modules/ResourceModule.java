package com.jcat.cloud.fw.infrastructure.modules;

import javax.inject.Singleton;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.AbstractModule;
import com.jcat.cloud.fw.infrastructure.configurations.ConfigurationFacadeAdapterProvider;
import com.jcat.cloud.fw.infrastructure.resources.BgwResource;
import com.jcat.cloud.fw.infrastructure.resources.BgwResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.BladeSystemResource;
import com.jcat.cloud.fw.infrastructure.resources.BladeSystemResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.DmxResource;
import com.jcat.cloud.fw.infrastructure.resources.DmxResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.EcmResource;
import com.jcat.cloud.fw.infrastructure.resources.EcmResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.EmcResource;
import com.jcat.cloud.fw.infrastructure.resources.EmcResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;
import com.jcat.cloud.fw.infrastructure.resources.FuelResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.LxcResource;
import com.jcat.cloud.fw.infrastructure.resources.LxcResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.OpenStackResource;
import com.jcat.cloud.fw.infrastructure.resources.OpenStackResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.RabbitMqServerResource;
import com.jcat.cloud.fw.infrastructure.resources.RabbitMqServerResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.SerialConsoleResourceProvider;
import com.jcat.cloud.fw.infrastructure.resources.SerialConsolesResource;
import com.jcat.cloud.fw.infrastructure.resources.VcFlexResource;
import com.jcat.cloud.fw.infrastructure.resources.VcFlexResourceProvider;

/**
 * Guice module for binding resource/configuration related classes
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-01-25 Initial version
 * @author ewubnhn 2013-02-19 Added RabbitMQ
 * @author esauali 2013-03-06 Removed binding of TestConfiguration.class
 * @author emulign 2013-03-14 Made {@link ConfigurationFacadeAdapter} singleton
 * @author esauali 2013-03-29 Change naming, put all *Resource into Singleton
 * @author ewubnhn 2013-04-11 Added binding for ConfigurationFacadeAdapter
 * @author emulign 2013-05-13 Added binding for ExtremeSwitchResource
 * @author ewubnhn 2013-07-01 Added binding for ECMResource
 * @author ezhgyin 2013-10-09 Remove binding for ResourceManager
 * @author ezhgyin 2014-04-14 Add binding for VcFlexResource
 * @author ezhgyin 2014-05-16 Add binding for ExtremeSwitchResourceGroup
 * @author elajkat 2014-08-08 Add binding for BgwResource
 */
public class ResourceModule extends AbstractModule {

    /**
     * Mandatory method to be overridden
     */
    @Override
    protected void configure() {
        bind(ConfigurationFacadeAdapter.class).toProvider(ConfigurationFacadeAdapterProvider.class).in(Singleton.class);
        bind(DmxResource.class).toProvider(DmxResourceProvider.class).in(Singleton.class);
        bind(OpenStackResource.class).toProvider(OpenStackResourceProvider.class).in(Singleton.class);
        bind(RabbitMqServerResource.class).toProvider(RabbitMqServerResourceProvider.class).in(Singleton.class);
        bind(ExtremeSwitchResourceGroup.class).toProvider(ExtremeSwitchResourceProvider.class).in(Singleton.class);
        bind(EcmResource.class).toProvider(EcmResourceProvider.class).in(Singleton.class);
        bind(SerialConsolesResource.class).toProvider(SerialConsoleResourceProvider.class).in(Singleton.class);
        bind(LxcResource.class).toProvider(LxcResourceProvider.class).in(Singleton.class);
        bind(EmcResource.class).toProvider(EmcResourceProvider.class).in(Singleton.class);
        bind(VcFlexResource.class).toProvider(VcFlexResourceProvider.class).in(Singleton.class);
        bind(BgwResource.class).toProvider(BgwResourceProvider.class).in(Singleton.class);
        bind(FuelResource.class).toProvider(FuelResourceProvider.class).in(Singleton.class);
        bind(BladeSystemResource.class).toProvider(BladeSystemResourceProvider.class).in(Singleton.class);
    }
}

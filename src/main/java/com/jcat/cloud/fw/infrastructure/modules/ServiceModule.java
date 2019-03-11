package com.jcat.cloud.fw.infrastructure.modules;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVm;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVm;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsBatVm;
import com.jcat.cloud.fw.components.model.compute.EcsBatVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsVm;
import com.jcat.cloud.fw.components.model.compute.EcsVmFactory;
import com.jcat.cloud.fw.components.model.target.*;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.CicSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.CicSessionFactory;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSessionFactory;
import com.jcat.cloud.fw.components.system.cee.ecssession.FuelSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.FuelSessionFactory;
import com.jcat.cloud.fw.components.system.cee.ecssession.LxcSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.LxcSessionFactory;
import com.jcat.cloud.fw.components.system.cee.ecssession.VmSessionVirsh;
import com.jcat.cloud.fw.components.system.cee.ecssession.VmSessionVirshFactory;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.glance.GlanceController;
import com.jcat.cloud.fw.components.system.cee.openstack.keystone.KeystoneController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.services.ceilometer.CeilometerController;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.components.system.cee.target.EcsScaleIoBladeList;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsLxc;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelLib;
import com.jcat.cloud.fw.components.system.cee.target.fuel.IFuelLib;
import com.jcat.cloud.fw.components.system.cee.tools.EcsOMTool;
import com.jcat.cloud.fw.components.system.cee.watchmen.WatchmenService;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentControllerProvider;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.ConsoleConnectorLib;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.IConsoleConnector;
import com.jcat.cloud.fw.hwmanagement.blademanagement.hp.VcFlexController;
import com.jcat.cloud.fw.hwmanagement.switches.extremeswitch.ExtremeSwitchLib;
import com.jcat.cloud.fw.hwmanagement.switches.extremeswitch.IExtremeSwitchLib;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Module for binding services higher level services (OpenStack, ECM, Unidentified APIs' ...)
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2012-01-25 Initial version
 * @author esauali 2013-02-21 Add INotificationClient,SimpleNotificationListener binding
 * @author esauali 2013-03-29 Put libraries into Singleton
 * @author emulign 2013-04-09 Added binding to {@link ConnectionFactory}
 * @author esauali 2013-05-20 Add IEquipmentController binding
 * @author emulign 2013-05-23 Added binding to {@link IExtremeSwitchLib}
 * @author epergat 2014-05-19 Added binding to IEquipmentControllerProvider
 * @author eqinann 2014-10-08 Added Openstack4JECS module
 * @author ethssce 2014-10-22 Added KeystoneControler binding in Singleton
 * @author epergat 2014-10-22 Added GlanceController binding as Singleton
 * @author ezhgyin 2014-11-03 Added NovaController binding as Singleton
 * @author eelimei 2014-11-03 Added CinderController binding as Singleton
 * @author ezhgyin 2014-11-25 Removed all singetons since they can't be shared between test-cases run in parallel.
 * @author epergat 2014-12-11 Added a binding for the EcsComputeBlade and EcsComputeBladeList
 * @author epergat 2014-12-12 Added a factory for the EcsVirtualMachine class.
 * @author zdagjyo 2017-02-07 Added a binding for EcsComputeBlade
 * @author zdagjyo 2017-02-12 Added factory for all target sessions.
 * @author zmousar 2018-02-10 Added a binding for EcsScaleIoBlade
 */
public class ServiceModule extends AbstractModule {

    /**
     * Mandatory method to be overridden
     */
    @Override
    protected void configure() {
        bind(IExtremeSwitchLib.class).to(ExtremeSwitchLib.class);
        bind(ConnectionFactory.class);
        bind(IConsoleConnector.class).to(ConsoleConnectorLib.class);
        bind(IEquipmentController.class).toProvider(IEquipmentControllerProvider.class);
        bind(VcFlexController.class);
        bind(OpenStack4jEcs.class).in(Singleton.class);
        bind(KeystoneController.class);
        bind(NovaController.class);
        bind(GlanceController.class);
        bind(CinderController.class);
        bind(NeutronController.class);
        bind(CeilometerController.class);
        bind(IFuelLib.class).to(FuelLib.class).in(Singleton.class);
        bind(EcsCicList.class);
        bind(EcsComputeBladeList.class);
        bind(EcsScaleIoBladeList.class);
        bind(EcsFuel.class);
        bind(EcsLxc.class);
        bind(EcsOMTool.class);
        bind(WatchmenService.class);
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsCic.class).build(EcsCicFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsCic.class).build(EcsCicFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsBatCic.class).build(EcsBatCicFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsBatVm.class).build(EcsBatVmFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsVm.class).build(EcsVmFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsComputeBlade.class)
                .build(EcsComputeBladeFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsAtlasVm.class).build(EcsAtlasVmFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsAtlasBatVm.class).build(EcsAtlasBatVmFactory.class));
        install(new FactoryModuleBuilder().implement(EcsSession.class, VmSessionVirsh.class).build(VmSessionVirshFactory.class));
        install(new FactoryModuleBuilder().implement(EcsSession.class, CicSession.class)
                .build(CicSessionFactory.class));
        install(new FactoryModuleBuilder().implement(EcsSession.class, ComputeBladeSession.class)
                .build(ComputeBladeSessionFactory.class));
        install(new FactoryModuleBuilder().implement(EcsSession.class, FuelSession.class)
                .build(FuelSessionFactory.class));
        install(new FactoryModuleBuilder().implement(EcsSession.class, LxcSession.class)
                .build(LxcSessionFactory.class));
        install(new FactoryModuleBuilder().implement(EcsTarget.class, EcsScaleIoBlade.class)
                .build(EcsScaleIoBladeFactory.class));
    }
}

package com.jcat.cloud.fw.fwservices.traffic.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVm;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.infrastructure.modules.PropertiesModule;
import com.jcat.cloud.fw.infrastructure.modules.ResourceModule;
import com.jcat.cloud.fw.infrastructure.modules.ServiceModule;

/**<p>
 *
 * Controller for Atlas BAT traffic
 *
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2018-04-18 initial version
 */
public class AtlasBatTrafficController extends EcsComponent implements ITrafficController {

    private static AtlasBatTrafficController mSingletonInstance = null;

    /* Static 'instance' method */
    public static AtlasBatTrafficController getInstance() {
        if (mSingletonInstance == null) {
            mSingletonInstance = new AtlasBatTrafficController();
        }
        return mSingletonInstance;
    }

    private NovaController mNovaController;
    private EcsAtlasBatVm mAtlasBatVm;

    private final EcsLogger mLogger = EcsLogger.getLogger(BatTrafficController.class);

    private AtlasBatTrafficController() {
        Injector injector = Guice.createInjector(new ResourceModule(), new PropertiesModule(), new ServiceModule());
        mNovaController = injector.getInstance(NovaController.class);
        mAtlasBatVm = mNovaController.getAtlasBatVm(mNovaController.getVmIdByName("BAT-A1-001"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAcceptableTraffic() {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareTrafficForStart() {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartTrafficStatistics() {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * {@inheritDoc}
     * Starts atlas BAT traffic
     */
    @Override
    public void startTraffic() {
        mAtlasBatVm.startAndVerifyBatTraffic();
    }

    /**
     * {@inheritDoc}
     * Shuts down atlas BAT traffic
     */
    @Override
    public void stopTraffic() {
        mAtlasBatVm.stopBatTraffic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTrafficStatistics() {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * Verifies that the atlas bat traffic is running
     */
    public void verifyTraffic() {
        mAtlasBatVm.verifyBatTraffic();
    }
}

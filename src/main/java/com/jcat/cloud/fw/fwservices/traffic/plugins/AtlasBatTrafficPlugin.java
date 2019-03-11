package com.jcat.cloud.fw.fwservices.traffic.plugins;

import java.util.List;

import org.apache.log4j.Logger;

import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.exceptions.TrafficSSHException;
import com.jcat.cloud.fw.fwservices.traffic.controllers.AtlasBatTrafficController;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.AtlasBatTraffic;

import se.ericsson.jcat.fw.ng.traffic.AbstractTrafficPlugin;
import se.ericsson.jcat.fw.ng.traffic.TrafficPoint;
import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlException;
import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlPluginException;

/**<p>
 *
 * Atlas BAT Traffic plugin
 *
 * Note that the plugin will be called by JCAT FW directly, any exceptions thrown in the plugin
 * WILL cause test STOP immediately. Therefore there cannot have ANY exceptions in the plugin
 *
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2018-04-18 initial version
 *
 */
public class AtlasBatTrafficPlugin extends AbstractTrafficPlugin {

    private AtlasBatTrafficController mAtlasBatController;
    private boolean mIsAtlasBatTrafficEnabled = true;
    private final Logger mLogger = Logger.getLogger(AtlasBatTrafficPlugin.class);

    /**
     * Checks if atlas bat traffic plugin is enabled in test configuration.
     *
     * return boolean
     */
    private boolean isAtlasBatTrafficEnabled() {
        TestConfiguration configuration = TestConfiguration.getTestConfiguration();
        if (configuration.getAtlasBatTrafficMode().equals(AtlasBatTraffic.OFF)) {
            mIsAtlasBatTrafficEnabled = false;
        }
        return mIsAtlasBatTrafficEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Object> getStatistics() {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * {@inheritDoc}
     * Configures parameters BAT needs to take, for example whether FIO is enabled or the number
     * of tenants
     *
     * Configurations are read from traffic plugin XML
     *
     */
    @Override
    public void initiate() throws TrafficControlPluginException {
        if (isAtlasBatTrafficEnabled()) {
            mLogger.debug("JCATTrafficPlugin:Instantiate traffic plugin");
            try {
                mAtlasBatController = AtlasBatTrafficController.getInstance();
            } catch (NullPointerException exception) {
                throw new EcsNotImplementedException(
                        "couldn't find atlas bat vms on node, Atlas Bat traffic can not be started without atlas bat vms");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAcceptableTraffic() throws TrafficSSHException {
        throw new EcsNotImplementedException("The method does not work for EcsAtlasBatVm");
    }

    /**
     * Helper method for unit test
     *
     * @param atlasBatLib
     */
    public void setBatTrafficLib(AtlasBatTrafficController atlasBatController) {
        mAtlasBatController = atlasBatController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTraffic(TrafficPoint startOn) throws TrafficControlException {
        if (isAtlasBatTrafficEnabled()) {
            mLogger.info("JCATTrafficPlugin:Starting Atlas BAT traffic");
            mAtlasBatController.startTraffic();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTraffic(TrafficPoint stopOn) throws TrafficControlException {
        if (isAtlasBatTrafficEnabled()) {
            mLogger.info("JCATTrafficPlugin:Stopping Atlas BAT traffic");
            mAtlasBatController.stopTraffic();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "plugin: TestAppTrafficPlugin; class: " + this.getClass().getSimpleName();
    }

    /**
     * Verifies that the atlas bat traffic is running
     */
    public void verifyTraffic() {
        mLogger.info("Verifying Atlas BAT traffic");
        mAtlasBatController.verifyTraffic();
    }
}

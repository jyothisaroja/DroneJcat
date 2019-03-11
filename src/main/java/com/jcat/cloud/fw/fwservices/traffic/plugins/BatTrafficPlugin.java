package com.jcat.cloud.fw.fwservices.traffic.plugins;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import se.ericsson.jcat.fw.ng.traffic.AbstractTrafficPlugin;
import se.ericsson.jcat.fw.ng.traffic.TrafficPoint;
import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlException;
import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlPluginException;

import com.jcat.cloud.fw.common.exceptions.TrafficSSHException;
import com.jcat.cloud.fw.fwservices.traffic.controllers.BatTrafficController;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.AtlasBatTraffic;


/**<p>
 *
 * BAT Traffic plugin based on TestApp and FIO
 *
 * Note that the plugin will be called by JCAT FW directly, any exceptions thrown in the plugin
 * WILL cause test STOP immediately. Therefore there cannot have ANY exceptions in the plugin
 *
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-07-31 initial version modified from TestAppTrafficPlugin
 *
 */
public class BatTrafficPlugin extends AbstractTrafficPlugin {

    private BatTrafficController mBatController;
    private boolean mIsAtlasBatTrafficEnabled = false;
    private final Logger mLogger = Logger.getLogger(BatTrafficPlugin.class);

    /**
     * Checks if alas bat traffic plugin is enabled in test configuration.
     *
     * return boolean
     */
    private boolean isAtlasBatTrafficEnabled() {
        TestConfiguration configuration = TestConfiguration.getTestConfiguration();
        if (configuration.getAtlasBatTrafficMode().equals(AtlasBatTraffic.ON)) {
            mIsAtlasBatTrafficEnabled = true;
        }
        return mIsAtlasBatTrafficEnabled;
    }

    /**
     * Helper method for unit test
     *
     * @param batLib
     */
    protected void setBatTrafficLib(BatTrafficController batController) {
        mBatController = batController;
    }

    /**
     * {@inheritDoc}
     * Not used yet.
     */
    @Override
    public List<Object> getStatistics() {
        List<Object> list = new LinkedList<Object>();
        // list.add(mBatController.getSentHistory());
        // list.add(mBatController.getReceivedHistory());
        return list;
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
        if (!isAtlasBatTrafficEnabled()) {
            mLogger.debug("JCATTrafficPlugin:Instantiate traffic plugin");
            mBatController = BatTrafficController.getInstance();
            Element e = getPluginXml().getConfiguration();
            NodeList fio = e.getElementsByTagName("fio");
            NodeList tenant = e.getElementsByTagName("tenant");
            mBatController.setFIOTrafficEnabled(Boolean.parseBoolean(fio.item(0).getTextContent()));
            mBatController.setNumberOfTenants(Integer.parseInt(tenant.item(0).getTextContent()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAcceptableTraffic() throws TrafficSSHException {
        mLogger.debug("JCATTrafficPlugin:Checking BAT traffic");
        return mBatController.isAcceptableTraffic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTraffic(TrafficPoint startOn) throws TrafficControlException {
        if (!isAtlasBatTrafficEnabled()) {
            switch (startOn) {
            case ONSUITE:
                mLogger.info("JCATTrafficPlugin:Starting BAT traffic");
                mBatController.prepareTrafficForStart();
                mBatController.startTraffic();
                break;
            case ONTEST:
                mLogger.debug("JCATTrafficPlugin:Restarting BAT traffic");
                mBatController.restartTrafficStatistics();
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTraffic(TrafficPoint stopOn) throws TrafficControlException {
        if (!isAtlasBatTrafficEnabled()) {
            switch (stopOn) {
            case ONSUITE:
                mLogger.info("JCATTrafficPlugin:Shuting down BAT traffic");
                mBatController.stopTraffic();
                break;
            case ONTEST:
                mLogger.debug("JCATTrafficPlugin:Stopping BAT traffic");
                mBatController.stopTrafficStatistics();
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "plugin: TestAppTrafficPlugin; class: " + this.getClass().getSimpleName();
    }
}

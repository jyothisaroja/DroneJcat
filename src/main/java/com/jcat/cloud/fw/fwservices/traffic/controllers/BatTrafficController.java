package com.jcat.cloud.fw.fwservices.traffic.controllers;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.model.compute.Server.Status;

import se.ericsson.jcat.fw.logging.JcatLoggingApi;
import se.ericsson.jcat.fw.ng.traffic.AbstractTrafficPlugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jcat.cloud.fw.common.exceptions.EcsTrafficException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.compute.EcsVm;
import com.jcat.cloud.fw.components.model.target.EcsBatCic;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.fwservices.traffic.model.FioResult;
import com.jcat.cloud.fw.fwservices.traffic.model.TestAppResult;
import com.jcat.cloud.fw.fwservices.traffic.plugins.BatTrafficPlugin;
import com.jcat.cloud.fw.infrastructure.modules.PropertiesModule;
import com.jcat.cloud.fw.infrastructure.modules.ResourceModule;
import com.jcat.cloud.fw.infrastructure.modules.ServiceModule;

/**<p>
 *
 * Controller for BAT traffic
 *
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-09-09 initial version
 * @author eelimei 2016-01-18 Added method to reset the bat cic since it is possible that another cic has taken over as active.
 * @author zpralak 2017-11-24 Modified method areBatVMsHealthy
 * @author zmousar 2018-12-21 Modified code for CEE9
 *
 */
public class BatTrafficController extends EcsComponent implements ITrafficController {

    private EcsBatCic mBatCic;
    private EcsCicList mEcsCicList;
    private boolean mFIOTrafficEnabled = true;
    private boolean mBatFaulty = false;
    private final EcsLogger mLogger = EcsLogger.getLogger(BatTrafficController.class);
    private int mNumberOfTenants = 1;
    private List<ArrayList<FioResult>> mPreviousFioResult = new ArrayList<ArrayList<FioResult>>();
    private List<ArrayList<TestAppResult>> mPreviousTestAppResult = new ArrayList<ArrayList<TestAppResult>>();
    private boolean mTrafficBeingMeasured = false;
    private static String POWER_STATE_RUNNING = "1";
    private NovaController mNovaController;
    private static BatTrafficController mSingletonInstance = null;
    private int mFailedPercent;
    private int mTimeoutPercent;
    private int mBatControllerVmIndex = 0;
    private static final int BAT_CONTROLLER_VM1 = 0;
    private static final int BAT_CONTROLLER_VM2 = 1;

    private BatTrafficController() {
        Injector injector = Guice.createInjector(new ResourceModule(), new PropertiesModule(), new ServiceModule());
        mEcsCicList = injector.getInstance(EcsCicList.class);
        mNovaController = injector.getInstance(NovaController.class);
        mBatCic = mEcsCicList.getBatCic();
    }

    /* Static 'instance' method */
    public static BatTrafficController getInstance() {
        if (mSingletonInstance == null) {
            mSingletonInstance = new BatTrafficController();
        }
        return mSingletonInstance;
    }

    /**
     * Methods checks if the expected number of BAT VMs are active & running where the expected number depends on nr of
     * compute blades and number of tenants for BAT.
     *
     * @return
     */
    private boolean areBatVMsHealthy() {
        if (isBatFaulty()) {
            return false;
        }
        mLogger.info(EcsAction.VALIDATING, "Healthy", "BAT VMs", "");
        List<EcsVm> servers = mNovaController.getAllVMsForAllTenants();
        // Number of BAT vms that can be booted on each compute is 3 i.e 1 A1 VM + 1 A2 VM + 1 B VM
        int numberOfBatVmsBootedOnEachCompute = 3;
        // Calculates the expected BAT VMs to be booted
        int numberOfExpectedBatVMs = 0;
        int numberOfFoundBatVMs = 0;
        int numberOfBatControllerVms = 2;
        boolean batCicFound = false;
        for(EcsBatCic batCic : mEcsCicList.getAllBatCics()) {
            /**
             *  BatController vms are required to start and test TestAppTraffic.
             *  When we run bat_controller_start.sh, 2 bat controller vms are launched.
             *  For 2 batcontroller vms, added those count in calculation part
             */
            numberOfExpectedBatVMs = (mNumberOfTenants * numberOfBatVmsBootedOnEachCompute
                    * batCic.getComputesWithBatVmsFromHwInv()) + numberOfBatControllerVms;

            if(numberOfExpectedBatVMs > 2) {
                mBatCic = batCic;
                break;
            }
        }
        if(!batCicFound) {
            mLogger.warn("Not found BatCic with updated hw_inventory.txt file");
            return false;
        }
        String serverName;
        for (EcsVm server : servers) {
            try {
                serverName = server.getName();
            } catch (Exception e) {
                JcatLoggingApi.setTestWarning("Could not get server name from: " + server.getId()
                        + " assuming that it is not a BAT Vm");
                continue;
            }
            if (serverName.contains("BAT-T")) {
                numberOfFoundBatVMs++;
                if (!(mNovaController.getVmStatus(server.getId()) == Status.ACTIVE && mNovaController.getPowerState(
                        server.getId()).equals(POWER_STATE_RUNNING))) {
                    mLogger.warn("At least one of the BAT VMs is not active & running: " + server.getName());
                    return false;
                }
            }
        }
        if (numberOfExpectedBatVMs != 2) {
            int VmsCountDifference = (numberOfExpectedBatVMs - numberOfFoundBatVMs);
            // bat Controllers are not yet launched, so launch them
            if( VmsCountDifference == 2) {
                mBatCic.launchBatControllerVms();
                VmsCountDifference = 0;
            }
            if(VmsCountDifference != 0){
                mLogger.warn("Expected: " + numberOfExpectedBatVMs + " but found: " + numberOfFoundBatVMs);
                return false;
            }
        } else {
            mLogger.warn("Could not calculate expected BAT vms using Hardware inventory");
            return false;
        }
        mBatCic.setBatPrepared(true);
        mLogger.info(Verdict.VALIDATED, + numberOfFoundBatVMs + " Healthy", "BAT VMs", "");
        return true;
    }

    /**
     * Check if FIO traffic is acceptable
     * @param batControllerVmIndex - Index of Bat Controller Vm
     *
     * @return
     */
    private boolean isAcceptableFioTraffic(int batControllerVmIndex) {
        if (isBatFaulty()) {
            return false;
        }
        if (mPreviousFioResult.get(batControllerVmIndex) == null) {
            mLogger.info(EcsAction.COLLECTING, "initial", FioResult.class, "");
            mPreviousFioResult.add(batControllerVmIndex, (ArrayList<FioResult>) mBatCic.getFioTraffic(batControllerVmIndex));
            if (mPreviousFioResult.get(batControllerVmIndex) == null) {
                mLogger.error("FIO traffic is not stable for bat Controller Vm : " + mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname());
                return false;
            }
            mLogger.info(Verdict.COLLECTED, "initial", FioResult.class, "");
            return isAcceptableFioTraffic(batControllerVmIndex);
        }
        mLogger.info(EcsAction.VALIDATING, "", "Fio Traffic", "");
        List<FioResult> results = mBatCic.getFioTraffic(batControllerVmIndex);
        if (results.size() != mPreviousFioResult.get(batControllerVmIndex).size()) {
            mLogger.error("FIO has different size of VMs since last time checked!");
            mLogger.error("Before FIO has " + mPreviousFioResult.get(batControllerVmIndex).size() + " instances:");
            for (FioResult result : mPreviousFioResult.get(batControllerVmIndex)) {
                mLogger.error(result.getVmName() + " read aggrbs:" + result.getReadAggrb());
                mLogger.error(result.getVmName() + " write aggrbs:" + result.getWriteAggrb());
            }
            mLogger.error("After: " + results);
            for (FioResult result : results) {
                mLogger.error(result.getVmName() + " read aggrbs:" + result.getReadAggrb());
                mLogger.error(result.getVmName() + " write aggrbs:" + result.getWriteAggrb());
            }
            return false;
        }
        boolean isTrafficStable = true;
        for (FioResult result : results) {
            if (result.getReadAggrb() == 0 || result.getWriteAggrb() == 0) {
                isTrafficStable = false;
                mLogger.error("No FIO " + (result.getReadAggrb() == 0 ? "Read" : "")
                        + (result.getWriteAggrb() == 0 ? "Write" : "") + " traffic on VM: " + result.getVmName());
            }
        }
        mPreviousFioResult.add(batControllerVmIndex, (ArrayList<FioResult>) results);
        mLogger.info(Verdict.VALIDATED, (isTrafficStable ? "" : "not ") + "stable", "Fio Traffic", "");
        return isTrafficStable;
    }

    /**
     * Check if TestApp traffic is acceptable
     * @param batControllerVmIndex - Index of Bat Controller MV
     *
     * @return
     */
    private boolean isAcceptableTestAppTraffic(int batControllerVmIndex) {
        if (isBatFaulty()) {
            return false;
        }
        if (mPreviousTestAppResult.get(batControllerVmIndex) == null) {
            mLogger.info(EcsAction.COLLECTING, "initial", TestAppResult.class, "");
            mPreviousTestAppResult.add(batControllerVmIndex, (ArrayList<TestAppResult>) mBatCic.getTestAppTraffic(batControllerVmIndex, false));
            if (mPreviousTestAppResult.get(batControllerVmIndex) == null) {
                mLogger.error("TestApp traffic is null for bat Controller Vm : "
                    + mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname());
                return false;
            }
            if(mPreviousTestAppResult.get(batControllerVmIndex).size() == 0) {
                mLogger.error("TestApp traffic is failed fetching statistics");
                return false;
            }
            mLogger.info(Verdict.COLLECTED, "initial", TestAppResult.class, "");
            return isAcceptableTestAppTraffic(batControllerVmIndex);
        }
        mLogger.info(EcsAction.VALIDATING, "", "TestApp Traffic", "");
        List<TestAppResult> results = mBatCic.getTestAppTraffic(batControllerVmIndex, false);
        if (results.size() != mPreviousTestAppResult.get(batControllerVmIndex).size()) {
            mLogger.error("TestApp has different size of VMs since last time checked!");
            mLogger.error("Before: TestApp has " + mPreviousTestAppResult.get(batControllerVmIndex).size() + " instances:");
            for (TestAppResult result : mPreviousTestAppResult.get(batControllerVmIndex)) {
                mLogger.error(result.name() + " fails:" + result.failed());
            }
            mLogger.error("After: TestApp has " + mPreviousTestAppResult.get(batControllerVmIndex).size() + " instances:");
            for (TestAppResult result : results) {
                mLogger.error(result.name() + " fails:" + result.failed());
            }
            return false;
        }
        boolean isTrafficStable = true;
        mFailedPercent = 0;
        mTimeoutPercent = 0;
        if( mBatCic.getNumberOfLinesInStats() <= 0) {
            mLogger.error("Either traffic is not present in bat-controller-vm : "
                + mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname() +
                " or not able to found 'script finished' for testapp_stats.sh");
            return false;
        }
        for (TestAppResult result : results) {
            if (result.sent() == 0 || result.received() == 0) {
                // If there is no traffic at all, traffic is not stable!
                mLogger.error("No traffic is " + (result.sent() == 0 ? "sent from: " : "received from: ")
                        + result.name());
                return false;
            }
            if(result.failed() != 0) {
                mFailedPercent = mFailedPercent + (1 / mBatCic.getNumberOfLinesInStats());
            }
            else if(result.timeout() != 0) {
                mTimeoutPercent = mTimeoutPercent + (1 / mBatCic.getNumberOfLinesInStats());
            }
            boolean found = false;
            for (TestAppResult oneResult : mPreviousTestAppResult.get(batControllerVmIndex)) {
                if (oneResult.name().equals(result.name())) {
                    found = true;
                    if (result.hasNoMoreFailureThan(oneResult)) {
                        break;
                    }
                    isTrafficStable = false;
                }
            }
            if (!found) {
                mLogger.error("VM " + result + " was not found last time");
            }
        }
        mPreviousTestAppResult.add(batControllerVmIndex, (ArrayList<TestAppResult>) results);
        if(mFailedPercent >= 0.5) {
            mLogger.info(Verdict.VALIDATED, "not stable", "TestApp Traffic", "as failed percent is:" + mFailedPercent
                + "in : " + mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname() );
            return false;
        }
        else if(mTimeoutPercent >= 0.5) {
             mLogger.info(Verdict.VALIDATED, "not stable", "TestApp Traffic", "as timeout percent is:" + mTimeoutPercent
                + "in : " + mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname());
            return false;
        }
        else if(results.size()  != mBatCic.getNumberOfLinesInStats()) {
            mLogger.info(Verdict.VALIDATED, "not stable", "TestApp Traffic", " in: " +
                 mBatCic.getBatControllerVms().get(batControllerVmIndex).getHostname()
                 + " as stats contain few VMs" + (mBatCic.getNumberOfLinesInStats() - results.size())
                 + " do not have total, send-failed etc.. attributes");
            return false;
        }
        mLogger.info(Verdict.VALIDATED, (isTrafficStable ? "" : "not ") + "stable", "TestApp Traffic", "");
        return isTrafficStable;
    }

    /*
     * Verify TestAppTraffic for both bat Controller VMs
     */
    private boolean isAcceptableTestAppTrafficForBothCtrlVms() {
        if (isAcceptableTestAppTraffic(BAT_CONTROLLER_VM1) && isAcceptableTestAppTraffic(BAT_CONTROLLER_VM2)) {
            return true;
        }
        return false;
    }

    /*
     * Verify Fio Traffic for both bat Controller VMs
     */
    private boolean isAcceptableFioTrafficForBothCtrlVms() {
        if (isAcceptableFioTraffic(BAT_CONTROLLER_VM1) && isAcceptableFioTraffic(BAT_CONTROLLER_VM2)) {
            return true;
        }
        return false;
    }

    /**
     * Check if BAT is installed on the target
     *
     * @return
     */
    private boolean isBatDeployed() {
        return mBatCic.doesDirectoryExist("/var/lib/glance/BATscripts/");
    }

    /**
     * Check if BAT is faulty
     *
     * @return
     */
    private boolean isBatFaulty() {
        if (mBatFaulty) {
            mLogger.error("BAT traffic is currently offline. Please check logs before to find out what happened.");
            return true;
        }
        return false;
    }

    private void throwNewEcsTrafficException(Class<? extends AbstractTrafficPlugin> trafficType, String message) {
        mBatFaulty = true;
        try {
            throw new EcsTrafficException(trafficType, message);
        } catch (EcsTrafficException ete) {
            mLogger.error("Exception is thrown in EcsBatCic class. However it is catched. Stacktrace:");
            ete.printStackTrace();
        }
    }

    /**
     * Loop helper will wait for BAT to be stable
     * Depending if FIO is enabled, waiting time is different
     */
    private void waitForBatTrafficStable() {
        if (isBatFaulty()) {
            return;
        }
        Timeout batTimeout = Timeout.TESTAPP_STABLE;
        if (mFIOTrafficEnabled) {
            batTimeout = Timeout.TESTAPP_AND_FIO_STABLE;
        }
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(batTimeout, "BAT not stable in "
                + Timeout.TESTAPP_STABLE.getTimeoutInSeconds() + " seconds", Boolean.TRUE, () -> {
            mLogger.debug("Checking if BAT is stable.");
            return isAcceptableTraffic();
        });
        loopHelper.setIterationDelay(CommonParametersValues.ITERATION_DELAY_10_SEC);
        loopHelper.run();
    }

    /**
     * {@inheritDoc}
     *
     * Depending if FIO is enabled, this method will check both TestApp and FIO traffic
     *
     */
    @Override
    public boolean isAcceptableTraffic() {
        if (isBatFaulty()) {
            return false;
        }
        mLogger.info(EcsAction.VALIDATING, "", "BAT Traffic", "");
        if (!mTrafficBeingMeasured) {
            mLogger.warn("BAT traffic is not being measured right now");
            return true;
        }
        if (mFIOTrafficEnabled) {
            return isAcceptableTestAppTrafficForBothCtrlVms() && isAcceptableFioTrafficForBothCtrlVms();
        } else {
            return isAcceptableTestAppTrafficForBothCtrlVms();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Prepares BAT environment for starting traffic
     *
     */
    @Override
    public void prepareTrafficForStart() {
        mLogger.info(EcsAction.PREPARING, "", "BAT Deploy", "");
        if (!isBatDeployed()) {
            mBatFaulty = true;
            throw new EcsTrafficException(
                    BatTrafficPlugin.class,
                    "BAT is not installed. For safty reasons, please deploy BAT manually from Jenkins server. https://fem004-eiffel018.rnd.ki.sw.ericsson.se:8443/jenkins/view/JCAT/job/ecs-deploy-siv-addons/");
        }
        if (!(areBatVMsHealthy() && mBatCic.isBatPrepared() && mBatCic.isRuntimeInventoryPrepared())) {
            try {
                mBatCic.prepareBat();
                mBatCic.startBatVms();
                mBatCic.pairBatVms();
                mBatCic.launchBatControllerVms();
            } catch (EcsTrafficException te) {
                throwNewEcsTrafficException(te.getTrafficType(), te.getMessage());
            } catch (LoopTimeoutException lte) {
                throwNewEcsTrafficException(BatTrafficPlugin.class, lte.getMessage());
            }
        } else {
            mLogger.info(Verdict.PREPARED, "", "BAT Deploy", "Already prepared before");
            return;
        }
        mLogger.info(Verdict.PREPARED, "", "BAT Deploy", "");
    }

    /**
     * Use this method to reset the bat cic if you have migrated/fall back the dhcp active cic.
     */
    public void resetBatCic() {
        mBatCic = mEcsCicList.getBatCic();
    }

    /**
     * {@inheritDoc} Resets BAT counters in between test cases
     */
    @Override
    public void restartTrafficStatistics() {
        mLogger.info(EcsAction.RESETING, "", "BAT Traffic", "");
        if (mTrafficBeingMeasured) {
            // resetting traffic
            for (mBatControllerVmIndex = 0; mBatControllerVmIndex < 2; mBatControllerVmIndex++ ) {
                try {
                    mBatCic.getTestAppTraffic(mBatControllerVmIndex, true);
                } catch(EcsTrafficException ex) {
                  mLogger.warn("Not resetted Properly for bat Controller Vm: " + mBatCic.getBatControllerVms().get(mBatControllerVmIndex).getHostname());
                }
                mPreviousTestAppResult.add(mBatControllerVmIndex, (ArrayList<TestAppResult>) mBatCic.getTestAppTraffic(mBatControllerVmIndex, false));
                if(mFIOTrafficEnabled) {
                    mPreviousFioResult.add(mBatControllerVmIndex, (ArrayList<FioResult>) mBatCic.getFioTraffic(mBatControllerVmIndex));
                }
            }
        }
    }

    /**
     * Enable or disable FIO traffic
     *
     * @param fioTrafficEnabled
     */
    public void setFIOTrafficEnabled(boolean fioTrafficEnabled) {
        mFIOTrafficEnabled = fioTrafficEnabled;
    }

    /**
     * Set the number of tenants
     *
     * @param numberOfTenants
     */
    public void setNumberOfTenants(int numberOfTenants) {
        mNumberOfTenants = numberOfTenants;
    }

    /**
     * {@inheritDoc}
     * Starting BAT traffic
     */
    @Override
    public void startTraffic() {
        if (isBatFaulty()) {
            return;
        }
        mLogger.info(EcsAction.STARTING, "", "BAT Traffic", "");
        List<FioResult> currentFioResult = null;
        List<TestAppResult> currentTestAppResult;
        boolean trafficRestarted = false;
        try {
            // add loop for two bat controller VMs
            for (mBatControllerVmIndex=0; mBatControllerVmIndex < 2; mBatControllerVmIndex++) {
                try {
                    if (mFIOTrafficEnabled) {
                        currentFioResult = mBatCic.getFioTraffic(mBatControllerVmIndex);
                    }
                } catch(EcsTrafficException ex) {
                    mLogger.info("Fio is not running");
                    currentFioResult = new ArrayList<FioResult>();
                }
                try {
                    // If traffic was not started, bat_testapp_stats.sh will print error message, which will make bat disabled.
                    // here is to make it enabled.
                    currentTestAppResult = mBatCic.getTestAppTraffic(mBatControllerVmIndex, false);
                } catch (EcsTrafficException te) {
                    mLogger.info("TestApp is not running");
                    currentTestAppResult = new ArrayList<TestAppResult>();
                }
                mTrafficBeingMeasured = true;
                if (mFIOTrafficEnabled) {
                    if (currentFioResult.isEmpty()) {
                        mBatCic.stopFioTraffic(mBatControllerVmIndex);
                        mBatCic.startFioTraffic(mBatControllerVmIndex);
                        trafficRestarted = true;
                    } else {
                        mPreviousFioResult.add(mBatControllerVmIndex, (ArrayList<FioResult>) currentFioResult);
                    }
                }
                if (currentTestAppResult.isEmpty()) {
                    mBatCic.stopTestAppTraffic(mBatControllerVmIndex);
                    mBatCic.startTestAppTraffic(mBatControllerVmIndex);
                    trafficRestarted = true;
                } else {
                    mPreviousTestAppResult.add(mBatControllerVmIndex, (ArrayList<TestAppResult>) currentTestAppResult);
                }
            }
            if (trafficRestarted) {
                waitForBatTrafficStable();
                restartTrafficStatistics();
            }
        } catch (EcsTrafficException te) {
            throwNewEcsTrafficException(te.getTrafficType(), te.getMessage());
        } catch (LoopTimeoutException lte) {
            throwNewEcsTrafficException(BatTrafficPlugin.class, lte.getMessage());
        }
        mLogger.info(Verdict.STARTED, "", "BAT Traffic", "");
    }

    /**
     * {@inheritDoc}
     * Shuts down BAT traffic after all test cases
     */
    @Override
    public void stopTraffic() {
        if (isBatFaulty()) {
            return;
        }
        mLogger.info(EcsAction.STOPPING, "", "BAT Traffic", "");
        if (!mTrafficBeingMeasured) {
            mLogger.info(Verdict.STOPPED, "", "BAT Traffic", "");
            return;
        }
        try {
            for (mBatControllerVmIndex = 0; mBatControllerVmIndex < 2 ; mBatControllerVmIndex++) {
               if (mFIOTrafficEnabled) {
                   mBatCic.stopFioTraffic(mBatControllerVmIndex);
               }
               mBatCic.stopTestAppTraffic(mBatControllerVmIndex);
           }
        } catch (EcsTrafficException te) {
            throwNewEcsTrafficException(te.getTrafficType(), te.getMessage());
        } catch (LoopTimeoutException lte) {
            throwNewEcsTrafficException(BatTrafficPlugin.class, lte.getMessage());
        }
        mTrafficBeingMeasured = false;
        mLogger.info(Verdict.STOPPED, "", "BAT Traffic", "");
    }

    /**
     * {@inheritDoc}
     * Not used
     */
    @Override
    public void stopTrafficStatistics() {
        mLogger.debug("Stop BAT traffic statistics");
    }
}

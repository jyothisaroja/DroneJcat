package com.jcat.cloud.fw.infrastructure.base;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.openstack4j.api.types.Facing;
import org.testng.annotations.AfterClass;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.glance.GlanceController;
import com.jcat.cloud.fw.components.system.cee.openstack.heat.HeatController;
import com.jcat.cloud.fw.components.system.cee.openstack.keystone.KeystoneController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.openstack.swift.SwiftController;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.components.system.cee.target.EcsScaleIoBladeList;
import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;
import com.jcat.cloud.fw.components.system.cee.watchmen.WatchmenEventListener;
import com.jcat.cloud.fw.fwservices.traffic.controllers.AtlasBatTrafficController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.switches.extremeswitch.IExtremeSwitchLib;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.LoggingLevel;

import se.ericsson.jcat.fw.annotations.JcatMethod;
import se.ericsson.jcat.fw.assertion.JcatAssertApi;
import se.ericsson.jcat.fw.logging.JcatLoggingApi;

/**
 * Superclass for all test classes. Provides JCAT exposed functionality as well as Cloud specific functionality
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-01-18 Initial version
 * @author esauali 2013-03-08 Add @Factory method for testng listener initialization through Guice workaround
 * @author esauali 2013-03-29 Add usage of LazyLoader, remove @Facotory, @Listeners, mInjector
 * @author ehosmol 2013-04-16 Add getter methods
 * @author emulign 2013-04-15 Modified libraries initialization to class level.
 * @author ezhgyin 2013-09-12 Add TASS resource booking/unbooking in @BeforeSuite / @AfterSuite
 * @author emulign 2013-10-17 Pointed base url to the configuration file
 * @author eqinann 2013-11-08 Added health check before and after test
 * @author ezhgyin 2013-11-19 Fix booking cancellation
 * @author emulign 2013-11-28 Added new nodeState library
 * @author emulign 2014-01-17 Renamed before/after methods and changed Health Check to be run at class level
 * @author ezhgyin 2014-02-06 Add function for specifying which states should be included in health check
 * @author ezhgyin 2014-02-11 Print FW version before suite
 * @author ezhgyin 2014-06-17 Change default node booking time to 5 hours
 * @author ezhgyin 2014-07-09 Add printSystemVersion
 * @author ezhgyin 2014-09-01 Catch & ignore all exceptions in printSystemVersion
 * @author eelimei 2014-11-03 Add injection and cleanup for NovaController
 * @author eelimei 2014-10-29 added injection and cleanup for Cinder- and Keystone Controller
 * @author ezhgyin 2014-11-12 Add injection for NeutronController
 * @author ezhgyin 2014-11-27 fix automatic cleanup
 * @author eelimei 2014-11-21 Add injection for CliController and send cli command through controller instead of
 *         openstacklib
 * @author eelimei 2015-01-28 Make cleanup final so that it cannot be overriden in the testcases
 * @author epergat 2015-05-19 Added WatchmenController to JcatTelcoDcTestCase
 * @author zdagjyo 2017-06-14 Added IExtremeSwitchLib and its cleanup to JcatTelcoDcTestCase
 * @author zmousar 2018-02-10 Added EcsScaleIoBladeList and its cleanup to JcatTelcoDcTestCase
 * @author zdagjyo 2019-01-02 Added alarm verification and healthcheck in afterclass
 */
public class JcatTelcoDcTestCase extends CloudTestCaseConfiguration {

    protected AtlasBatTrafficController mAtlasBatController;

    @Inject
    protected CinderController mCinderController;

    @Inject
    protected EcsComputeBladeList mEcsComputeBladeList;

    @Inject
    protected EcsScaleIoBladeList mEcsScaleIoBladeList;

    @Inject
    protected IEquipmentController mEquipmentController;

    @Inject
    protected IExtremeSwitchLib mExtremeSwitch;

    @Inject
    protected GlanceController mGlanceController;

    @Inject
    protected HeatController mHeatController;

    @Inject
    protected KeystoneController mKeystoneController;

    @Inject
    protected WatchmenEventListener mWatchmenEventListener;

    /**
     * Logger instance to be used just for base class
     */
    private final EcsLogger mTestBaseClassLogger = EcsLogger.getLogger(this.getClass());

    @Inject
    protected NeutronController mNeutronController;

    @Inject
    protected NovaController mNovaController;

    @Inject
    protected SwiftController mSwiftController;

    /**
     * Clean up controllers after each test case
     * Renamed to controller clean up to avoid overriding by sub classes
     */
    final protected void controllersCleanup() {
        JcatLoggingApi.setTestStepBegin("Cleaning up resources created by test case");
        if (mAllTestSucceed || mConfiguration.forceCleanup()) {
            if (mAllTestSucceed) {
                mLogger.info("All tests passed, cleanup now!");
            } else {
                mLogger.warn("There are tests failing, but force cleanup was set, cleanup now!");
            }
            // In case test case failed with non-admin client
            mOpenStack4jEcs.getClientForAdminUser(Facing.PUBLIC);
            JcatLoggingApi.setTestStepBegin("Clean up Nova Controller");
            mNovaController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up Neutron Controller");
            mNeutronController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up Glance Controller");
            mGlanceController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up Heat Controller");
            mHeatController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up Cinder Controller");
            mCinderController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up Keystone Controller");
            mKeystoneController.cleanup();
            JcatLoggingApi.setTestStepBegin("Clean up ECS CIC List");
            mEcsCicList.deinitialize();
            JcatLoggingApi.setTestStepBegin("Clean up ECS Compute Blade List");
            mEcsComputeBladeList.deinitialize();
            JcatLoggingApi.setTestStepBegin("Clean up ECS ScaleIo Blade List");
            mEcsScaleIoBladeList.deinitialize();
            JcatLoggingApi.setTestStepBegin("Clean up ECS Fuel");
            mEcsFuel.deinitialize();
            JcatLoggingApi.setTestStepBegin("Clean up Extreme Switch");
            mExtremeSwitch.cleanup();
        } else {
            mLogger.warn("There are tests(or setup) failing, skip cleanup!");
        }
        mLogger.info(Verdict.DONE, "Clean up", EcsComponent.class, "");
    }

    /**
     * Method which will be run after each test class
     */
    @JcatMethod(testTag = "", testTitle = "Perform Bat Check after test")
    @AfterClass(alwaysRun = true, dependsOnMethods = "postTestHealthCheck")
    protected void postTestBatCheck() {
        if (!mConfiguration.getLoggingLevel().equals(LoggingLevel.CI)) {
            JcatLoggingApi.setTestStepBegin("Perform BAT check before testcase execution");
            JcatAssertApi.saveAssertTrue("BAT was not started correctly, traffic is not acceptable",
                    mBatController.isAcceptableTraffic());
        }
    }

    /**
     * Method which will be run after each test class
     */
    @JcatMethod(testTag = "", testTitle = "Perform health check after test")
    @AfterClass(alwaysRun = true, dependsOnMethods = "postTestVerifyAlarms")
    protected void postTestHealthCheck() throws IOException, InterruptedException, ExecutionException {
        if (!mConfiguration.getLoggingLevel().equals(LoggingLevel.CI)) {
            JcatLoggingApi.setTestStepBegin("Perform health check after testcase execution");
//            performHealthCheck();

        }
    }

    /**
     * Method which will be run after each test class
     */
    @JcatMethod(testTag = "", testTitle = "Perform resource cleanup after test")
    @AfterClass(alwaysRun = true, dependsOnMethods = "postTestBatCheck")
    protected void postTestResourceCleanup() {
        controllersCleanup();
        JcatAssertApi.assertTrue("Not all alarms/alerts were captured by WatchmentEventListener",
                mWatchmenEventListener.getResult());
    }

    /**
     * Method which will be run after each test class
     */
    @JcatMethod(testTag = "", testTitle = "Verify watchmen alarms after test")
    @AfterClass(alwaysRun = true)
    protected void postTestVerifyAlarms() throws IOException {
        JcatLoggingApi.setTestStepBegin("Collect active alarms and alarm history after testcase");
        List<EcsSnmpEvent> alarmHistoryAfterTest = mWatchmenService.getAlarmHistory();
        alarmHistoryAfterTest.removeAll(mAlarmHistoryBeforeTest);
        mLogger.info(Verdict.FOUND, "", "alarms in history during testcase", alarmHistoryAfterTest.toString());
        List<EcsSnmpEvent> activeAlarmListAfterTest = mWatchmenService.listActiveAlarms();
        activeAlarmListAfterTest.removeAll(mActiveAlarmListBeforeTest);
        JcatAssertApi.saveAssertTrue(
                "Found few outstanding active alarms during testcase:" + activeAlarmListAfterTest.toString(),
                activeAlarmListAfterTest.size() == 0);
        if (activeAlarmListAfterTest.size() == 0) {
            mLogger.info(Verdict.FOUND, "", "","no outstanding alarms during testcase");
        }
        JcatLoggingApi.setTestStepBegin("Download core dumps to local file system if they exist");
        mEcsFuel.downloadCoreDumps(this.getClass().getSimpleName());
    }

    /**
     * {@link EcsLogger#testStep(String)}}
     */
    protected void testStep(String testStep) {
        mLogger.testStep(testStep);
    }
}

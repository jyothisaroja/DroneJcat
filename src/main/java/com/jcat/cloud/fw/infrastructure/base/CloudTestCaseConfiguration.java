package com.jcat.cloud.fw.infrastructure.base;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openstack4j.api.types.Facing;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.xml.sax.SAXException;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.ericsson.tass.adapter.TassAdapter;
import com.ericsson.tass.booker.IBooker;
import com.ericsson.tass.booker.exceptions.BookerException;
import com.ericsson.tass.booker.exceptions.BookingAlreadyExistException;
import com.ericsson.tass.booker.exceptions.NoSuchResourceException;
import com.ericsson.tass.booker.exceptions.ResourceManagementException;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.identity.EcsCredentials;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel.HealthCheckResults;
import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent;
import com.jcat.cloud.fw.components.system.cee.watchmen.WatchmenService;
import com.jcat.cloud.fw.fwservices.traffic.controllers.BatTrafficController;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.LoggingLevel;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.ResourceAdapter;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.TassBooking;
import com.jcat.cloud.fw.infrastructure.modules.PropertiesModule;
import com.jcat.cloud.fw.infrastructure.modules.ResourceModule;
import com.jcat.cloud.fw.infrastructure.modules.ServiceModule;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

import se.ericsson.jcat.fw.annotations.JcatMethod;
import se.ericsson.jcat.fw.assertion.JcatAssertApi;
import se.ericsson.jcat.fw.logging.JcatLoggingApi;

@Guice(modules = { PropertiesModule.class, ResourceModule.class, ServiceModule.class })
public class CloudTestCaseConfiguration {
    // By default, the node will be booked for 5 hours
    protected static final int DEFAULT_BOOKING_PERIOD = 5;
    /**
     * StringBuilder for taking method execution time log for successful ones
     */
    protected static StringBuilder mExecFailStrBuilder = new StringBuilder();
    /**
     * StringBuilder for taking method execution time log for successful ones
     */
    protected static StringBuilder mExecSuccessStrBuilder = new StringBuilder().append("\n[ * ---------------- * ]\n");
    protected boolean mAllTestSucceed = true;
    protected boolean mBookingCancelNeeded = true;
    protected TestConfiguration mConfiguration;
    protected Long mResourceBookingId;
    protected TassAdapter mTassAdapter;
    protected List<EcsSnmpEvent> mActiveAlarmListBeforeTest;
    protected List<EcsSnmpEvent> mAlarmHistoryBeforeTest;
    protected IBooker mTassBooker;

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    @Inject
    protected EcsFuel mEcsFuel;

    @Inject
    protected EcsCicList mEcsCicList;

    @Inject
    protected OpenStack4jEcs mOpenStack4jEcs;

    @Inject
    protected WatchmenService mWatchmenService;

    protected BatTrafficController mBatController = BatTrafficController.getInstance();

    /**
     * Logger instance to be used in a test case
     */
    protected final EcsLogger mLogger = EcsLogger.getLogger(this.getClass());
    private final Logger mResultLogger = Logger.getLogger("TestResults");

    /**
     * Save info to execution time log
     *
     * @param methodName
     * @param time
     * @param passed int representing result of test, can only be 1, 2 or 3
     */
    private static void addToExecTimeLog(String methodName, String time, int passed) {
        String str = String.format("  %-60s : %s\n", methodName, time);
        if (passed == ITestResult.SUCCESS) {
            mExecSuccessStrBuilder.append("[ * METHOD EXECUTION * ] ").append(" [ PASS ]").append(str);
        } else if (passed == ITestResult.FAILURE) {
            mExecFailStrBuilder.append("[ * METHOD EXECUTION * ] ").append(" [ FAIL ]").append(str);
        } else if (passed == ITestResult.SKIP) {
            mExecFailStrBuilder.append("[ * METHOD EXECUTION * ] ").append(" [ SKIP ]").append(str);
        }
    }

    /**
     * Local method for getting the TASS booker
     *
     * @return
     */
    private IBooker getTassBooker() {
        // NOTE: should only have ONE TASS and booker instance, not to create multiple of them.
        if (null == mTassAdapter) {
            mTassAdapter = new TassAdapter(mConfiguration.getBaseUrl(), mConfiguration.getNode(),
                    mConfiguration.getUserName());
        }
        if (null == mTassBooker) {
            mTassBooker = mTassAdapter.getBooker();
        }
        return mTassBooker;
    }

    /**
     * This method prints system information (for now, it is the contents of /etc/version on CIC)
     * TODO: needs to update getAllCics to getCic(Status.ACTIVE) when available
     */
    private void printSystemVersion() {
        try {
            mLogger.info("Current System Version Information is: " + mEcsFuel.getCeeVersion());
        } catch (Exception e) {
            mLogger.warn("Could not print System Version Information due to " + e.getMessage());
        }
    }

    @AfterMethod(alwaysRun = true)
    protected void afterMethod(ITestResult result) {
        if (!result.isSuccess()) {
            mAllTestSucceed = false;
        }
        long executionTimeInMilliseconds = result.getEndMillis() - result.getStartMillis();
        DateFormat df = new SimpleDateFormat("m 'mins,' s 'seconds,' S 'milliseconds'");
        addToExecTimeLog(
                result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName(),
                df.format(new Date(executionTimeInMilliseconds)), result.getStatus());
    }

    /**
     * Method which will be run after test suite.
     */
    @AfterSuite(alwaysRun = true)
    protected void afterSuite() {
        printExecutionTime();
        cancelNodeBooking();
    }

    /**
     * Method which runs before each test class which initialize @Inject resources and OpenStack4jEcs resource and
     * capture node states
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws ClassNotFoundException
     */
    @BeforeClass(alwaysRun = true)
    protected void beforeClass()
            throws ClassNotFoundException, SAXException, IOException, ParserConfigurationException {
        mLogger.info("Initializing " + getClass().getSimpleName());
        mLogger.info("Allocating resources");
    }

    /**
     * Before Suite. It performs:</br>
     * - Book the node to use </br>
     * - Print System version information </br>
     *
     * @throws BookerException
     */
    @BeforeSuite(alwaysRun = true)
    protected void beforeSuite() throws BookerException {
        printFwVersion();
        bookNode();
        printSystemVersion();
    }

    /**
     * Before Test, it performs BAT check
     */
    @JcatMethod(testTag = "", testTitle = "Perform BAT-Check before test")
    @BeforeClass(alwaysRun = true)
    protected void preTestBatCheck() {
        if (!mConfiguration.getLoggingLevel().equals(LoggingLevel.CI)) {
            JcatLoggingApi.setTestStepBegin("Perform BAT check before testcase execution");
            JcatAssertApi.saveAssertTrue("BAT was not started correctly, traffic is not acceptable",
                    mBatController.isAcceptableTraffic());
        }
    }

    /**
     * Before Test, it performs health check
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws IOException
     */
    @JcatMethod(testTag = "", testTitle = "Perform healthcheck before test")
    @BeforeClass(alwaysRun = true, dependsOnMethods = "preTestBatCheck")
    protected void preTestHealthCheck() throws IOException, InterruptedException, ExecutionException {
        if (!mConfiguration.getLoggingLevel().equals(LoggingLevel.CI)) {
            JcatLoggingApi.setTestStepBegin("Perform health check before testcase execution");
            //performHealthCheck();
        }
    }

    /**
     * Before Test, it performs alarm verification
     */
    @JcatMethod(testTag = "", testTitle = "Verify watchmen alarms before test")
    @BeforeClass(alwaysRun = true, dependsOnMethods = "preTestHealthCheck")
    protected void preTestVerifyAlarms() {
        JcatLoggingApi.setTestStepBegin("Collect active alarms and alarm history before testcase");
        mActiveAlarmListBeforeTest = mWatchmenService.listActiveAlarms();
        mLogger.info(Verdict.FOUND, "", "active alarms", mActiveAlarmListBeforeTest.toString());
        mAlarmHistoryBeforeTest = mWatchmenService.getAlarmHistory();
        mLogger.info(Verdict.FOUND, "", "alarm history at " + new Date(), mAlarmHistoryBeforeTest.toString());
    }

    /**
     * This method Check that the -node property is in place and book the node if TASS is used
     *
     * @return - bookId from TASS
     * @throws BookerException
     */
    protected Long bookNode() throws BookerException {
        // Check pre-conditions
        String node = mConfiguration.getNode();
        JcatAssertApi.assertNotNull("JVM Property -Dnode is missing", node);

        // if TASS resource is used
        if (mConfiguration.getResourceAdapter().equals(ResourceAdapter.TASS)) {
            mLogger.debug("Tass resource is used!");
            // if TASS booking is NOT turned OFF
            if (TassBooking.ON.equals(mConfiguration.getTassBookingMode())) {
                mLogger.debug("Tass booking is ON.");
                mLogger.info("Booking Node:" + node + " in TASS");
                try {
                    mResourceBookingId = getTassBooker().checkAndBookResource(mTassAdapter.getTestConfigurationId(),
                            DEFAULT_BOOKING_PERIOD);
                    mLogger.info("Node booked, booking id:" + mResourceBookingId);
                } catch (BookingAlreadyExistException e) {
                    mBookingCancelNeeded = false;
                    mLogger.info("Node already booked by same user, skipping");
                }
            }
        }
        return mResourceBookingId;
    }

    /**
     * Cancel the node booking after the execution
     */
    protected void cancelNodeBooking() {
        // if TASS resource is used
        if (mConfiguration.getResourceAdapter().equals(ResourceAdapter.TASS)) {
            mLogger.debug("Tass resource is used!");
            // if TASS booking is NOT turned OFF
            if (mConfiguration.getTassBookingMode() == TassBooking.ON && mBookingCancelNeeded
                    && null != mResourceBookingId) {
                mLogger.info("Cancelling booking with ID " + mResourceBookingId + " in Tass.");
                try {
                    getTassBooker().cancelBooking(mResourceBookingId);
                } catch (NoSuchResourceException e) {
                    mLogger.warn("Error during Node unbooking", e);
                } catch (ResourceManagementException e) {
                    mLogger.warn("Error during Node unbooking", e);
                } catch (Exception e) {
                    mLogger.warn("Error during Node unbooking", e);
                }

            }
        }
    }

    /**
     * Getter method for the member variable {@link #mConfiguration}
     *
     * @return {@link #mConfiguration}
     */
    protected TestConfiguration getConfiguration() {
        return mConfiguration;
    }

    /**
     *
     * @param configurationDataId - id of a {@link ConfigurationData}
     * @return
     */
    protected boolean isResourceConfiguredInTass(String configurationDataId) {
        return mCfa.contains(configurationDataId);
    }

    /**
     * perform health check on node
     */
    protected void performHealthCheck() throws IOException, InterruptedException, ExecutionException {
        HealthCheckResults healthCheckResults = mEcsFuel.performHealthCheck();
        int warnings = healthCheckResults.getWarningsCount();
        int failures = healthCheckResults.getFailedCount();
        String logFile = healthCheckResults.getLogFile();
        boolean healthCheckPassed = (warnings == 0 && failures == 0);
        JcatAssertApi.saveAssertTrue(
                "Healthcheck failed, found " + warnings + " warnings and " + failures + " failures", healthCheckPassed);
        if (healthCheckPassed) {
            mLogger.info(Verdict.VALIDATED, "", "Health Check", "passed with 0 warnings and 0 failures");
        } else {
            mLogger.error("Healthcheck failed, found " + warnings + " warnings and " + failures + " failures");
        }
        JcatLoggingApi.setTestStepBegin("Download RabbitMQ core dumps to local file system if they exist");
        mEcsFuel.downloadRabbitMQCoreDumps(this.getClass().getSimpleName());
        JcatLoggingApi.setTestStepBegin("Print the health check log file generated by health check script");
        mEcsFuel.readFile("", logFile);
    }

    /**
     * print execution time for test methods
     */
    protected void printExecutionTime() {
        mExecFailStrBuilder.append("[ * ---------------- * ]\n");
        String str = new StringBuilder().append(mExecSuccessStrBuilder.toString())
                .append(mExecFailStrBuilder.toString()).toString();
        mResultLogger.info(str);
    }

    /**
     * Print the current FW version
     */
    protected void printFwVersion() {
        // print FW version
        String version = "unknown";
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("project");
            version = resourceBundle.getString("project.version");
        } catch (MissingResourceException e) {
            mLogger.warn("Error while reading current FW version.");
        }
        mLogger.info("Current FW version is: " + version);
    }

    /**
     * Set {@link TassAdapter}
     *
     * @param tassAdapter
     */
    public void setTassAdapter(TassAdapter tassAdapter) {
        mTassAdapter = tassAdapter;
    }

    /**
     * Set {@link TestConfiguration}
     *
     * @param configuration
     */
    @Inject
    public final void setTestConfiguration(TestConfiguration configuration) {
        mConfiguration = configuration;
    }

    /**
     * Use different Tenant/User to get openstack authentication, all the follow up operations will use this
     * tenant/user.
     *
     * @param credentials
     */
    public void switchTenantUser(EcsCredentials credentials) {
        mOpenStack4jEcs.getClient(Facing.PUBLIC, credentials);
    }
}

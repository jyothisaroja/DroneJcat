package com.jcat.cloud.fw.infrastructure.configurations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.testng.Assert;
import org.testng.ISuite;

import se.ericsson.jcat.fw.utils.TestInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.jcat.cloud.fw.common.annotations.EcsTestProperty;
import com.jcat.cloud.fw.components.model.image.EcsImage.CeeVmImage;
import com.jcat.cloud.fw.components.model.image.VmImage;

/**
 * <p>
 * Class for handling configuration properties. It must be initialized by Guice in order to parse system properties
 * before JCAT and override/provide required properties to JCAT
 * </p>
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2012-12-28 Initial version
 * @author emulign 2013-01-25 Refactoring to use Guice and make it Singleton
 * @author esauali 2013-03-08 Re-design, add property injection, parseTestNgSuiteProperties(),
 *         configureLog4jConfiguration()
 * @author esauali 2013-03-15 Add getLoggingLevel(), sort
 * @author esauali 2013-03-20 Add getCatlogUrl(), setSuiteName()
 * @author emulign 2013-03-19 Add mNodeName and mIsTassAdapter
 * @author esauali 2013-03-24 Add enum ResourceAdapter, mResourcePath, redesign setters since class is not going to be
 *         created by Guice anymore
 * @author esauali 2013-04-05 Add support for lower case letters for property "resourceAdapter"
 * @author ewubnhn 2013-04-17 Added error messages for improperly set properties
 * @author ewubnhn 2013-05-07 Fixed bug preventing loading of debug log4j file from jar
 * @author ehosmol 2013-05-17 Add support for lower case letters for mNodeName
 * @author ehosmol 2013-06-14 Fix overrideTestNgSuiteParameters(ISuite)
 * @author emulign 2013-08-01 Added new execution variable ecs.serviceurls to choose from internal or public service
 *         endpoints urls
 * @author ewubnhn 2013-08-02 Added "offlinehtml" option for local logging
 * @author esauali 2013-08-06 Fix fail to execute due to ecs.serviceurls property not provided
 * @author ewubnhn 2013-08-08 Fix "offlinehtml" option. Move DB logwriter removal to JcatTelcoDcListener
 * @author esauali 2013-09-05 Add service url handling mode options
 * @author ezhgyin 2013-09-12 Add TASS booking mode configuration
 * @author eqinann 2013-09-30 Add traffic xml and openstack zone name for specifying zone used on node
 * @author emulign 2013-10-14 Fixed configdb endpoint
 * @author emulign 2013-10-29 Fixed detected sonar errors
 * @author eqinann 2013-11-06 Add health check switch
 * @author esauali 2013-11-20 Remove dependency on System.getProperties()
 * @author esauali 2013-01-10 Add LXC_REDUNDACY in OsServiceUrlMode
 * @author emulign 2014-02-05 Add regexp checker for properties
 * @author ezhgyin 2014-02-06 Add health check configuration file path
 * @author epergat 2014-05-08 Added possiblity to add options to the EcsInject functionality.
 * @author ehosmol 2014-06-25 Added VM booting mode configuration, checkValueAgainstValidvalues()
 * @author efalart 2014-07-02 Added ExcelLogger
 * @author efalart 2014-07-08 Fixed excel log writer to allow configuration catlog,excel
 * @author ezhgyin 2014-10-31 Added os4j logging in debug mode
 * @author ezhgyin 2014-11-27 add force cleanup option
 * @author ethssce 2015-01-21 changed default vm boot to boot from image
 * @author ehkiyki 2015-07-21 Added possibility to extract the suite id from the Catlog URL
 * @author epergat 2015-07-07 Adapted to JCAT version R4C05.
 */
public class TestConfiguration {
    /**
     *
     * Enum representing JCAT JVM properties
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2013
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author esauali 2013-03-06 Initial implementation
     * @author ewubnhn 2014-04-14 Updated logdb.properties location (removed /)
     */
    protected enum JcatProperty {
        ENABLELOG(TestInfo.ENABLE_LOG, "false"), LOGDB(TestInfo.DB_PROPERTIES, "logdb.properties"), LOGWRITERS(
                TestInfo.LOG_WRITERS, null);

        private String mDefaultValue;
        private String mParameterName;

        /**
         * Main constructor
         *
         * @param parameterName
         * @param defaultValue
         */
        private JcatProperty(String parameterName, String defaultValue) {
            mParameterName = parameterName;
            mDefaultValue = defaultValue;
        }

        /**
         * Get default value
         *
         * @return
         */
        public String getDefaultValue() {
            return mDefaultValue;
        }

        /**
         * Get parameter name
         *
         * @return
         */
        public String getParameterName() {
            return mParameterName;
        }
    }

    /**
     * ENUM defining Atlas Bat Traffic mode
     *
     * @author zdagjyo 2018-04-23 Initial version
     */
    public enum AtlasBatTraffic {
        OFF, ON;
        public static final String PARAMETER_NAME = "ecs.atlasBatTraffic";
    }

    /**
     * ENUM defining log output format JCAT
     *
     * @author esauali 2013-03-08 Initial version
     * @author ewubnhn 2013-07-15 Added OFFLINEHTML
     */
    protected enum LoggingType {
        CATLOG, JCATHTML, NONE, OFFLINEHTML, EXCEL;
        public static final String PARAMETER_NAME = "ecs.loggingMode";
    }

    public enum LoggingLevel {
        CI, DEBUG, INFO;
        public static final String PARAMETER_NAME = "ecs.loggingLevel";
    }

    /**
     * Options for choosing which OpenStack service URL's handling mode to use
     *
     * @author esauali
     *
     */
    public enum OsServiceUrlMode {
        /**
         * Use admin urls
         */
        ADMIN,
        /**
         * Use internal URL's
         */
        INTERNAL,
        /**
         * Rewrite Public URL host, this is needed for node
         * configuration when connection to it is done through LXC port forwarding
         */
        LXC,
        /**
         * Rewrite Public URL host, this is needed for node
         * configuration when connection to it is done through LXC port forwarding. Doesn't change port numbers
         */
        LXC_REDUNDANCY,
        /**
         * Use public URL's, default in jClouds
         */
        PUBLIC
    }

    /**
     * Resource adapter types, currently FILE_XML and TASS
     *
     * @author esauali 2013-03-27 Initial version
     *
     */
    public enum ResourceAdapter {
        FILE_XML, TASS;
        public static final String PARAMETER_NAME = "ecs.resourceAdapter";
    }

    /**
     * ENUM defining TASS resource booking mode
     *
     * @author ezhgyin 2013-09-11 Initial version
     */
    public enum TassBooking {
        OFF, ON;
        public static final String PARAMETER_NAME = "ecs.tassBooking";
    }

    /**
     * Enum represanting virtual machine (server) booting mode in openstack
     *
     * @author ehosmol 2014-06-25 Initial version
     */
    public enum VmBootMode {
        IMAGE, VOLUME;
        public static final String PARAMETER_NAME = "ecs.vmBootMode";

    }

    /**
     * Path to log4j XML configuration file containing setup for debugging
     */
    private static final String LOG4J2_DEBUG_FILE_PATH = "log4j2-debug.xml";

    /**
     * Path to the log4j XML configuration file containing setup for CI execution.
     */
    private static final String LOG4J2_CI_CONFIGURATION_FILE_PATH = "log4j2-ci.xml";

    /**
     * Path used by XML adapter
     */
    private static final String RESOURCE_CONFIGURATION_PATH = "/resourcesConfiguration.xml";

    /**
     * Used to know the TASS endpoint
     */
    private static final String TASS_URL = "https://cloud-lab.rnd.ki.sw.ericsson.se";

    /**
     * Prefix for system properties to be treated as Jcat suite parameters. Value: {@value}
     */
    private static final String TESTNG_SUITE_PARAM_PREFIX = "tparam.";

    private static TestConfiguration mTestConfiguration;

    /**
     * Property defining whether Atlas BAT traffic is to be started
     */
    private AtlasBatTraffic mAtlasBatTraffic = AtlasBatTraffic.OFF;

    /**
     * Property defining the virtual machine booting mode. Default mode is set to image.
     */
    private VmBootMode mVmBootingMode = VmBootMode.IMAGE;

    /**
     * Property defines whether cleanup should be run regardless of test result
     */
    private boolean mForceCleanup = true;

    /**
     * Property specifies whether health check is enabled or not
     */
    private boolean mHealthCheckEnabled = false;

    private String mHealthCheckConfigurationFilePath;

    private Properties mInputProperties;

    private String mLogDbFilePath;

    private final Logger mLogger = LogManager.getLogger(TestConfiguration.class);

    /**
     * Property defining logging level (debug)
     */
    private LoggingLevel mLoggingLevel = LoggingLevel.INFO;

    /**
     * Property defining logging mode, a mix of {@link LoggingType}
     */
    private final List<LoggingType> mLoggingMode = new ArrayList<>();

    /**
     * Property containing node name to fetch information
     */
    private String mNodeName;

    /**
     * Property specifies OpenStack zone name that is used in framework
     */
    private String mOpenStackZoneName;

    /**
     * Options given in the EcsInject annotation
     */
    private String mOptions;

    /**
     * Selected {@link OsServiceUrlMode}
     */
    private OsServiceUrlMode mOsServiceUrlHandlingMode = OsServiceUrlMode.LXC_REDUNDANCY;

    /**
     * Resource adapter to be used for getting resource information, default is TASS
     */
    private ResourceAdapter mResourceAdapter = ResourceAdapter.TASS;

    /**
     * Path to resource file used when ResourceAdapter = XML | JSON
     */
    private String mResourcePath;

    /**
     * Suite name
     */
    private String mSuiteName;

    /**
     *  Property enabling tag logger
     */
    private boolean mTagLoggerEnabled = true;

    /**
     * Property defining whether TASS resource booking is needed
     */
    private TassBooking mTassBooking = TassBooking.ON;

    /**
     * Holder for parameters to be used to override test suite parameters.
     *
     * @see #TESTNG_SUITE_PARAM_PREFIX
     */
    private Map<String, String> mTestNgSuiteProperties;

    /**
     * Property specifies Traffic XML file
     */
    private String mTrafficXml;

    /**
     * System user name
     */
    private String mUserName;

    /**
     * Array of image names
     */
    private VmImage[] mVmImages = CeeVmImage.values();

    // Singleton pattern doesn't allow public Constructors since that works against
    // the whole idea... This has package visibility for the sake of unit testing.
    TestConfiguration() {
        mLogger.debug("Initializing TestConfiguration");

    }

    public static TestConfiguration getTestConfiguration() {
        if (mTestConfiguration == null) {
            mTestConfiguration = new TestConfiguration();
            mTestConfiguration.parseProperties(System.getProperties());
        }

        return mTestConfiguration;
    }

    /**
     * Checks the value provided in properties matches the expected values. It is not case sensitive
     *
     * @param propertyValue
     * @param annotation
     */
    private void checkValueAgainstValidValues(String propertyValue, EcsTestProperty annotation) {

        ArrayList<String> validValues = new ArrayList<>();
        for (String validValue : annotation.validValues()) {
            validValues.add(validValue.toUpperCase());
        }
        // Split "," separated property values to string array
        String[] propertyValues = propertyValue.split("\\,+");
        if (!validValues.get(0).equalsIgnoreCase("")) {
            for (String value : propertyValues) {
                if (!validValues.contains(value.toUpperCase())) {
                    mLogger.error(annotation.errorMessage());
                    Assert.fail("Undifined Value: " + value + " for Property: " + annotation.value());
                }
            }
        }
    }

    /**
     * Checks that the value provided in the properties matches the expected regular expression
     *
     * @param propertyValue
     * @param regexp
     */
    private void checkValueWithRegexp(String propertyValue, EcsTestProperty annotation) {
        String regexp = annotation.regexp();
        if (!propertyValue.matches(regexp)) {
            mLogger.error(annotation.errorMessage());
            Assert.fail("Property " + annotation.value() + " is not following the pattern expected: \"" + regexp + "\"");
        }
    }

    /**
     * Options for parameter "logging":
     * <ul>
     * <li>
     * - "catlog" - catlog + framework statistics logging</li>
     * <li>
     * - "jcathtml" - JCAT html logging + framework statistics logging</li>
     * <li>
     * - "offlinehtml" - JCAT html logging with NO framework stats logging (local html only)</li>
     * <li>
     * - "catlog,jcathtml" - Catlog + JCAT html + default framework stats logging</li>
     * <li>
     * - "excel" - excel file with test case result only, no logging - even info level</li>
     * <li>
     * - "catlog,excel" - Catlog + excel file</li>
     * <li>
     * - "none" - no Catlog, no JCAT html, no framework stats logging</li>
     * <li>
     * - "" (empty, not provided) - catlog + framework statistics logging</li>
     * <li>
     * "ci" - All above options, saved to file.</li>
     * </ul>
     */
    private void configureJcatLogWriters() {
        // Add framework statistics log writers by default
        List<String> logWriters = new ArrayList<>();
        if (null != mLoggingMode && mLoggingMode.isEmpty()) {
            // Set default
            mLoggingMode.add(LoggingType.CATLOG);
        }
        setJcatProperty(JcatProperty.ENABLELOG.getParameterName(), "false");
        for (LoggingType loggingType : mLoggingMode) {
            switch (loggingType) {
            case NONE:
                logWriters = null;
                setJcatProperty(JcatProperty.LOGWRITERS.getParameterName(), "");
                break;
            case CATLOG:
                break;
            case JCATHTML:
                setJcatProperty(JcatProperty.ENABLELOG.getParameterName(), "true");
                break;
            case OFFLINEHTML:
                logWriters = null;
                setJcatProperty(JcatProperty.LOGWRITERS.getParameterName(), "");
                setJcatProperty(JcatProperty.ENABLELOG.getParameterName(), "true");
                break;
            default:
                break;
            }
        }

        if (null != logWriters) {
            setJcatProperty(JcatProperty.LOGWRITERS.getParameterName(), Joiner.on(",").join(logWriters));
        }
    }

    /**
     * Creates an error message using the provided value and info from {@link EcsTestProperty} annotation
     *
     * @param annotation {@link EcsTestProperty} Annotation for property with error
     * @param propertyValue String Property value provided by user
     * @return String The error message
     *
     */
    private String genPropertyErrorMessage(EcsTestProperty annotation, String propertyValue) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append(annotation.errorMessage());
        errorMessage.append(" Provided value: \"" + propertyValue);
        if (annotation.validValues() != null) {
            errorMessage.append("\" not in valid values: {");
            for (String value : annotation.validValues()) {
                errorMessage.append(value + ",");
            }
            errorMessage.deleteCharAt(errorMessage.lastIndexOf(",")).append("}.");
        } else {
            errorMessage.append("\"");
        }
        return errorMessage.toString();
    }

    /**
     * Set specified JCAT property
     *
     * @param key
     * @param value
     */
    private void setJcatProperty(String key, String value) {
        mLogger.debug("Setting property:" + key + " to:" + value);
        System.setProperty(key, value);
    }

    /**
     * Get {@link #mTestNgSuiteProperties}
     *
     * @return - Map<String, String> if at least one parameter is defined, null otherwise
     */
    protected Map<String, String> getTestNgSuiteProperties() {
        return mTestNgSuiteProperties;
    }

    /**
     *
     * Parse system properties to populate {@link #mTestNgSuiteProperties}
     */
    protected Map<String, String> parseTestNgSuitePropertiesFromSystem() {
        mLogger.debug("Looking TestNg suite properties");
        int suiteParamLength = TESTNG_SUITE_PARAM_PREFIX.length();
        Map<String, String> testNgProperties = new HashMap<>();
        for (String propertyName : mInputProperties.stringPropertyNames()) {
            if (propertyName.startsWith(TESTNG_SUITE_PARAM_PREFIX)) {
                testNgProperties.put(propertyName.substring(suiteParamLength),
                        mInputProperties.getProperty(propertyName));
            }
        }
        return (testNgProperties.size() > 0) ? testNgProperties : null;
    }

    /**
     * Configure JCAT related properties
     */
    public void configureJcatProperties() {
        mLogger.debug("Initializing JCAT TelcoDC specific properties");
        configureJcatLogWriters();
        // Set default logdb file location
        if (null == mLogDbFilePath || mLogDbFilePath.isEmpty()) {
            mLogDbFilePath = JcatProperty.LOGDB.getDefaultValue();
            setJcatProperty(JcatProperty.LOGDB.getParameterName(), JcatProperty.LOGDB.getDefaultValue());
        }
        mTestNgSuiteProperties = parseTestNgSuitePropertiesFromSystem();
    }

    public void configureLog4j2Configuration() {

        String pathToLog4jConfigFile = null;

        // When running for CI, JCAT will log extensively and save to file.
        if (mLoggingLevel.equals(LoggingLevel.CI)) {
            pathToLog4jConfigFile = LOG4J2_CI_CONFIGURATION_FILE_PATH;
            mLogger.debug("Logging has been configured to CI (Extended logging to file)");
        } else if (mLoggingLevel.equals(LoggingLevel.DEBUG)) { // != null
            // enable os4j debug
            System.getProperties().setProperty(
                    org.openstack4j.core.transport.internal.HttpLoggingFilter.class.getName(), "true");
            pathToLog4jConfigFile = LOG4J2_DEBUG_FILE_PATH;
            mLogger.debug("Logging has been configured to DEBUG");
        } else {
            mLogger.debug("Logging has been configured to INFO");
            return;
        }
        ConfigurationFactory configFactory = XmlConfigurationFactory.getInstance();
        ConfigurationFactory.setConfigurationFactory(configFactory);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classloader.getResourceAsStream(pathToLog4jConfigFile);
        try {
            ConfigurationSource configurationSource = new ConfigurationSource(inputStream);
            ctx.start(configFactory.getConfiguration(configurationSource));
        } catch (IOException e) {
            mLogger.error("Could not load Log4j configuration from file:" + pathToLog4jConfigFile, e);
        }
    }

    /**
     * Getter method for whether to force cleanup
     *
     * @return {@link #mForceCleanup}
     */
    public boolean forceCleanup() {
        return mForceCleanup;
    }

    /**
     * Get Atlas Bat Traffic mode.
     *
     * @return AtlasBatTraffic indicating whether Atlas Bat Traffic is to be started
     */
    public AtlasBatTraffic getAtlasBatTrafficMode() {
        return mAtlasBatTraffic;
    }

    /**
     * Getter for TASS url, i.e: {@value #TASS_URL}
     *
     * @return
     */
    public String getBaseUrl() {
        return TASS_URL;
    }

    /**
     * Get URL to suite logs in Catlog <br>
     * <b>Note: correct URL is returned only when Catlog logging is enabled</b>
     *
     * @return
     */
    public String getCatlogUrl() {
        return TestInfo.getLogUrl();
    }

    /**
     * Get Health Check configuration file path
     *
     * @return {@link #mHealthCheckConfigurationFilePath}
     */
    public String getHealthCheckConfigurationFilePath() {
        return mHealthCheckConfigurationFilePath;
    }

    public Properties getInputProperties() {
        return mInputProperties;
    }

    /**
     * Get {@link #mLoggingLevel}
     *
     * @return
     */
    public LoggingLevel getLoggingLevel() {
        return mLoggingLevel;
    }

    /**
     * Get Node's name
     *
     * @return {@link #mNodeName}
     */
    public String getNode() {
        return mNodeName;
    }

    /**
     * Get OpenStack zone name
     *
     * @return {@link #mOpenStackZoneName}
     */
    public String getOpenStackZoneName() {
        return mOpenStackZoneName;
    }

    /**
     * Getter for the injection options
     *
     * @return
     */
    public String getOptions() {
        return mOptions;
    }

    /**
     * Get selected OpenStack service URL handling option
     *
     * @return {@link #mOsServiceUrlHandlingMode}
     *
     */
    public OsServiceUrlMode getOsServiceUrlHandlingMode() {
        return mOsServiceUrlHandlingMode;
    }

    /**
     * Get {@link #mResourceAdapter}
     *
     * @return {@link ResourceAdapter}
     */
    public ResourceAdapter getResourceAdapter() {
        return mResourceAdapter;
    }

    /**
     * Get {@link #mResourcePath}
     *
     * @return {@link #mResourcePath}
     */
    public String getResourcePath() {
        if (null == mResourcePath) {
            mResourcePath = RESOURCE_CONFIGURATION_PATH;
        }
        return mResourcePath;
    }

    /**
     * Get the suite id from the Catlog URL.
     * <b>Note: correct URL is returned only when Catlog logging is enabled</b>
     *
     * @return The suite id
     */
    public int getSuiteId() {
        Pattern pattern = Pattern.compile("suiteId=(.*?)&");
        Matcher matcher = pattern.matcher(TestInfo.getLogUrl());
        matcher.find();
        String suiteIdStr = matcher.group(1);
        return Integer.parseInt(suiteIdStr);
    }

    /**
     * Get {@link #mSuiteName}
     *
     * @return
     */
    public String getSuiteName() {
        return mSuiteName;
    }

    /**
     * Get TASS booking mode.
     *
     * @return TassBooking indicating whether TASS booking is needed
     */
    public TassBooking getTassBookingMode() {
        return mTassBooking;
    }

    /**
     * Get Traffic Xml file path
     *
     * @return
     */
    public String getTrafficXml() {
        return mTrafficXml;
    }

    /**
     * Get user name that is currently set
     *
     * @return userName which was fetched from system property: user.name
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * Getter method for VM booting mode
     *
     * @return {@link #mVmBootingMode}
     */
    public VmBootMode getVmBootingMode() {
        return mVmBootingMode;
    }

    /**
     * Gets an array of Vm Images to be used during test
     *
     * @return An array of {@link VmImage}
     */
    public VmImage[] getVmImages() {
        return mVmImages;
    }

    /**
     * Get health check enabled status
     *
     * @return {@link #mHealthCheckEnabled}
     */
    public boolean isHealthCheckEnabled() {
        return mHealthCheckEnabled;
    }

    /**
     * Whether Tag Logger is enabled
     * @return
     */
    public boolean isTagLoggerEnabled() {
        return mTagLoggerEnabled;
    }

    /**
     * Override suite properties if there are any provided in {@link #mTestNgSuiteProperties} with the same name
     *
     * <b>Note! This only overrides parameters at Suite level</b>
     *
     * @param suite
     */
    public void overrideTestNgSuiteParameters(ISuite suite) {
        mLogger.debug("Injecting suite params from system params");
        Map<String, String> suiteParametersFromSystem = getTestNgSuiteProperties();
        if (null != suiteParametersFromSystem) {
            Map<String, String> suiteXmlProperties = suite.getXmlSuite().getParameters();
            for (Map.Entry<String, String> entry : suiteParametersFromSystem.entrySet()) {
                mLogger.debug("Parameter " + entry.getKey() + " was written " + suiteXmlProperties.get(entry.getKey())
                        + " -> " + entry.getValue());
                suiteXmlProperties.put(entry.getKey(), entry.getValue());
            }
            suite.getXmlSuite().setParameters(suiteXmlProperties);
        }
        if (null != mSuiteName) {
            mLogger.info("Overriding suite name to:" + mSuiteName);
            suite.getXmlSuite().setName(mSuiteName);
        }
    }

    /**
     * Parse system properties by scanning methods of this class annotated with @Inject and @Named
     *
     * @param properties
     */
    public void parseProperties(Properties properties) {
        mInputProperties = properties;
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(EcsTestProperty.class)) {
                EcsTestProperty annotation = method.getAnnotation(EcsTestProperty.class);
                String propertyName = annotation.value();
                String propertyValue = properties.getProperty(propertyName);
                mLogger.debug("Found property value:" + propertyValue + " for name:" + propertyName);
                if (propertyValue != null && !(propertyValue.isEmpty())) {
                    checkValueWithRegexp(propertyValue, annotation);
                    checkValueAgainstValidValues(propertyValue, annotation);
                    try {
                        method.invoke(this, propertyValue);
                    } catch (Exception e) {
                        mLogger.error(genPropertyErrorMessage(annotation, propertyValue), e);
                        Assert.fail(e.toString());
                    }
                } else {
                    // Throw alert only if property was mandatory and not provided
                    if (!annotation.optional()) {
                        Assert.fail("JVM Property:" + propertyName + " is mandatory !");
                    }
                }
            }
        }
        if (null == mOpenStackZoneName) {
            if (null != mTrafficXml) {
                Assert.fail("JVM property ecs.openstack.zone is missing when property trafficxml is given!");
            }
            if (mHealthCheckEnabled) {
                Assert.fail("JVM property ecs.openstack.zone is missing when property ecs.healthcheck is given!");
            }
        }
    }

    /**
     * To be called after onExecutionStart to remove the {@link DbFrameworkStatsWriterRIPNG} from the JCAT logwriters
     * list.
     */
    public void removeDbLogWriter() {
        // @TODO Disable this one when running offline mode.
        // if (mLoggingMode.contains(LoggingType.OFFLINEHTML)) {
        // String[] dbStats = new String[] { DbFrameworkStatsWriterRIPNG.class.getName() };
        // LogWriterControl.removeLogWriters(dbStats);
        // }
    }

    /**
     * Set Atlas BAT Traffic mode {@link #mAtlasBatTraffic}
     *
     * @param trafficMode
     */
    @EcsTestProperty(value = AtlasBatTraffic.PARAMETER_NAME, optional = true, errorMessage = "Invalid Atlas Bat Traffic mode specified!", validValues = {
            "on", "off" })
    public void setAtlasBatTraffic(String trafficMode) {
        if (trafficMode.equalsIgnoreCase(AtlasBatTraffic.ON.toString())) {
            mLogger.info("Atlas Bat Traffic mode is ON.");
            mAtlasBatTraffic = AtlasBatTraffic.ON;
        }
        // otherwise Atlas BAT Traffic is always OFF
    }

    /**
     * Set force cleanup {@link #mForceCleanup}
     */
    @EcsTestProperty(value = "ecs.forceCleanup", optional = true, errorMessage = "Invalid force cleanup boolean provided!", validValues = {
            "true", "false" })
    public void setForceCleanUpSwitch(String forceCleanupSwitch) {
        mForceCleanup = Boolean.parseBoolean(forceCleanupSwitch);
    }

    /**
     * Set optional property {@link #mHealthCheckConfigurationFilePath}
     */
    @EcsTestProperty(value = "ecs.healthCheckConfigFile", optional = true, regexp = ".*.xml", errorMessage = "Invalid health check configuration file path provided!")
    public void setHealthCheckConfigurationFilePath(String path) {
        mHealthCheckConfigurationFilePath = path;
    }

    /**
     * Set health check switch
     *
     * @param healthCheckSwitch
     * @throws Exception
     */
    @EcsTestProperty(value = "ecs.healthcheck", optional = true, errorMessage = "Invalid health check boolean provided!", validValues = {
            "true", "false" })
    // maybe we should change the name "errorMessage" to "invocationErrorMsg"
    public void setHealthCheckSwitch(String healthCheckSwitch) {
        mHealthCheckEnabled = Boolean.parseBoolean(healthCheckSwitch);
    }

    /**
     * Set logdb.properties file path
     *
     * @param path
     */
    @EcsTestProperty(value = TestInfo.DB_PROPERTIES, optional = true, errorMessage = "Invalid logdb.properties value specified!")
    public void setLogDbFilePath(String path) {
        mLogDbFilePath = path;
    }

    /**
     * Set optional property {@link #mLoggingLevel}
     *
     * @param loggingLevel
     */
    @EcsTestProperty(value = LoggingLevel.PARAMETER_NAME, optional = true, errorMessage = "Invalid logging level specified!", validValues = {
            "debug", "ci" })
    public void setLoggingLevel(String loggingLevel) {
        if (loggingLevel != null) {
            mLoggingLevel = LoggingLevel.valueOf(loggingLevel.trim().toUpperCase());
        }
    }

    /**
     * Set optional property {@link #mLoggingMode}
     *
     * @param logging {@see LoggingType}
     */
    @EcsTestProperty(value = LoggingType.PARAMETER_NAME, optional = true, errorMessage = "Invalid logging type(s) specified!", validValues = {
            "catlog", "jcathtml", "offlinehtml", "none", "excel" })
    public void setLoggingMode(String logging) {
        if (null != logging && !logging.isEmpty()) {
            for (String loggingType : Splitter.on(",").split(logging)) {
                mLoggingMode.add(LoggingType.valueOf(loggingType.trim().toUpperCase()));
            }
        }
    }

    /**
     * Set node Name {@link #mNodeName}
     *
     * @param nodeName
     */
    @EcsTestProperty(value = "node", optional = false, errorMessage = "Invalid node specified!")
    public void setNode(String nodeName) {
        mNodeName = nodeName.toUpperCase();
    }

    /*
     * Setter for the injection options
     * @param mOptions
     */
    public void setOptions(String options) {
        mOptions = options;
    }

    /**
     * Configure the jClouds service endpoint url handling, check {@link OsServiceUrlMode} for more info
     *
     * @param serviceUrlsHandlingMode - mode option in String format
     */
    @EcsTestProperty(value = "ecs.serviceurls", optional = true, errorMessage = "Invalid value provided on jclouds.serviceurls property!", validValues = {
            "internal", "public", "admin", "lxc", "lxc_redundancy" })
    public void setOsServiceUrlHandlingMode(String serviceUrlsHandlingMode) {
        mOsServiceUrlHandlingMode = OsServiceUrlMode.valueOf(serviceUrlsHandlingMode.toUpperCase());
    }

    /**
     * Set {@link #ResourceAdapter}
     *
     * @param String value of {@link ResourceAdapter}
     */
    @EcsTestProperty(value = ResourceAdapter.PARAMETER_NAME, optional = true, errorMessage = "Invalid resource adapter type specified!", validValues = {
            "tass", "file_xml" })
    public void setResourceAdapter(String resourceAdapter) {
        if (null != resourceAdapter && !resourceAdapter.isEmpty()) {
            mResourceAdapter = ResourceAdapter.valueOf(resourceAdapter.trim().toUpperCase());
        }
    }

    /**
     * Set {@link #mResourcePath}
     *
     * @param String value of {@link #mResourcePath}
     */
    @EcsTestProperty(value = "ecs.resourcePath", optional = true, regexp = ".*.xml", errorMessage = "Invalid resource path specified!")
    public void setResourcePath(String resourcePath) {
        mResourcePath = resourcePath;
    }

    /**
     * Set {@link #mSuiteName}
     *
     * @param userName
     */
    @EcsTestProperty(value = "ecs.suiteName", optional = true, errorMessage = "Invalid suite name specified!")
    public void setSuiteName(String suiteName) {
        mSuiteName = suiteName;
    }

    /**
     * Set whether TagLogger is enabled
     * @param tagLoggerEnabled
     */
    @EcsTestProperty(value = "ecs.tagLoggerEnabled", optional = true, errorMessage = "Invalid boolean parameter provided!", validValues = {
            "true", "false" })
    public void setTagLoggerEnabled(String tagLoggerEnabled) {
        mTagLoggerEnabled = Boolean.parseBoolean(tagLoggerEnabled);
    }

    /**
     * Set TASS booking mode {@link #mTassBooking}
     *
     * @param bookingMode
     */
    @EcsTestProperty(value = TassBooking.PARAMETER_NAME, optional = true, errorMessage = "Invalid TASS booking mode specified!", validValues = {
            "on", "off" })
    public void setTassBookingMode(String bookingMode) {
        if (bookingMode.equalsIgnoreCase(TassBooking.OFF.toString())) {
            mLogger.info("Tass booking mode is OFF.");
            mTassBooking = TassBooking.OFF;
        }
        // otherwise TASS booking is always ON
    }

    /**
     * Set Traffic Xml file {@link #mTrafficXml}
     *
     * @param trafficXml
     */
    @EcsTestProperty(value = "trafficxml", optional = true, errorMessage = "Invalid traffic xml provided!")
    public void setTrafficXml(String trafficXml) {
        mTrafficXml = trafficXml;
    }

    /**
     * Set user name which user that runs the test, the value is normally set to user.name as default value. If that is
     * ok you don't need to set it.
     *
     * @param userName String
     */
    @EcsTestProperty(value = "user.name", optional = true, errorMessage = "Invalid username")
    public void setUserName(String userName) {
        mUserName = userName;
    }

    /**
     * Set VM booting mode {@link #mVmBootingMode}
     *
     * @param vmBootingMode
     */
    @EcsTestProperty(value = VmBootMode.PARAMETER_NAME, optional = true, errorMessage = "Invalid Server booting mode specified", validValues = {
            "image", "volume" })
    public void setVmBootMode(String vmBootingMode) {
        if (Enum.valueOf(VmBootMode.class, vmBootingMode.toUpperCase()).equals(VmBootMode.VOLUME)) {
            mLogger.info("Vm booting mode is set to: " + VmBootMode.VOLUME.toString());
            mVmBootingMode = VmBootMode.VOLUME;
        } else {
            mLogger.info("Vm booting mode is set to: " + VmBootMode.IMAGE.toString());
            mVmBootingMode = VmBootMode.IMAGE;
        }
    }

    /**
     * Sets the vm images to be used during test execution
     * @param vmImages array of images
     */
    public void setVmImages(VmImage[] vmImages) {
        mVmImages = vmImages;
    }

    /**
     * Set OpenStack zone name {@link #mOpenStackZoneName}
     *
     * @param zoneName
     */
    @EcsTestProperty(value = "ecs.openstack.zone", optional = true, errorMessage = "Invalid zone name provided!")
    public void setZoneName(String zoneName) {
        mOpenStackZoneName = zoneName;
    }
}

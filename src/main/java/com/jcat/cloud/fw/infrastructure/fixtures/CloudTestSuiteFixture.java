package com.jcat.cloud.fw.infrastructure.fixtures;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.testng.annotations.Test;

import se.ericsson.jcat.fw.annotations.FixtureDescriptor;
import se.ericsson.jcat.fw.fixture.CommonSuiteFixture;
import se.ericsson.jcat.fw.model.JcatModelHolder;
import se.ericsson.jcat.fw.model.JcatTestCase;
import se.ericsson.jcat.fw.model.JcatTestSuite;

import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

@FixtureDescriptor(priority = 4)
public class CloudTestSuiteFixture extends CommonSuiteFixture {
    private final Logger mLogger = Logger.getLogger("CloudTestSuiteFixture");
    private final Logger mGraduateCandidateLogger = Logger.getLogger("CloudIncubatorGraduateCandidate");

    private static final String TESTCASE_COLUMN = "[Test Case Name]";
    private static final String STARTDATE_COLUMN = "[Start Date]";
    private static final String DAYSLEFT_COLUMN = "[Days left in incubator]";
    private static final String RESULT_COLUMN = "[Result]";
    private static final String FORMAT = "%1$-25s %2$-20s %3$-15s %4$12s ";
    private static final String DESCRIPTION_PATTERN = "\\s*(\\d{4}-\\d{2}-\\d{2})\\s*(TestNgGroups.\\S*)";
    private static final String DESCRIPTION_PATTERN_EXAMPLE = "YYYY-MM-DD TestNgGroups.GROUP_NAME";
    private static final String INCUBATOR_SUITE_NAME = "JCAT Incubator Suite";
    private static final String INCUBATOR_GRADUATE_LOG = "target/test-data/IncubatorGraduateCandidate.log";

    // Number of days that a test case should stay in incubator
    private static final int INCUBATING_PERIOD = 10;

    private int calculateDaysLeftInIncubator(String date) {
        DateTime startDate = DateTime.parse(date);
        DateTime currentDate = DateTime.now();
        int days = Days.daysBetween(startDate, currentDate).getDays();
        return INCUBATING_PERIOD - days;
    }

    /**
     * Local help method which creates log4j file appender for logging incubator graduate candidate info
     */
    private void createFileAppender() {
        PatternLayout layout = new PatternLayout("%m%n");
        FileAppender fileAppender;
        try {
            fileAppender = new FileAppender(layout, INCUBATOR_GRADUATE_LOG, false);
            mGraduateCandidateLogger.addAppender(fileAppender);
        } catch (IOException e) {
            mLogger.warn("Unable to create appender for logging gradute candidate.");
        }

    }

    private void printInfoForIncubator() {
        createFileAppender();
        mLogger.info(String.format(FORMAT, TESTCASE_COLUMN, STARTDATE_COLUMN, RESULT_COLUMN, DAYSLEFT_COLUMN));
        List<JcatTestCase> testCaseList = JcatModelHolder.getAllRunningTestCase();
        mLogger.debug(String.format("Current suite has %s running test case.", testCaseList.size()));
        StringBuilder warn = new StringBuilder();
        for (JcatTestCase testCase : testCaseList) {
            Class<?> testClass = testCase.getRealClass();
            if (testClass.isAnnotationPresent(Test.class)) {
                String description = testClass.getAnnotation(Test.class).description();
                Pattern p = Pattern.compile(DESCRIPTION_PATTERN);
                Matcher m = p.matcher(description);
                if (!m.matches()) {
                    warn.append(String
                            .format("Can not find valid start date information for test case %s, please add it in @Test(%s) \n",
                                    testClass.getName(), DESCRIPTION_PATTERN_EXAMPLE));
                    continue;
                }
                // found correct description
                String startDate = m.group(1);
                String targetGroup = m.group(2).trim();
                int daysLeft = calculateDaysLeftInIncubator(startDate);
                mLogger.info(String.format(FORMAT, testClass.getSimpleName(), startDate, testCase.getTestResult(),
                        daysLeft));
                if (daysLeft <= 0) {
                    // test case is ready to graduate, add full class name and target TestNG group info to log file
                    mLogger.debug(testClass.getName() + " is ready to graduate from incubator.");
                    mGraduateCandidateLogger.info(testClass.getName() + " " + targetGroup);
                }
            }
        }
        if (warn.length() != 0) {
            mLogger.warn(warn.toString());
        }
    }

    @Override
    public void onExecutionStart() {
        TestConfiguration mConfiguration = TestConfiguration.getTestConfiguration();
        mConfiguration.configureLog4j2Configuration();
        // Must call the following method to override JCAT system properties
        mConfiguration.configureJcatProperties();
        super.onExecutionStart();
        // Must remove DB logwriter here (after super onExecutionStart()) for OFFLINEHTML mode.
        mConfiguration.removeDbLogWriter();
    }

    @Override
    public void onSuiteFinish(JcatTestSuite suite) {
        super.onSuiteFinish(suite);
        // Only print following info in the Incubator Job
        if (suite.getSuiteName().equals(INCUBATOR_SUITE_NAME)) {
            mLogger.info(String.format("Current Incubator has %s Test cases ", suite.getTestCaseNumber()));
            printInfoForIncubator();
        }
    }
}

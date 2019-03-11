package com.jcat.cloud.fw.infrastructure.fixtures;

import static se.ericsson.jcat.fw.fixture.JcatFixtureHelper.calcExecutionTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import se.ericsson.jcat.fw.annotations.FixtureDescriptor;
import se.ericsson.jcat.fw.fixture.JcatTestCaseFixture;
import se.ericsson.jcat.fw.model.JcatMethodType;
import se.ericsson.jcat.fw.model.JcatModelHolder;
import se.ericsson.jcat.fw.model.JcatTestCase;

/**
 * JCAT Cloud specific TestNG test-case fixture. This fixture adds some pretty-printing between test methods.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-07-07 Initial version
 * @author eqinann ezhgyin 2015-09-21 Added missing functions, fixed format
 */
@FixtureDescriptor(priority = 1)
public class CloudTestCaseFixture implements JcatTestCaseFixture {

    private final Logger mLogger = Logger.getLogger("CloudTestCaseFixture");
    private final Logger mNonCILogger = Logger.getLogger("CloudTestCaseFixtureNonCI");

    private static final DateFormat df = new SimpleDateFormat("m 'mins,' s 'seconds,' S 'milliseconds'");

    /**
     * Helper method returning test class and method name stripping down useless info
     * @param method
     * @return
     */
    private String getTestName(JcatTestCase test) {
        String classLongName = test.getRealClass().getName();
        return classLongName.substring(classLongName.lastIndexOf('.') + 1) + "." + test.getTestCaseName();
    }

    /**
     * Helper method returning formatted string of test method parameter names.
     *
     * @param paramNames
     * @return
     */
    private String printParameters(Object[] paramNames) {
        StringBuilder result = new StringBuilder();
        if (paramNames.length > 0) {
            result.append("[");
            for (int i = 0; i < paramNames.length; i++) {
                result.append(" ").append(paramNames[i]);
            }
            result.append("]");
        }
        return result.toString();
    }

    @Override
    public void afterMethod(JcatTestCase test) {
        calcExecutionTime(test);
        if (JcatMethodType.TestCase == test.getMethodType()) {
            long executionTimeInMilliseconds = test.getEndTime() - test.getStartTime();
            mLogger.info(String.format("[%s] %s %s", test.getTestResult(), getTestName(test),
                    df.format(new Date(executionTimeInMilliseconds))));
            JcatModelHolder.addRunningTestCase(test);
        }
    }

    /**
     * Executes before the test method has been run.
     * {@inheritDoc}
     */
    @Override
    public void beforeMethod(JcatTestCase test) {
        mNonCILogger.info("[TestCase] ====== " + getTestName(test) + "("
                + printParameters(test.getParameterList().toArray()) + ") ======");
    }
}

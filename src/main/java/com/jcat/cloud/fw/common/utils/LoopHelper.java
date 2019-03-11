package com.jcat.cloud.fw.common.utils;

import java.util.concurrent.TimeUnit;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.fwservices.monitoring.tag.TagLogger;

/**
 * Helper class for making loops with periodic checks.
 *
 * LoopHelper is designed for check based loop logics. The exit condition will
 * be checked within a period between delays.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @param <T> state type
 *
 * @author esauali 2012-01-30 Initial version
 * @author esauali 2013-02-25 Add ICheckHandle to be able to pass handling of
 *         timeout to the test case
 * @author esauali 2013-03-20 Change failure logging to use
 *         JcatNGTestBase.JcatAssertApi.assertTrue() until JcatNGTestBase.fail() is fixed in
 *         JCAT
 * @author ehosmol 2013-04-16 Add getter methods for the member variables
 * @author emulign 2013-10-17 Changed JcatNGTestBase asserts for TestNG due to
 *         unexpected exceptions
 * @author ezhgyin 2014-05-06 Add handling of LoopInterruptedExcetion
 * @author eqinann 2014-08-19 Upgraded to new ENUM based time out
 * @author ethssce 2015-01-27 Added LoopTimeoutException and changes in run()
 *         (removal of asserts, ICheckHandle in timeout block)
 * @author ehosmol 2015-03-18 Introduced new structure
 * @auther ehkiyki 2015-07-21 Introduced the TagLogger and added the
 *          TestConfiguration
 */
public class LoopHelper<T> {

    /**
     * Interface to be implemented when using @link {@link LoopHelper}. Will
     * Fail a test case in case of timeout
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2013
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @param <T> state type
     *
     * @author ehosmol 2015-01-30 Initial version
     */
    public interface ICheck<T> {

        /**
         * Method which will be periodically called by {@link LoopHelper} code.
         *
         * @return Must return TRUE (will stop the loop) if check was
         *         successful, return FALSE otherwise.
         * @throws LoopInterruptedException
         *             - exception to be thrown when the loop should be stopped
         *             since certain condition prevents the check to be
         *             successful
         */

        public T getCurrentState();

    }

    public static class LoopInterruptedException extends RuntimeException {
        /**
         * Generated serial version UID
         */
        private static final long serialVersionUID = -6674122137959309313L;

        protected LoopInterruptedException(String skipMessage, Throwable cause) {
            super(skipMessage, cause);
        }

        public LoopInterruptedException(String skipMessage) {
            super(skipMessage);
        }
    }

    public static class LoopTimeoutException extends RuntimeException {

        /**
         * Generated serial version UI
         */
        private static final long serialVersionUID = -3151088008940740487L;

        protected LoopTimeoutException(String skipMessage, Throwable cause) {
            super(skipMessage, cause);
        }

        public LoopTimeoutException(String skipMessage) {
            super(skipMessage);
        }
    }

    /**
     * Object performing checks
     */
    private ICheck<?> mCheckable;

    /**
     * <T> state type
     * error state
     */
    private T mErrorState;

    /**
     * TagLogger used to store the timeout tag in the database on execution
     */
    private TagLogger mTagLogger = TagLogger.getInstance();

    /**
     * <T> state type
     * expected state
     */
    private T mExpectedState;

    /**
     * Sleep delay between loop iterations
     */
    private int mIterationDelaySeconds = CommonParametersValues.ITERATION_DELAY;

    private final EcsLogger mLogger = EcsLogger.getLogger(LoopHelper.class);

    /**
     * Timeout Enum
     */
    private Timeout mTimeOutEnum;

    /**
     * Message to be logged if timeout was reached, {@link #mTimeOutEnum} and {@link #mIterationDelaySeconds} values
     * will be added automatically
     */
    private String mTimeoutMessage;

    /**
     * Multiplier for scalable operations
     */
    private int mTimeoutMultiplier = 1;

    private int mLoopCounter = 0;

    /**
     * constructor with timeout multiplier
     * Used when looping for a generic scalable operation
     *
     * @param timeout
     *            - {@link #mTimeOutEnum}
     * @param timeoutMultiplier
     *            - {@link #mTimeoutMultiplier
     * @param timeoutMessage
     *            - {@link #mTimeoutMessage}
     * @param expected state
     * @param checkable
     *            - {@link LoopHelper.ICheck} or {@link LoopHelper.ICheckHandle} implementations
     */
    public LoopHelper(Timeout timeout, int timeoutMultiplier, String timeoutMessage, T expectedState,
            ICheck<?> checkable) {
        this(timeout, timeoutMessage, expectedState, checkable);
        mTimeoutMultiplier = timeoutMultiplier;
    }

    /**
     * constructor with default iteration
     *
     * @param timeout
     *            - {@link #mTimeOutEnum}
     * @param timeoutMessage
     *            - {@link #mTimeoutMessage}
     * @param expected state
     * @param checkable
     *            - {@link LoopHelper.ICheck} or {@link LoopHelper.ICheckHandle} implementations
     */
    public LoopHelper(Timeout timeout, String timeoutMessage, T expectedState, ICheck<?> checkable) {
        mCheckable = checkable;
        mTimeOutEnum = timeout;
        mTimeoutMessage = timeoutMessage;
        mExpectedState = expectedState;
    }

    /**
     * Getter method for member variable {@link #mCheckable}
     *
     * @return {@link ICheck}
     */
    protected ICheck<?> getCheckable() {
        return mCheckable;
    }

    /**
     * Getter method for member variable {@link #mErrorState}
     * @return {@link #mErrorState}
     */
    protected T getErrorState() {
        return mErrorState;
    }

    /**
     * Getter method for member variable {@link #mIterationDelaySeconds}
     *
     * @return {@link #mIterationDelaySeconds}
     */
    protected int getIterationDelaySeconds() {
        return mIterationDelaySeconds;
    }

    /**
     * Getter method for member variable {@link #mTimeoutMessage}
     *
     * @return {@link #mTimeoutMessage}
     */
    protected String getTimeoutMessage() {
        return mTimeoutMessage;
    }

    /**
     * Getter method for member variable {@link #mTimeOutEnum}
     *
     * @return {@link #mTimeOutEnum}
     */
    protected int getTimeOutSeconds() {
        return mTimeOutEnum.getTimeoutInSeconds() * mTimeoutMultiplier;
    }

    /**
     * method for unit test
     * @param logger
     */
    protected void setTagLogger(TagLogger logger) {
        mTagLogger = logger;
    }

    /**
     * Main method which starts the loop
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public void run() {
        final long timeStarted = System.currentTimeMillis();
        long timeFinished = 0;
        T currentState = null;
        while (((timeFinished = System.currentTimeMillis()) - timeStarted) < (TimeUnit.SECONDS.toMillis(mTimeOutEnum
                .getTimeoutInSeconds() * mTimeoutMultiplier))) {
            mLoopCounter++;
            currentState = (T) mCheckable.getCurrentState();
            if (mExpectedState.equals(currentState)) {
                mLogger.info(Verdict.LOOPED, currentState.toString(), mExpectedState.getClass().getSimpleName(),
                        ((System.currentTimeMillis() - timeStarted) / 1000) + " seconds");
                mTagLogger.tag(mTimeOutEnum, Result.SUCCESSFUL, timeStarted, timeFinished);
                return;
            }
            if (mErrorState != null) {
                if (mErrorState.equals(currentState)) {
                    String errorMessage = "Loop interrupted: " + mTimeoutMessage + " But status: "
                            + mErrorState.toString() + ". LoopHelper checked for "
                            + ((System.currentTimeMillis() - timeStarted) / 1000) + " seconds with "
                            + mIterationDelaySeconds + " second delays. ";
                    mTagLogger.tag(mTimeOutEnum, Result.EXCEPTION, timeStarted, timeFinished);
                    throw new LoopInterruptedException(errorMessage);
                }
            }
            // Delay before next check
            try {
                TimeUnit.SECONDS.sleep(mIterationDelaySeconds);
            } catch (InterruptedException e) {
                mLogger.error(this.getClass().getSimpleName() + " delay sleep was interrupted");
            }
        }
        String errorMessage = mTimeoutMessage + ", was checking for "
                + ((System.currentTimeMillis() - timeStarted) / 1000) + " seconds with " + mIterationDelaySeconds
                + " second delays. Last checking result was : " + currentState;
        mTagLogger.tag(mTimeOutEnum, Result.TIMED_OUT, timeStarted, timeFinished);
        throw new LoopTimeoutException(errorMessage);
    }

    /**
     * Set error state for LoopHelper.
     * If getCurrentState reaches error state,  LoopInterruptedException will be thrown.
     * @param errorState
     */
    public LoopHelper<T> setErrorState(T errorState) {
        mErrorState = errorState;
        return this;
    }

    /**
     * Set a customized iteration delay for LoopHelper
     * @param iterationDelaySeconds
     */
    public LoopHelper<T> setIterationDelay(int iterationDelaySeconds) {
        mIterationDelaySeconds = iterationDelaySeconds;
        return this;
    }
}

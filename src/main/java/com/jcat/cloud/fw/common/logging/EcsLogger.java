package com.jcat.cloud.fw.common.logging;

import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.ericsson.jcat.fw.logging.JcatLoggingApi;

import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;

/**<p>
 * Ecs Logger is a customized logger for Ecs JCAT. It uses log4j2 as it's foundation
 * and adding modeled logging over it to provide a clearer logging output.
 * </p>
 * <b>Copyright:</b> Copyright (c) 2016
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2016-05-09 initial version
 * @author eqinann 2017-10-31 Migrated to log4j 2.6.2, removed the inheritance
 *
 */
public class EcsLogger {
    private Stack<EcsAction> actionStack = new Stack<EcsAction>();
    private Logger mLogger;

    private EcsLogger(@SuppressWarnings("rawtypes") Class clazz) {
        mLogger = LogManager.getLogger(clazz);
    }

    /**
     * Using your own class as parameter to get a EcsLogger based on it
     *
     * @param clazz
     * @return
     */
    public static EcsLogger getLogger(@SuppressWarnings("rawtypes") Class clazz) {
        return new EcsLogger(clazz);
    }

    /**
     * Generate caller method who called EcsLogger
     *
     * @return method name of the caller
     */
    private String getCallerMethod() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int i = 1;
        // Get out of EcsLogger class
        while (stack[i].getClassName().equals(this.getClass().getName())) {
            i++;
        }
        return stack[i].getMethodName();
    }

    /**
     * Generate caller package path who called EcsLogger
     * If the depth given is bigger than the depth of the calling object, it will return the
     * complete class path including class name
     *
     * @param level The depth of package path is needed, starting from root package
     * @return package path down to the depth required, or the complete class path
     */
    private String getCallerPackage(int level) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int i = 1;
        // Get out of EcsLogger class
        while (stack[i].getClassName().equals(this.getClass().getName())) {
            i++;
        }
        String callerCompleteClassPath = stack[i].getClassName();
        String callerPackageSubLevel = "";
        StringTokenizer st = new StringTokenizer(callerCompleteClassPath, ".");
        // Extract exact level that is needed
        while (st.hasMoreTokens() && level > 0) {
            callerPackageSubLevel += st.nextToken();
            callerPackageSubLevel += ".";
            level--;
        }
        // Remove the last dot
        callerPackageSubLevel = callerPackageSubLevel.substring(0, callerPackageSubLevel.length() - 1);
        return callerPackageSubLevel;
    }

    /**
     * TODO: implement pair checking
     *
     * @param aciton
     * @param verdict
     * @return
     */
    private boolean isActionVerdictPair(EcsAction aciton, Verdict verdict) {
        return true;
    }

    public void debug(String msg) {
        mLogger.debug(msg);
    }

    public void error(String msg) {
        mLogger.error(msg);
    }

    public void error(String msg, Throwable e) {
        mLogger.error(msg, e);
    }

    public void error(Throwable e) {
        mLogger.error(e);
    }

    public void fatal(String msg) {
        mLogger.fatal(msg);
    }

    public void info(EcsAction action, Class<? extends EcsComponent> receiver) {
        info(action, receiver, "");
    }

    public void info(EcsAction action, Class<? extends EcsComponent> object, Class<? extends EcsComponent> receiver,
            String text) {
        info(action, object.getSimpleName(), receiver, text);
    }

    public void info(EcsAction action, Class<? extends EcsComponent> receiver, Object supplementaryText) {
        info(action, "", receiver, supplementaryText.toString());
    }

    public void info(EcsAction action, EcsComponent object) {
        info(action, object.getClass(), object.toString());
    }

    public void info(EcsAction action, String object, Class<? extends EcsComponent> receiver, String supplementaryText) {
        info(action, object, receiver.getSimpleName(), supplementaryText);
    }

    /**
     * Base INFO Action method
     *
     * Action is the key to INFO level logging
     *
     * @param action Action Enum(#Hashtag) the log is going to present
     * @param object Explanatory text to describe Action Enum if needed
     * @param receiver Receiver that accepts the Action
     * @param supplementaryText Any extra information contained with the Action
     */
    public void info(EcsAction action, String object, String receiver, String supplementaryText) {
        if (action == EcsAction.CHECKING && !getCallerPackage(4).equals("com.jcat.cloud.tests")) {
            throw new EcsNotImplementedException("EcsAction.CHECKING can ONLY be used in Test Case");
        }
        String printout = String.format("\t[%-15s]\t[%-10s]\t-->\t[%-20s]\t<--\t[%s]", action, object, receiver,
                supplementaryText);
        mLogger.info(printout);
        actionStack.push(action);
    }

    public void info(EcsAction action, Verdict verdict, Class<? extends EcsComponent> receiver, String supplementaryText) {
        info(action, verdict.toString(), receiver, supplementaryText);
    }

    @Deprecated
    public void info(Object message) {
        mLogger.info("\t[Free Text] \t" + message);
    }

    public void info(Verdict verdict, Class<? extends EcsComponent> object, Class<? extends EcsComponent> receiver,
            String payload) {
        info(verdict, object.getSimpleName(), receiver, payload);
    }

    public void info(Verdict verdict, Class<? extends EcsComponent> receiver, Object payload) {
        info(verdict, "", receiver, payload);
    }

    public void info(Verdict verdict, EcsComponent object) {
        info(verdict, object.getClass(), object.toString());
    }

    public void info(Verdict verdict, Object object, Class<? extends EcsComponent> receiver, Object payload) {
        info(verdict, object, receiver.getSimpleName(), payload);
    }

    /**
     * Base INFO Verdict method
     *
     * Each verdict is related to an action that issued before, forming an action-verdict pair
     *
     * Verdict pairs are checked while printing to make sure that each action has correct information.
     *
     * @param verdict The Verdict
     * @param object
     * @param receiver
     * @param payload
     */
    public void info(Verdict verdict, Object object, String receiver, Object payload) {
        // Verified only in test case
        // done only used where method contains cleanup or deinitialize, non-cap
        if (verdict == Verdict.VERIFIED && !getCallerPackage(4).equals("com.jcat.cloud.tests")) {
            throw new EcsNotImplementedException("Verdict.VERIFIED can ONLY be used in Test Case");
        }
        if (verdict == Verdict.DONE && !getCallerPackage(4).equals("com.jcat.cloud.tests")) {
            if (!getCallerMethod().toLowerCase().contains("cleanup")
                    && !getCallerMethod().toLowerCase().contains("deinitialize")) {
                throw new EcsNotImplementedException(
                        "Verdict.DONE usage outside test case can only be in cleanup methods.");
            }
        }

        String printout = String.format("\t[%-15s]\t[%-10s]\t-->\t[%-20s]\t-->\t[%s]", verdict, object, receiver,
                payload);
        mLogger.info(printout);
        if (actionStack.isEmpty()) {
            // super.warn("Verdict has no action pair");
        } else if (!isActionVerdictPair(actionStack.pop(), verdict)) {
            // super.warn("Verdict has no action pair");
        }
    }

    /**
     * Starts a new test step
     *
     * @param testStep
     */
    public void testStep(String testStep) {
        JcatLoggingApi.setTestStepEnd();
        JcatLoggingApi.setTestStepBegin(testStep);
    }

    public void warn(String msg) {
        mLogger.warn(msg);
    }

    public void warn(String msg, Throwable e) {
        mLogger.warn(msg, e);
    }
}

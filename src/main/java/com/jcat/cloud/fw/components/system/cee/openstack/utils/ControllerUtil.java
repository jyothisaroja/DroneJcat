package com.jcat.cloud.fw.components.system.cee.openstack.utils;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstack4j.common.RestService;

import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;

/**
 * This class contains a collection of useful procedures for controllers.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce 2014-11-21 initial version
 * @author eelimei 2015-01-28 Add enum for deletion level of ecs objects.
 * @author ethssce 2015-05-08 Add clone with set parameter
 * @author zdagjyo 2017-09-22 Add method handleCatchAllException
 * @author zdagjyo 2018-01-25 Add method printCollectedLogFileInfo
 */
public class ControllerUtil {

    protected static final String TEMP_FILE_LOCAL_PATH = System.getProperty("user.dir");

    /**
     * enum DeletionLevel to be used to set deletion level of Ecs objects. Objects with this feature available will have this option in their respective Ecs builders. If the option is not used in the builder, TEST_CASE level will be chosen as default.
     */
    public enum DeletionLevel {
        /** Object will be deleted after the current test case **/
        TEST_CASE,

        /** Object will not be cleanup/deleted**/
        PERMANENT;
    }

    /**
     *
     * @param className - String - example: com.jcat.ecs.tests.examples.OpenStack4jMultipleActionsExample
     * @return String - short class name, example: OpenStack4jMultipleActionsExample
     */
    private static String getShortClassName(String className) {
        String substring = "";
        StringTokenizer st = new StringTokenizer(className, ".");
        while (st.hasMoreTokens()) {
            substring = st.nextToken();
        }
        if (substring.startsWith("Ecs")) {
            substring = substring.substring(3, substring.length());
        }
        return substring;
    }

    /**
     * this method is called in createName() and is used to create relevant substrings from a given method name in order
     * to construct a name
     *
     * @param methodName - String
     * @return String - a relevant substring in the methodname for name creation
     */
    private static String getShortMethodName(String methodName) {
        StringBuilder relevantSubstring = new StringBuilder();
        Pattern pattern = Pattern.compile("([a-z]*(([A-Z][a-z]*){1,3}))");
        // Find out if it matches.
        Matcher matcher = pattern.matcher(methodName);
        if (matcher.find()) {
            if (matcher.group(1).startsWith("create")) {
                relevantSubstring.append(matcher.group(2));
            } else {
                relevantSubstring.append(matcher.group(1));
            }
        }
        return relevantSubstring.toString();
    }

    /**
     * Checks if an OpenStack Service is null. Throw readable exception if null
     *
     * @param service The service object needs to be checked
     * @param serviceClass The class the service belongs to
     */
    public static <T extends RestService> T checkRestServiceNotNull(T service, Class<?> serviceClass) {
        if (null == service) {
            throw new EcsOpenStackException("OpenStack Service ["
                    + serviceClass.getName().substring(serviceClass.getName().lastIndexOf('.') + 1) + "] not available",
                    serviceClass);
        }
        return service;
    }

    /**
     *
     * Helper methods to clone a list of strings
     *
     * @param stringList
     * @return clone of the list
     */
    public static List<String> clone(List<String> stringList) {
        List<String> cloneList = new ArrayList<String>();
        cloneList.addAll(stringList);
        return cloneList;
    }

    /**
     *
     * Helper methods to clone a list of strings
     *
     * @param stringSet
     * @return clone of the list
     */
    public static List<String> clone(Set<String> stringSet) {
        List<String> cloneList = new ArrayList<String>();
        cloneList.addAll(stringSet);
        return cloneList;
    }

    /**
     * This method creates a name containing a timestamp and substrings of the methods it has been called from.
     * It shall be used in the EcsObject builder classes or createMethods in the Controllers, otherwise "good" naming
     * cannot be guaranteed
     *
     * @return String - a name containing a timestamp
     */
    public static String createName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // get short test class name and use as prefix
        StringBuilder createdName = new StringBuilder(getShortClassName(stack[3].getClassName()));
        createdName.append('-');
        // get EcsObject name (port, server) from either method or class
        String callingMethod = stack[2].getMethodName();
        if (callingMethod.contains("builder")) {
            createdName.append(getShortClassName(stack[2].getClassName()));
        } else {
            createdName.append(getShortMethodName(stack[2].getMethodName()));
        }
        createdName.append('-');
        createdName.append(createTimeStamp());
        return createdName.toString();
    }

    /**
     * creates a timestamp of format "MMdd_HH_mm_ss_S"
     * (MM=month, dd=day, HH=hours, mm=minutes, ss=seconds, S=milliseconds)
     *
     * @return - String - a timestamp including date and time
     */
    public static String createTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd_HH_mm_ss_S");
        return sdf.format(calendar.getTime());
    }

    /**
     * Method that displays the exact exception type thrown when
     * an Exception object is caught.
     *
     * @param logger - EcsLogger object to log the error message
     * @param exception - Exception object that is caught
     */
    public static void handleCatchAllException(EcsLogger logger, Exception exception) {
        logger.error(exception.getClass().getSimpleName()
                + " has been thrown, please contact jcat team with this piece of information to add"
                + " this exception into catch list");
    }

    /**
     * Method that displays where the specified core dumps log file is stored in local file system.
     *
     * @param logger - EcsLogger object to log the message
     * @param logFile - The name of the log file to be stored
     * @param filePath - The location where the log file is stored
     */
    public static void printCollectedCoreDumpsInfo(EcsLogger logger, String logFile, String filePath) {
        String ipAddress;
        String hostname;
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ipAddress = socket.getLocalAddress().getHostAddress();
            hostname = socket.getLocalAddress().getHostName();
            logger.error("Core dump [ " + logFile + " ] is found and it's collected to [ " + hostname + " : "
                    + ipAddress + " ] under [ " + filePath + " ]");
        } catch (SocketException | UnknownHostException ex) {
            logger.error("The Core dump file [ " + logFile + " ] is collected to [ " + filePath + " ]");
        }
    }

    /**
     * Method that displays where the specified log file is stored in local file system.
     *
     * @param logger - EcsLogger object to log the message
     * @param logFile - The name of the log file to be stored
     */
    public static void printCollectedLogFileInfo(EcsLogger logger, String logFile) {
        String ipAddress;
        String hostname;
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ipAddress = socket.getLocalAddress().getHostAddress();
            hostname = socket.getLocalAddress().getHostName();
            logger.error("The log file [ " + logFile + " ] is collected to [ " + hostname + " : " + ipAddress
                    + " ] under [ " + TEMP_FILE_LOCAL_PATH + " ]");
        } catch (SocketException | UnknownHostException ex) {
            logger.error("The log file [ " + logFile + " ] is collected to [ " + TEMP_FILE_LOCAL_PATH + " ]");
        }
    }
}

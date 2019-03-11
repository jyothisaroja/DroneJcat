package com.jcat.cloud.fw.components.model.target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.python.google.common.primitives.Ints;

import com.ecs.artifactory.ArtifactoryClient;
import com.ecs.artifactory.ArtifactoryClientConfiguration;
import com.ericsson.commonlibrary.remotecli.Cli;
import com.ericsson.commonlibrary.remotecli.CliFactory;
import com.ericsson.commonlibrary.remotecli.Scp;
import com.ericsson.commonlibrary.remotecli.exceptions.ReadTimeoutException;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.EcsSessionException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVm;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.target.ConfigYamlParser;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsLxc;

/**
 * This class contains available functionality to a Operating System
 * running in the ECS.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2014-12-08 initial version
 * @author ethssce 2015-01-22 added isPingSuccessful() and skeleton for ping()
 * @author eedsla 2015-01-29 added getProcessesSupervisedByUpstart, getProcessId, waitForProcess,
 *         implemented killProcess
 * @author eedann 2015-02-04 added createFile, readFile, deleteFile,
 *         makeDir, isApplicationRunning
 * @author epergat 2015-03-02 add kernelPanic() and getSystemUptime()
 * @author ezhgyin 2015-03-03 add EcsSystemUptime
 * @author epergat 2015-03-14 Add ping() method
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author eedann 2015-04-07 Added method sendCommand
 * @author ehosmol 2015-04-20 added {@link #getSystemUsers()} {@link #getUser()}
 * @author eelimei 2015-05-04 Removed workaround for regex prompt matching and added functionality to set the next
 *         possible password that the session will be prompted for.
 * @author eedsla 2015-05-05 updated getProcessID, added getProcessIdOfService, isServiceRunning, verifyAtdIsRunnung,
 *         waitForSshConnection, startService and stopService methods
 * @author eedann 2015-05-28 updated createFile,createDirectory and readFile methods
 * @author ethssce 2015-06-11 fixed changeUser(), removed asserts from create and deleteDirectory, changed info logger
 *         to debug, fixed file and directory methods
 * @author eelimei 2015-06-12 Remove copyFile, it is never used and does not work for all target implementations
 * @author ethssce 2015-06-22 fixed getSystemUptime()
 * @author zdagjyo 2016-12-14 Add method renameFile
 * @author zdagjyo 2017-01-05 modified createFile method to add loggers
 * @author zdagjyo 2017-02-17 Add methods getDate, getNtpOffset and moveTimeAheadByMinutes
 * @author zdagjyo 2017-02-23 Add method to get brigde management IP of CIC/Compute
 * @author zpralak 2017-03-03 Add method copyFile()
 * @author zdagjyo 2017-03-06 modified deleteFile method to add loggers
 * @author zdagjyo 2017-03-08 Add method getFileChecksum
 * @author zpralak 2017-03-23 Add method dmesg()
 * @author zdagjyo 2017-04-20 modified method createFile
 * @author zdagjyo 2017-05-19 modified methods isServiceRunning, extractPid and getServicesSupervisedByUpstart
 * @author zpralak 2017-06-06 Add methods killProcessByServiceName, waitAndVerifyProcessKill and Modify method killProcess
 * @author zpralak 2017-09-14 Add methods downloadFileFromArtifactortyToLocal and putRemoteFile
 * @author zdagjyo 2017-09-15 moved method configYaml from EcsCic to EcsTarget, added methods changePermissions,
 *         getFileSize, getOpenstackAdminUser, getOpenstackBackupAdminUser, getOpenstackUsers and moveFile.
 * @author zdagjyo 2017-11-03 Added overloaded method listFiles
 * @author zdagjyo 2017-11-29 Added methods compressFile, editFile, getFileFromLxc, runCheck, transferFileToLxc,
 *         modified EcsTarget to implement interface LxcConnectivity and added abstract method startStabilityCollection
 * @author zdagjyo 2017-12-06 Added overloaded method createDirectory
 * @author zdagjyo 2018-01-05 Added abstract method startScalabilityCollection
 * @author zdagjyo 2018-01-09 Added method duplicateFile
 * @author zdagjyo 2018-01-29 Added method getAdminIp
 * @author zmousar 2018-01-03 Added methods doesLogTimeIsAfterInitialTime, isLogTimeAfterInitialTime and overloaded method doesFileExist
 */

public abstract class EcsTarget extends EcsComponent implements LxcConnectivity {
    protected enum ServiceAction {
        START("start"), STOP("stop"), STATUS("status"), RESTART("restart"), RELOAD("reload"), FORCE_RELOAD(
                "force-reload");
        private String mService;

        ServiceAction(String service) {
            mService = service;
        }

        @Override
        public String toString() {
            return mService;
        }
    }

    /**
     * Class represents the system uptime
     */
    public class EcsSystemUptime implements Comparable<EcsSystemUptime> {
        private final int mDays;
        private final int mHours;
        private final int mMinutes;

        private EcsSystemUptime(int days, int hours, int minutes) {
            mDays = days;
            mHours = hours;
            mMinutes = minutes;
        }

        /*
         * {@InheritDoc}
         */
        @Override
        public int compareTo(EcsSystemUptime previous) {
            int result = -1;
            if (this.mDays > previous.mDays) {
                result = 1;
            } else if (this.mDays == previous.mDays) {
                if (this.mHours > previous.mHours) {
                    result = 1;
                } else if (this.mHours == previous.mHours) {
                    if (this.mMinutes > previous.mMinutes) {
                        result = 1;
                    } else if (this.mMinutes == previous.mMinutes) {
                        result = 0;
                    }
                }
            }
            return result;
        }

        /**
         * This method tells whether current uptime is less than the given system uptime
         *
         * @param previous - the previous system uptime which we will compare with
         * @return boolean - true if the current system uptime is less than the given one, otherwise return false
         */
        public boolean lessThan(EcsSystemUptime previous) {
            return (this.compareTo(previous) < 0);
        }

        @Override
        public String toString() {
            return mDays + " days, " + mHours + " hours, " + mMinutes + " minutes.";
        }
    }

    public enum FileSystemType {
        EXT2("ext2"), EXT3("ext3"), EXT4("ext4");

        private String mName;

        FileSystemType(String name) {
            mName = name;
        }

        public String type() {
            return mName;
        }
    }

    private static final String ADMIN_IP_CMD = "ip r|grep 'br-fw-admin'|grep proto|awk '{print$9}'";
    private static final String ATD_ERROR_MSG = "No atd running";
    private static final String ATD_FILE = "abc.txt";
    private static final String BRIDGE_IP_CMD = "ip r|grep 'br-mgmt'|grep proto|awk '{print$9}'";
    private static final String CD_TO_UPSTART_CONFIG_DIR = "cd /etc/init";
    private static final String COMPRESS_FILE_CMD = "tar -czvf %s %s";
    private static final String EDIT_FILE_CMD = "sed -i 's/%s/%s/g' %s";
    private static final String UPSTART_CONFIG_PATH = "/etc/init";
    private static final String CHECKSUM_CMD = "md5sum %s |awk '{print $1}'";
    private static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy";
    private static final String FILE_OR_DIR_NOT_FOUND = "No such file or directory";
    private static final String GREP_FILES_WITH_UPSTART_CONFIG = "grep -l '^respawn' *.conf";
    private static final String HARDKILL_CMD = "sudo kill -9 %s";
    private static final String HARDKILL_BY_SERVICE_NAME_CMD = "sudo pkill -9 %s";
    private static final String KILL_CMD = "sudo kill %s";
    private static final String LS_PROC_FILES = "ls /proc/%s";
    private static final String PS_CMD_CONSOLE_TTY = "ps --forest --no-header -C getty | grep %s | cut -d 't' -f1";
    private static final String PS_CMD_DEFAULT = "ps --forest --no-headers -C %s -o pid | head -1 ";
    private static final String REBOOT_CMD = "reboot &";
    protected static final String FILE_SIZE_CMD = "ls -l %s | awk '{print $5}'";
    protected static final String SERVICE_CMD = "sudo service %s %s";
    private static final String VERIFY_ATD_CMD = "touch abc.txt ; at -f abc.txt now +10 minutes";
    protected static final String JCAT_USER_PREFIX = "jcat_";
    protected static final String ROOT_USERNAME = "root";
    private static final String SUDO_MKDIR_CMD_FORMAT = "sudo mkdir -p %s";
    private static final String MKDIR_CMD_FORMAT = "mkdir -p %s";
    private static final String CMD_FDISK_FORMAT = "(echo n; echo p; echo 1; echo \"\"; echo \"\"; echo w) | fdisk %s %s";
    private static final String CMD_MKFS_FORMAT = "mkfs -t %s %s %s";
    private static final String CMD_CD_FORMAT = "cd %s";
    private static final String CMD_TO_COPY_FILE = "cp %s %s";
    private static final String CMD_MOUNT_FORMAT = "mount %s %s";
    private static final String CMD_UMOUNT_FORMAT = "umount %s";
    private static final String CMD_DF_FORMAT = "df -h";
    private static final String CMD_TOUCH_FORMAT = "touch %s";
    private static final String OFFSET_CMD = "ntpq -pn|sed -n 3p|awk '{print$9}'";
    protected static final String TEMP_FILE_LOCAL_PATH = System.getProperty("user.dir");
    protected static final String UNSUPPORTED_SERVICE_STATUS = "Unknown status - status not supported";
    private static ConfigYamlParser mConfigYamlParser;
    private final static String CONFIG_YAML_FILE_NAME = "config.yaml";
    private final static String CONFIG_YAML_REMOTE_PATH = "/mnt/cee_config/";
    private static final String CMD_USED_DISK_SPACE = "df %s|sed -n 2p|awk '{print $5}'";
    private static final String CMD_FREE_DISK_SPACE = "df %s |sed -n 2p|awk '{print $4}'";
    private static final String CMD_CREATE_SPARSE_FILE = "dd if=/dev/zero of=%s bs=1 count=0 seek=%sG";
    private static final String CMD_VERIFY_ALLOCATED_SPACE_SPARSE_FILE = "du -sh %s|awk '{print $1}'";
    private static final String CMD_VERIFY_ACTUAL_SPACE_SPARSE_FILE = "du -sh --apparent-size %s|awk '{print $1}'";
    private String mInitialUser = "root";

    private boolean mIsNestedUserSession = false;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsTarget.class);
    protected EcsSession mSshSession;
    private boolean mTargetWentOffline;
    @Inject
    private EcsFuel mFuel;
    @Inject
    protected EcsLxc mLxc;

    /**
     * Parse EcsSystemUptime from uptime in sec
     *
     * @param seconds
     * @return
     */
    private EcsSystemUptime calculateTime(long seconds) {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(day);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.DAYS.toMinutes(day)
                - TimeUnit.HOURS.toMinutes(hours);
        return new EcsSystemUptime(day, Ints.checkedCast(hours), Ints.checkedCast(minute));
    }

    private boolean didTargetWentOffline() {
        return mTargetWentOffline;
    }

    /**
     * Exits current user session. should preferably be used when having changed from one user session to another before
     * using changeUser()
     */
    private void exitUserSession() {
        String response = sendCommand("exit");
        mLogger.debug("Response after running command exit: " + response);
        String user = sendCommand("whoami");
        mLogger.debug("user after running command exit: " + user);
        if (!response.matches(mSshSession.getCurrentPrompt()) && !user.equals(mInitialUser)) {
            throw new EcsSessionException(
                    "Something went wrong exiting users \"" + getUser() + "\" session: " + response);
        }
    }

    private int extractPid(String serviceStatus) {
        List<String> elements = Arrays.asList(serviceStatus.trim().split(" "));
        if (serviceStatus.contains("process")) {
            return Integer.parseInt(elements.get(elements.size() - 1));
        }
        if (serviceStatus.contains("pid")) {
            // fix reply like this: "crond (pid 7238) is running..."
            String pidString = elements.get(elements.size() - 3);
            return Integer.parseInt(pidString.substring(0, pidString.length() - 1));
        }
        if (serviceStatus.contains("PID")) {
            // fix for reply like this: "Main PID: 18465 (crond)"
            String pid = elements.get(elements.indexOf("PID:") + 1);
            return Integer.parseInt(pid);
        }
        mLogger.warn("No PID found. Service action returns: " + serviceStatus);
        return 0;
    }

    /**
     * Initializes the mSshSession variable of current target instance
     */
    private void initializeSession() {
        if (mSshSession == null) {
            this.sendCommand("hostname");
        }
    }

    /**
     * Checks if the time in LogResult is greater than initial time
     * ex: return true if log Result dateTime list  [ 2018-01-02T13:55:31.090, 2018-01-02T13:55:31.204] > 2018-01-02T13:55:31.182 (initialTime)
     *
     * @param initialTime - initial LocalDateTime value
     * @param logResult - represents the data of a log file which contains the datetime pattern "([\\s]+[0-9-]+[T][0-9:]+(/.)[0-9]{3})" ex(2018-02-12T12:03:22.678)
     * ex: here logResult - contains /log-collector/audit.log file content
     *     <14>1 2018-02-19T07:45:34.553831+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1519022734.551:1905): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     *     <14>1 2018-02-22T09:59:24.406654+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1519289964.401:1232): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     *     <14>1 2018-02-22T10:08:39.400265+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1519290519.396:1284): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     * @return - true if logTime in logResult > initialTime  otherwise false
     */
    private boolean isLogTimeAfterInitialTime(LocalDateTime initialTime, String logResult) {
        if (initialTime != null) {
            String[] times = logResult.split("\n");
            for (String time : times) {
                String dateTime = null;
                Matcher matcher = Pattern.compile("([\\s]+[0-9-]+T[0-9:]+[/.][0-9]{3})").matcher(time);
                if (matcher.find()) {
                    dateTime = matcher.group().trim();
                    LocalDateTime logTime = LocalDateTime.parse(dateTime.trim());
                    if (initialTime.isBefore(logTime)) {
                        return true;
                    }
                }
            }
            return false;
        }
        throw new EcsTargetException("Unable to compare the dateTime as initialTime is not provided");
    }

    /**
     * This method waits for the process kill within timeout.
     * Throws runtime exception if not successful.
     *
     */
    private void waitAndVerifyProcessKill(final String processId) {
        String errorMessage = "ERROR: Process has not been killed";

        new LoopHelper<Boolean>(Timeout.PROCESS_KILL, errorMessage, Boolean.TRUE, () -> {
            // process is killed if the related file is removed from /proc directory
            String output = sendCommand(String.format(LS_PROC_FILES, processId));
            return output.contains("No such file or directory");
        }).run();
        mLogger.info(Verdict.KILLED, "", "Process", processId);

    }

    /**
     * Get access to config yaml parser
     *
     * @return ConfigYamlParser
     */
    protected ConfigYamlParser configYaml() {
        if (null == mConfigYamlParser) {
            mConfigYamlParser = new ConfigYamlParser(
                    mFuel.getRemoteFile(CONFIG_YAML_REMOTE_PATH, CONFIG_YAML_FILE_NAME));
        }
        return mConfigYamlParser;
    }

    /**
     * Ping an ip address inside its network namespace or outside the network namespace
     * (if no valid networkId is given).
     *
     * @param String ipAddress - ip address to ping
     * @param String networkId - Id of network in which ping shall be performed
     *            or null if ping should not be performed in namespace
     * @return Boolean - true if ping result is 0% packet loss, otherwise false
     */
    protected boolean pingIp(final String ipAddress, final String networkId) {
        String namespaceSwitching = "";
        if (networkId != null) {
            namespaceSwitching = "sudo ip netns exec qdhcp-" + networkId + " ";
        }
        final String pingCmd = namespaceSwitching + "ping -c 5 " + ipAddress;
        try {
            new LoopHelper<Boolean>(Timeout.PING_VM,
                    "Trying to ping " + ipAddress + " on network namespace:" + networkId == null ? "<N/A>" : networkId,
                    Boolean.TRUE, () -> {
                        boolean pingSuccesful = false;
                        try {

                            mLogger.debug("Send cmd: " + pingCmd);
                            String pingResponse = sendCommand(pingCmd);
                            pingSuccesful = pingResponse.contains(" 0% packet loss");
                        } catch (ReadTimeoutException f) {
                            // Ok to end up here...
                        }
                        return pingSuccesful;
                    }).run();
        } catch (Exception e) {
            mLogger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Executes the specified process in background and ensures that the process is running.
     *
     * @param absoluteDirectory - String - absolute path of the directory of process in target
     * @param processToCheck - String - the name of the process to be executed
     * @param timeToCheck - int - the time (in seconds) to check information for
     * @return int - the process id of the executed process
     */
    protected int runCheck(String absoluteDirectory, String processToCheck, int timeToCheck) {
        mLogger.info(EcsAction.STARTING, "process", processToCheck, "on " + getHostname());
        changeDirectory(absoluteDirectory);
        if (!doesFileExist(processToCheck)) {
            throw new EcsTargetException(
                    "The process " + processToCheck + " does not exist in directory " + absoluteDirectory);
        }
        sendCommand("nohup ./" + processToCheck + " " + timeToCheck + " &");
        String processId = getProcessId(processToCheck);
        if (processId == null) {
            new LoopHelper<Boolean>(Timeout.PROCESS_READY, "Failed to start process " + processToCheck, Boolean.TRUE,
                    () -> {
                        String pid = getProcessId(processToCheck);
                        return pid != null;
                    }).run();
        }
        processId = getProcessId(processToCheck);
        mLogger.info(Verdict.STARTED, "process", processToCheck, "on " + getHostname());
        return Integer.parseInt(processId);
    }

    protected void setTargetWentOffline(boolean newValue) {
        mTargetWentOffline = newValue;

    }

    /**
     * Issuing "service [service name] [start/stop/restart/status]" on the target
     *
     * @param serviceName
     * @param serviceAction
     * @return the result of issued command
     */
    protected String systemServiceAction(String serviceName, ServiceAction serviceAction) {
        if (serviceAction == ServiceAction.STATUS) {
            String status = sendCommand(String.format(SERVICE_CMD, serviceName, serviceAction) + " | grep Active");
            if (status.contains("Usage:") || status.equals(mSshSession.getReturnedPrompt())) {
                mLogger.error(
                        "Service \"" + serviceName + "\" doesn't support action STATUS (Let's hope it is running)");
                return UNSUPPORTED_SERVICE_STATUS;
            }
            if (status.contains("running")) {
                String pidResult = sendCommand(String.format(SERVICE_CMD, serviceName, serviceAction) + " | grep PID");
                return pidResult;
            } else {
                return status;
            }
        }
        return sendCommand(String.format(SERVICE_CMD, serviceName, serviceAction));
    }

    /**
     * Wrapper for command "mkfs"
     *
     * @param fstype
     * @param options
     * @param filesys
     */
    public void buildFileSystem(String fstype, String options, String filesys) {
        String optionStr = options == null ? "" : options;
        String mkfsCommand = String.format(CMD_MKFS_FORMAT, fstype, optionStr, filesys);
        sendCommand(mkfsCommand);
    }

    public void changeDirectory(String path) {
        String cdCommand = String.format(CMD_CD_FORMAT, path);
        sendCommand(cdCommand);
    }

    /**
     * Sets specified permissions for the specified absolute path(file/directory)
     *
     * @param absolutePath - the absolute path of the file/directory
     * @return boolean
     */
    public boolean changePermissions(String requiredPermission, String absolutePath) {
        if (!doesDirectoryExist(absolutePath)) {
            if (!doesFileExist(absolutePath)) {
                throw new EcsTargetException(absolutePath + " does not exist");
            }
        }
        String command = "sudo chmod ";
        if (this instanceof EcsAtlasBatVm) {
            command = "chmod ";
        }
        String result = mSshSession.send(command + requiredPermission + " " + absolutePath);
        if (result.contains("not permitted")) {
            throw new EcsTargetException("Failed to set " + requiredPermission + " permissions for " + absolutePath);
        }
        return true;
    }

    /**
     * Change the current user
     *
     * @param - String - new user
     * @return boolean - true if user has been changed successfully
     */
    public boolean changeUser(String newUser) {
        if (mIsNestedUserSession) {
            exitUserSession();
            mIsNestedUserSession = false;
        }
        if (getUser().matches(newUser)) {
            return true;
        } else {
            mInitialUser = sendCommand("whoami");
            sendCommand("sudo -i -u " + newUser);
            mIsNestedUserSession = true;
        }
        if (getUser().matches(newUser)) {
            mLogger.debug("User has been changed to: " + newUser);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compresses specified source file and stores it to specified destination location on target.
     *
     * @param srcFileAbsolutePath - String - absolute path of the file to be compressed in target
     * @param destFileAbsolutePath - String - absolute path where the compressed file is to be created
     * @return boolean
     */
    public boolean compressFile(String srcFileAbsolutePath, String destFileAbsolutePath) {
        mLogger.info(EcsAction.COMPRESSING, "", srcFileAbsolutePath, "to " + destFileAbsolutePath);
        initializeSession();
        mSshSession.setSendTimeoutMillis(Timeout.COMPRESS_FILE.getTimeoutInMilliSeconds());
        sendCommand(String.format(COMPRESS_FILE_CMD, destFileAbsolutePath, srcFileAbsolutePath));
        mSshSession.setSendTimeoutMillis(mSshSession.getUniversalSendTimeout());
        if (doesFileExist(destFileAbsolutePath)) {
            mLogger.info(Verdict.COMPRESSED, "", srcFileAbsolutePath, "to " + destFileAbsolutePath);
            return true;
        }
        return false;
    }

    /**
     * Method which copies a file to specified path
     *
     * @param sourceFilePath
     * @param destinationFilePath
     */
    public boolean copyFile(String sourceFilePath, String destinationFilePath) {
        // remove if file exists before copy
        String fileName = sourceFilePath.substring(sourceFilePath.lastIndexOf("/") + 1);
        List<String> fileListBeforeCopy = listFiles(destinationFilePath);
        if (fileListBeforeCopy.contains(fileName)) {
            deleteFile(destinationFilePath, fileName);
        }
        sendCommand(String.format(CMD_TO_COPY_FILE, sourceFilePath, destinationFilePath));
        // check if file was correctly copied
        List<String> fileListAfterCopy = listFiles(destinationFilePath);
        if (fileListAfterCopy.contains(fileName)) {
            return true;
        }
        throw new EcsTargetException("File was not copied correctly");
    }

    /**
     * Creates the requested directory
     * Fails if directory could not be created
     *
     * @param absolutePath - String - directory which should be created
     * @return boolean - true, if directory was created successful
     */
    public boolean createDirectory(String absolutePath) {
        return createDirectory(absolutePath, false);
    }

    /**
     * Creates the requested directory
     * Fails if directory could not be created
     *
     * @param absolutePath - String - directory which should be created
     * @param isRootUser - boolean - true if the directory is to be created with sudo rights
     * @return boolean - true, if directory was created successful
     */
    public boolean createDirectory(String absolutePath, boolean isRootUser) {
        mLogger.info(EcsAction.CREATING, "Directory", absolutePath, "on " + getHostname());
        // make directory even if it is already existing
        String command = null;
        if (isRootUser) {
            command = String.format(SUDO_MKDIR_CMD_FORMAT, absolutePath);
        } else {
            command = String.format(MKDIR_CMD_FORMAT, absolutePath);
        }
        mLogger.debug("Send command: " + command);
        String commandResult = sendCommand(command);
        mLogger.debug("Got response: " + commandResult);
        // check if directory was correctly created
        command = String.format("ls -d %s", absolutePath);
        mLogger.debug("Send command: " + command);
        String checkResult = sendCommand(command);
        mLogger.debug("Got response: " + commandResult);
        if (!commandResult.contains(FILE_OR_DIR_NOT_FOUND) && checkResult.equals(absolutePath)) {
            mLogger.info(Verdict.CREATED, "Directory", absolutePath, "on " + getHostname());
            return true;
        }
        throw new EcsTargetException("Directory was not correctly created");
    }

    /**
     * Creates a file
     * fails if the file could not correctly be created
     *
     * @param absolute path - String path where the file should be stored
     * @param fileContent - String string which should be stored in the file
     * @param fileName - String name of the file
     * @return boolean - true, if the file was created correctly
     */
    public boolean createFile(String absolutePath, String fileContent, String fileName) {
        mLogger.info(EcsAction.CREATING, absolutePath, fileName, ": " + fileContent);
        // create given directory (without checking if it exists before)k
        String command;
        if (absolutePath == null || absolutePath.equals("")) {
            command = String.format("echo %s > %s", fileContent, fileName);
        } else {
            createDirectory(absolutePath);
            command = String.format("echo %s > %s/%s", fileContent, absolutePath, fileName);
        }
        // create file with given content
        mLogger.debug("Send command: " + command);
        String result = sendCommand(command);
        mLogger.debug("Got response: " + result);
        // check if file was correctly created
        if (readFile(absolutePath, fileName).equals(fileContent)) {
            mLogger.info(Verdict.CREATED, absolutePath, fileName, ": " + fileContent);
            return true;
        } else if (fileContent.startsWith("'")) {
            String readContent = readFile(absolutePath, fileName);
            fileContent = fileContent.substring(1, fileContent.length() - 1);
            String[] lines = fileContent.split("\n");
            String first = lines[0];
            String last = lines[lines.length - 1];
            if (readContent.startsWith(first) && readContent.endsWith(last)) {
                mLogger.info(Verdict.CREATED, absolutePath, fileName, ": " + fileContent);
                return true;
            }
        }
        throw new EcsTargetException("File was not correctly created");
    }

    /**
     * Creates a Sparse file, fails if the file could not correctly be created
     * (A Sparse file is a file that looks like a 200 GB file, but in reality
     * only requires a directory entry on disk.)
     *
     *
     * @param absolutePath - String - absolute path where the file should be stored
     * @param fileName - String - name of the file
     * @param fileSizeInGB - the size of the file in GB
     * @return File - the created file
     */
    public File createSparseFile(String absolutePath, String fileName, int fileSizeInGB) {
        mLogger.info(EcsAction.CREATING, "", absolutePath, fileName + " on " + getHostname());
        // create given directory (without checking if it exists before)
        if (!(absolutePath == null || absolutePath.equals(""))) {
            createDirectory(absolutePath);
            changeDirectory(absolutePath);
        }
        String output = mSshSession.send(String.format(CMD_CREATE_SPARSE_FILE, fileName, fileSizeInGB));
        if (!output.contains("0 bytes copied")) {
            throw new EcsTargetException("Failed to create sparse file " + fileName + " command output:" + output);
        }
        String allocatedSpaceForFile = mSshSession.send(String.format(CMD_VERIFY_ALLOCATED_SPACE_SPARSE_FILE, fileName))
                .trim();
        if (allocatedSpaceForFile.equals("0")) {
            String actualSpaceForFile = mSshSession.send(String.format(CMD_VERIFY_ACTUAL_SPACE_SPARSE_FILE, fileName))
                    .trim();
            if (actualSpaceForFile.contains(String.valueOf(fileSizeInGB))) {
                File sparseFile = new File(fileName);
                mLogger.info(Verdict.CREATED, "", absolutePath, fileName + " on " + getHostname());
                return sparseFile;
            }
        }
        throw new EcsTargetException("Failed to create sparse file");
    }

    /**
     * deletes the given directory
     *
     * @param absolutePath - String - directory which should be deleted
     * @return boolean - true, if directory was deleted successful
     */
    public boolean deleteDirectory(String absolutePathToDirectory) {
        mLogger.info(EcsAction.DELETING, "Directory", absolutePathToDirectory, "on " + getHostname());
        sendCommand("sudo rm -rf " + absolutePathToDirectory);
        String command = String.format("ls -d %s", absolutePathToDirectory);
        String result = sendCommand(command);
        if (result.contains("No such file or directory")) {
            mLogger.info(Verdict.DELETED, "Directory", absolutePathToDirectory, "on " + getHostname());
            return true;
        }
        throw new EcsTargetException("Directory was not correctly deleted");
    }

    /**
     * Deletes a file
     *
     * @param absolutePath - path where the file is stored
     * @param fileName - name of the file
     * @return boolean - true, if the file was deleted
     */
    public boolean deleteFile(String absolutePath, String fileName) {
        mLogger.info(EcsAction.DELETING, "", absolutePath + fileName, "on " + getHostname());
        String pathPrefix = "";
        // try to delete given file

        // TODO: Implement smarter file path handling/validation
        if (absolutePath.endsWith("/")) {
            pathPrefix = absolutePath;
        } else if (!absolutePath.equals("")) {
            pathPrefix = absolutePath + "/";
        }
        String output = sendCommand(String.format("rm " + pathPrefix + fileName));

        if (!output.contains(FILE_OR_DIR_NOT_FOUND)) {
            mLogger.debug(pathPrefix + fileName + " is deleted");
            mLogger.info(Verdict.DELETED, "", absolutePath + fileName, "on " + getHostname());
            return true;
        }
        throw new EcsTargetException("Given file could not be deleted");
    }

    /**
     * Wrapper for command "fdisk"
     *
     * @param options
     * @param disk
     */
    public void diskPartition(String options, String disk) {
        String optionStr = options == null ? "" : options;
        String fdiskCommand = String.format(CMD_FDISK_FORMAT, optionStr, disk);
        sendCommand(fdiskCommand);
    }

    /**
     * Wrapper for command "df -h"
     *
     * @return
     */
    public String diskUsage() {
        String output = sendCommand(String.format(CMD_DF_FORMAT));
        if (output.contains("Mounted on")) {
            return output;
        }
        throw new EcsTargetException("Diskfree command output is not correct and the output is " + output);
    }

    /**
     * Sends a dmesg command on VM which displays the kernel messages on screen.
     */
    public String dmesg() {
        return sendCommand("dmesg");
    }

    /**
     * Check if a directory exists on target
     *
     * @param absolutePath
     * @return true if the path points to a directory
     */
    public boolean doesDirectoryExist(String absolutePath) {
        mLogger.info(EcsAction.FINDING, " ", this.getClass(), absolutePath);
        if (doesFileExist(absolutePath)) {
            mLogger.error(absolutePath + " is a file!");
            return false;
        }
        return sendCommand("test -e " + absolutePath + ";echo $?").equals("0");
    }

    /**
     * Check if a file exists
     *
     * @param absolutePath
     * @return true if the path points to a file
     */
    public boolean doesFileExist(String absolutePath) {
        mLogger.debug("Checking if file exist: " + absolutePath);
        return sendCommand("test -f " + absolutePath + ";echo $?").equals("0");
    }

    /**
     * Check if the given file exists in specified user's home folder.
     *
     * @param user
     * @param absolutePath
     * @return true if the path points to a file
     */
    public boolean doesFileExist(String user, String absolutePath) {
        mSshSession.send("sudo -i -u " + user);
        return doesFileExist(absolutePath);
    }

    /**
     * verifies the action "grep a word from the given log file and checks if the time in LogResult is greater than initial time"
     * ex: root@atlas:~# cat /log-collector/audit.log | grep kalle
     *     <14>1 2018-02-19T14:19:53.417615+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1518170281.722:1981): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     *     <14>1 2018-02-22T09:59:24.406654+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1519289964.401:1232): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     *     <14>1 2018-02-22T10:08:39.400265+01:00 localhost audispd - - -  node=atlas type=EXECVE msg=audit(1519290519.396:1284): argc=4 a0="sudo" a1="-i" a2="-u" a3="kalle"
     *
     * @param logFilePath - absolute path of the log file
     * @param grepKeyword - Keyword that used for search
     * @param initialTime - initial LocalDateTime is the time before we do specific task
     *                      for example: "Reboot cic"
     * @return true - if logTime > initialTime otherwise false
     */
    public boolean doesLogTimeIsAfterInitialTime(String logFilePath, String grepKeyword, LocalDateTime initialTime) {
        if (!doesFileExist(logFilePath)) {
            throw new EcsTargetException("Given file does not exist");
        }
        if (grepKeyword != null && (!grepKeyword.isEmpty())) {
            sendCommand("sudo -i");
            String logResult = sendCommand(String.format("cat %s | grep %s", logFilePath, grepKeyword));
            if (logResult.contains(grepKeyword)) {
                return isLogTimeAfterInitialTime(initialTime, logResult);
            }
        }
        throw new EcsTargetException(grepKeyword + " not found in " + logFilePath);
    }

    /**
     * Method which downloads a file from Artifactory to Local
     *
     * @param pathInArtifactory - String - full path of the file along with name of the file in Artifactory
     *
     * @return returns a file object of downloaded file
     * @throws IOException
     *
     */
    public File downloadFileFromArtifactoryToLocal(String filePathInArtifactory) throws IOException {
        mLogger.info(EcsAction.DOWNLOADING, filePathInArtifactory, "from Artifactory", "to local system");
        ArtifactoryClientConfiguration artifactoryConfig = new ArtifactoryClientConfiguration(
                CommonParametersValues.ARTIFACTORY_URL, CommonParametersValues.ARTIFACTORY_USERNAME,
                CommonParametersValues.ARTIFACTORY_PASSWORD);
        Response artifactResponse = new ArtifactoryClient(artifactoryConfig)
                .getArtifactResponse(CommonParametersValues.ARTIFACTORY_URL + filePathInArtifactory);
        if (artifactResponse == null) {
            throw new EcsOpenStackException("Could not get file from artifactory path" + filePathInArtifactory);
        }
        String extensionOftheFile = FilenameUtils.getExtension(filePathInArtifactory);
        String fileName = "File_" + ControllerUtil.createTimeStamp() + "." + extensionOftheFile;
        InputStream inputStream = artifactResponse.readEntity(InputStream.class);
        File file = new File(fileName);
        OutputStream outputStream = new FileOutputStream(file);
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        inputStream.close();
        outputStream.close();
        mLogger.info(Verdict.DOWNLOADED, filePathInArtifactory, "from Artifactory", "to local system");
        return file;
    }

    /**
     * Method which copies a file to a new file in the same location (duplicates).
     *
     * @param absolutSourceFile - the absolute path of the source file to be duplicated
     * @param absoluteDestFile - the absolute path of the duplicated file
     * @return boolean
     */
    public boolean duplicateFile(String absolutSourceFile, String absoluteDestFile) {
        String newFileName = absoluteDestFile.substring(absoluteDestFile.lastIndexOf("/") + 1);
        String directory = absoluteDestFile.substring(0, absoluteDestFile.lastIndexOf("/"));
        changeDirectory(directory);
        sendCommand(String.format(CMD_TO_COPY_FILE, absolutSourceFile, absoluteDestFile));
        // check if file was correctly copied
        List<String> fileListAfterCopy = listFiles(directory);
        if (fileListAfterCopy.contains(newFileName)) {
            return true;
        }
        throw new EcsTargetException("File was not copied correctly");
    }

    /**
     * Edits the specified file.
     *
     * @param fileAbsolutePath - the absolute file path of the file to be edited
     * @param oldContent - the old content to be replaced in the file
     * @param newContent - the new content to replace the old content with
     * @return boolean - true if the file has been edited properly
     */
    public boolean editFile(String fileAbsolutePath, String oldContent, String newContent) {
        /*
         * Example command:
         * atlasadm@atlas:~$ sed -i 's/<### your DC number ###>/155/g' File_1114_09_36_18_709.sh
         * atlasadm@atlas:~$
         */
        sendCommand(String.format(EDIT_FILE_CMD, oldContent, newContent, fileAbsolutePath));
        String fileContent = readFile(null, fileAbsolutePath);
        if (fileContent.contains(oldContent)) {
            throw new EcsTargetException("Failed to edit file " + fileAbsolutePath);
        }
        return fileContent.contains(newContent);
    }

    /**
     * Returns the bridge fw-admin Ip address of cic/compute
     * (For BSP nodes, file copy from LXC to compute nodes is possible only via
     *  br-fw-admin IP and not via br-mgmt IP. Hence use this API for BSP nodes).
     *
     * @return the br-fw-admin IP Address of cic/compute
     */
    public String getAdminIp() {
        String bridgeIp = sendCommand(ADMIN_IP_CMD).trim();
        Pattern pattern = Pattern.compile("(\\d+).(\\d+).(\\d+).(\\d+)");
        Matcher match = pattern.matcher(bridgeIp);
        if (!match.find()) {
            throw new EcsTargetException("Was not able to parse bridge fw-admin IP for " + getHostname());
        }
        return match.group();
    }

    /**
     * Returns the bridge management Ip address of cic/compute
     *
     * @return the br-mgmt IP Address of cic/compute
     */
    public String getBridgeIp() {
        String bridgeIp = sendCommand(BRIDGE_IP_CMD).trim();
        Pattern pattern = Pattern.compile("(\\d+).(\\d+).(\\d+).(\\d+)");
        Matcher match = pattern.matcher(bridgeIp);
        if (!match.find()) {
            throw new EcsTargetException("Was not able to parse bridge management IP for " + getHostname());
        }
        return match.group();
    }

    /** Method to get current date
     *
     * @return - Date- Current Date
     *
     */
    public Date getDate() {
        Date currentTime = null;
        // "Tue Jun 23 15:26:42 UTC 2015";
        String date = sendCommand("date");
        // TODO: try to handle other time format as well
        String parsedDate = date.replace(" CEST ", " ");
        parsedDate = parsedDate.replace(" UTC ", " ");
        parsedDate = parsedDate.replace(" CET ", " ");
        parsedDate = parsedDate.replace(" MDT ", " ");
        parsedDate = parsedDate.replace(" MST ", " ");
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        try {
            currentTime = dateFormat.parse(parsedDate);
        } catch (ParseException e) {
            throw new EcsTargetException(String.format(
                    "The format of the command 'date' has changed on the " + getHostname()
                            + "! Expected format was %s, but the actual date to be parsed was %s. %s",
                    DATE_FORMAT, parsedDate, e.getMessage()));
        }
        return currentTime;
    }

    /**
     * Returns the checksum of a file
     *
     * @param absolutePath - the path to the file whose checksum is needed
     *
     * @return String - the checksum of the file
     */
    public String getFileChecksum(String absolutePath) {
        return sendCommand(String.format(CHECKSUM_CMD, absolutePath)).trim();
    }

    /**
     * Transfers file from LXC to current target (cic/fuel).
     *
     * @param absoluteSrcFilePath - String - absolute file path in LXC
     * @param destinationFilePath - String - the location in current object where the file is to be copied
     * @return boolean
     */
    public boolean getFileFromLxc(String absoluteSrcFilePath, String destinationFilePath) {
        if (!hasLxcConnectivity()) {
            throw new EcsTargetException(
                    ("Cannot copy file from LXC as there is no LXC connectivity from " + getHostname()));
        }
        mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, "from LXC",
                "to " + getHostname() + " :" + destinationFilePath);
        initializeSession();
        mSshSession.setNextPossiblePassword("Jumpstart");
        String result = sendCommand(
                "scp lxcpxe@" + mLxc.getIpAddress() + ":" + absoluteSrcFilePath + " " + destinationFilePath);
        if (!result.contains("100%")) {
            throw new EcsTargetException("copying file from LXC failed");
        }
        mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, "from LXC",
                "to " + getHostname() + " :" + destinationFilePath);
        return true;
    }

    /**
     * Returns the size of the file specified in bytes
     *
     * @param absoluteFilePath - the absolute path of the file
     * @return Long - the size of the file in bytes
     */
    public Long getFileSize(String absoluteFilePath) {
        if (!doesFileExist(absoluteFilePath)) {
            throw new EcsTargetException("Given file does not exist");
        }
        String fileSize = sendCommand(String.format(FILE_SIZE_CMD, absoluteFilePath));
        return Long.parseLong(fileSize);
    }

    /**
     * Returns the free disk space in the specified directory in bytes.
     *
     * @param directory - String - absolute path of the directory
     * @return Long - the free disk space in the directory
     */
    public Long getFreeDiskSpaceInBytes(String directory) {
        String freeDiskSpace = mSshSession.send(String.format(CMD_FREE_DISK_SPACE, directory)).trim();
        return Long.parseLong(freeDiskSpace);
    }

    /**
     * Get host name of the target
     *
     * @return hostname of the target
     */
    public String getHostname() {
        initializeSession();
        return mSshSession.getHostname();
    }

    /**
     * Method which retrieves the NTP offset value of a cic/compute
     *
     * @return - float - the NTP offset value of cic/compute
     */
    public float getNtpOffset() {
        String offsetValue = sendCommand(OFFSET_CMD).trim();
        Pattern pattern = Pattern.compile("[0-9\\.-]*");
        Matcher match = pattern.matcher(offsetValue);
        if (!match.find()) {
            throw new EcsTargetException("Was not able to parse NTP offset value for " + getHostname());
        }
        String offset = match.group();
        return Float.parseFloat(offset);
    }

    /**
     * Get openstack admin user
     *
     * @return admin user
     */
    public String getOpenstackAdminUser() {
        return configYaml().getIdamSystemAdminUser();
    }

    /**
     * Get openstack backup admin user
     *
     * @return backup admin user
     */
    public String getOpenstackBackupAdminUser() {
        return configYaml().getIdamBackupAdminUser();
    }

    /**
     * Get openstack users with passwords
     *
     * @return users along with passwords
     */
    public Map<String, String> getOpenstackUsers() {
        return configYaml().getUsers();
    }

    /**
     * Returns the process ID of a process
     *
     * @param processName - String - name of process
     * @return String - process ID
     */
    public String getProcessId(String processName) {
        mLogger.info(EcsAction.FINDING, "", "PID", "of " + processName);
        String psCmdToUse = PS_CMD_DEFAULT;

        // use specific PS command to search for processes of terminal services
        if (processName.startsWith("tty")) {
            psCmdToUse = PS_CMD_CONSOLE_TTY;
        }

        // get ID of process using process name
        String processId = sendCommand(String.format(psCmdToUse, processName)).trim();

        // if service name does not return valid process ID, add "d" to service name and try again
        // (services like ssh use a daemon process, which has an additional "d" at the end of the name)
        String tempProcessName = null;
        if (!(Pattern.matches("^[0-9]+$", processId))) {
            tempProcessName = processName + "d";
            processId = sendCommand(String.format(psCmdToUse, tempProcessName)).trim();
        }

        // if still no valid process ID is returned, set processID to null
        if (!(Pattern.matches("^[0-9]+$", processId))) {
            processId = null;
        }
        mLogger.info(Verdict.FOUND, "", "PID", processId + " of " + processName + " on " + getHostname());
        return processId;
    }

    /**
     * Returns the process ID of a service
     *
     * @param serviceName - String - name of service
     * @return String - process ID
     */
    public String getProcessIdOfService(String serviceName) {

        String processName = serviceName;

        // some process names differ from service name
        switch (serviceName) {
        case "dbus":
            processName = "dbus-daemon";
            break;
        case "keystone":
            processName = "keystone-all";
            break;
        case "libvirt-bin":
            processName = "libvirtd";
            break;
        case "neutron-plugin-openvswitch-agent":
            processName = "neutron-openvswitch-agent";
            break;
        case "mongodb":
            processName = "mongod";
            break;
        case "ssh":
            processName = "sshd";
        }

        return getProcessId(processName);
    }

    /**
     * method which copies a remote file from target to local
     *
     * @param ipAddress - target IP address
     * @param user - user used to connect to target
     * @param path - the remote file path on target
     * @param fileName - remote file name on target
     * @param password - password for the user
     * @param port - port to use for scp
     * @return a File represents the copied local file
     */
    public File getRemoteFile(String ipAddress, String user, String password, Integer port, String remotePath,
            String fileName) {
        Scp scp;
        if (port == null) {
            scp = CliFactory.newScp(ipAddress, user, password);
        } else {
            scp = CliFactory.newScp(ipAddress, user, password, port);
        }
        scp.connect();
        mLogger.info(EcsAction.TRANSFERING, fileName, TEMP_FILE_LOCAL_PATH, remotePath);
        scp.get(remotePath + fileName, TEMP_FILE_LOCAL_PATH);
        // TODO : use loophelper to make sure file is copied
        mLogger.info(Verdict.TRANSFERED, fileName, TEMP_FILE_LOCAL_PATH, remotePath);
        scp.disconnect();
        return new File(TEMP_FILE_LOCAL_PATH + "/" + fileName);
    }

    public String getReturnedPrompt() {
        initializeSession();
        return mSshSession.getReturnedPrompt();
    }

    /**
     * Returns the list of services, which are supervised by Upstart
     * (criteria:
     * a) service has a .conf file in /etc/init/
     * b) that .conf file includes a line that starts with string "respawn"
     * c) service does not have a .override file in /etc/init/)
     *
     * @param none
     * @return List<String> - list of services, which are supervised
     */
    public List<String> getServicesSupervisedByUpstart() {

        List<String> supervisedServices = new ArrayList<String>();
        String serviceName = null;

        // get list of files in /etc/init/ , which contain the string respawn
        sendCommand(CD_TO_UPSTART_CONFIG_DIR);
        String result = sendCommand(GREP_FILES_WITH_UPSTART_CONFIG);
        String[] fileList = result.split("\n");

        for (String fileName : fileList) {
            // cut out service name from file name and check that
            // no .override file exists for this service
            serviceName = fileName.substring(0, fileName.indexOf(".conf"));
            if (listFiles(UPSTART_CONFIG_PATH).contains(serviceName + ".override")) {
                continue;
            }

            // service python-teal is not supported by CEE supervision, console and umm-console process is by default
            // not started on CEE
            // cinder-volume and mysql are configured for Upstart, which is not correct --> TR HT51046
            if (serviceName.endsWith("console") || serviceName.equals("python-teal")
                    || serviceName.equals("cinder-volume") || serviceName.equals("mysql")) {
                continue;
            }
            supervisedServices.add(serviceName);
        }
        return supervisedServices;
    }

    /**
     * Returns the time that the node has been running without restart.
     *
     * @return EcsSystemUptime - the time that the node has been online.
     */
    public EcsSystemUptime getSystemUptime() {
        String output = sendCommand("cat /proc/uptime");
        try {
            String rawUptimeInSec = output.substring(0, output.indexOf(" "));
            return calculateTime(Long.valueOf(rawUptimeInSec.substring(0, rawUptimeInSec.indexOf("."))));

        } catch (Exception e) {
            mLogger.debug("Output from 'uptime' is " + output);
            throw new EcsTargetException("Could not parse uptime from output!: " + e.getMessage());
        }
    }

    /**
     * Only for backwards compatability
     */
    public Cli getUnsupervisedConnection() {
        initializeSession();
        return mSshSession.getUnsupervisedConnection();
    }

    /**
     * Returns the used disk space % in the specified directory.
     *
     * @param directory - String - absolute path of the directory
     * @return int - the used disk space % in the directory
     */
    public int getUsedDiskSpacePercent(String directory) {
        String usedPercent = null;
        String usedDiskSpace = mSshSession.send(String.format(CMD_USED_DISK_SPACE, directory)).trim();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher match = pattern.matcher(usedDiskSpace);
        if (match.find()) {
            usedPercent = match.group();
        }
        return Integer.parseInt(usedPercent);
    }

    /**
     * Return the current user. If user is prefixed with "jcat_" then actual user without this prefix will be returned.
     *
     * @return String - current user
     */
    public String getUser() {
        String user = sendCommand("whoami");
        if (user.startsWith(JCAT_USER_PREFIX)) {
            return user.substring(JCAT_USER_PREFIX.length());
        }
        return user;
    }

    /**
     * Checks if an application is running on the operating system
     *
     * @param name - name of the application
     * @return boolean - true if the application is running
     */
    public boolean isApplicationRunning(String name) {
        String result = sendCommand("pidof " + name);
        // is the application running?
        if (result.contains("Stopped")) {
            // TODO Is there a better way to do this? Sometimes the output contains the "Stopped process....bla" even if
            // this info is not related to the current command.
            Scanner scanner = new Scanner(result);
            result = scanner.nextLine();
            scanner.close();
        }
        result = result.trim();

        // There are more processes for this application running
        if (result.contains(" ")) {
            result = result.substring(0, result.indexOf(" "));
        }

        try {
            int pid = Integer.valueOf(result);
            return pid > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Use service status command to verify if service is running
     *
     * @param service - String - service name
     * @return true if service is running with status "start/running"
     */
    public boolean isServiceRunning(String service) {
        String status = systemServiceAction(service, ServiceAction.STATUS);
        return (status.contains("PID") || status.equals(UNSUPPORTED_SERVICE_STATUS));
    }

    /**
     * Makes the kernel go into panic, i.e. crashes the node.
     * The node will be restarted when this method was called. The restart was NOT verified within this method, user has
     * to do it in the test.
     * NOTE: Use this one with caution.
     */
    public void kernelPanic() {
        sendCommand("echo c > /proc/sysrq-trigger&");
    }

    /**
     * Kill a process
     *
     * @param processId - Id of the process to kill.
     * @param hardkill - If the process should be terminated directly
     */
    public void killProcess(final String processId, boolean hardkill) {
        mLogger.info(EcsAction.KILLING, "", "Process", processId);
        if (hardkill) {
            sendCommand(String.format(HARDKILL_CMD, processId));
        } else {
            sendCommand(String.format(KILL_CMD, processId));
        }
        // Waiting for the process to be killed.
        waitAndVerifyProcessKill(processId);
    }

    /**
     * Kill a process by service name
     *
     * @param processName - Name of the process to kill.
     * @param processId - Id of the process to kill.
     */
    public void killProcessByServiceName(final String processName, final String processId) {
        mLogger.info(EcsAction.KILLING, "", "Process", processName);
        sendCommand(String.format(HARDKILL_BY_SERVICE_NAME_CMD, processName));
        // Waiting for the process to be killed.
        waitAndVerifyProcessKill(processId);
    }

    /**
     * Lists files in directory
     *
     * @param directory - absolute path to the directory to get files.
     * @return A list of files.
     */
    public List<String> listFiles(String directory) {
        return listFiles(directory, false, false);
    }

    /**
     * Lists files in directory, including hidden files and/or recursive files if specified.
     *
     * @param directory - absolute path to the directory to get files.
     * @param isHidden - true if hidden files are to be listed, else false
     * @param isRecursive - true if files are to be listed recursively, else false
     * @return A list of files.
     */
    public List<String> listFiles(String directory, boolean isHidden, boolean isRecursive) {
        List<String> files = new ArrayList<String>();
        String result = sendCommand("cd " + directory);

        if (result.contains("No such file or directory")) {
            return null;
        }
        if (isHidden && isRecursive) {
            result = sendCommand("ls -aR");
        } else if (isHidden) {
            result = sendCommand("ls -a");
        } else if (isRecursive) {
            result = sendCommand("ls -R");
        } else {
            result = sendCommand("ls");
        }
        StringTokenizer st = new StringTokenizer(result);

        if (result.contains("No such file or directory")) {
            return null;
        }

        while (st.hasMoreTokens()) {
            files.add(st.nextToken());
        }
        sendCommand("cd ");
        return files;
    }

    /**
     * Wrapper for command "mount"
     *
     * @param device
     * @param dir
     * @return
     */
    public String mountFileSystem(String device, String dir) {
        String mountCommand = String.format(CMD_MOUNT_FORMAT, device, dir);
        return sendCommand(mountCommand);
    }

    /**
     * Moves specified file from one location to another
     *
     * @param filePath - the initial location of the file
     * @param fileName - the name of the file
     * @param destDirectoryPath - the path of the directory where the file is to be moved
     * @return boolean
     */
    public boolean moveFile(String filePath, String fileName, String destDirectoryPath) {
        String absoluteFilePath = filePath + fileName;
        if (filePath == null || filePath == "") {
            absoluteFilePath = fileName;
        } else if (!filePath.endsWith("/")) {
            absoluteFilePath = filePath + "/" + fileName;
        }
        if (!doesFileExist(absoluteFilePath)) {
            throw new EcsTargetException("Given file does not exist");
        }
        sendCommand("sudo mv " + absoluteFilePath + " " + destDirectoryPath);
        return listFiles(destDirectoryPath).contains(fileName);
    }

    /**
     * Method to move the current time ahead by the specified amount of time(in minutes)
     *
     * @param - minutes - number of minutes to move the time ahead
     *
     */
    public void moveTimeAheadByMinutes(int minutes) {
        mLogger.info(EcsAction.CHANGING, "Date", "on " + getHostname(), "ahead by " + minutes + " minutes");
        Date currentTime = getDate();
        int updatedMinutes = currentTime.getMinutes() + minutes;
        currentTime.setMinutes(updatedMinutes);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = dateFormat.format(currentTime);
        sendCommand(String.format("date -s %s", time));
        mLogger.info(Verdict.CHANGED, "Date", "on " + getHostname(), "ahead by " + minutes + " minutes");
    }

    /**
     * method which copies a file from local to target
     *
     * @param ipAddress - target IP address
     * @param user - user used to connect to target
     * @param remoteFilePath - the remote file path on target
     * @param localFilePath - the  file path from local
     * @param password - password for the user
     * @param port - port to use for scp
     */
    public void putRemoteFile(String localFilePath, String ipAddress, String user, String password, Integer port,
            String remoteFilePath) {
        Scp scp = CliFactory.newScp(ipAddress, user, password, port);
        scp.connect();
        mLogger.info(EcsAction.TRANSFERING, localFilePath, remoteFilePath, "on " + getHostname());
        scp.put(localFilePath, remoteFilePath);
        mLogger.info(Verdict.TRANSFERED, localFilePath, remoteFilePath, "on " + getHostname());
        scp.disconnect();
    }

    /**
     * Reads a file and returns the contents as a string
     * Fails if the given file could not be read
     *
     * @param absolutePath - path where the file should be stored
     * @param fileName - name of the file
     * @return String - contents of the file
     */
    public String readFile(String absolutePath, String fileName) {
        // try to read given file
        String command;
        if (absolutePath == null || absolutePath.equals("")) {
            command = String.format("cat %s", fileName);
        } else {
            command = String.format("cat %s/%s", absolutePath, fileName);
        }
        mLogger.debug("Send command: " + command);
        String output = sendCommand(command);
        // check if file could be read
        if (!output.equals("cat: " + fileName + ": " + FILE_OR_DIR_NOT_FOUND)) {
            return output;
        }
        throw new EcsTargetException("Given file could not be read: " + absolutePath + "/" + fileName);
    }

    /**
     * Reboots the target OS and verifies/monitors that the connection goes down and then comes back up within the
     * default ECS_OS_RESTART timeout.
     * Throws runtime exception if not successful.
     *
     * @return
     */
    public void reboot() {
        reboot(Timeout.ECS_OS_RESTART);
    }

    /**
     * Reboots the target OS and verifies/monitors that the connection goes down and then comes back up within the
     * provided timeout.
     * Throws runtime exception if not successful.
     *
     * @param cicReboot
     *
     * @return
     */
    public void reboot(Timeout cicReboot) {
        sendCommand(REBOOT_CMD);
        waitAndVerifyRestart(cicReboot);
        mLogger.debug("reboot done");
    }

    /**
     * Renames a file
     *
     * @param absolutePath
     *            - path where the file is stored
     * @param fileName
     *            - name of the file
     * @return boolean - true, if the file was renamed
     */
    public boolean renameFile(String absolutePath, String oldFileName, String newFileName) {
        String pathPrefix = "";
        // try to rename given file

        if (absolutePath.endsWith("/")) {
            pathPrefix = absolutePath;
        } else {
            pathPrefix = absolutePath + "/";
        }
        String output = sendCommand(String.format("mv " + pathPrefix + oldFileName + " " + pathPrefix + newFileName));
        if (!output.contains(FILE_OR_DIR_NOT_FOUND)) {
            mLogger.debug(pathPrefix + oldFileName + " is renamed to " + newFileName);
            return true;
        }
        throw new EcsTargetException(
                String.format("Given file %s in the path %s could not be renamed", oldFileName, absolutePath));
    }

    /**
     * Restart a service by using command "service <service-name> restart"
     *
     * @param service - String - service to be restarted
     * @return PID of the new service
     */
    public int restartService(String service) {
        String errorMessage = "Service " + service + " has not been restarted";
        int pid = extractPid(systemServiceAction(service, ServiceAction.STATUS));
        String restart = systemServiceAction(service, ServiceAction.RESTART);
        if (restart.contains("Error") || restart.contains("Usage")) {
            throw new EcsTargetException("Service \"" + service + "\" doesn't support restart");
        }
        new LoopHelper<Boolean>(Timeout.PROCESS_READY, errorMessage, Boolean.TRUE, () -> (isServiceRunning(service)))
                .run();
        int newPid = extractPid(systemServiceAction(service, ServiceAction.STATUS));
        if (pid == newPid && pid == 0) {
            mLogger.error("Unable to determine the status of service \"" + service + "\"");
            return newPid;
        }
        if (pid == newPid || newPid == 0) {
            throw new EcsTargetException(
                    "Service \"" + service + "\" failed to restart. PID found not changed: " + newPid);
        }
        return newPid;
    }

    /**
     * Sends a command
     *
     * @param commandName - name of the command to be send
     * @return the result of sending the command
     */
    public String sendCommand(String command) {
        initializeSession();
        mLogger.debug("Send command: " + command);
        return mSshSession.send(command);
    }

    /**
     *
     * If the session for this target is prompted for a password while sending a command. This password will be used.
     * Uses of this method more than once will overwrite the previous password.
     *
     * @param newPasswordForThisTargetSession
     */
    public void setNextPromptedPasswordForSession(String nextPromptedPassword) {
        initializeSession();
        mSshSession.setNextPossiblePassword(nextPromptedPassword);
    }

    /**
     * Starts an application on the operating system.
     *
     * @param name - name of the application
     * @return The process id or -1 if the pro.
     */
    public int startApplication(String name) {
        int processId = -1;
        String result = sendCommand(name + "&");

        // Did the user try to start an application that doesn't exist?
        if (result.contains("Command not found")) {
            processId = -1;
        }

        // The expected result is [nr] [processId].
        Matcher m = Pattern.compile("(\\[\\d+\\])([ ]*)(\\d+)").matcher(result);
        if (m.matches()) {
            // It's the second value that represents the process id
            String processIdInText = m.group(3);

            // Guaranteed to be an integer since the regexp mathced above.
            processId = Integer.parseInt(processIdInText);
        } else {
            String pidStr = sendCommand("pidof " + name);
            // is the application running?
            if (pidStr.contains("Stopped")) {
                // TODO Is there a better way to do this? Sometimes the output contains the "Stopped process....bla"
                // even if this info is not related to the current command.
                Scanner scanner = new Scanner(pidStr);
                pidStr = scanner.nextLine();
                scanner.close();
            }
            pidStr = pidStr.trim();
            if (pidStr.contains(" ")) {
                pidStr = pidStr.substring(0, pidStr.indexOf(" "));
                mLogger.warn("When starting application " + name
                        + " there are more than one process of this application running and therefore the returned process id could be the id of one of the other processes of the same application");
            }
            try {
                int pid = Integer.valueOf(pidStr);
                processId = pid;
            } catch (NumberFormatException ex) {
                mLogger.warn("Was not able to retrieve a process id of the started application");
            }
        }
        return processId;
    }

    /**
     * Starts scalability collection by running scalability scripts on target.
     *
     * @return boolean
     */
    abstract public boolean startScalabilityCollection();

    /**
     * Start a service by using command "service <service-name> start"
     *
     * @param service - String - service to be started
     * @return PID of the new service
     */
    public int startService(String service) {
        systemServiceAction(service, ServiceAction.START);
        int pid = extractPid(systemServiceAction(service, ServiceAction.STATUS));
        if (pid == 0) {
            throw new EcsTargetException("Service \"" + service + "\" cannot be started");
        }
        new LoopHelper<Boolean>(Timeout.PROCESS_READY, "Service " + service + " cannot be started", Boolean.TRUE,
                () -> isServiceRunning(service)).run();
        return pid;
    }

    /**
     * Starts stability collection by running stability scripts on target.
     *
     * @return boolean
     */
    abstract public boolean startStabilityCollection();

    /**
     * Stop a service by using command "service <service-name> stop"
     *
     * @param service - String - service to be stopped
     *
     */
    public void stopService(String service) {
        String result = systemServiceAction(service, ServiceAction.STOP);
        if (result.contains("unrecognized service")) {
            throw new EcsTargetException("Unrecognized service: " + service);
        }
        new LoopHelper<Boolean>(Timeout.PROCESS_KILL, "Service " + service + " cannot be stopped", Boolean.TRUE,
                () -> !isServiceRunning(service)).run();
    }

    /**
     * Force changed blocks to disk, update the super block
     */
    public String sync() {
        return sendCommand("sync");
    }

    /**
     * Read the last maximal 50 lines of a given file
     * Fails if the given file could not be read
     *
     * @param absolutePath - path where the file should be stored
     * @param fileName - name of the file
     * @return String - contents of the file
     */
    public String tailFile(String absolutePath, String filename) {
        return tailFile(absolutePath, filename, 20);
    }

    /**
     * Read the last specific amount of lines of a given file
     * Fails if the given file could not be read
     *
     * @param absolutePath - path where the file should be stored
     * @param fileName - name of the file
     * @return String - contents of the file
     * @param lines - number of lines to read
     * @return String - contents of the file
     */
    public String tailFile(String absolutePath, String filename, int lines) {
        // try to read given file
        String command;
        if (absolutePath == null || absolutePath.equals("")) {
            command = String.format("tail -n %s %s", lines, filename);
        } else {
            command = String.format("tail -n %s %s/%s", lines, absolutePath, filename);
        }
        mLogger.debug("Send command: " + command);
        String output = sendCommand(command);
        // check if file could be read
        if (output.contains("tail: cannot open") && output.contains(filename)
                && output.contains("for reading: " + FILE_OR_DIR_NOT_FOUND)) {
            throw new EcsTargetException("Given file could not be tail");
        }
        return output;
    }

    public String touchFile(String file) {
        return sendCommand(String.format(CMD_TOUCH_FORMAT, file));
    }

    /**
     * Transfers file from current target (cic/fuel) to LXC.
     *
     * @param absoluteSrcFilePath - String - absolute file path in current object
     * @param destinationFilePath - String - the location in LXC where the file is to be copied
     * @return boolean
     */
    public boolean transferFileToLxc(String absoluteSrcFilePath, String destinationFilePath) {
        if (!hasLxcConnectivity()) {
            throw new EcsTargetException(
                    ("Cannot copy file to LXC as there is no LXC connectivity from " + getHostname()));
        }
        mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, "from " + getHostname(),
                "to LXC :" + destinationFilePath);
        initializeSession();
        mSshSession.setNextPossiblePassword("Jumpstart");
        String result = sendCommand(
                "scp " + absoluteSrcFilePath + " lxcpxe@" + mLxc.getIpAddress() + ":" + destinationFilePath);
        if (!result.contains("100%")) {
            throw new EcsTargetException("copying file to LXC failed");
        }
        mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, "from " + getHostname(),
                "to LXC :" + destinationFilePath);
        return true;
    }

    /**
     * Wrapper for command "unmount"
     *
     * @param dir
     * @return
     */
    public boolean umountFileSystem(String dir) {
        String output = sendCommand(String.format(CMD_UMOUNT_FORMAT, dir));
        if (output.length() > 0) {
            throw new EcsTargetException("Unexpected return message detected, the umount command returned: " + output);
        }
        return true;
    }

    /**
     * Check that atd service is running by using "at" command
     *
     * @return true if service is running correctly
     */
    public boolean verifyAtdIsRunning() {
        String answerPrintout = sendCommand(VERIFY_ATD_CMD);
        deleteFile(sendCommand("pwd"), ATD_FILE);
        if (answerPrintout.contains(ATD_ERROR_MSG) || answerPrintout.contains("SERVICE_NOT_FOUND")) {
            mLogger.debug("Believe atd is not running due to answer print out: " + answerPrintout);
            return false;
        }
        if (!answerPrintout.contains("job")) {
            mLogger.debug(
                    "Believe that atd is not running because the answer print out did not contain the format: job nr date, but instead: "
                            + answerPrintout);
            return false;
        }
        return true;

    }

    /**
     * This method first waits for the os to go down within timeout and then waits for it to come up again within
     * timeout. This is verified by checking ssh connection status.
     * Throws runtime exception if not successful.
     *
     * @param cicReboot
     */
    public void waitAndVerifyRestart(Timeout cicReboot) {
        initializeSession();
        String hostName = mSshSession.getHostname();
        mLogger.info(EcsAction.WAITING, " ", hostName, "to go DOWN");
        setTargetWentOffline(false);
        String errorMessage = String.format("Was unable to verify restart of %s", hostName);
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(cicReboot, errorMessage, Boolean.TRUE, () -> {
            if (didTargetWentOffline()) {
                mLogger.info(EcsAction.RECONNECTING, " ", hostName, "");
                if (mSshSession.reconnect()) {
                    mLogger.info(Verdict.CONNECTED, " ", hostName, "");
                    return true;
                } else {
                    return false;
                }
            } else if (!mSshSession.isConnected()) {
                setTargetWentOffline(true);
                mLogger.info(Verdict.VALIDATED, " ", hostName, "is DOWN");
                return false;
            } else {
                return false;
            }
        });
        loopHelper.setIterationDelay(CommonParametersValues.ITERATION_DELAY_10_SEC);
        loopHelper.run();
        setTargetWentOffline(false);
    }

    /**
     * Wait until the process of a service is started
     *
     * @param service - String - name of service
     * @return true if process was started, otherwise false.
     */
    public void waitForProcess(final String service) {

        String errorMessage = String.format("Process for service %s has not been started", service);
        Timeout timeout = Timeout.PROCESS_READY;

        new LoopHelper<Boolean>(timeout, errorMessage, Boolean.TRUE, () -> {
            String processId = getProcessIdOfService(service);
            return (null != processId);
        }).run();
    }

    /**
     * Checks if service instance reaches specified status within default timeout limit. Exception will be thrown if the
     * service hasn't reached expected status within that timeout.
     *
     * @param service - String - Name of the service
     */
    public void waitForServiceRunningStatus(final String service) {
        String desiredStatus = "start/running";
        mLogger.info(EcsAction.STATUS_CHANGING, EcsTarget.class, service + ", Timeout:"
                + Timeout.PROCESS_READY.getTimeoutInSeconds() + "seconds. Target status: " + desiredStatus);
        String errorMessage = String.format("Service %s did not reach status: %s", service, desiredStatus);
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.PROCESS_READY, errorMessage, Boolean.TRUE,
                () -> {
                    if (!isServiceRunning(service)) {
                        mLogger.info(EcsAction.WAITING, EcsTarget.class,
                                "for the service " + service + " to change status " + desiredStatus);
                        return false;
                    }
                    return true;
                });
        loopHelper.setIterationDelay(10);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, desiredStatus, EcsTarget.class, service);
    }

    /**
     * Wait until the SSH connection is re-established (e.g. after killing SSH process)
     *
     * @return true if process was started and the current connection is connected again, otherwise false.
     */
    public boolean waitForSshConnection() {

        String errorMessage = String.format("SSH connection to node has not been re-established!");
        Timeout timeout = Timeout.SSH_RECONNECT;
        initializeSession();
        try {
            LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(timeout, errorMessage, Boolean.TRUE, () -> {
                mLogger.debug("Waiting for sshConnection to re-establish");
                return mSshSession.reconnect();
            });
            loopHelper.run();
        } catch (LoopTimeoutException ex) {
            mLogger.error(
                    "waitForSshConnection returned false because the ssh session was not connected within the time limit of s: "
                            + timeout.getTimeoutInSeconds());
            return false;
        } catch (Exception e) {
            mLogger.debug("Exception caught while waiting: " + e.getMessage());
            return false;
        }
        return true;
    }
}

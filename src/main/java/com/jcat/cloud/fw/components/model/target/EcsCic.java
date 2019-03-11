package com.jcat.cloud.fw.components.model.target;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.commonlibrary.remotecli.CliFactory;
import com.ericsson.commonlibrary.remotecli.Scp;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsCicException;
import com.jcat.cloud.fw.common.exceptions.EcsPingException;
import com.jcat.cloud.fw.common.exceptions.EcsSessionException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVm;
import com.jcat.cloud.fw.components.model.services.checker.EcsChecker;
import com.jcat.cloud.fw.components.model.services.database.EcsDatabaseClient;
import com.jcat.cloud.fw.components.system.cee.ecssession.CicSessionFactory;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.EcsAgent;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.EcsAgent.Type;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.services.backup.EcsBackupClient;
import com.jcat.cloud.fw.components.system.cee.services.checker.EcsExtremePeriodicChecker;
import com.jcat.cloud.fw.components.system.cee.services.crm.CrmService;
import com.jcat.cloud.fw.components.system.cee.services.database.EcsMysql;
import com.jcat.cloud.fw.components.system.cee.services.fee.EcsFeeService;
import com.jcat.cloud.fw.components.system.cee.services.rabbitmq.RabbitMqService;
import com.jcat.cloud.fw.components.system.cee.target.AstuteYamlParser;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;

/**
 * This class contains available functionality to a CIC running in the ECS.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2014-12-08 initial version
 * @author epergat 2015-01-05 Added databaseClient and auditor support
 * @author eqinann 2015-01-26 Moved getSystemVersion here
 * @author epergat 2015-03-18 Added ping methods
 * @author ehosmol Implement {@link #getStatus()}, Add {@link #getHostname()}
 * @author eelimei 2015-03-18 Add usage of EcsFeeService
 * @author ethssce 2015-03-19 added createUser(), deleteUser()
 * @author ezhgyin 2015-03-23 add method astuteYaml()
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author ehosmol 2015-04-20 modified {@link #createUser(String)} {@link #createUser(String, String)}
 *         {@link #changeUser(String)} {@link #deleteUser(String)} {@link #lazyInitialize()}
 * @author ehosmol 2015-05-18 Pass the EcsCic to the services
 * @author ethssce 2015-06-03 fixed exitUserSession()
 * @author eelimei 2015-06-12 Removed getTime and ChangeTime. The ChangeTime
 *         method did not work in the new sofware since the service used does
 *         not exist.
 * @author eelimei 2016-01-27 Add methods
 *         dataValuesProducesForComputeHostInLastPmReport and
 *         pmReportProducedAfterTimeStamp
 * @author zdagjyo 2016-11-24 Added enum CicService,modified pingVmOnAnyNetwork
 *         method
 * @author zdagjyo 2017-03-05 Removed method getDate and moved it to EcsTarget class
 * @author zpralak 2016-01-23 Added isQuorumLost method
 * @author zdagjyo 2017-03-18 Added method getRabbitMqService
 * @author zdagjyo 2017-04-27 Added method getEcsBackupFileName
 * @author zpralak 2017-04-31 Added getDotDataFilesCount and getGlanceFlavor methods
 * @author zpralak 2017-05-08 Added setGlanceFlavor
 * @author zpralak 2017-06-13 Added Enum GlanceFlavor
 * @author zdagjyo 2017-09-07 Moved method configYaml to EcsTarget, modified deinitialize method
 * @author zdagjyo 2017-10-16 Added methods doesAtlasImageExist, getAtlasVm, getFileFromAtlasVm,
 *         installAtlasVm and transferFileToAtlasVm
 * @author zdagjyo 2017-12-01 Moved methods execLabSetup and execLabSetupRestore from EcsOmtool to EcsCic
 * @author zdagjyo 2018-01-08 Added method isMongoDbReplicated
 * @author zdagjyo 2018-01-09 Added method getHostingCompute
 * @author zdagjyo 2018-02-12 Added method sendCommand
 * @author zmousar 2018-01-31 Added method getParameterValueFromLocalrcOfAtlas
 * @author zmousar 2018-03-06 Modified method isQuorumLost
 * @author zdagjyo 2019-01-28 Added method getOpenstackPassword
 */
public class EcsCic extends EcsTarget {

    /**
     * Describes the services running on CIC
     *
     */
    public enum CicService {
        RABBITMQ_MASTER, ZABBIX_SERVER;
    }

    /**
     * Describes the glance flavor
     */
    public static enum GlanceFlavor {
        KEYSTONE("keystone"), KEYSTONE_PLUS_CACHEMANAGEMENT("keystone+cachemanagement");

        private final String mFlavorType;

        GlanceFlavor(String flavorType) {
            mFlavorType = flavorType;
        }

        /**
         * Find the Glance Flavor with a given flavor type
         *
         * @param flavorType of Glance
         * @return GlanceFlavor object
         */
        public static GlanceFlavor withName(String flavorType) {
            if (flavorType.trim().equals("keystone")) {
                return GlanceFlavor.KEYSTONE;
            } else if (flavorType.trim().equals("keystone+cachemanagement")) {
                return GlanceFlavor.KEYSTONE_PLUS_CACHEMANAGEMENT;
            } else {
                throw new EcsCicException("Was not able to find the valid flavor type");
            }
        }

        public String getFlavorType() {
            return mFlavorType;
        }
    }

    /**
     * Describes which mode the CIC is.
     */

    public static enum Mode {
        FULL_OPERATIONAL, MAINTENANCE, UNKNOWN;
    }

    /**
     * Describes the state of the service
     *
     */
    public enum ServiceState {
        OFFLINE, ONLINE;
    }

    /**
     * Describes the power-state of the controller.
     */
    public static enum Status {
        ACTIVE, OFFLINE, STANDBY;
    }

    private static final String OPENSTACK_PASSWORD_CMD = "cat openrc|grep OS_PASSWORD=|awk '{print $2}'";
    private static final String ATLAS_IMAGE_DIRECTORY = "/root/artifacts";
    private static final String ATLAS_INSTALL_CMD = "./atlas_install.sh > %s &";
    private static final String ATLAS_INSTALL_LOG_FILE = "/root/artifacts/jcat_atlas_install_log.txt";
    private static final String ATLAS_INSTALL_SCRIPT_PATH = "/root/artifacts/atlas_install.sh";
    private static final String BACKUP_FILE_NAME = "ls /var/lib/glance/backup/cic_data_backup.%s|grep cic";
    private static final String CIC_SCALABILITY_DIRECTORY = "/var/lib/glance/images/.Scalability";
    private static final String CIC_STABILITY_DIRECTORY = "/var/lib/glance/images/.Stab";
    private final static String DATE_FORMAT_LS = "MMM dd HH:mm yyyy";
    private final static String ASTUTE_YAML_FILE_NAME = "astute.yaml";
    private final static String ASTUTE_YAML_REMOTE_PATH = "/etc/";
    private static final String CHECK_GLANCE_API_CMD = "cat /etc/glance/glance-api.conf | grep 'flavor =' | awk {'print $3'}";
    private static final String CIC_EXCEPTION_MESSAGE = "Was not able to successfully execute command \"%s\" for user \"%s\". System returned: \"%s\"";
    private final static String CMD_ADD_USER_TO_SUDO_GROUP = "sudo cee-idam user-modify -G sudo -l ";
    private static final String CMD_CHECK_HOMEFOLDER_DELETE = "cd /home/";
    private final static String CMD_COROSYNC_START = "service corosync start";
    private final static String CMD_COROSYNC_STATUS = "service corosync status";
    private static final String CMD_HOMEFOLDER_DELETE = "sudo rm -rf /home/";
    private final static String CMD_DATA_FILES_COUNT = "du -ah /var/lib/glance/node | grep -c data";
    private static final String CMD_IDAM_CHANGE_INITIAL_PASSWORD = "sudo cee-idam user-modify -p '%s' -l %s";
    private static final String CMD_PM_REPORT_LATEST_REPORT = "ls -aogFt /var/cache/pmreports/A*.xml | head -1 | %s";
    private static final String CMD_PM_REPORT_COMPUTE_RESTART = "awk '/%s/,/<\\/measValue>/' %s";
    private final static String CMD_MODE_QRY = "umm status";
    private final static String CMD_USER_CREATE = "sudo cee-idam user-create ";
    private static final String CMD_USER_DELETE = "sudo cee-idam user-delete -l ";
    private final static String EXPECTED_OUTPUT_COROSYNC_START = "Starting corosync daemon corosync";
    private final static String EXPECTED_OUTPUT_COROSYNC_STATUS = "corosync is running";
    private final static String MODIFY_FLAVOR_TYPE_IN_GLANCE_CONF_CMD = "sed -i 's/flavor = %s/flavor = %s/g' /etc/glance/glance-api.conf";
    private static final String LAB_SETUP_SCRIPT = "lab_setup.py";
    private static final String ABSOLUTE_PATH_FOR_LAB_SETUP = "/var/lib/glance/tools/lab_setup/";
    private static AstuteYamlParser mAstuteYamlParser;
    // Shared among all the EcsCic instances
    private static Map<String, EcsUser> mUsers = new HashMap<String, EcsUser>();
    private static final String RESPONSE_ALREADY_IN_GROUP = "already in group";
    private static final String RESPONSE_DELETED_FOLDER = "No such file or directory";
    private static final String RESPONSE_ERROR = "ERROR";
    private static final String RESPONSE_IDAM_SUCCESS = "SUCCESS";
    private static final String RESPONSE_TAKEN = "already taken";
    private final static String STATUS_OUTPUT_MAINTENANCE = "umm";
    private final static String STATUS_OUTPUT_OPERATIONAL = "runlevel N 5";
    private final static String CMD_MONGO_DB_CREDS = "cat /root/.mongorc.js|grep \"db.auth\"";
    private final static String CMD_MONGODB_PRIMARY = "mongo --username %s --password %s admin --eval 'printjson(db.runCommand( { replSetGetStatus : 1 } ))' | grep 'id\\|stateStr\\|name'|grep PRIMARY|wc -l";
    private final static String CMD_MONGODB_SECONDARY = "mongo --username %s --password %s admin --eval 'printjson(db.runCommand( { replSetGetStatus : 1 } ))' | grep 'id\\|stateStr\\|name'|grep SECONDARY|wc -l";
    private final List<EcsBackupClient> mBackupClients;
    private EcsChecker mChecker = null;
    private EcsMysql mDbClient;
    private EcsFeeService mFeeService = null;
    private CrmService mCrmService = null;
    private RabbitMqService mRabbitMqService = null;
    private final String mIpAddress;
    private final int mPort;
    private EcsAtlasVm mAtlasVm;
    @Inject
    private CicSessionFactory mCicSessionFactory;

    // This user comes from openstack resource in tass and just used by
    // framework functions
    private final String mUsername;
    private final String mPassword;

    private boolean mIsNestedUserSession;

    private final EcsLogger mLogger = EcsLogger.getLogger(EcsCic.class);

    @Inject
    private NeutronController mNeutronController;

    @Inject
    private NovaController mNovaController;

    @Inject
    protected EcsCicList mEcsCicList;

    @Inject
    protected EcsComputeBladeList mEcsComputeBladeList;

    @Inject
    public EcsCic(@Assisted("username") String userName, @Assisted("password") String password,
            @Assisted("ipAddress") String ipAddress, @Assisted("port") int port) {
        mUsername = userName;
        mPassword = password;
        mIpAddress = ipAddress;
        mPort = port;
        mBackupClients = new ArrayList<EcsBackupClient>();
        mIsNestedUserSession = false;
    }

    /**
     * Exits current user session. should preferably be used when having changed
     * from one user session to another before using changeUser()
     */
    private void exitUserSession() {
        String response = mSshSession.send("exit");
        mLogger.debug("Response after running command exit: " + response);
        String user = mSshSession.send("whoami");
        mLogger.debug("user after running command exit: " + user);
        if (!response.matches(mSshSession.getCurrentPrompt()) && !user.equals(mUsername)) {
            throw new EcsSessionException(
                    "Something went wrong exiting users \"" + getUser() + "\" session: " + response);
        }
    }

    /**
     * Retrieves MongoDB login password.
     * Command used:
     * root@cic-1:~# cat /root/.mongorc.js|grep "db.auth"
     * db.auth('admin', 'rnbbeisNwUn51q9FqKV3YXWy')
     *
     * @return - String - the password of MongoDB
     */
    private String getMongoDbPassword() {
        String result = mSshSession.send(CMD_MONGO_DB_CREDS);
        Matcher matcher = Pattern.compile("'(.*?)'(.*)'(.*?)'").matcher(result);
        if (matcher.find()) {
            return matcher.group(3);
        }
        throw new EcsCicException("Failed to retrieve MongoDB password, command returned " + result);
    }

    /**
     * Retrieves MongoDB login username.
     * Command used:
     * root@cic-1:~# cat /root/.mongorc.js|grep "db.auth"
     * db.auth('admin', 'rnbbeisNwUn51q9FqKV3YXWy')
     *
     * @return - String - the username of MongoDB
     */
    private String getMongoDbUserName() {
        String result = mSshSession.send(CMD_MONGO_DB_CREDS);
        Matcher matcher = Pattern.compile("'(.*?)'(.*)'(.*?)'").matcher(result);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new EcsCicException("Failed to retrieve MongoDB username, command returned " + result);
    }

    /**
     * Populates the mUsers map with initial system + cee-idam users IF mUsers is empty.
     */
    private void initializeCurrentIdamAndSystemUsers() {
        if (mUsers.isEmpty()) {
            for (EcsUser ecsUser : getSystemUsers()) {
                mUsers.put(ecsUser.getUsername(), ecsUser);
            }

            for (EcsUser ecsUser : getCeeIdamUsers()) {
                mUsers.put(ecsUser.getUsername(), ecsUser);
            }
        }
    }

    /**
     * @param newMode
     * @return true if the current mode is equal to desiredMode
     */
    private boolean isCurrentMode(Mode desiredMode) {
        return getMode() == desiredMode;
    }

    private void setFullOperationalMode() {
        if (getMode().equals(Mode.FULL_OPERATIONAL)) {
            throw new EcsCicException(
                    "Can't set CIC to full operational mode since it's already in full operational mode");
        }
        mSshSession.send("umm off");
        waitForControllerOperationalMode();
    }

    private void setMaintenanceMode() {
        mLogger.info(EcsAction.STATUS_CHANGING, "UMM", getHostname(), "into Maintenance Mode");
        if (getMode().equals(Mode.MAINTENANCE)) {
            throw new EcsCicException("Can't set CIC to maintenance mode since it's already in maintenance mode");
        }
        mSshSession.send("umm on");
        waitAndVerifyRestart(Timeout.ECS_OS_RESTART);
        if (!isCurrentMode(Mode.MAINTENANCE)) {
            throw new EcsCicException("CIC did not reach maintenance mode successfully");
        }
    }

    /**
     * This method assumes that the controller is in maintenance mode and waits
     * for it to reach full operational mode within timeout period
     */
    private void waitForControllerOperationalMode() {
        try {
            new LoopHelper<Mode>(Timeout.CIC_ONLINE_STATUS_CHANGE,
                    "Was not able to verify that cic reached online status", Mode.FULL_OPERATIONAL, () -> getMode())
                            .run();
        } catch (Exception e) {
            mLogger.error(e.getMessage());
            throw new EcsCicException("Was not able to verify that cic reached online status within timeout period");
        }
    }

    /**
     * Get access to astute yaml parser
     *
     * @return AstuteYamlParser
     */
    public AstuteYamlParser astuteYaml() {
        if (null == mAstuteYamlParser) {
            if (!mUsername.equals("root")) {
                throw new EcsCicException("Astute Yaml requires ROOT access, current user: " + mUsername);
            }
            mAstuteYamlParser = new AstuteYamlParser(getRemoteFile(ASTUTE_YAML_REMOTE_PATH, ASTUTE_YAML_FILE_NAME));
        }
        return mAstuteYamlParser;
    }

    /**
     * Change the current user. Method will ignore changes to same user and
     * always return to root first before changing to another user. It always
     * automatically prefix the username with " jcat_ ", if it is not a system
     * user.
     *
     * @param -
     *        String - new user
     * @return boolean - true if user has been changed successfully
     */
    @Override
    public boolean changeUser(String userName) {
        initializeCurrentIdamAndSystemUsers();
        if (mIsNestedUserSession) {
            // Set back to the initial user first, as it might have the highest access level
            exitUserSession();
            mIsNestedUserSession = false;
        }
        String currentUsername = getUser();
        mLogger.info(EcsAction.CHANGING, EcsUser.class.getSimpleName(), getHostname(), userName);
        if (mUsers.containsKey(userName)) {
            if (!userName.equals(currentUsername)) {
                // if the new user is the same as the current user, then we don't need to change, other
                // wise change to one of the system users
                mSshSession.send("sudo -i -u " + userName);
                mIsNestedUserSession = true;
                if (getUser().matches(userName)) {
                    mLogger.info(Verdict.CHANGED, EcsUser.class.getSimpleName(), getHostname(), userName);
                    mSshSession.setCurrentUser(mUsers.get(userName));
                    return true;
                }
            } else {
                mLogger.info(Verdict.CHANGED, EcsUser.class.getSimpleName(), getHostname(), userName);
                return true;
            }

        } else if (mUsers.containsKey(JCAT_USER_PREFIX + userName)) {
            String jcatUserName = JCAT_USER_PREFIX + userName;
            mSshSession.send("sudo -i -u " + jcatUserName);
            mIsNestedUserSession = true;
            if (getUser().matches(jcatUserName)) {
                mLogger.info(Verdict.CHANGED, EcsUser.class.getSimpleName(), getHostname(),
                        jcatUserName + " All users created by Jcat FW are prefixed with 'jcat_' for traceable reasons");
                mSshSession.setCurrentUser(mUsers.get(jcatUserName));
                return true;
            }
            mLogger.error("JCAT user " + jcatUserName + " change failed");
        }

        throw new EcsCicException("User " + userName + " does not exist");
    }

    /**
     * Create/Update a user for sudo group on CIC. Throws exception if not
     * successful. If user exist will try to update the user group and password.
     * It always automatically prefix the username with " jcat_ ", if it is not
     * a system user.
     *
     * @param userName
     *            - name for new user to be created
     * @param password
     *            - password for user
     */
    public void createUser(String userName, String password) {
        initializeCurrentIdamAndSystemUsers();
        if (mUsers.containsKey(userName)) {
            if (mUsers.get(userName).isSystemUser()) {
                mLogger.info(Verdict.EXISTED, EcsUser.class.getSimpleName(), getHostname(),
                        userName + " is a system user and already exist");
                return;
            }
        }
        String jcatUserName = JCAT_USER_PREFIX + userName;
        mLogger.info(EcsAction.CREATING, EcsUser.class.getSimpleName(), getHostname(),
                userName + " actual name: " + jcatUserName);
        String response = mSshSession.send(CMD_USER_CREATE + jcatUserName);
        if (!response.contains(RESPONSE_IDAM_SUCCESS) && !response.contains(RESPONSE_TAKEN)) {
            throw new EcsCicException(String.format(CIC_EXCEPTION_MESSAGE, CMD_USER_CREATE, jcatUserName, response));
        }
        response = mSshSession.send(CMD_ADD_USER_TO_SUDO_GROUP + jcatUserName);
        if (!response.contains(RESPONSE_IDAM_SUCCESS) && !response.contains(RESPONSE_ALREADY_IN_GROUP)) {
            throw new EcsCicException(
                    String.format(CIC_EXCEPTION_MESSAGE, CMD_ADD_USER_TO_SUDO_GROUP, jcatUserName, response));
        }
        response = mSshSession.send(String.format(CMD_IDAM_CHANGE_INITIAL_PASSWORD, password, jcatUserName));
        if (!response.contains(RESPONSE_IDAM_SUCCESS)) {
            throw new EcsCicException(String.format(
                    CIC_EXCEPTION_MESSAGE
                            + "Make sure the password chosen contains enough complexity (There are very specific requirements on pwd complexity) and, if updated, is not the same as previous password",
                    CMD_IDAM_CHANGE_INITIAL_PASSWORD, jcatUserName, response));
        }
        mLogger.info(Verdict.CREATED, EcsUser.class.getSimpleName(), getHostname(),
                jcatUserName + " All users created by Jcat FW are prefixed with 'jcat_' for traceable reasons");
        mUsers.put(jcatUserName, new EcsUser(jcatUserName, password, false));
    }

    /**
     * Check to see if the last pm report contains meas data values for the
     * provided compute host. Note: This meathod has to be used on the cic that
     * is the active_mark cic.
     *
     * @param mHostName
     * @return
     */
    public boolean dataValuesProducesForComputeHostInLastPmReport(String mHostName) {
        if (!getCrmService().activeMark().equals(mSshSession.getHostname())) {
            throw new EcsCicException("Method is not allowed for other cics than the active_mark cic");
        }
        String latestReportPath = mSshSession.send(String.format(CMD_PM_REPORT_LATEST_REPORT, "awk '{print $7}'"));
        mLogger.warn("Last pm report path: " + latestReportPath);
        String computeRestart = mSshSession
                .send(String.format(CMD_PM_REPORT_COMPUTE_RESTART, mHostName, latestReportPath));
        if (computeRestart.contains("<r p=")) { // We have some xml values in
            // the output
            return true;
        } else {
            return false;
        }
    }

    /*
     * {@inheritDoc}
     * @see ecs.jcat.lib.controllers.EcsComponent#deinitialize
     */
    @Override
    public Boolean deinitialize() {
        if (mUsers.size() > 0) {
            Map<String, EcsUser> users = new HashMap<String, EcsUser>();
            users.putAll(mUsers);
            for (String user : users.keySet()) {
                if (!users.get(user).isSystemUser()) {
                    if (user.contains(JCAT_USER_PREFIX)) {
                        user = user.substring(user.indexOf("_") + 1);
                    }
                    deleteUser(user);
                }
            }
        }

        if (mDbClient != null) {
            mDbClient.deinitialize();
        }
        for (EcsBackupClient client : mBackupClients) {
            client.deinitialize();
        }
        if (getMode().equals(Mode.MAINTENANCE)) {
            mLogger.debug("The CIC is in maintenance mode, will switch back to full operational");
            // The Cic will always be reset to fully operational mode.
            setMode(Mode.FULL_OPERATIONAL);
        }
        if (mSshSession != null) {
            mSshSession.disconnect();
        }
        return true;
    }

    /**
     * Deletes an existing user on CIC and its home folder. Throws exception if
     * deletion fails or user does not exist. It always automatically prefix the
     * user name with " jcat_ ".
     *
     * @param userName
     *            - name for new user to be deleted
     */
    public void deleteUser(String userName) {
        initializeCurrentIdamAndSystemUsers();
        if (mUsers.containsKey(userName)) {
            if (mUsers.get(userName).isSystemUser()) {
                mLogger.warn("User " + userName + " is a system user and can not be deleted");
                return;
            }
        } else if (mUsers.containsKey(JCAT_USER_PREFIX + userName)) {
            String jcatUserName = JCAT_USER_PREFIX + userName;
            mLogger.info(EcsAction.DELETING, EcsUser.class.getSimpleName(), getHostname(), jcatUserName);
            String response = mSshSession.send(CMD_USER_DELETE + jcatUserName);
            if (!response.contains(RESPONSE_IDAM_SUCCESS) || response.contains(RESPONSE_ERROR)) {
                throw new EcsCicException(
                        String.format(CIC_EXCEPTION_MESSAGE, CMD_USER_DELETE, jcatUserName, response));
            }
            mSshSession.send(CMD_HOMEFOLDER_DELETE + jcatUserName);
            response = mSshSession.send(CMD_CHECK_HOMEFOLDER_DELETE + jcatUserName);
            if (!response.contains(RESPONSE_DELETED_FOLDER)) {
                throw new EcsCicException(
                        String.format(CIC_EXCEPTION_MESSAGE, CMD_CHECK_HOMEFOLDER_DELETE, jcatUserName, response));
            }
            mLogger.info(Verdict.DELETED, EcsUser.class.getName(), getHostname(),
                    jcatUserName + " (All users created by Jcat FW are prefixed with 'jcat_' for traceable reasons)");
            mUsers.remove(jcatUserName);
        } else {
            throw new EcsCicException("User " + userName + " does not exist and can not be deleted.");
        }
    }

    /**
     * Checks if the current cic has atlas image downloaded.
     *
     * @return - boolean - true if atlas image exists on this cic.
     */
    public boolean doesAtlasImageExist() {
        return doesDirectoryExist(ATLAS_IMAGE_DIRECTORY) && doesFileExist(ATLAS_INSTALL_SCRIPT_PATH);
    }

    /**
     * Returns true if user exist. It always automatically prefix the user name
     * with " jcat_ ".
     *
     * @param username
     *            - user to query for
     * @return true - If user exist.
     */
    public boolean doesUserExist(String username) {
        initializeCurrentIdamAndSystemUsers();
        if (mUsers.containsKey(username)) {
            if (mUsers.get(username).isSystemUser()) {
                return true;
            }
        } else if (mUsers.containsKey(JCAT_USER_PREFIX + username)) {
            String response = mSshSession.send("sudo -i -u " + JCAT_USER_PREFIX + username);
            if (response.contains("unknown user")) {
                return false;
            }
            exitUserSession();
            return true;
        }
        return false;
    }

    /**
     * Executes lab_setup.py script
     * @throws FileNotFoundException
     */
    public void execLabSetup() throws FileNotFoundException {
        if (!doesFileExist(ABSOLUTE_PATH_FOR_LAB_SETUP + LAB_SETUP_SCRIPT)) {
            throw new FileNotFoundException(
                    "Could not execute script as the file lab_setup.py is not found on cic " + getHostname());
        }
        sendCommand("python " + ABSOLUTE_PATH_FOR_LAB_SETUP + LAB_SETUP_SCRIPT);
    }

    /**
     * Executes lab_setup.py script with restore argument to restore configuration
     * @throws FileNotFoundException
     */
    public void execLabSetupRestore() throws FileNotFoundException {
        if (!doesFileExist(ABSOLUTE_PATH_FOR_LAB_SETUP + LAB_SETUP_SCRIPT)) {
            throw new FileNotFoundException(
                    "Could not execute script as the file lab_setup.py is not found on cic " + getHostname());
        }
        sendCommand("python " + ABSOLUTE_PATH_FOR_LAB_SETUP + LAB_SETUP_SCRIPT + " -restore");
    }

    /**
     * Returns atlas vm installed on this cic
     *
     * @return EcsAtlasVm
     */
    public EcsAtlasVm getAtlasVm() {
        if (mAtlasVm == null) {
            installAtlasVm();
        }
        return mAtlasVm;
    }

    /**
     * get the backup client that is installed on cics. backup client always
     * uses the same user "ceeadm".
     *
     * @return
     */
    public EcsBackupClient getBackupClient() {
        EcsBackupClient backupClient = new EcsBackupClient(mSshSession, this);
        mBackupClients.add(backupClient);
        return backupClient;
    }

    /**
     * Returns list of the cee-idam users of the target under test, check for
     * possible password in the yaml file
     *
     * @return List<{@link EcsUser}> - List of users
     */
    public List<EcsUser> getCeeIdamUsers() {
        List<EcsUser> ceeIdamUsers = new ArrayList<EcsUser>();

        String response = mSshSession.send("sudo cee-idam user-list");
        List<String> ceeIdamUsersResponse = new ArrayList<String>(Arrays.asList(response.split("\r\n")));
        int firstItemIndex = 0;
        ceeIdamUsersResponse.remove(firstItemIndex); // Remove two first lines, they are headers
        ceeIdamUsersResponse.remove(firstItemIndex);

        Map<String, String> yamlUsers = configYaml().getUsers();
        for (String ceeIdamUser : ceeIdamUsersResponse) {
            ceeIdamUser = ceeIdamUser.trim();
            ceeIdamUser = ceeIdamUser.split("\\s+")[1];
            if (yamlUsers.containsKey(ceeIdamUser)) {
                ceeIdamUsers.add(new EcsUser(ceeIdamUser, yamlUsers.get(ceeIdamUser), true));
            } else {
                ceeIdamUsers.add(new EcsUser(ceeIdamUser, null, false));
            }
        }

        return ceeIdamUsers;
    }

    /**
     *
     * @return
     */
    public EcsChecker getChecker() {
        if (mChecker == null) {
            mChecker = new EcsExtremePeriodicChecker(mSshSession, this);
        }
        return mChecker;
    }

    /**
     * @return crm service instance
     */
    public CrmService getCrmService() {
        if (null == mCrmService) {
            mCrmService = new CrmService(mSshSession, mEcsCicList);
        }
        return mCrmService;
    }

    /**
     * Returns a specific database from the CIC or null if the database couldn't
     * be found.
     *
     * @param name
     * @return
     */
    public EcsDatabaseClient getDatabaseClient() {
        if (mDbClient != null) {
            return mDbClient;
        }

        String database = mSshSession.send("which mysql");

        if (database.contains("mysql")) {
            mDbClient = new EcsMysql(mSshSession, this);
            return mDbClient;
        }

        throw new EcsCicException("The cic with ip: " + mIpAddress + " does not have a database mysql installed");
    }

    /**
     * Method to get Number of dot data files
     *
     * @return Number of dot data files in Swift folder in each CIC.
     */
    public int getDotDataFilesCount() {
        return Integer.parseInt(mSshSession.send(String.format(CMD_DATA_FILES_COUNT)));
    }

    /**
     * Gets the file name of the specified backup.
     *
     * @param backupNumber - int - the backup number(0 if it is the first backup, 1 if second..)
     * @return String - the name of the backup file
     */
    public String getEcsBackupFileName(int backupNumber) {
        String result = mSshSession.send(String.format(BACKUP_FILE_NAME, backupNumber)).trim();
        if (result.contains("No such file or directory")) {
            throw new EcsCicException("The cic " + getHostname() + " does not have the specified backup file");
        }
        return result;
    }

    /**
     *
     * @return fee service instance
     */
    public EcsFeeService getFeeService() {
        if (mFeeService == null) {
            mFeeService = new EcsFeeService(mSshSession);
        }
        return mFeeService;
    }

    /**
     * Transfers file from atlas vm to cic
     *
     * @param srcFilePath - String - the source file path
     * @param fileName - String - the name of the file
     * @return - boolean
     */
    public boolean getFileFromAtlasVm(String srcFilePath, String fileName) {
        EcsAtlasVm atlasVm = getAtlasVm();
        mLogger.info(EcsAction.TRANSFERING, srcFilePath + fileName, EcsAtlasVm.class, "to " + getHostname());
        final String cmdRemoveFromKnownHosts = "ssh-keygen -R " + atlasVm.getVmIP();
        // SSH remove atlas "vmIp" from known_hosts file
        mLogger.info(EcsAction.SENDING, EcsCic.class, "cmd: " + cmdRemoveFromKnownHosts);
        String response = mSshSession.send(cmdRemoveFromKnownHosts);
        mLogger.info(Verdict.RECEIVED, EcsCic.class, response);
        mSshSession.setNextPossiblePassword("qwqwqw");
        String result = mSshSession
                .send("scp " + atlasVm.getUser() + "@" + atlasVm.getVmIP() + ":" + srcFilePath + fileName + " .");
        if (!result.contains("100%")) {
            throw new EcsCicException("copying file from atlas VM failed");
        }
        mLogger.info(Verdict.TRANSFERED, srcFilePath + fileName, EcsAtlasVm.class, "to " + getHostname());
        return true;
    }

    /**
     * Method to get glance flavor.
     */
    public GlanceFlavor getGlanceFlavor() {
        String flavorType = mSshSession.send(CHECK_GLANCE_API_CMD).split("\n")[0];
        return GlanceFlavor.withName(flavorType);
    }

    /**
     * Retrieves the compute blade that hosts the current cic.
     *
     * @return - EcsComputeBlade
     */
    public EcsComputeBlade getHostingCompute() {
        String cicName = null;
        Matcher matcher = Pattern.compile("((.*)-.)(.*)").matcher(getHostname());
        if (matcher.find()) {
            cicName = matcher.group(1);
        }
        if (cicName == null) {
            throw new EcsCicException("Failed to retrieve cic name from " + getHostname());
        }
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            if (blade.sendCommand("virsh list").contains(cicName)) {
                return blade;
            }
        }
        return null;
    }

    /**
     * Returns the cic's password defined on TASS-Openstack resource
     * if initilized user is root, return null
     *
     * @return password for non-root user and null for root user
     */
    public String getInitialPassword() {
        if (mUsername == "root") {
            return null;
        }
        return mPassword;
    }

    /**
     * Returns the cic's username defined on TASS-Openstack resource
     *
     * @return the username
     */
    public String getInitialUsername() {
        return mUsername;
    }

    /**
     *
     * @return
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     *
     * @return enum current Mode of CIC
     */
    public Mode getMode() {
        String output = mSshSession.send(CMD_MODE_QRY);
        if (output.equals("runlevel unknown")) {
            return Mode.UNKNOWN;
        } else if (output.equals(STATUS_OUTPUT_OPERATIONAL)) {
            return Mode.FULL_OPERATIONAL;
        } else if (output.equals(STATUS_OUTPUT_MAINTENANCE)) {
            return Mode.MAINTENANCE;
        } else {
            mLogger.debug("Result from " + CMD_MODE_QRY + " was " + output);
            throw new EcsCicException("Was not able to determine CIC mode based on command output");
        }
    }

    public String getNetworkNamespace(final String networkId) {
        final String cmd = "ip netns list | grep " + networkId;
        try {
            new LoopHelper<Boolean>(Timeout.NETWORK_NAMESPACE_READY,
                    "Was not able to find the network ns for netid: " + networkId, true, () -> {
                        String netns = mSshSession.send(cmd);
                        return netns.contains(networkId);
                    }).run();
        } catch (LoopTimeoutException ex) {
            throw new EcsPingException("Network namespace (id='" + networkId + "') was not found");
        }
        String netns = mSshSession.send(cmd);
        return netns;
    }

    /**
     * Retrieves the openstack admin password
     *
     * @return - String - the openstack admin password
     */
    public String getOpenstackPassword(){
        String result = sendCommand(OPENSTACK_PASSWORD_CMD);
        Pattern pattern = Pattern.compile("'(\\w*\\d*)'");
        Matcher match = pattern.matcher(result);
        if (match.find()) {
            return match.group(1);
        }
        throw new EcsTargetException("Failed to retrieve openstack password from openrc file");
    }

    /**
     * Retrieves the parameter value from localrc under /artifacts/ on CIC (Atlas image should exist on cic)
     *
     * @param parameter - parameter name whose value to be retrieved from localrc file
     * @return String - if parameter exists return its value otherwise Exception
     */
    public String getParameterValueFromLocalrcOfAtlas(String parameterName) {
        String fetchParameter = "";
        String fileContent = readFile("", "/root/artifacts/localrc");
        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            Matcher matcher = Pattern.compile("([${][A-Z_=]+[0-9A-Za-z]+)").matcher(line);
            if (matcher.find()) {
                fetchParameter = line.substring(matcher.start() + 1, matcher.end());
                if (fetchParameter.substring(0, fetchParameter.indexOf("=")).equals(parameterName)) {
                    return fetchParameter.substring(fetchParameter.indexOf("=") + 1);
                }
            }
        }
        throw new EcsCicException("Given " + parameterName + " is not found in localrc file");
    }

    public int getPort() {
        return mPort;
    }

    /**
     * @return RabbitMQ service instance
     */
    public RabbitMqService getRabbitMqService() {
        if (null == mRabbitMqService) {
            mRabbitMqService = new RabbitMqService(mSshSession, mEcsCicList);
        }
        return mRabbitMqService;
    }

    /**
     * method which copies a remote file from cic to local
     *
     * @param path
     *            - the remote file path
     * @return a File represents the copied local file
     */
    public File getRemoteFile(String remotePath, String fileName) {
        return super.getRemoteFile(mIpAddress, mUsername, mPassword, mPort, remotePath, fileName);
    }

    /**
     * Returns the status of the CIC with respect to the OpenVswitch-agent status.
     *
     * @return
     */
    public Status getStatus() {
        List<EcsAgent> agents = mNeutronController.agentList();

        for (EcsAgent agent : agents) {
            if (agent.getAgentType() == Type.OPEN_VSWITCH && agent.getAlive() == true) {
                if (agent.getHost().equals(getHostname())) {
                    return Status.ACTIVE;
                }
            }
        }
        return Status.STANDBY;
    }

    /**
     * Returns list of the system users of the target under test, check for
     * possible password in the yaml file
     *
     * @return List<{@link EcsUser}> - List of users
     */
    public List<EcsUser> getSystemUsers() {
        String response = mSshSession.send("awk -F':' '{ print $1}' /etc/passwd");
        List<String> systemUsers = Arrays.asList(response.split("\r\n"));
        List<EcsUser> ecsSystemUsers = new ArrayList<EcsUser>();
        Map<String, String> yamlUsers = configYaml().getUsers();
        for (String systemUser : systemUsers) {
            if (yamlUsers.containsKey(systemUser)) {
                ecsSystemUsers.add(new EcsUser(systemUser, yamlUsers.get(systemUser), true));
            } else {
                ecsSystemUsers.add(new EcsUser(systemUser, null, true));
            }
        }
        for (String username : yamlUsers.keySet()) {
            if (!systemUsers.contains(username)) {
                ecsSystemUsers.add(new EcsUser(username, yamlUsers.get(username), false));
            }
        }
        return ecsSystemUsers;
    }

    /**
     * Return the password associated with the user
     *
     * @param user
     * @return String - password
     */
    public String getUserPassword(String userName) {
        initializeCurrentIdamAndSystemUsers();
        if (mUsers.containsKey(userName)) {
            return mUsers.get(userName).getPassword();
        } else if (mUsers.containsKey(JCAT_USER_PREFIX + userName)) {
            return mUsers.get(JCAT_USER_PREFIX + userName).getPassword();
        }
        throw new EcsCicException("The user " + userName + "can not be found in the system");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLxcConnectivity() {
        return pingIp(mLxc.getIpAddress(), null);
    }

    /**
     * Installs atlas vm on node (Atlas image should exist on cic)
     *
     * @return String - the id of installed Atlas VM
     */
    public String installAtlasVm() {
        // check if atlas vm is already installed on node
        String serverId = mNovaController.getVmIdByName("atlas_vm");
        // install if it is not installed yet
        if (serverId == null) {
            mLogger.info(EcsAction.INSTALLING, EcsAtlasVm.class, "");
            changeDirectory(ATLAS_IMAGE_DIRECTORY);
            // execute atlas installation script
            sendCommand(String.format(ATLAS_INSTALL_CMD, ATLAS_INSTALL_LOG_FILE));
            // wait for atlas installation to complete
            new LoopHelper<Boolean>(Timeout.INSTALL_ATLAS_VM, "Atlas installation doesn't seem to finish in time",
                    Boolean.TRUE, () -> {
                        if (doesFileExist(ATLAS_INSTALL_LOG_FILE)) {
                            String content = readFile("", ATLAS_INSTALL_LOG_FILE);
                            if (content.contains("Installed On")) {
                                return true;
                            }
                        } else {
                            mLogger.warn("Atlas installation has not started yet");
                            return false;
                        }
                        mLogger.warn("Atlas installation is still in progress");
                        return false;
                    }).setIterationDelay(60).run();
            deleteFile("", ATLAS_INSTALL_LOG_FILE);
            serverId = mNovaController.getVmIdByName("atlas_vm");
        } else {
            mLogger.warn("Atlas VM is already installed on node");
        }
        if (serverId == null) {
            throw new EcsCicException("Atlas VM deployment failed");
        }
        mAtlasVm = mNovaController.getAtlasVm();
        return serverId;
    }

    /**
     * Checks if MongoDB is properly replicated on all vCics(1 primary and 2 secondary members)
     *
     * @return - boolean
     */
    public boolean isMongoDbReplicated() {
        String primaryCount = mSshSession
                .send(String.format(CMD_MONGODB_PRIMARY, getMongoDbUserName(), getMongoDbPassword()));
        String secondaryCount = mSshSession
                .send(String.format(CMD_MONGODB_SECONDARY, getMongoDbUserName(), getMongoDbPassword()));
        return primaryCount.equals("1") && secondaryCount.equals("2");
    }

    /**
     * checks if Quorum is lost after reboot of vCics by taking the time when pacemaker.logs are updated with "Quorum lost"
     * or "Quorum acquired" statement as reference.
     *
     * @param timeBeforeReboot
     *                     - Time before rebooting vCics
     * @return boolean
     */
    public boolean isQuorumLost(LocalDateTime timeBeforeReboot) {
        /**
         * Command to retrive the lines that contains "quorum lost" or "quorum acquired" from pacemaker log files
         * Ex : root@cic-1:~# grep -iw 'quorum lost\|quorum acquired' /var/log/pacemaker.log*
         *      /var/log/pacemaker.log:Nov 08 11:08:47 [22087] cic-2.domain.tld pacemakerd:   notice: pcmk_quorum_notification: Membership 20: quorum lost(1)
         *      /var/log/pacemaker.log.1:Nov 08 11:36:30 [8252] cic-2.domain.tld pacemakerd:   notice: pcmk_quorum_notification: Membership 32: quorum lost (1)
         *      /var/log/pacemaker.log.1:Nov 08 11:50:34 [8002] cic-2.domain.tld pacemakerd:   notice: pcmk_quorum_notification: Membership 48: quorum acquired (1)
         */
        String pacemakerLogResult = mSshSession
                .send("grep -iw 'quorum lost\\|quorum acquired' /var/log/pacemaker.log*");
        if (!pacemakerLogResult.isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
            String year = dateFormat.format(getDate());
            String[] lines = pacemakerLogResult.split("\n");
            for (String line : lines) {
                String dateTime = null;
                /**
                 * Regex to retrive the time stamp from the above command result ("/var/log/pacemaker.log:Nov 08 11:08:47 [22087]
                 * cic-2.domain.tld pacemakerd:
                 * notice: pcmk_quorum_notification: Membership 20: quorum lost(1)")
                 * Ex : Nov 08 11:08:47
                 *
                 */
                Matcher matcher = Pattern.compile("([A-Za-z]+\\s\\d+\\s\\d+:\\d+:\\d+)").matcher(line);
                if (matcher.find()) {
                    dateTime = matcher.group().trim() + " " + year.trim();
                    DateTimeFormatter dTF = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss uuuu");
                    LocalDateTime logTime = LocalDateTime.parse(dateTime.trim(), dTF);
                    if (timeBeforeReboot.isBefore(logTime) || timeBeforeReboot.isEqual(logTime)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * {@inheritDoc}
     * @see ecs.jcat.lib.controllers.EcsComponent#initialize
     */
    @Override
    public Boolean lazyInitialize() {
        mSshSession = mCicSessionFactory.create(mUsername, mPassword, mIpAddress, mPort);
        return true;
    }

    /**
     * Returns true if any pm report has been produces since the provided
     * timestamp.
     *
     * @param timeStampt
     * @return
     */
    public boolean pmReportProducedAfterTimeStamp(Date timeStamp) {
        String response = mSshSession.send(String.format(CMD_PM_REPORT_LATEST_REPORT, "awk '{print $4, $5, $6}'"));
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_LS);

        try {
            Date lastTimeStamp = dateFormat.parse(response + " " + Calendar.getInstance().get(Calendar.YEAR));
            mLogger.warn("Last pm report produced: " + response);
            return lastTimeStamp.after(timeStamp);
        } catch (ParseException e) {
            throw new EcsCicException(String.format(
                    "The return string of the command '%s' has changed on the Cic! Expected format was %s, but we got %s. %s",
                    CMD_PM_REPORT_LATEST_REPORT, DATE_FORMAT_LS, response, e.getMessage()));
        }
    }

    /**
     * Method which copies a local file to cic
     *
     * @param remoteFilePath
     * @param localFilePath
     */
    public void putRemoteFile(String remoteFilePath, String localFilePath) {
        mSshSession.send("hostname");
        Scp scp = CliFactory.newScp(mIpAddress, mUsername, mPassword, mPort);
        scp.connect();
        mLogger.info(EcsAction.TRANSFERING, localFilePath, remoteFilePath, "on " + getHostname());
        scp.put(localFilePath, remoteFilePath);
        // TODO : use loohelper to make sure file is copied
        mLogger.info(Verdict.TRANSFERED, localFilePath, remoteFilePath, "on " + getHostname());
        scp.disconnect();
    }

    @Override
    public String sendCommand(String command) {
        if (mSshSession == null) {
            mSshSession = mCicSessionFactory.create(mUsername, mPassword, mIpAddress, mPort);
        }
        return mSshSession.send(command);
    }

    /**
     * Method to set current flavor to required flavor in glance-api.conf file.
     */
    public String setGlanceFlavor(GlanceFlavor actualFlavorType, GlanceFlavor requiredFlavorType) {
        String result = mSshSession.send(String.format(MODIFY_FLAVOR_TYPE_IN_GLANCE_CONF_CMD,
                actualFlavorType.getFlavorType(), requiredFlavorType.getFlavorType()));
        restartService("glance-api");
        return result;
    }

    /**
     * Set mode of CIC. If the newMode is MAINTENANCE, a cic reboot will occur
     * and method will wait/verify cic recovery. The method will verify that the
     * correct mode is set before returning. Throws runtime exception if not
     * successful.
     *
     * @param newMode
     *            - new mode that the CIC should be set to.
     */
    public void setMode(Mode newMode) {
        if (newMode.equals(Mode.MAINTENANCE)) {
            setMaintenanceMode();
        } else if (newMode.equals(Mode.FULL_OPERATIONAL)) {
            setFullOperationalMode();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startScalabilityCollection() {
        if (runCheck(CIC_SCALABILITY_DIRECTORY, "scalability_script.sh", 60) > 0) {
            return true;
        }
        return false;
    }

    /**
     * Starts corosync service which will start pacemaker. Note: To run this
     * command this controller must be in MAINTENANCE mode, otherwise runtime
     * exception will be thrown.
     */
    public void startServiceCorosync() {
        if (!isCurrentMode(Mode.MAINTENANCE)) {
            throw new EcsCicException(
                    "The controller must be in maintenance mode when starting corosync and pacemaker");
        }

        if (mSshSession.send(CMD_COROSYNC_START).contains(EXPECTED_OUTPUT_COROSYNC_START)
                && mSshSession.send(CMD_COROSYNC_STATUS).contains(EXPECTED_OUTPUT_COROSYNC_STATUS)) {
            return;
        } else {
            throw new EcsCicException("Was not able to start corosync service correctly");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startStabilityCollection() {
        int script1 = runCheck(CIC_STABILITY_DIRECTORY, "stat_script.sh", 120);
        int script2 = runCheck(CIC_STABILITY_DIRECTORY, "misc_script.sh", 120);
        int script3 = runCheck(CIC_STABILITY_DIRECTORY, "BAT_stat.sh", 120);
        if (script1 > 0 && script2 > 0 && script3 > 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return mSshSession.getHostname();
    }

    /**
     * Transfers file from cic to atlas vm
     *
     * @param srcFilePath - String - the source file path
     * @param destFilePath - String - the destination file path
     * @return - boolean
     */
    public boolean transferFileToAtlasVm(String srcFilePath, String destFilePath) {
        EcsAtlasVm atlasVm = getAtlasVm();
        String fileName = srcFilePath;
        if (srcFilePath.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        mLogger.info(EcsAction.TRANSFERING, srcFilePath, getHostname(), "to atlas vm");
        final String cmdRemoveFromKnownHosts = "ssh-keygen -R " + atlasVm.getVmIP();
        // SSH remove atlas "vmIp" from known_hosts file
        mLogger.info(EcsAction.SENDING, EcsCic.class, "cmd: " + cmdRemoveFromKnownHosts);
        String response = mSshSession.send(cmdRemoveFromKnownHosts);
        mLogger.info(Verdict.RECEIVED, EcsCic.class, response);
        mSshSession.setNextPossiblePassword("qwqwqw");
        String result = mSshSession
                .send("scp " + srcFilePath + " " + atlasVm.getUser() + "@" + atlasVm.getVmIP() + ":" + destFilePath);
        if (!result.contains("100%")) {
            throw new EcsCicException("copying file to atlas VM failed");
        }
        mLogger.info(Verdict.TRANSFERED, srcFilePath, getHostname(), "to atlas vm");
        return true;
    }
}

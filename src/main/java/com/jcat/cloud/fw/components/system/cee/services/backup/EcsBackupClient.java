package com.jcat.cloud.fw.components.system.cee.services.backup;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService.Result;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsCic.Mode;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;

/**
 * This class contains available functionality to a CIC running
 * in the ECS.
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
 * @author ezhgyin 2015-03-23 change to pass backupUser in constructor
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author ehosmol 2015-05-18 adopt to {@link EcsCic} and new way of changing to admin user
 * @author eqinann 2015-11-09 Renamed create backup method
 * @author zdagjyo 2017-04-27 Added methods checkRestoreCommandCompletion, isBackupCreated,
 *         performCicDataRestore, performPreCicDataRestore, restoreEcsBackup, showBackupDetails,
 *         waitAndVerifyOpenstackAvailablity and modified execute() method to private
 * @author zdagjyo 2017-12-27 Added methods getLatestBackupName and getOldestBackupName
 */
public class EcsBackupClient extends EcsComponent {

    private final EcsSession mSshSession;
    private final EcsCic mEcsCic;
    private final String mBackupAdminUser;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsBackupClient.class);

    private static final String BACKUP_CHANGEPOLICY = "cic-data-backup changePolicy ";
    private static final String BACKUP_CLEAN = "cic-data-backup clean ";
    private static final String BACKUP_CREATE = "cic-data-backup create ";
    private static final String BACKUP_FILE_PATH = "/var/lib/glance/backup";
    private static final String BACKUP_LIST = "cic-data-backup list";
    private static final String BACKUP_REMOVEALL = "yes | cic-data-backup removeAll ";
    private static final String BACKUP_RESTORE = "cic-data-restore -f /var/lib/glance/backup/cic_data_backup.%s/%s -m All";
    private static final String BACKUP_SHOW = "cic-data-backup show ";
    private static final String BACKUP_SHOWHISTORY = "cic-data-backup showHistory";
    private static final String BACKUP_SHOWLOG = "cic-data-backup showLog";
    private static final String BACKUP_SHOWPOLICY = "cic-data-backup showPolicy";
    private static final String MYSQL_FILE_PATH = "cd /var/run/resource-agents/mysql-wss/";
    private static final String PRE_BACKUP_RESTORE = "pre-cic-data-restore &";
    private static final String RESTART_ISCSI = "sudo /etc/init.d/open-iscsi restart";
    private static final String COMPLETED = "COMPLETED";
    private static final String PIPE_GREP = " | grep ";
    private static final String PIPE_WC_L = " | wc -l";
    private static final String HAS_BEEN_EXECUTED = " has been executed";
    private static final String NO_BACKUPS = "There are no backups";
    private static final int INDEXFOUR = 4;
    private static final int ITERATION_DELAY = 1;
    private static final String GET_LATEST_BACKUP = "cic-data-backup list|sed -n 4p|awk '{print $10}'";
    private static final String GET_OLDEST_BACKUP = "cic-data-backup list|sed -n 'x;$p'|awk '{print $10}'";
    private static final String CREATE_BKP_ERROR_MSG = "ERROR: Only 2 controllers with MySQL started";


    private static final String CONFIG_FILE = "/etc/rsnapshot-cee.conf";

    /**
     * Constructor (suppose to be used by EcsCic only)
     *
     */
    public EcsBackupClient(EcsSession sshSession, EcsCic ecsCic) {
        mSshSession = sshSession;
        mEcsCic = ecsCic;
        mBackupAdminUser = ecsCic.getOpenstackBackupAdminUser();
        mLogger.info(Verdict.FOUND, "IdamSystemAdminUser", sshSession.getHostname(), mBackupAdminUser);
    }

    /**
     * Executes cic data restore command and makes sure that the command gets executed completely.
     * (Makes use of parallel execution service as the command takes around 20 minutes to execute)
     *
     * @param command - the cic data restore command
     * @param logFile - the name of the file where the restore command output is redirected to
     * @param expectedOutput - the expected output of command execution
     * @param timeout - the time needed for command execution
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void checkRestoreCommandCompletion(String command, String logFile, String expectedOutput, Timeout timeout)
            throws InterruptedException, ExecutionException {
        Runnable task1 = () -> {
            String currentUser = mEcsCic.getUser();
            mEcsCic.changeUser(mBackupAdminUser);
            mSshSession.send(command);
            mEcsCic.changeUser(currentUser);
        };
        Runnable task2 = () -> {
            String currentUser = mEcsCic.getUser();
            mEcsCic.changeUser(mBackupAdminUser);
            new LoopHelper<Boolean>(timeout, "command execution doesn't seem to finish in time", Boolean.TRUE, () -> {
                if (mEcsCic.doesFileExist(logFile)) {
                    String content = mEcsCic.readFile("", logFile);
                    if (content.contains(expectedOutput)) {
                        return true;
                    }
                } else {
                    mLogger.info("command execution has not started yet");
                    return false;
                }
                mLogger.info("command is still executing");
                return false;
            }).setIterationDelay(60).run();
            mEcsCic.deleteFile("", logFile);
            mEcsCic.changeUser(currentUser);
        };

        ParallelExecutionService service = new ParallelExecutionService();
        Map<Runnable, Result> tasks = new HashMap<Runnable, Result>();

        tasks.put(task1, Result.SUCCESS);
        service.executeTasks(tasks);
        String taskResult1 = service.waitAndGetTaskResult(task1);

        tasks.put(task2, Result.SUCCESS);
        service.executeTasks(tasks);
        String taskResult2 = service.waitAndGetTaskResult(task2);

        if (taskResult1 == null && taskResult2 == null) {
            mLogger.info(Verdict.RESTORED, "", "CEE Backup", "created earlier ");
        }
    }

    /**
     * Converts a string to an Integer, in a secure way
     *
     * @param string
     * @return the integer
     */
    private int convertToInteger(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            mLogger.error("This string is not a parsable integer, the value will be set to 0");
            return 0;
        }
    }

    /**
     * Execute command backup. This method first switch to Admin user then executes the command and switch back
     * to the original user.
     *
     * @param cmd
     * @return String
     */
    private String execute(String cmd) {
        String currentUser = mEcsCic.getUser();
        mEcsCic.changeUser(mBackupAdminUser);
        String response = mSshSession.send(cmd);
        mEcsCic.changeUser(currentUser);
        return response;
    }

    /**
     * Performs cic-data-restore as part of restoring the backup.
     *
     * @param backupNumber - int - the backup number(0 if it is the first backup, 1 if second..)
     * @param backupFileName - String - the name of the backup file as in backup folder(example:cic_data_backup_18042017_120754.tgz)
     * @param cicList - EcsCicList object that holds all cics.
     * @param logFile -String - the name of the file where the restore command output is redirected to
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void performCicDataRestore(int backupNumber, String backupFileName, EcsCicList cicList, String logFile)
            throws InterruptedException, ExecutionException {
        mLogger.info(EcsAction.RESTORING, "", "CEE Backup", " created earlier");
        String command = String.format(BACKUP_RESTORE, backupNumber, backupFileName);
        command = command + " > " + logFile + " &";
        checkRestoreCommandCompletion(command, logFile, "Run sudo umm off command", Timeout.BACKUP_COMMAND_CEE);
    }

    /**
     * Performs pre-cic-data-restore as part of restoring the backup.
     *
     * @param cicList - EcsCicList object that holds all cics.
     */
    private boolean performPreCicDataRestore(EcsCicList cicList) throws InterruptedException, ExecutionException {
        ParallelExecutionService service = new ParallelExecutionService();
        Map<Runnable, Result> tasks = new HashMap<Runnable, Result>();
        for (EcsCic cic : cicList.getAllCics()) {
            Runnable task = () -> {
                cic.waitAndVerifyRestart(Timeout.CIC_REBOOT_MAX_LONG);
            };
            tasks.put(task, Result.SUCCESS);
        }
        service.executeTasks(tasks);
        mLogger.info(EcsAction.STARTING, "", "pre backup restore", "on " + mEcsCic.getHostname());
        execute(PRE_BACKUP_RESTORE);
        String taskResult = null;
        for (Map.Entry<Runnable, Result> entry : tasks.entrySet()) {
            taskResult = service.waitAndGetTaskResult(entry.getKey());
            if (taskResult != null) {
                mLogger.warn("Failed to perform pre-backup restore on " + mEcsCic.getHostname());
                return false;
            }
        }
        mLogger.info(Verdict.STARTED, "", "pre backup restore", "on " + mEcsCic.getHostname());
        return true;
    }

    /**
     * Change the Backup Retention Policy
     *
     * @param policy
     * @return boolean
     */
    public boolean changeRetentionPolicy(int policy) {
        mLogger.info(EcsAction.CHANGING, "retention policy", mEcsCic.getHostname(), "Policy: " + policy);
        execute(BACKUP_CHANGEPOLICY + policy);
        if (getRetentionPolicy() != policy) {
            return false;
        }
        mLogger.info(Verdict.CHANGED, "retention policy", mEcsCic.getHostname(), "Policy: " + policy);
        return true;
    }

    /**
     * Clean the Backup directory from temporary backup files
     *
     * @return boolean true if the command have been executed
     */
    public boolean clean() {
        execute(BACKUP_CLEAN);
        return verifyBackupCommandExecuted(BACKUP_CLEAN);

    }

    /**
     * Create a backup and wait for it to complete
     *
     * @param backupName
     */
    public void createBackupAndWaitForCompletion(String backupName) {
        String cmd = String.format("%s %s", BACKUP_CREATE, backupName);
        String output = execute(cmd);
        mLogger.info(output + " (" + execute("whoami") + ")");
        if (output.contains(CREATE_BKP_ERROR_MSG)) {
            new LoopHelper<Boolean>(Timeout.MYSQL_READY, "MYSQL is still down on rebooted cic", Boolean.TRUE, () -> {
                String result = execute(cmd);
                return !(result.contains(CREATE_BKP_ERROR_MSG));
            }).setIterationDelay(ITERATION_DELAY).run();
        }
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.BACKUP_CREATE_CEE,
                BACKUP_CREATE + "not finished before Timeout", Boolean.TRUE, () -> {
                    String cmdList = String.format("%s %s %s %s %s", BACKUP_LIST, PIPE_GREP, backupName, PIPE_GREP,
                            COMPLETED);
                    String respondText = execute(cmdList);
                    return (respondText.contains(backupName));
                });
        loopHelper.setIterationDelay(ITERATION_DELAY);
        loopHelper.run();
    }

    /**
     * Create a backup as a background process
     *
     * @param backupName String
     */
    public String createBackupUnblocking(String backupName) {
        String cmd = BACKUP_CREATE + backupName + " &";
        return execute(cmd);

    }

    @Override
    public Boolean deinitialize() {
        removeAllBackups();
        return true;
    }

    /**
     * get the pid of an ongoing backup
     *
     * @param backupName
     */
    public int getBackupPid(String backupName) {
        String pid = createBackupUnblocking(backupName).substring(INDEXFOUR);

        mLogger.info("pid for the ongoing backup is: " + pid);
        return convertToInteger(pid);
    }

    /**
     * Returns the name of the latest (recently created) backup.
     *
     * @return String - the name of the latest backup
     */
    public String getLatestBackupName() {
        return execute(GET_LATEST_BACKUP).trim();
    }

    /**
     * Count the number of Backup files in /var/lib/glance/backup
     *
     * @return The number of Backup files in /var/lib/glance/backup
     */
    public int getNumBackupFiles() {
        String cmd = String.format("ls -al %s", BACKUP_FILE_PATH);
        execute(cmd);

        cmd = String.format("ls -al %s | grep cee_infra | wc -l", BACKUP_FILE_PATH);
        int numberOfFiles = convertToInteger(execute(cmd));

        mLogger.info("Number of Backup files are: " + numberOfFiles);
        return numberOfFiles;
    }

    /**
     * Get the number of completed Backups
     *
     * @return The number of completed Backups according to the "cee-backup -list" command
     */
    public int getNumCompletedBackups() {
        String result = execute(BACKUP_LIST);
        mLogger.info("Command " + BACKUP_LIST + " returned " + result);
        String cmd = BACKUP_LIST + PIPE_GREP + COMPLETED + PIPE_WC_L;
        int numBackups = convertToInteger(execute(cmd));
        mLogger.info("The number of present Backups is: " + numBackups);
        return numBackups;
    }

    /**
     * Get the number of completed Backups with a specified name
     *
     * @return The number of completed Backups according to the "cee-backup -list" command
     */
    public int getNumCompletedBackups(String backupName) {
        String cmd = String.format("%s %s %s %s %s %s", BACKUP_LIST, PIPE_GREP, backupName, PIPE_GREP, COMPLETED,
                PIPE_WC_L);
        return convertToInteger(execute(cmd));

    }

    /**
     * Count the number ERRORS in the ShowLog
     *
     * @return The number of WARNINGS, ERRORS etc in the ShowLog
     */
    public int getNumErrors() {
        String cmd = BACKUP_SHOWLOG + "  | grep " + "ERROR" + "| wc -l";
        int numErrors = convertToInteger(execute(cmd));
        return (numErrors);
    }

    /**
     * Returns the name of the oldest backup.
     *
     * @return String - the name of the oldest backup
     */
    public String getOldestBackupName() {
        return execute(GET_OLDEST_BACKUP).trim();
    }

    /**
     * Get the number of backups in the Retention policy
     *
     * @return The number of backups in the Retention policy
     */
    public int getRetentionPolicy() {
        String respondText = execute(BACKUP_SHOWPOLICY);
        String sLine = null;
        String retentionPolicy = "";
        String createdPattern = "Retention policy=";
        if (!(respondText.contains(createdPattern))) {
            mLogger.warn("There is no stored Retention Policy");
            return 0;
        }

        Scanner psScanner = new Scanner(respondText);
        while (psScanner.hasNextLine()) {
            sLine = psScanner.nextLine();
            Matcher wantedPattern = Pattern.compile(createdPattern).matcher(sLine);
            // line matches the defined pattern
            if (wantedPattern.find()) {
                retentionPolicy = sLine.substring(wantedPattern.end(), sLine.length());
                break;
            }
        }
        psScanner.close();
        mLogger.info("The current value of the Retention Policy is: " + retentionPolicy);
        return Integer.parseInt(retentionPolicy);
    }

    /**
     * Checks if the specified backup exists.
     *
     * @param backupName - the name of the backup
     * @return boolean
     */
    public boolean isBackupCreated(String backupName) {
        String cmdList = String.format("%s %s %s %s %s", BACKUP_LIST, PIPE_GREP, backupName, PIPE_GREP, COMPLETED);
        String respondText = execute(cmdList);
        return (respondText.contains(backupName));
    }

    /**
     * get the pid of an ongoing backup
     *
     * @param pid int - process id
     */
    public void killBackup(int pid) {
        execute("kill " + pid);
    }

    /**
     * Output a backup
     *
     * @param backupName String
     */
    @Deprecated
    public String outputBackup(String backupName) {
        String cmd = String.format("%s %s", BACKUP_CREATE, backupName);
        return execute(cmd);

    }

    /**
     * Remove all previous Backups
     *
     * @return boolean true if all backup files have been removed
     */
    public boolean removeAllBackups() {
        String result = execute(BACKUP_REMOVEALL);
        mLogger.info(result);
        mLogger.info(execute("whoami"));
        mLogger.info(BACKUP_REMOVEALL + " command has been executed");

        String respondText = execute(BACKUP_LIST);
        return respondText.contains(NO_BACKUPS);
    }

    /**
     * Restores the backup created earlier.
     *
     * It includes the below steps:
     * a) pre-cic-data-restore(this puts all cics under maintenance mode)
     * b) restart open-iscsi in all cics
     * c) restore backup
     * d) move all cics from maintenanace mode to operational mode
     * e) wait until all services on cic are up and running
     *
     * @param backupNumber - int - the backup number(0 if it is the first backup, 1 if second..)
     * @param backupFileName - String - the name of the backup file as in backup folder(example:cic_data_backup_18042017_120754.tgz)
     * @param cicList - EcsCicList object that holds all cics.
     */
    public void restoreEcsBackup(int backupNumber, String backupFileName, EcsCicList cicList)
            throws InterruptedException, ExecutionException {
        performPreCicDataRestore(cicList);
        mLogger.info(EcsAction.RESTARTING, "", "ISCSI service", "on all cics");
        for (EcsCic cic : cicList.getAllCics()) {
            if (cic.getHostname().equals(mEcsCic.getHostname())) {
                continue;
            }
            cic.changeUser(mBackupAdminUser);
            cic.sendCommand(RESTART_ISCSI);
        }
        execute(RESTART_ISCSI);
        mLogger.info(Verdict.RESTARTED, "", "ISCSI service", "on all cics");
        performCicDataRestore(backupNumber, backupFileName, cicList, "restore_log.txt");
        mLogger.info(EcsAction.CHANGING, EcsCic.class, "all cics to full operational mode");
        for (EcsCic cic : cicList.getAllCics()) {
            cic.setMode(Mode.FULL_OPERATIONAL);
        }
        mLogger.info(Verdict.CHANGED, EcsCic.class, "all cics to full operational mode");
        mLogger.info(EcsAction.WAITING, "", " services", "on cics to be up and running");
        cicList.waitAndVerifyAllCicsAndServicesOnline();
        mLogger.info(Verdict.VALIDATED, "", " services", "on cics are up and running");
    }

    /**
     * Set the Backup Retention Policy, by writing in the Config file
     *
     * @param policy
     */
    public void setRetentionPolicy(int policy) {
        String cmd = "sudo sed -ir 's/retain" + "\\t" + "cee_infra.*/retain" + "\\t" + "cee_infra" + "\\t" + "'" + '"'
                + policy + '"' + "'/ ' " + CONFIG_FILE;
        execute(cmd);
    }

    /**
     * Retrieves the details of the specified backup.
     *
     * @param backupName - the name of the backup
     * @return String
     */
    public String showBackupDetails(String backupName) {
        String cmdList = String.format("%s %s", BACKUP_SHOW, backupName);
        String respondText = execute(cmdList);
        return respondText;
    }

    /**
     * Verify if a Backup is completed and correct order
     *
     * @param backupName
     * @return true if backup is completed
     */
    public boolean verifyBackupAvailableandinOrder(String backupName, int orderNumber) {
        String cmd = String.format("%s %s %s %s %s", BACKUP_LIST, PIPE_GREP, backupName, PIPE_GREP, COMPLETED);
        String respondText = execute(cmd);
        mLogger.info("Grep for: " + backupName + " in BACKUP_LIST command shows: " + respondText);
        return (respondText.contains(backupName) && respondText.contains("cee_infra." + orderNumber));
    }

    /**
     * Verify that Backup command was executed
     *
     * @param command
     * @return true if command was executed
     */
    public boolean verifyBackupCommandExecuted(String command) {
        String parameter = command.substring(INDEXFOUR).trim();
        String cmd = BACKUP_SHOWLOG + PIPE_GREP + '"' + parameter + '"' + " | grep backup | wc -l";

        int numCommands = convertToInteger(execute(cmd));
        mLogger.info(command + HAS_BEEN_EXECUTED + " " + numCommands + " times");
        return (numCommands > 0);
    }

    /**
     * Verify that the Backup History is empty
     *
     * @return true if the Backup History is empty
     */
    public boolean verifyBackupHistoryEmpty() {
        mLogger.info(execute(BACKUP_SHOWHISTORY));
        String cmd = BACKUP_SHOWHISTORY + PIPE_WC_L;
        int numLines = convertToInteger(execute(cmd));
        mLogger.info("Backup History consists of " + numLines + " lines");
        // Note that Backup History always contains at least 1 line
        return (numLines < 2);
    }

    /**
     * Verify that the Backup Log File is empty
     *
     * @return true if the Backup Log File is empty
     */
    public boolean verifyBackupLogFileEmpty() {
        // Note that after cleaning the Logfile, the BACKUP_SHOWLOG command gives 1 empty line
        String cmd = String.format("%s %s", BACKUP_SHOWLOG, PIPE_WC_L);
        int numLines = convertToInteger(execute(cmd));
        mLogger.info("Backup Log file consists of " + numLines + " lines");
        return (numLines < 2);
    }

    /**
     * Verify the number of WARNINGS, ERRORS etc in the Backup Log
     *
     * @return true if The number of WARNINGS, ERRORS etc in the Backup Log are as expected
     */
    public boolean verifyNumWarningsandErrors(int expCriticals, int expErrors, int expWarnings) {
        mLogger.info(execute(BACKUP_SHOWLOG));

        String cmd = String.format("%s | grep CRITICAL | wc -l", BACKUP_SHOWLOG);
        int numCriticals = convertToInteger(execute(cmd));
        mLogger.info(BACKUP_SHOWLOG + " showed: " + numCriticals + " CRITICALS");

        cmd = String.format("%s | grep ERROR | wc -l", BACKUP_SHOWLOG);
        int numErrors = convertToInteger(execute(cmd));
        mLogger.info(BACKUP_SHOWLOG + " showed: " + numErrors + " ERRORS");

        cmd = String.format("%s | grep WARNING | wc -l", BACKUP_SHOWLOG);
        int numWarnings = convertToInteger(execute(cmd));
        mLogger.info(BACKUP_SHOWLOG + " showed: " + numWarnings + " WARNINGS");

        return ((numCriticals == expCriticals) && (numErrors == expErrors) && (numWarnings == expWarnings));
    }

    /**
     * Waits for the openstack services to be available after the backup is restored
     *
     * @param - keystoneController - keystone controller object
     * @param - tenantId - the id of the tenant created after backup is taken
     */
    public void waitAndVerifyOpenstackAvailablity(NeutronController neutronController, String networkId) {
        mLogger.info(EcsAction.WAITING, "", "openstack services", "to be available after backup is restored");
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.SERVICE_AVAILABLE,
                "could not verify that openstack services are available after backup is restored", Boolean.TRUE,
                () -> {
                    try {
                        neutronController.getNetwork(networkId);
                        return true;
                    } catch (EcsConnectionException e) {
                        mLogger.info("Going to check again after 10 seconds");
                        return false;
                    } catch (Exception e) {
                        if (e.getMessage().contains("ServerResponseException")
                                || e.getMessage().contains("Service Unavailable")) {
                            mLogger.info("Going to check again after 10 seconds");
                            return false;
                        } else {
                            throw e;
                        }
                    }
                });
        loopHelper.setIterationDelay(10);
        loopHelper.run();
        mLogger.info(Verdict.VALIDATED, "", "openstack services", "available after backup is restored");
    }
}

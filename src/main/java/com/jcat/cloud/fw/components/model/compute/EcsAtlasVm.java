package com.jcat.cloud.fw.components.model.compute;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.system.cee.ecssession.VmSessionVirshFactory;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.BspNetconfLib;

/**
 * Class which represents Atlas VM and handles operations performed on it.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2017-10-10 initial version
 * @author zmousar 2018-01-12 Added methods areServicesActive, verifyFileActionsLoggedInAuditLog,
 *         verifyServiceActive and waitAndVerifyServiceActive
 * @author zmousar 2018-02-03 Added method verifyValidSshInAuthLog
 */
public class EcsAtlasVm extends EcsVm {

    private static final String PERSONALIZE_TEMPLATE_CMD = "ovft capp-template-personalize -e %s -o %s %s";
    private static final String OVFT_CAPP_EXPORT_CMD = "ovft capp-export --file /tmp %s";
    private static final String UPLOAD_HOT_FILE_CMD = "ovft capp-create --type HOT --file %s --name %s";
    private static final String GET_OVFT_CAPP_STATUS_CMD = "ovft capp-list|grep %s|sed -n 1p|awk '{print $6}'";
    private static final String GET_OVFT_CAPP_ID_CMD = "ovft capp-list|grep %s|sed -n 1p|awk '{print $2}'";
    private static final String GET_OVFT_CAPP_NAME_CMD = "ovft capp-list|grep %s|sed -n 1p|awk '{print $4}'";
    private static final String OVFT_CAPP_UPDATE_CMD = "ovft capp-update --name %s --desc '%s' %s";
    private static final String OVFT_CAPP_DELETE_CMD = "ovft capp-delete %s";
    private static final String ATLAS_VM_NETWORK_NAME = "tenant_3583";
    private static final String ATLAS_VM_NETWORK_NAME_BSP = "provider_51";
    private static final String CREATE_BACKUP_CMD = "sudo atlas backup-create --name %s --p %s";
    private static final String GET_BACKUP_ID_CMD = "sudo atlas backup-list | grep %s | awk {'print $2'}";
    private static final String RESTORE_BACKUP_CMD = "sudo atlas backup-restore --d %s --p %s";
    private static final String SERVICE_STATE = "systemctl  status %s | grep Active | awk {'print $2'}";
    private static final String GREP_AUTH_LOG_FILES = "ll  | grep auth.log | awk '{print $9}'";
    private static final String AUTH_LOG_SESSION_OPENED_CMD = "cat %s | grep \"opened for user atlasadm\"";
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsAtlasVm.class);

    private EcsCic mEcsCic;
    @Inject
    private VmSessionVirshFactory mVmSessionFactory;
    @Inject
    private IEquipmentController mEquipmentController;
    @Inject
    private EcsCicList mEcsCicList;

    @Inject
    public EcsAtlasVm(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp) {
        super(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId, vmIp);
    }

    /**
     * Monitor the Services in Atlas are active
     *
     * @param rebootflag - true if atlas vm is rebooted, services will take time to be up
     * @return true - if all services are active otherwise false
     */
    public boolean areServicesActive(boolean rebootflag) {
        mLogger.info(EcsAction.VALIDATING, EcsAtlasVm.class, "Services are active");
        List<String> requiredServices = Arrays.asList("rsyslog", "rabbitmq-server", "ovft-engine", "ovft-api", "mysql",
                "memcached", "heat-engine", "heat-api-cloudwatch", "heat-api-cfn", "heat-api", "auditd");
        mSshSession.send("sudo -i");
        for (String serviceName : requiredServices) {
            if (rebootflag) {
                waitAndVerifyServiceActive(serviceName);
            } else {
                if (!verifyServiceActive(serviceName)) {
                    return false;
                }
            }
        }
        mLogger.info(Verdict.VALIDATED, EcsAtlasVm.class, " Services are active");
        return true;
    }

    /**
     * method which copies a file between users (root & atlasadm) of atlas vm
     *
     * @param srcUser - the source user
     * @param destUser - the destination user
     * @param srcFilePath - the source file path
     * @param fileName - the file name
     * @param destFilePath - the destination file path
     * @return boolean
     */
    public boolean copyFileBetweenUsers(String srcUser, String destUser, String srcFilePath, String fileName,
            String destFilePath) {
        mLogger.info(EcsAction.TRANSFERING, srcFilePath, EcsAtlasVm.class, "from " + srcUser + " to " + destUser);
        if (!getUser().equals("root")) {
            changeUser("root");
        }
        String cmdRemoveFromKnownHosts = "ssh-keygen -R " + getVmIP();
        mSshSession.send(cmdRemoveFromKnownHosts);
        String result = null;
        if (destUser.equals("root")) {
            result = mSshSession
                    .send("scp " + srcUser + "@" + getVmIP() + ":" + srcFilePath + fileName + " " + destFilePath);
        } else {
            result = mSshSession
                    .send("scp " + srcFilePath + fileName + " " + destUser + "@" + getVmIP() + ":" + destFilePath);
        }
        if (!result.contains("100%")) {
            throw new EcsOpenStackException("copying file inside atlas vm failed ");
        }
        mLogger.info(Verdict.TRANSFERED, srcFilePath, EcsAtlasVm.class, "from " + srcUser + " to " + destUser);
        return true;
    }

    /**
     * Creates backup of Atlas VM
     *
     * @param backupName - Name of the backup to be created
     * @param password - Password to create the backup with
     * @return String - the id of the created backup
     */
    public String createBackup(String backupName, String password) {
        String backupId = null;
        mLogger.info(EcsAction.CREATING, EcsAtlasVm.class, "Backup " + backupName);
        mSshSession.send("sudo -i");
        String cmd = String.format(CREATE_BACKUP_CMD, backupName, password);
        String output = mSshSession.send(cmd);
        if (!output.contains("Backup Complete!")) {
            throw new EcsOpenStackException("failed to create backup of atlas vm");
        }
        backupId = mSshSession.send(String.format(GET_BACKUP_ID_CMD, backupName)).trim();
        mLogger.info(Verdict.CREATED, EcsAtlasVm.class, "Backup " + backupName + ", id:" + backupId);
        return backupId;
    }

    /**
     * Deletes the specified Capp application.
     *
     * @param cappId - the id of the Capp application to be deleted
     */
    public void deleteCappApplication(String cappId) {
        mLogger.info(EcsAction.DELETING, EcsAtlasVm.class, "capp applicaton, id: " + cappId);
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-delete 07aa9580-a013-4941-937b-9957c54fa12e
         * atlasadm@atlas:~$
         */
        mSshSession.send(String.format(OVFT_CAPP_DELETE_CMD, cappId));
        new LoopHelper<Boolean>(Timeout.DELETE_OVFT_APPLICATION,
                "Was not able to verify that the application has been deleted successfully", true, () -> {
                    return !doesCappExist(cappId);
                }).run();
        mLogger.info(Verdict.DELETED, EcsAtlasVm.class, "capp applicaton, id: " + cappId);
    }

    /**
     * Checks if the specified capp application exists in Atlas VM.
     *
     * @param cappId - the id of the capp application
     * @return true - if the capp application exists
     */
    public boolean doesCappExist(String cappId) {
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-list|grep 1973e9ea-8d12-4880-a590-28513b20ae2c|sed -n 1p|awk '{print $2}'
         * 1973e9ea-8d12-4880-a590-28513b20ae2c
         * atlasadm@atlas:~$
         */
        return mSshSession.send(String.format(GET_OVFT_CAPP_ID_CMD, cappId)).contains(cappId);
    }

    /**
     * Exports Capp application to a yaml file.
     * (The yaml file gets saved as {cappName}.yaml in /tmp folder which
     * is then moved to "/home/atlasadm" folder)
     *
     * @param cappId - The id of the capp to be exported
     * @param cappName - The name of the Capp application
     * @param destinationDirectory - the directory path where the created yaml file is to be stored
     */
    public void exportCappApplication(String cappId, String cappName) {
        mLogger.info(EcsAction.EXPORTING, EcsAtlasVm.class, "capp application, id: " + cappId);
        /*
         * Example command:
         * atlasadm@atlas:~# ovft capp-export --file /tmp ab8b3e78-334f-4b74-a885-b8cdd834ed6f
         * atlasadm@atlas:~$
         */
        mSshSession.send(String.format(OVFT_CAPP_EXPORT_CMD, cappId));
        String exportedFileName = cappName + ".yaml";
        if (!doesFileExist("/tmp/" + exportedFileName)) {
            throw new EcsOpenStackException("Exporting capp application failed");
        }
        moveFile("/tmp", exportedFileName, "/home/atlasadm");
        changePermissions("644", exportedFileName);
        mLogger.info(Verdict.EXPORTED, EcsAtlasVm.class, "capp application, id: " + cappId);
    }

    /**
     * method which copies a remote file from atlas vm to local (via cic)
     *
     * @param remoteFilePath - the remote file path
     * @param fileName - the file name
     */
    public void getRemoteFile(String remoteFilePath, String fileName) {
        // copy file from atlas vm to cic
        mEcsCic.getFileFromAtlasVm(remoteFilePath, fileName);
        // copy file from cic to local
        mEcsCic.getRemoteFile("~/", fileName);
        // delete copied file on cic
        mEcsCic.deleteFile("~/", fileName);
    }

    public String getVmIP() {
        return mVmIp;
    }

    /*
     * {@inheritDoc}
     * @see ecs.jcat.lib.controllers.EcsComponent#initialize
     */
    @Override
    public Boolean lazyInitialize() {
        if (mEquipmentController instanceof BspNetconfLib) {
            mVmIp = getIPs(ATLAS_VM_NETWORK_NAME_BSP).get(0);
        } else {
            mVmIp = getIPs(ATLAS_VM_NETWORK_NAME).get(0);
        }
        mEcsCic = mEcsCicList.getRandomCic();
        EcsImage image = getImage();
        mSshSession = mVmSessionFactory.create(" ", mVmIp, image.getUserName(), image.getPassword(),
                image.getRegexPrompt());
        return true;
    }

    /**
     * Personalizes the specified capp template using the personalize file(.yaml) and stores it to the specified output file(.yaml).
     *
     * @param cappId - the id of the capp to be personalized
     * @param personalizeFilePath - the absolute path of the personalize file to add personality to the VM
     * @param outputFileName - the name of the output file obtained from personalizing capp template
     */
    public void personalizeCappTemplate(String cappId, String personalizeFilePath, String outputFileName) {
        mLogger.info(EcsAction.PERSONALIZING, EcsAtlasVm.class, "template for capp,id:" + cappId);
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-template-personalize -e personalize-env.yaml -o BAT_output.yaml
         * 7f409db8-4588-4f64-9914-ba35a509b3a7
         * atlasadm@atlas:~$
         */
        mSshSession.send(String.format(PERSONALIZE_TEMPLATE_CMD, personalizeFilePath, outputFileName, cappId));
        if (!listFiles("/home/atlasadm/").contains(outputFileName)) {
            throw new EcsOpenStackException("failed to personalize HOT template");
        }
        mLogger.info(Verdict.PERSONALIZED, EcsAtlasVm.class, "template for capp,id:" + cappId);
    }

    /**
     * Method which copies a local file to atlas vm (via cic)
     *
     * @param remoteFilePath
     * @param localFilePath
     */
    public void putRemoteFile(String remoteFilePath, String localFilePath) {
        String fileName = localFilePath;
        if (localFilePath.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        // copy local file to cic
        mEcsCic.putRemoteFile("~/", localFilePath);
        // copy file from cic to atlas vm
        mEcsCic.transferFileToAtlasVm("~/" + fileName, remoteFilePath);
        // delete the copied file on cic
        mEcsCic.deleteFile("~/", fileName);
    }

    /**
     *
     * Performs a vm reboot
     */
    @Override
    public void reboot() {
        reboot(Timeout.ECS_OS_RESTART);
    }

    /**
     * Restores backup of Atlas VM
     *
     * @param backupId - Id of the backup to be restored
     * @param password - Password used while creating backup
     * @return boolean
     */
    public boolean restoreAtlas(String backupId, String password) {
        mLogger.info(EcsAction.RESTORING, EcsAtlasVm.class, "Backup, id: " + backupId);
        mSshSession.setSendTimeoutMillis(Timeout.RESTORE_ATLAS_VM.getTimeoutInMilliSeconds());
        String output = mSshSession.send(String.format(RESTORE_BACKUP_CMD, backupId, password));
        mSshSession.setSendTimeoutMillis(mSshSession.getUniversalSendTimeout());
        if (output.contains("Successfully restored")) {
            mLogger.info(Verdict.RESTORED, EcsAtlasVm.class, "Backup, id: " + backupId);
            return true;
        }
        mLogger.warn("atlas backup restore command failed, command output is :" + output);
        throw new EcsOpenStackException("failed to restore atlas backup");
    }

    /**
     * Updates name and description of specified Capp application.
     *
     * @param cappId - The id of the capp to be updated
     * @param newName - The new name of the Capp application
     * @param description - the description for the Capp application
     */
    public void updateCappApplication(String cappId, String newName, String description) {
        mLogger.info(EcsAction.UPDATING, EcsAtlasVm.class, "capp application, id: " + cappId);
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-update --name BAT_mini_exported --desc 'updating capp applcn to exported'
         * ab8b3e78-334f-4b74-a885-b8cdd834ed6f
         * +--------------+--------------------------------------+
         * | Property | Value |
         * +--------------+--------------------------------------+
         * | created_at | 2017-11-08T04:02:35.000000 |
         * | description | updateing capp applcn to exported |
         * atlasadm@atlas:~$
         */
        mSshSession.send(String.format(OVFT_CAPP_UPDATE_CMD, newName, description, cappId));
        String updatedName = mSshSession.send(String.format(GET_OVFT_CAPP_NAME_CMD, cappId));
        if (!updatedName.equals(newName)) {
            throw new EcsOpenStackException("Updation of capp application failed");
        }
        mLogger.info(Verdict.UPDATED, EcsAtlasVm.class, "capp application, id: " + cappId);
    }

    /**
     * Uploads specified HOT file(.yaml file) to ovft database.
     *
     * @param file - The HOT file to be uploaded
     * @param applicationName - The name of the Capp application to be created
     * @return String - the id of the created application
     */
    public String uploadHotFileToOvft(File file, String applicationName) {
        mLogger.info(EcsAction.UPLOADING, EcsAtlasVm.class, "HOT file");
        if (!listFiles("/tmp").contains(file.getName())) {
            moveFile("/home/atlasadm", file.getName(), "/tmp");
        }
        changeDirectory("/tmp");
        changePermissions("644", file.getName());
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-create --type HOT --file File_1031_06_06_38_139.yaml --name BAT_mini
         * +--------------+--------------------------------------+
         * | Property | Value |
         * +--------------+--------------------------------------+
         * | created_at | 2017-10-31T05:23:53.000000 |
         * | description | |
         * atlasadm@atlas:~$
         */
        mSshSession.send(String.format(UPLOAD_HOT_FILE_CMD, file.getName(), applicationName));
        new LoopHelper<Boolean>(Timeout.UPLOAD_HOT_FILE, "Was not able to verify that HOT file has been uploaded", true,
                () -> {
                    String output = mSshSession.send(String.format(GET_OVFT_CAPP_STATUS_CMD, applicationName)).trim();
                    return output.equals("active");
                }).run();
        changeDirectory("/home/atlasadm");
        mLogger.info(Verdict.UPLOADED, EcsAtlasVm.class, "HOT file");
        /*
         * Example command:
         * atlasadm@atlas:~$ ovft capp-list|grep BAT_mini_exported|sed -n 1p|awk '{print $6}'
         * active
         * atlasadm@atlas:~$
         */
        return mSshSession.send(String.format(GET_OVFT_CAPP_ID_CMD, applicationName)).trim();
    }

    /*
     * Verifies that action 'Execution of file permission change with specified user in cic  (ex: chmod +x file)' is logged in audit.log
     * after the specified time.
     * ex: root@atlas:~# cat /log-collector/audit.log | grep dummy
     *     <14>1 2018-01-02T13:55:31.090058+01:00 cic-1.domain.tld audispd 28663 - -  node=cic-1.domain.tld type=PATH
     *         msg=audit(1514897731.086:183958): item=1 name="/etc/dummy" inode=28384 dev=fc:04 mode=0100640 ouid=0 ogid=0 rdev=00:00 nametype=CREATE
     *     <14>1 2018-01-02T13:55:31.204298+01:00 cic-1.domain.tld audispd 28663 - -  node=cic-1.domain.tld type=PATH
     *         msg=audit(1514897731.198:183961): item=0 name="/etc/dummy" inode=28384 dev=fc:04 mode=0100640 ouid=0 ogid=0 rdev=00:00 nametype=NORMAL
     *
     * @param timeBefore - time before execute the file permission change
     * @param fileName - Name of file created in cic
     * @param user - required to Login into cic
     * @return true if the execution of file permission change in cic present in audit.log (at expected log time which is greater than timeBefore)
     */
    public boolean verifyFileActionsLoggedInAuditLog(LocalDateTime timeBefore, String fileName, String user)
            throws FileNotFoundException {
        boolean fileFound = false;
        mSshSession.send("sudo -i");
        if (fileName == null) {
            throw new EcsOpenStackException(
                    fileName + "file is not provided to check the permission operation in audit.log");
        }
        // Check whether file exists in any controller
        if (user != null) {
            for (EcsCic ecsCic : mEcsCicList.getAllCics()) {
                if (ecsCic.doesFileExist(user, fileName)) {
                    fileFound = true;
                    break;
                }
            }
            if (!fileFound) {
                throw new FileNotFoundException("File Not Found in any cic.");
            }
        }
        if (!doesLogTimeIsAfterInitialTime("/log-collector/audit.log", fileName, timeBefore)) {
            throw new EcsOpenStackException(
                    String.format("File permission operation with %s as user is not performed in cic after time %s",
                            user, timeBefore));
        }
        return true;
    }

    /**
     * verify the specified service is active
     * ex: atlasadm@atlas:~$ systemctl  status heat-api | grep Active | awk {'print $2'}
     *     active
     *
     * @param serviceName - Name of Atlas service
     * @return true - if status is active otherwise false.
     */
    public boolean verifyServiceActive(String serviceName) {
        String status = mSshSession.send(String.format(SERVICE_STATE, serviceName));
        return status.contains("active");
    }

    /**
     * verify that action 'new session opened for Atlas Vm' is logged in /var/log/auth.log files after the specified time
     * (when session is established for atlas vm, corresponding time is logged in auth.log file)
     * ex:
     * 1. root@atlas:/var/log# ll  | grep auth.log | awk '{print $9}'
     *     auth.log
     *     auth.log.1
     *     auth.log.2
     * 2. root@atlas:/var/log#  cat auth.log |  grep "opened for user atlasadm"
     *    root@atlas:/var/log#
     *    root@atlas:/var/log#  cat auth.log.1  | grep "opened for user atlasadm"
     *     6,10,Jan 12 07:21:17,localhost,sshd[16475]:, pam_unix(sshd:session): session opened for user atlasadm by (uid=0)
     *     6,10,Jan 12 07:21:30,localhost,sshd[16513]:, pam_unix(sshd:session): session opened for user atlasadm by (uid=0)
     *     6,10,Jan 12 07:21:52,localhost,sshd[21186]:, pam_unix(sshd:session): session opened for user atlasadm by (uid=0)
     *
     * @param dateBefore - time before new session is established
     * @return true if datebefore < LogDate otherwise false
     * @throws ParseException
     */
    public boolean verifyValidSshInAuthLog(Date dateBefore) throws ParseException {
        mSshSession.send("sudo -i");
        // get list of auth.log files
        changeDirectory("/var/log");
        String output = mSshSession.send(GREP_AUTH_LOG_FILES);
        String[] authLogFiles = output.split("\n");
        String authLogResult = "";
        // check whether the session opened for user atlasadm is logged in auth.log files
        for (String file : authLogFiles) {
            authLogResult = mSshSession.send(String.format(AUTH_LOG_SESSION_OPENED_CMD, file.trim()));
            if (authLogResult.contains("session opened")) {
                break;
            }
        }
        if (!authLogResult.contains("session opened")) {
            return false;
        }
        changeDirectory("/root/atlasadm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
        SimpleDateFormat newDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'CET' yyyy");
        String[] lines = authLogResult.split("\n");
        for (String line : lines) {
            String logDateTime = null;
            Matcher matcher = Pattern.compile("([A-Za-z]+[\\s][0-9 :]+)").matcher(line);
            if (matcher.find()) {
                logDateTime = matcher.group().trim();
                Date formattedLogDate = dateFormat.parse(logDateTime);
                formattedLogDate = newDateFormat.parse(newDateFormat.format(formattedLogDate));
                Date formattedDateBefore = dateFormat.parse(dateFormat.format(dateBefore));
                if (formattedDateBefore.before(formattedLogDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * wait for specified service to be active
     *
     * @param serviceName - Name of Atlas service
     */
    public void waitAndVerifyServiceActive(String serviceName) {
        if (!verifyServiceActive(serviceName)) {
            new LoopHelper<Boolean>(Timeout.ATLAS_SERVICE_STATE_CHANGE, serviceName + " service is not active",
                    Boolean.TRUE, () -> {
                        mLogger.info(EcsAction.WAITING, EcsAtlasVm.class,
                                "for the service " + serviceName + " to change status Active");
                        return verifyServiceActive(serviceName);
                    }).setIterationDelay(10).run();
        }
    }
}

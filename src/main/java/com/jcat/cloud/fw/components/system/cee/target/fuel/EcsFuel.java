package com.jcat.cloud.fw.components.system.cee.target.fuel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService.Result;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade.NumaNode;
import com.jcat.cloud.fw.components.model.target.EcsTarget;
import com.jcat.cloud.fw.components.system.cee.ecssession.FuelSessionFactory;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.BspNetconfLib;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;

/**
 * Handles Fuel
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehosmol 2015- initial version
 * @author ethssce - 2015-06-04 - removed mSshSession declaration
 * @author zdagjyo - 2017-01-05 - Added enum VNX and methods addVnxSecurityFile,getVnxDiskStatus,
 *         getVnxLunStatus,getVnxStoragePoolStatus,isVnxCallerPrivileged
 *         and rebootVnx
 * @author zdagjyo - 2017-02-17 - Added enum Host, methods getDateFromAllComputes,
 *         getNtpReferenceIpsforAllHosts and getNtpRemoteIpsforAllHosts
 * @author zdagjyo - 2017-08-28 - Added methods areAllFuelUtilsReady, areAllHostsOnline, areAllTasksReady,
 *         getActiveFuelComputeName, getDefaultPrompt, getPassiveFuelComputeName, waitAndVerifyAllFuelUtilsReady,
 *         waitAndVerifyAllHostsOnline and waitAndVerifyAllTasksReady
 * @author zpralak - 2017-09-14 - Add methods getFreeHugePagesOnBothNUmaNodesOfCompute, getFreeIsolatedCpuOnBothNUmaNodesOfCompute,
 *         getCheckCpuPinScript and getTotalCpuPairsOnEachNumaNodeOfCompute
 * @author zdagjyo - 2018-01-31 - Added enum HealthcheckResult and methods downloadCoreDumps, getHealthCheckScript,
 *         getFileFromComputeBlade and performHealthCheck
 * @author zdagjyo - 2018-02-12 - Added method sendCommand
 * @author zdagjyo - 2018-12-18 - Modified performHealthCheck to support CEE9
 */
public class EcsFuel extends EcsTarget {

    /**
     * Class that stores the health check results
     * like warnings count, failed count and healthcheck
     * logfile name
     */
    public class HealthCheckResults {
        private int warningsCount;
        private int failedCount;
        private String logFile;

        public int getFailedCount() {
            return failedCount;
        }

        public String getLogFile() {
            return logFile;
        }

        public int getWarningsCount() {
            return warningsCount;
        }

        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        public void setLogFile(String logFile) {
            this.logFile = logFile;
        }

        public void setWarningsCount(int warningsCount) {
            this.warningsCount = warningsCount;
        }
    }

    public enum Host {
        CIC("cic"), COMPUTE("compute");
        private final String mHost;

        Host(String host) {
            mHost = host;
        }

        public String getHost() {
            return mHost;
        }
    }

    public enum VNX {
        SPA("SP-A", "192.168.2.12"), SPB("SP-B", "192.168.2.13");

        private final String mName;
        private final String mIpAddress;

        VNX(String vnxName, String vnxIpAddress) {
            mName = vnxName;
            mIpAddress = vnxIpAddress;
        }

        public String ipAddress() {
            return mIpAddress;
        }

        public String vnxName() {
            return mName;
        }
    }

    private static final String ACTIVE_PASSIVE_COMPUTE_IP_CMD = "for node in primary secondary ; do ip=$(get_vfuel_info"
            + " --ip --$node); name=$(ssh $ip hostname -s 2>&1 | grep compute); stat=$(ssh $ip sudo virsh list --all "
            + "2>&1 | grep fuel) stat=$(echo $stat | awk '{print $3 \" \" $4}'); printf \"%-10s | %s  |   %s\\n\" "
            + "\"$name\" \"$ip\"  \"$stat\"; done";
    private static final String NTP_REFERENCE_IP_CMD = "for node in $(fuel node | awk '/%s/ {print $5}');"
            + " do echo '${node}:'; ssh -q ${node} ntpq -pn; done|awk '{print $2}'|grep '\\.'";
    private static final String NTP_REMOTE_IP_CMD = "for node in $(fuel node | awk '/%s/ {print $5}');"
            + " do echo '${node}:'; ssh -q ${node} ntpq -pn; done|awk '{print $1}'|grep '*'";
    private static final String COMPUTE_DATE_CMD = "for node in $(fuel node | awk '/compute/  {print $5}');"
            + " do date; echo; done|grep CET";
    private static final String VNX_CMD = "/opt/Navisphere/bin/naviseccli -h %s";
    private static final String PATH_OF_SCRIPT_IN_ARTIFACTORY = "simple/proj-ecs-dev-local/se/ericsson/ecs/bat/check-cpupin_V2.sh";
    private static final String ROOT_PATH = "/root/";
    private static final String STORAGE_CMD = VNX_CMD + " storagepool -list";
    private static final String STORAGE_POOL_CMD = VNX_CMD + " storagepool -list| grep LUN";
    private static final String LUN_CMD = VNX_CMD + " lun -list| grep Performance";
    private static final String DISK_CMD = VNX_CMD + " getdisk 0_0_24 | grep Maximum";
    private static final String REBOOT_VNX_CMD = VNX_CMD + " -user admin -password ericsson -scope 0 rebootSP";
    private static final String SECURITY_FILE_CMD = VNX_CMD
            + " -addusersecurity -user admin -password ericsson -scope 0";
    private static final String AGENT_CMD = VNX_CMD + " getagent";
    private static final String HOSTS_ONLINE_CMD = "fuel node|grep 1|cut -d \"|\" -f9 | grep 1 -c";
    private static final String TOTAL_HOSTS_CMD = "fuel node|cut -d \"|\" -f1 | sed 1,2d | wc -l";
    private static final String TOTAL_TASKS_CMD = "fuel task|cut -d \"|\" -f3| sed 1,2d | wc -l";
    private static final String TASKS_READY_CMD = "fuel task|grep ready -c";
    private static final String FUEL_UTILS_CMD = "fuel-utils";
    private static final String FUEL_SCALABILITY_DIRECTORY = "/var/.Scalability";
    private static final String FUEL_STABILITY_DIRECTORY = "/var/lib/docker/.Stab";
    private static final String ACTIVE_FUEL_UTILS_CMD = "fuel-utils check_all|grep ready|wc -l";
    private static final String HEALTHCHECK_LOG_FILE_JCAT = "Jcat_health_check_log.txt";

    private FuelResource mFuel;
    private String mIpAddress;
    @Inject
    private FuelSessionFactory mFuelSessionFactory;
    @Inject
    private EcsCicList mEcsCicList;
    @Inject
    private EcsComputeBladeList mEcsComputeBladeList;
    @Inject
    private IEquipmentController mEquipmentController;
    private File mLogsFolder;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsFuel.class);

    @Inject
    public EcsFuel(FuelResource fuel) {
        mFuel = fuel;
        mIpAddress = mFuel.getIpPublic();
    }

    /**
     * Checks if all fuel utils are ready.
     *
     * @return true if all fuel utils are ready, false otherwise
     */
    private boolean areAllFuelUtilsReady() {
        /*
         * get the output of fuel-utils command:
         * [root@fuel ~]# fuel-utils
         * Usage: fuel-utils <subcommand>
         * Available subcommands:
         * check_all - Check whether all Fuel services are up and running
         * check_service <service_name> - Check whether a particular service is up and running
         * Available service names:
         * astute
         * cobbler
         * keystone
         * mcollective
         * [root@fuel ~]#
         */
        String[] fuelUtilsOutput = sendCommand(FUEL_UTILS_CMD).split("\n");
        List<String> fuelUtilsList = Arrays.asList(fuelUtilsOutput);
        fuelUtilsList.replaceAll(String::trim);
        // get the index of "Available service names:" from the output of fuel-utils command
        int availableServicesIndex = fuelUtilsList.indexOf("Available service names:");
        // fetch the list of available services from the output of fuel-utils command
        List<String> availableServices = fuelUtilsList.subList(availableServicesIndex + 1, fuelUtilsList.size());
        int availableServicesCount = availableServices.size();
        /*
         * get the number of services that are active.
         * Command to retrive the active services count:
         * [root@fuel ~]# fuel-utils check_all|grep ready|wc -l
         * 11
         * [root@fuel ~]#
         */
        String activeServicesCount = sendCommand(ACTIVE_FUEL_UTILS_CMD);
        int numberOfActiveServices = Integer.parseInt(activeServicesCount);
        return availableServicesCount == numberOfActiveServices;
    }

    /**
     * Verifies that healthcheck command executes completely.
     *
     * @param logFile
     * @param expectedOutput
     * @param timeout
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void checkHealthCheckCompletion(String logFile, String expectedOutput, Timeout timeout)
            throws InterruptedException, ExecutionException {
        Runnable task1 = () -> {
            sendCommand("/usr/bin/healthcheck.py > " + logFile + " &");
        };
        Runnable task2 = () -> {
            new LoopHelper<Boolean>(timeout, "healthcheck doesn't seem to finish in time", Boolean.TRUE, () -> {
                if (doesFileExist(logFile)) {
                    String content = readFile("", logFile);
                    if (content.contains(expectedOutput)) {
                        return true;
                    }
                } else {
                    mLogger.warn("health check execution has not started yet");
                    return false;
                }
                mLogger.warn("health check is still going on");
                return false;
            }).setIterationDelay(60).run();
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
            mLogger.info(Verdict.FINISHED, "", "health check", "");
        }
    }

    /**
     * Method to get check-cpupin_V2.sh script from artifactory and put that file to fuel
     *
     * @return - returns name of the file downladed from artifactory.
     * @throws IOException
     */
    private String getCheckCpuPinScript() throws IOException {
        // Download script from artifactory
        File file = downloadFileFromArtifactoryToLocal(PATH_OF_SCRIPT_IN_ARTIFACTORY);
        // Copy file (downloaded from artifactory) from local to fuel
        putRemoteFile(ROOT_PATH, file.getAbsolutePath());
        changePermissions("777", ROOT_PATH + file.getName());
        return file.getName();
    }

    /**
     * Initializes the mSshSession variable of fuel
     */
    private void initializeSession() {
        if (mSshSession == null) {
            mSshSession = mFuelSessionFactory.create(mIpAddress, mFuel.getFuelPublicSshPort());
        }
    }

    /**
     * method which adds security file.
     * This is needed to execute VNX commands.
     *
     * @param vnx - enum
     *
     */
    public void addVnxSecurityFile(VNX vnx) {
        sendCommand(String.format(SECURITY_FILE_CMD, vnx.ipAddress()));
        sendCommand(String.format(AGENT_CMD, vnx.ipAddress()));
    }

    /**
     * Checks if all cic/compute hosts on fuel are online.
     * (Total hosts command returns the number of values in
     * first column of "fuel node" output, which is nothing
     * but the total number of hosts. Online hosts command
     * returns the number of occurances of digit "1" in the
     * column "online" of output, which is the
     * number of hosts that are online).
     * Sample output of fuel node:
     * id | status | name        | cluster | ip           | mac               | roles             | pending_roles | online | group_id
     * ---+--------+-------------+---------+--------------+-------------------+-------------------+---------------+--------+---------
     * 1 | ready  | compute-0-1 |       1 | 192.168.0.20 | 9c:b6:54:98:f8:d0 | compute, virt     |               |      1 |        1
     * 3 | ready  | compute-0-3 |       1 | 192.168.0.22 | 9c:b6:54:9b:22:c8 | compute           |               |      1 |        1
     *
     * @return true if all hosts are online, false otherwise
     */
    public boolean areAllHostsOnline() {
        String totalHosts = sendCommand(TOTAL_HOSTS_CMD);
        if (totalHosts.contains("Error") || totalHosts.contains("Traceback")) {
            return false;
        }
        int totalNumberOfHosts = Integer.parseInt(totalHosts);
        String onlineHosts = sendCommand(HOSTS_ONLINE_CMD);
        if (onlineHosts.contains("Error") || onlineHosts.contains("Traceback")) {
            return false;
        }
        int numberOfHostsOnline = Integer.parseInt(onlineHosts);
        return totalNumberOfHosts == numberOfHostsOnline;
    }

    /**
     * Checks if all fuel tasks are ready.
     * (Total tasks command returns the number of values in the
     * column "name" of "fuel task" output, which is the total
     * number of tasks. Ready tasks command returns the number
     * of occurances of "ready" in the output, which is the
     * number of tasks that are in ready state).
     * Sample output of fuel task:
     * id | status | name           | cluster | progress | uuid
     * ---+--------+----------------+---------+----------+-------------------------------------
     * 1  | ready  | check_networks | 1       | 100      | 48b0ad38-e1cd-4968-984c-ec46ce2774be
     * 4  | ready  | deployment     | 1       | 100      | e045e35a-1990-4c32-b00d-384eba2fccb8
     *
     * @return true if all tasks are ready, false otherwise
     */
    public boolean areAllTasksReady() {
        String totalTasks = sendCommand(TOTAL_TASKS_CMD);
        if (totalTasks.contains("Error") || totalTasks.contains("Traceback")) {
            return false;
        }
        int totalNumberOfTasks = Integer.parseInt(totalTasks);
        String readyTasks = sendCommand(TASKS_READY_CMD);
        if (readyTasks.contains("Error") || readyTasks.contains("Traceback")) {
            return false;
        }
        int numberOfTasksReady = Integer.parseInt(readyTasks);
        return totalNumberOfTasks == numberOfTasksReady;
    }

    @Override
    public boolean changeUser(String newUser) {
        throw new EcsNotImplementedException("Change user functionality is not supported for target Fuel");
    }

    /**
     * Creates a directory in the local file system to store logs from testcase
     *
     * @param directoryName - the name of the directory
     */
    public void createLogsDirectory(String directoryName) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date();
        String currentDate = dateFormat.format(date);
        String logsFolderName = TEMP_FILE_LOCAL_PATH + "/" + directoryName + "_" + currentDate;
        File logsFolder = new File(logsFolderName);
        mLogsFolder = logsFolder;
    }

    @Override
    public Boolean deinitialize() {
        mLogger.info(EcsAction.STARTING, "Clean up", EcsFuel.class, "");
        if (mSshSession != null) {
            mSshSession.disconnect();
        }
        mLogger.info(Verdict.DONE, "Clean up", EcsFuel.class, "");
        return true;
    }

    /**
     * Downloads the core dumps to local file system.
     *
     * @param directoryName - the name of the folder where core dumps are to be stored
     */
    public void downloadCoreDumps(String directoryName) throws IOException {
        if (mLogsFolder == null) {
            createLogsDirectory(directoryName);
        }
        String coreDumpsDirectory = mLogsFolder.getAbsolutePath() + "/Core_Dumps";
        File coreDumpsFile = new File(coreDumpsDirectory);
        String directory = "/var/log/crash/cores/";
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            if (cic.doesDirectoryExist(directory)) {
                List<String> files = cic.listFiles(directory);
                for (String file : files) {
                    if (file.contains("/")) {
                        continue;
                    }
                    mLogger.warn(
                            "Core Dumps were generated on " + cic.getHostname() + ", saving them to local file system");
                    cic.getRemoteFile(directory, file);
                    coreDumpsFile.mkdirs();
                    Files.move(Paths.get(TEMP_FILE_LOCAL_PATH + "/" + file),
                            Paths.get(coreDumpsFile.getAbsolutePath() + "/" + file));
                    ControllerUtil.printCollectedCoreDumpsInfo(mLogger, file, coreDumpsFile.getAbsolutePath());
                }
            }
        }
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            if (blade.doesDirectoryExist(directory)) {
                List<String> files = blade.listFiles(directory);
                for (String file : files) {
                    if (file.contains("/")) {
                        continue;
                    }
                    mLogger.warn("Core Dumps were generated on " + blade.getHostname()
                            + ", saving them to local file system");
                    blade.getRemoteFile(directory, file);
                    coreDumpsFile.mkdirs();
                    Files.move(Paths.get(TEMP_FILE_LOCAL_PATH + "/" + file),
                            Paths.get(coreDumpsFile.getAbsolutePath() + "/" + file));
                    ControllerUtil.printCollectedCoreDumpsInfo(mLogger, file, coreDumpsFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Downloads the RabbitMQ core dumps to local file system.
     *
     * @param directoryName - the name of the folder where core dumps are to be stored
     */
    public void downloadRabbitMQCoreDumps(String directoryName) throws IOException {
        if (mLogsFolder == null) {
            createLogsDirectory(directoryName);
        }
        String coreDumpsDirectory = mLogsFolder.getAbsolutePath() + "/RabbitMQCoreDumps";
        File coreDumpsFile = new File(coreDumpsDirectory);
        String directory = "/var/lib/rabbitmq/MnesiaCore*/";
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            if (cic.doesDirectoryExist(directory)) {
                List<String> files = cic.listFiles(directory);
                for (String file : files) {
                    if (file.contains("/")) {
                        continue;
                    }
                    mLogger.warn("Rabbit MQ Core Dumps were generated on " + cic.getHostname()
                            + ", saving them to local file system");
                    cic.getRemoteFile(directory, file);
                    coreDumpsFile.mkdirs();
                    Files.move(Paths.get(TEMP_FILE_LOCAL_PATH + "/" + file),
                            Paths.get(coreDumpsFile.getAbsolutePath() + "/" + file));
                    ControllerUtil.printCollectedCoreDumpsInfo(mLogger, file, coreDumpsFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Returns the name of the compute blade hosting active fuel master
     * (example: compute-0-2)
     *
     * @return String - name of the active fuel hosting compute blade
     */
    public String getActiveFuelComputeName() {
        return sendCommand(ACTIVE_PASSIVE_COMPUTE_IP_CMD + " |grep running|awk '{print $1}'");
    }

    public String getCeeVersion() {
        return sendCommand("cat /etc/cee_version.txt | grep VERSION").substring(8);
    }

    /**
     * Method to get current date from all compute blades
     *
     * @return - String[] - array of dates from all compute blades
     *
     */
    public String[] getDateFromAllComputes() {
        return sendCommand(COMPUTE_DATE_CMD).split("\n");
    }

    /**
     * Transfers file from specified compute blade to Fuel.
     *
     * @param blade - EcsComputeBlade - the compute blade from where the file is to be copied
     * @param absoluteSrcFilePath - String - absolute file path in compute blade
     * @param destinationFilePath - String - the location in fuel where the file is to be copied
     * @return boolean
     */
    public boolean getFileFromComputeBlade(EcsComputeBlade blade, String absoluteSrcFilePath,
            String destinationFilePath) {
        mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, "from " + blade.getHostname(),
                "to fuel :" + destinationFilePath);
        String bladeIp = blade.getBridgeIp();
        if (mEquipmentController instanceof BspNetconfLib) {
            bladeIp = blade.getAdminIp();
        }
        String result = sendCommand("scp root@" + bladeIp + ":" + absoluteSrcFilePath + " " + destinationFilePath);
        if (!result.contains("100%")) {
            throw new EcsTargetException("copying file from compute blade to fuel failed");
        }
        mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, "from " + blade.getHostname(),
                "to fuel :" + destinationFilePath);
        return true;
    }

    /**
     * Method to get free hugepages of specified numa node of each compute
     *
     * @param computeHost - Compute on which number of free hugepages to be retrieved
     * @param numaNode - numa node of compute on which number of free hugepages to be retrieved
     *
     * @return Integer - returns number of free hugepages on both numa nodes of compute
     * @throws IOException
     */
    public int getFreeHugePagesOnSpecifiedNumaNodesOfCompute(String computeHost, NumaNode numaNode) throws IOException {
        String freeHugePage = sendCommand("./" + getCheckCpuPinScript() + " " + computeHost
                + String.format("|grep \"NUMA %s HugePages_Free:\"|awk '{print $4}'", numaNode.getName()));
        return Integer.parseInt(freeHugePage.trim());
    }

    /**
     * Method to get free isolated cpu on both Numa nodes of each compute
     *
     * @param computeHost - compute on which number of free isolated cpus to be retrieved
     *
     * @return List - returns collection of free isolated cpus
     * @throws IOException
     */
    public List<Integer> getFreeIsolatedCpuOnBothNUmaNodesOfCompute(String computeHost) throws IOException {
        String freeIsolatedCpu = sendCommand(
                "./" + getCheckCpuPinScript() + " " + computeHost + "|grep \"free isolated cpus\"|grep -o [0-9].*");
        List<Integer> freeIsolatedCpuList = new ArrayList<Integer>();
        Matcher matcher = Pattern.compile("(\\d+)").matcher(freeIsolatedCpu.trim());
        while (matcher.find()) {
            freeIsolatedCpuList.add(Integer.parseInt(matcher.group()));
        }
        if (!freeIsolatedCpuList.isEmpty()) {
            return freeIsolatedCpuList;
        } else {
            throw new EcsOpenStackException("Failed to get free isolated cpus on compute");
        }
    }

    /**
     * Method to get NTP Reference IPs for all cics/computes
     *
     * @param - enum Host - cic/compute
     *
     * @return - String[] - array of NTP Reference IP addresses
     *
     */
    public String[] getNtpReferenceIpsforAllHosts(Host host) {
        return sendCommand(String.format(NTP_REFERENCE_IP_CMD, host.getHost())).split("\n");
    }

    /**
     * Method to get NTP Remote IPs for all cics/computes
     *
     * @param - enum Host - cic/compute
     *
     * @return - String[] - array of NTP Remote IP addresses
     *
     */
    public String[] getNtpRemoteIpsforAllHosts(Host host) {
        return sendCommand(String.format(NTP_REMOTE_IP_CMD, host.getHost())).split("\n");
    }

    /**
     * Returns the name of the compute blade hosting passive fuel master
     * (example: compute-0-3)
     *
     * @return String - name of the passive fuel hosting compute blade
     */
    public String getPassiveFuelComputeName() {
        return sendCommand(ACTIVE_PASSIVE_COMPUTE_IP_CMD + " |grep 'shut off'|awk '{print $1}'");
    }

    /**
     * method which copies a remote file from cic to local
     *
     * @param path
     *            - the remote file path
     * @return a File represents the copied local file
     */
    public File getRemoteFile(String remotePath, String fileName) {
        return super.getRemoteFile(mIpAddress, mFuel.getUser(), mFuel.getPassword(), mFuel.getFuelPublicSshPort(),
                remotePath, fileName);
    }

    @Override
    public List<String> getServicesSupervisedByUpstart() {
        // TODO investigate why the services supervised by upstart cannot be
        // used as getProcessIdOfService or why they
        // are not show by service --status-all
        throw new EcsNotImplementedException(
                "The method currently does not function properly for target fuel, please contact the JCAT team");
    }

    /**
     * Method which gets the total vcpu on specified Numa node of compute blade
     *
     * @param computeHost - compute host on which total vcpu to be retrieved
     * @param numaNode - numa node of compute on total vcpu to be retrieved
     *
     * @return List - returns a collection of total available cpus on specified Numa node
     * @throws IOException
     */
    public List<Integer> getTotalVcpuOnSpecifiedNumaNodeOfCompute(String computeHost, NumaNode numaNode)
            throws IOException {
        String totalCpuOnNuma = null;
        if (numaNode.getName().equals(NumaNode.NUMA0.getName())) {
            totalCpuOnNuma = sendCommand("./" + getCheckCpuPinScript() + " " + computeHost
                    + "|grep Core|awk '{print $3 $4}'|grep  -o [0-9]*");

        } else {
            totalCpuOnNuma = sendCommand("./" + getCheckCpuPinScript() + " " + computeHost
                    + "|grep Core|awk '{print $5 $6}'|grep  -o [0-9]*");

        }
        List<Integer> availableCpuList = new ArrayList<Integer>();
        Matcher matcher = Pattern.compile("(\\d+)").matcher(totalCpuOnNuma.trim());
        while (matcher.find()) {
            availableCpuList.add(Integer.parseInt(matcher.group()));
        }
        if (!availableCpuList.isEmpty()) {
            return availableCpuList;
        } else {
            throw new EcsOpenStackException("Failed to get total available cpus on compute");
        }
    }

    /**
     * method which gets the disk status of VNX
     *
     * @param vnx - enum
     *
     * @return String
     */
    public String getVnxDiskStatus(VNX vnx) {
        return sendCommand(String.format(DISK_CMD, vnx.ipAddress()));
    }

    /**
     * method which gets the LUN status of VNX
     *
     * @param vnx - enum
     *
     * @return String
     */
    public String getVnxLunStatus(VNX vnx) {
        return sendCommand(String.format(LUN_CMD, vnx.ipAddress()));
    }

    /**
     * method which gets the storage pool status of VNX
     *
     * @param vnx - enum
     *
     * @return String
     */
    public String getVnxStoragePoolStatus(VNX vnx) {
        return sendCommand(String.format(STORAGE_POOL_CMD, vnx.ipAddress()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLxcConnectivity() {
        return pingIp(mLxc.getIpAddress(), null);
    }

    /**
     * method which checks if the caller of vnx is privileged to execute VNX commands.
     * Security file needs to be added if the caller is not privileged.
     *
     * @param vnx - enum
     *
     * @return boolean
     */
    public boolean isVnxCallerPrivileged(VNX vnx) {
        String result = sendCommand(String.format(STORAGE_CMD, vnx.ipAddress()));
        if (result.contains("Security file not found") || result.contains("Caller not privileged")) {
            return false;
        }
        return true;
    }

    @Override
    public void kernelPanic() {
        throw new EcsNotImplementedException(
                "Kernel panic has not been verified for target Fuel, please contact the JCAT team");
    }

    /**
     * Runs healthcheck.py script from fuel.
     *
     * @return HealthCheckResults - a class that stores the health check results like warnings
     *                        count, failed count and healthcheck logfile name.
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public HealthCheckResults performHealthCheck() throws IOException, InterruptedException, ExecutionException {
        HealthCheckResults healthCheckResults = new HealthCheckResults();
        mLogger.info(EcsAction.STARTING, "", "health check", "It might take a while to finish");
        checkHealthCheckCompletion(HEALTHCHECK_LOG_FILE_JCAT, "Log file:", Timeout.HEALTH_CHECK);
        String fuelLog = sendCommand("cat " + HEALTHCHECK_LOG_FILE_JCAT + " |grep 'Log file:'|awk '{print $3}'").trim();
        deleteFile("", HEALTHCHECK_LOG_FILE_JCAT);
        if (fuelLog == null) {
            mLogger.error("Something was wrong with health check, log file was not generated");
            return null;
        }
        // get warnings and failed count from log file
        int warningsCount = Integer.parseInt(sendCommand("cat " + fuelLog + "|grep Warnings:|awk '{print $2}'").trim());
        int failedCount = Integer.parseInt(sendCommand("cat " + fuelLog + "|grep Failed:|awk '{print $2}'").trim());
        healthCheckResults.setWarningsCount(warningsCount);
        healthCheckResults.setFailedCount(failedCount);
        healthCheckResults.setLogFile(fuelLog);
        return healthCheckResults;
    }

    public void provisionUpgrade() {
        throw new EcsNotImplementedException("Not yet!");
    }

    /**
     * Method which copies a local file to fuel
     *
     * @param remoteFilePath
     * @param localFilePath
     */
    public void putRemoteFile(String remoteFilePath, String localFilePath) {
        super.putRemoteFile(localFilePath, mIpAddress, mFuel.getUser(), mFuel.getPassword(),
                mFuel.getFuelPublicSshPort(), remoteFilePath);
    }

    /**
     * method which reboots VNX
     *
     * @param vnx - enum
     *
     */
    public void rebootVnx(VNX vnx) {
        mLogger.info(EcsAction.REBOOTING, "", "VNX", vnx.vnxName());
        String result = sendCommand(String.format(REBOOT_VNX_CMD, vnx.ipAddress()));
        if (result.contains("Error") || result.contains("error")) {
            throw new EcsOpenStackException("Reboot of VNX " + vnx.vnxName() + " failed");
        }
        mLogger.info(Verdict.REBOOTED, "", "VNX", vnx.vnxName() + " rebooted successfully");
    }

    @Override
    public String sendCommand(String command) {
        initializeSession();
        return mSshSession.send(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startScalabilityCollection() {
        if (runCheck(FUEL_SCALABILITY_DIRECTORY, "scalability_script.sh", 60) > 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startStabilityCollection() {
        if (runCheck(FUEL_STABILITY_DIRECTORY, "stat_script.sh", 120) > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyAtdIsRunning() {
        throw new EcsNotImplementedException("Service atd currently does not run on target Fuel");
    }

    /**
     * waits for all fuel utils to be ready/active
     */
    public void waitAndVerifyAllFuelUtilsReady() {
        new LoopHelper<Boolean>(Timeout.RECONNECT_SSH_CONNECTION,
                "Was not able to verify that all fuel utils are ready ", true, () -> {
                    mLogger.info(EcsAction.VALIDATING, EcsFuel.class, " all fuel utils are ready");
                    return areAllFuelUtilsReady();
                }).run();
        mLogger.info(Verdict.VALIDATED, EcsFuel.class, " all fuel utils are ready");
    }

    /**
     * waits for all cic/compute hosts on fuel to be online
     */
    public void waitAndVerifyAllHostsOnline() {
        new LoopHelper<Boolean>(Timeout.RECONNECT_SSH_CONNECTION, "Was not able to verify that all hosts are online ",
                true, () -> {
                    mLogger.info(EcsAction.VALIDATING, EcsFuel.class, " all hosts are ready on fuel");
                    return areAllHostsOnline();
                }).run();
        mLogger.info(Verdict.VALIDATED, EcsFuel.class, " all hosts are ready on fuel");
    }

    /**
     * waits for all tasks on fuel to be ready/online
     */
    public void waitAndVerifyAllTasksReady() {
        new LoopHelper<Boolean>(Timeout.RECONNECT_SSH_CONNECTION,
                "Was not able to verify that all fuel tasks are ready ", true, () -> {
                    mLogger.info(EcsAction.VALIDATING, EcsFuel.class, " all tasks are ready on fuel");
                    return areAllTasksReady();
                }).run();
        mLogger.info(Verdict.VALIDATED, EcsFuel.class, " all tasks are ready on fuel");
    }

    @Override
    public boolean waitForSshConnection() {
        throw new EcsNotImplementedException("Ssh processes restart not fully implemeted/tested on fuel.");
    }
}

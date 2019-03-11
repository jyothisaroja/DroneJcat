package com.jcat.cloud.fw.components.model.target;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService.Result;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSession;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSessionFactory;
import com.jcat.cloud.fw.components.system.cee.services.fee.EcsFeeService;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;

/**
 * This class contains available functionality to a Compute blade
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
 * @author epergat 2015-01-12 Added skeleton method for further implementation.
 * @author eedsla 2015-02-03 use hostname instead of id to identify computeBlade, removed class variable mSesion so that
 *         SSH session is inherited from EcsOperatingSystem
 * @author eelimei 2015-03-18 Add usage of EcsFeeService
 * @author eelimei 2016-01-08 Add methods areAllMultipathConnectionsIntact/multipathInstalled and
 *         getRebootTimeStampInfoForNovaCompute
 * @author zdagjyo - 2017-02-17 - Add enum CicName and methods areNtpServersReachable, disableNtpServers ,
 *         doesAnyCicExist, enableNtpServers and getNtpReferenceServerIps
 * @author zdagjyo 2017-03-15 Add methods getBayNumber, getBaySubrack and getComputeMatcher
 * @author zpralak 2017-05-31 Modify method getLastSigtermTimestamp
 * @author zpralak 2017-05-05 Add methods getMacAddressOfInstance and getvCpuPinningInfo
 * @author zpralak 2017-06-05 Add method getAvailableCpu
 * @author zdagjyo 2017-08-28 Add methods destroyFuelVm, disableFuelAutostart,
 *         enableFuelAutostart, isConnectedViaFuel, isFuelVmActive, setConnectionViaFuel, setConnectionViaLxc,
 *         startFuelVm, overloaded method transferLargeFileToCompute, waitForFuelDown and waitForFuelUp.
 * @author zpralak 2017-09-14 Add enum NumaNode Add method getMaximumNumberOfPossibleServersCanBeBooted
 * @author zdagjyo 2017-12-01 Add methods dumpXML and getInstanceIds
 * @author zdagjyo 2017-01-25 Add method setIsFailSafeEnabled
 * @author zmousar 2018-01-22 Add method doesActiveFuelExist
 * @author zdagjyo 2018-01-25 Add method getRemoteFile
 * @author zdagjyo 2018-02-12 Add method sendCommand
 * @author zmousar 2018-01-12 Add method virshRebootVm
 */
public class EcsComputeBlade extends EcsTarget {

    public enum NetworkInterfaceState {
        UP, DOWN;
    }

    public enum NumaNode {
        NUMA0("node0"), NUMA1("node1");

        private final String mNumaNodeName;

        NumaNode(String name) {
            mNumaNodeName = name;
        }

        public String getName() {
            return mNumaNodeName;
        }
    }

    public enum vCicVmName {

        VCIC_VM_1("cic-1_vm"), VCIC_VM_2("cic-2_vm"), VCIC_VM_3("cic-3_vm");
        private final String mName;

        vCicVmName(String name) {
            mName = name;
        }

        public String cicName() {
            return mName;
        }
    }

    private static final String CMD_GREP_MAC_ADDRESS_FORMAT = "ifconfig %s | grep -o \"HWaddr.*\" | awk '{print $2}'";
    private static final String CMD_INSTANCE_ID = "virsh list --all | grep instance | awk '{print $2}'";
    private static final String CMD_XML_DUMP = "virsh dumpxml %s";
    private static final String CMD_MAC_ADDRESS_INSTANCE_FORMAT = "virsh dumpxml %s | grep -o \"mac address.*\" | grep -o \"'[^']\\+'\"";
    private static final String CMD_BREAK_LINK_FORMAT = "ip link set %s down";
    private static final String CMD_RESTORE_LINK_FORMAT = "ip link set %s up";
    private static final String CMD_SHOW_LINK_FORMAT = "ip link show %s";
    private static final String CMD_CHECK_MULTIPATH_CONNECTIONS = "multipath -ll";
    private static final String CMD_FUEL_EXIST = "sudo virsh list --all|grep fuel_master";
    private static final String COMPUTE_STABILITY_DIRECTORY = "/var/lib/nova/.Stab";
    private static final String COMPUTE_SCALABILITY_DIRECTORY = "/var/lib/nova/.Scalability";
    private static final String DISABLE_NTP_CMD = "route add -host %s reject";
    private static final String ENABLE_NTP_CMD = "route del -host %s reject";
    private static final String NTP_REFERENCE_SERVER_IP_CMD = "egrep '^server|^peer' /etc/ntp.conf|grep server|awk '{print $2}'";
    private static final String PING_CMD = "ping -c 5 %s";
    private static final String SCP_CMD = "sudo scp %s %s@%s:%s";
    private static final String TEST_DIR = "/var/lib/nova/copiedfile/";
    @Inject
    private EcsFuel mEcsFuel;
    @Inject
    private ComputeBladeSessionFactory mComputeBladeSessionFactory;
    private EcsFeeService mFeeService = null;
    private boolean mIsConnectedViaFuel = true;
    private final String mFuelIpAddress;
    private final int mFuelPortFwd;
    private final String mIpAddress;
    private final String mHostName;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsComputeBlade.class);

    /**
     * Constructor for GUICE.
     *
     * @param hostname - hostname of this compute blade
     * @param ipAddress - ip v4 for this compute blade
     * @param fuelIpAddress - IP address for Fuel
     * @param fuelPortFwd - port forwarding on Fuel for connecting to this compute blade
     */
    @Inject
    public EcsComputeBlade(@Assisted("hostname") String hostName, @Assisted("ipAddress") String ipAddress,
            @Assisted("fuelIpAddress") String fuelIpAddress, @Assisted("fuelPortFwd") int fuelPortFwd) {
        mHostName = hostName;
        mIpAddress = ipAddress;
        mFuelIpAddress = fuelIpAddress;
        mFuelPortFwd = fuelPortFwd;
    }

    private static boolean isLong(String s) {
        try {
            Long.parseLong(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }

        return true;
    }

    private Long getBootUptimeInEpochSec() {
        String bootTimeSinceUnixEpoch = sendCommand("cat /proc/stat |grep btime").replace("btime ", "").trim();
        if (!isLong(bootTimeSinceUnixEpoch)) {
            throw new EcsTargetException("Parsing failed while getting the boot time stamp");
        } else {
            return Long.parseLong(bootTimeSinceUnixEpoch);
        }
    }

    /**
     * Method to get the matcher object for compute host subrack and baynumber.
     * For example, if the compute host name is 'compute-0-1.domain.tld', this method would return matcher object with groups 0 and 1.
     *
     * @return Matcher - the matcher object
     */
    private Matcher getComputeMatcher() {
        Matcher matcher = null;
        String name = getHostname();
        Pattern pattern = Pattern.compile("-(\\d+)-(\\d+)");
        Matcher match = pattern.matcher(name);
        if (match.find()) {
            matcher = match;
        }
        return matcher;
    }

    private String getLastSigtermTimestamp() {
        String pathFile = sendCommand("ls /var/log/mcollective.log*").trim();
        String rebootCommandsResult = "";
        // There could be more than one log file
        for (String messageFile : pathFile.split(" |\n")) {
            messageFile = messageFile.trim();
            if (messageFile.isEmpty()) {
                continue;
            }
            String get_reboot_command_ts = "cat " + messageFile + " |grep 'SIGTERM'";
            rebootCommandsResult = sendCommand(get_reboot_command_ts).trim();
            if (rebootCommandsResult.contains("SIGTERM")) {
                break;
            }
        }
        if (!rebootCommandsResult.contains("SIGTERM")) {
            throw new EcsTargetException("Was not able to parse timestamp from /var/log/mcollective.log");
        }
        // If there are more than one reboot command, we want the latest.
        String[] rebootCommands = rebootCommandsResult.split("\n");
        String lastRebootCommand = rebootCommands[rebootCommands.length - 1];
        Pattern timestampPattern = Pattern.compile("(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]");
        Matcher matcher = timestampPattern.matcher(lastRebootCommand);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new EcsTargetException("Was not able to parse timestamp from mcollective.log");
        }

        // TODO if this does not work we should also see if any of the log files are compressed and therefore could not
        // be read.
        // get_reboot_command_ts="zgrep 'exiting on signal 15' '%s'" % file_list[i]
    }

    private long getProcessUpTimeInEpochSec(String pid) {
        String cmdGetUpTime = "ps -p " + pid + " -o etime=";
        String uptime = sendCommand(cmdGetUpTime).trim();
        String[] timeVector = uptime.split(":|-");
        if (timeVector.length < 2 || timeVector.length > 4) {
            throw new EcsTargetException("Tried to get process up time of pid " + pid + " using cmd " + cmdGetUpTime
                    + " but was not able to parse the answer.");
        }
        for (String number : timeVector) {
            if (!isLong(number)) {
                throw new EcsTargetException("Tried to get process up time of pid " + pid + " using cmd " + cmdGetUpTime
                        + " but was not able to parse the answer.");
            }
        }
        long secondsSinceStart = Long.parseLong(timeVector[timeVector.length - 1])
                + (60 * Long.parseLong(timeVector[timeVector.length - 2]));
        if (timeVector.length > 2) {
            secondsSinceStart = secondsSinceStart + (60 * 60 * Long.parseLong(timeVector[timeVector.length - 3]));
        }
        if (timeVector.length == 4) {
            secondsSinceStart = secondsSinceStart + (60 * 60 * 24 * Long.parseLong(timeVector[timeVector.length - 4]));
        }

        return Instant.now().getEpochSecond() - secondsSinceStart;

    }

    private void initializeSession() {
        if (mSshSession == null) {
            mSshSession = mComputeBladeSessionFactory.create(mIpAddress, mFuelIpAddress, mFuelPortFwd);
            mSshSession.setHostname(mHostName);
        }
    }

    /**
     * Waits for fuel_master to go down on the current blade
     */
    private void waitForFuelDown() {
        new LoopHelper<Boolean>(Timeout.FUEL_STATE_CHANGE, "Was not able to verify that fuel is down", true, () -> {
            return !isFuelVmActive();
        }).setIterationDelay(10).run();
    }

    /**
     * Waits for fuel_master to be up on the current blade
     */
    private void waitForFuelUp() {
        new LoopHelper<Boolean>(Timeout.FUEL_STATE_CHANGE, "Was not able to verify that fuel is online", true, () -> {
            return isFuelVmActive();
        }).setIterationDelay(10).run();
    }

    /**
     * Checks all multipath connections
     *
     * @return true unless any connection is listed as "faulty" or "failed"
     */
    public boolean areAllMultipathConnectionsIntact() {
        if (!multipathInstalled()) {
            return false;
        }
        String cmdResult = sendCommand(CMD_CHECK_MULTIPATH_CONNECTIONS);
        return !(cmdResult.contains("faulty") || cmdResult.contains("failed"));
    }

    /**
     * Method to check if NTP servers of the compute blade are accessible.
     * Note:ICMP from virt computes is not allowed, so it is not possible to verify connection with ping.
     * However when the NTP servers are disabled, ping will return "no route to host".
     * @return - boolean
     */
    public boolean areNtpServersReachable() {
        boolean reachable = false;
        String result;
        for (String ip : getNtpReferenceServerIps()) {
            result = sendCommand(String.format(PING_CMD, ip));
            if (!result.contains("No route to host")) {
                reachable = true;
            }
        }
        return reachable;
    }

    @Override
    public Boolean deinitialize() {
        if (mFeeService != null) {
            mFeeService.deinitialize();
        }
        if (!mIsConnectedViaFuel) {
            setConnectionViaFuel();
        }
        if (mSshSession != null) {
            mSshSession.disconnect();
        }
        return true;
    }

    /**
     * Destroys fuel_master on the current blade
     *
     * @return true if fuel is destroyed, false otherwise
     */
    public boolean destroyFuelVm() {
        setConnectionViaLxc();
        if (!isFuelVmActive()) {
            throw new EcsTargetException(getHostname() + " does not host active fuel vm");
        }
        String output = sendCommand("sudo virsh destroy fuel_master");
        boolean fuelDestroyed = output.contains("destroyed");
        mSshSession.disconnect();
        if (fuelDestroyed) {
            waitForFuelDown();
            return true;
        }
        return false;
    }

    /**
     * Disables autostart of fuel_master on the current blade
     *
     * @return true if autostart is disabled, false otherwise
     */
    public boolean disableFuelAutostart() {
        String result = sendCommand("sudo virsh autostart --disable fuel_master");
        return result.contains("unmarked as autostarted");
    }

    /**
     * Method to disable the NTP servers of a compute blade.
     */
    public void disableNtpServers() {
        mLogger.info(EcsAction.DISABLING, "", "all remote ntp servers ", "on " + getHostname());
        for (String ip : getNtpReferenceServerIps()) {
            mLogger.info(EcsAction.DISABLING, "", "Remote ntp server " + ip, "on " + getHostname());
            sendCommand(String.format(DISABLE_NTP_CMD, ip));
            mLogger.info(Verdict.DISABLED, "", "Remote ntp server " + ip, "on " + getHostname());
        }
        mLogger.info(Verdict.DISABLED, "", "all remote ntp servers ", "on " + getHostname());
    }

    /**
     * Checks if any cic resides on the current Compute Blade.
     *
     * @return boolean
     */
    public boolean doesAnyCicExist() {
        return (doesVmExist(vCicVmName.VCIC_VM_1.cicName()) || doesVmExist(vCicVmName.VCIC_VM_2.cicName())
                || doesVmExist(vCicVmName.VCIC_VM_3.cicName()));
    }

    /**
     * Check if fuel_master is running on compute blade
     *
     * @return boolean
     */
    public boolean doesActiveFuelExist() {
        String result = sendCommand(CMD_FUEL_EXIST);
        return result.contains("fuel_master") && result.contains("running");
    }

    /**
     * Checks if a VM resides on the current Compute Blade.
     *
     * @param hypervisorId
     * @return
     */
    public boolean doesVmExist(String hypervisorId) {
        return sendCommand("virsh list").contains(hypervisorId);
    }

    /**
     * Returns the output of "virsh dumpxml" on the specified instance.
     *
     * @param instanceId - String - the id of the instance to dump xml
     * @return String - output of 'dumpxml' command
     */
    public String dumpXML(String instanceId) {
        String output = sendCommand(String.format(CMD_XML_DUMP, instanceId));
        if (output.contains("error")) {
            throw new EcsTargetException("Failed to dump xml on instance id: " + instanceId + ", blade:" + getHostname()
                    + ", received command output:" + output);
        }
        return output;
    }

    /**
     * Enables autostart of fuel_master on the current blade
     *
     * @return true if autostart is enabled, false otherwise
     */
    public boolean enableFuelAutostart() {
        String result = sendCommand("sudo virsh autostart fuel_master");
        return result.contains("marked as autostarted");
    }

    /**
     * Method to enable the NTP servers of a compute blade.
     */
    public void enableNtpServers() {
        mLogger.info(EcsAction.ENABLING, "", "all remote ntp servers ", "on " + getHostname());
        for (String ip : getNtpReferenceServerIps()) {
            mLogger.info(EcsAction.ENABLING, "", "Remote ntp server " + ip, "on " + getHostname());
            sendCommand(String.format(ENABLE_NTP_CMD, ip));
            mLogger.info(Verdict.ENABLED, "", "Remote ntp server " + ip, "on " + getHostname());
        }
        mLogger.info(Verdict.ENABLED, "", "all remote ntp servers ", "on " + getHostname());
    }

    /**
     * Get a list of available physical cpus on NUMA nodes
     * which can be used to pin virtual cpus
     *
     * @param numaNode - NUMA node where physical cpus will be available.
     * @return List - list of available physical cpus on NUMA nodes
     */
    public List<Integer> getAvailableCpu(NumaNode numaNode) {
        String lsCpuOutput = sendCommand(String.format("lscpu|grep -o %s.*|grep -o [2-9].*", numaNode.getName()));
        List<Integer> availableCpuList = new ArrayList<Integer>();
        Matcher matcher = Pattern.compile("(^([\\d+,])+$)").matcher(lsCpuOutput.trim());
        if (matcher.matches()) {
            Matcher m = Pattern.compile("(\\d+)").matcher(matcher.group());
            while (m.find()) {
                availableCpuList.add(Integer.parseInt(m.group()));
            }
            return availableCpuList;
        } else {
            throw new EcsOpenStackException("Failed to get available cpus on numa nodes");
        }
    }

    /**
     * Method to get the bay number of a compute blade.
     *
     * @return int - the bay number of the compute blade
     */
    public int getBayNumber() {
        String bayNumber = getComputeMatcher().group(2);
        return Integer.parseInt(bayNumber);
    }

    /**
     * Method to get the subrack number of a compute blade.
     *
     * @return int - the subrack number of the specified compute blade
     */
    public int getBaySubrack() {
        String subRack = getComputeMatcher().group(1);
        return Integer.parseInt(subRack);
    }

    /**
     *
     * @return fee service instance
     */
    public EcsFeeService getFeeService() {
        if (mFeeService == null) {
            EcsSession feeSession = mComputeBladeSessionFactory.create(mIpAddress, mFuelIpAddress, mFuelPortFwd);
            mFeeService = new EcsFeeService(feeSession);
        }
        return mFeeService;
    }

    /**
     * Retrieves ids of VMs created on this blade.
     *
     * @return List<String> - list of IDs of VMs on this blade
     */
    public List<String> getInstanceIds() {
        String[] instances = sendCommand(CMD_INSTANCE_ID).split("\n");
        List<String> instanceIds = Arrays.asList(instances);
        instanceIds.replaceAll(String::trim);
        return instanceIds;
    }

    public String getIpAddress() {
        return mIpAddress;
    }

    public String getMacaddress(String networkInterface) {
        return sendCommand(String.format(CMD_GREP_MAC_ADDRESS_FORMAT, networkInterface)).trim();
    }

    /**
     * Get an info string with mac address of instance
     *
     * @param instanceId
     * @return string with mac address of instance
     */
    public String getMacAddressOfInstance(String instanceId) {
        String macAddressOfInstance = sendCommand(String.format(CMD_MAC_ADDRESS_INSTANCE_FORMAT, instanceId)).trim();
        ArrayList<String> aList = new ArrayList<>(Arrays.asList(macAddressOfInstance.split("\n")));
        for (String macAddress : aList) {
            if (!macAddress.trim().matches("\\S+\\:\\S+\\:\\S+\\:\\S+\\:\\S+\\:\\S+")) {
                throw new EcsOpenStackException("Failed to get mac address of instance" + instanceId);
            }
        }
        return macAddressOfInstance;
    }

    /**
     * Calculates maximum possible number of servers that can be booted on specified numa node of each compute blade
     *
     * To boot a server, the cpus and hugepages must be available on compute blade, So by considering these two parameters
     * this method calculates maximum possible number of servers that can booted on compute blade
     *
     * @param requiredHugePagesPerServer - Required hugepages to launch a VM
     * @param requiredVcpu - Required vcpus to launch a VM
     * @param numaNode - Numa node on which maximum possible number of bootable servers to be calculated
     *
     * @return Integer - returns maximum possible number Of servers that can be booted  on specified numa node of each compute blade
     * @throws IOException
     */
    public int getMaximumNumberOfPossibleServersCanBeBooted(int requiredHugePagesPerServer, int requiredVcpu,
            NumaNode numaNode) throws IOException {
        int numberOfServers = 0;
        String computeHost = getHostname().substring(0, getHostname().indexOf("."));
        List<Integer> totalCpuOnSpecifiedNumaNode = mEcsFuel.getTotalVcpuOnSpecifiedNumaNodeOfCompute(computeHost,
                numaNode);
        List<Integer> freeIsolatedCpuOnBothNumaNodes = mEcsFuel.getFreeIsolatedCpuOnBothNUmaNodesOfCompute(computeHost);
        // Finding total number of available vcpus on specified numa node
        List<Integer> availableCpuOnNuma = freeIsolatedCpuOnBothNumaNodes.stream()
                .filter(totalCpuOnSpecifiedNumaNode::contains).collect(Collectors.toList());
        int totalAvailableHugePagesOnNumaNode = mEcsFuel.getFreeHugePagesOnSpecifiedNumaNodesOfCompute(computeHost,
                numaNode);
        if (totalAvailableHugePagesOnNumaNode >= requiredHugePagesPerServer) {
            int serversCanBootWithHugePage = totalAvailableHugePagesOnNumaNode / requiredHugePagesPerServer;
            int serversCanBootWithVcpu = 0;
            if (availableCpuOnNuma.size() >= requiredVcpu) {
                serversCanBootWithVcpu = availableCpuOnNuma.size() / requiredVcpu;
            }
            if (serversCanBootWithHugePage <= serversCanBootWithVcpu) {
                numberOfServers += serversCanBootWithHugePage;
            } else {
                numberOfServers += serversCanBootWithVcpu;
            }
        }
        return numberOfServers;
    }

    public NetworkInterfaceState getNetworkInterfaceState(String networkInterface) {
        String command = String.format(CMD_SHOW_LINK_FORMAT, networkInterface);
        String result = sendCommand(command);
        if (result.matches("[\\s\\S]* state UP [\\s\\S]*")) {
            return NetworkInterfaceState.UP;
        } else if (result.matches("[\\s\\S]* state DOWN [\\s\\S]*")) {
            return NetworkInterfaceState.DOWN;
        } else {
            throw new EcsTargetException(String.format("Could not find link state for %s. Command %s got response %s.",
                    networkInterface, command, result));
        }
    }

    /**
     * Method to get the IP addresses of NTP servers of a compute blade.
     *
     * @return - List<String> - list of IP addresses
     *
     */
    public List<String> getNtpReferenceServerIps() {
        List<String> ntpServerIps = new ArrayList<String>();
        String[] output = sendCommand(NTP_REFERENCE_SERVER_IP_CMD).split("\n");
        if (!(output[0].startsWith("192") || output[0].startsWith("172"))) {
            String[] ntpIPs = new String[output.length];
            Pattern pattern = Pattern.compile("(\\d+).(\\d+).(\\d+).(\\d+)");
            Matcher match;
            for (int i = 0; i < output.length; i++) {
                match = pattern.matcher(output[i]);
                if (!match.find()) {
                    throw new EcsTargetException("Was not able to parse NTP reference IPs for " + getHostname());
                }
                ntpIPs[i] = match.group();
            }
            ntpServerIps = Arrays.asList(ntpIPs);
            ntpServerIps.replaceAll(String::trim);
        }
        return ntpServerIps;
    }

    /**
     * Get an info string with timestamp of last reboot, when the terminal signal was received and process uptime for
     * nova-compute
     *
     * @return info string with timestamps
     */
    public String getRebootTimeStampInfoForNovaCompute() {
        String processId = getProcessIdOfService("nova-compute");

        String sigtermTimestamp = getLastSigtermTimestamp();
        Date bootTimestamp = new Date(getBootUptimeInEpochSec() * 1000);
        Date processTimeStamp = new Date(getProcessUpTimeInEpochSec(processId) * 1000);

        String infoString = "Sigterm received at:" + sigtermTimestamp + "\n" + "Boot started at:"
                + bootTimestamp.toString() + "\n" + "Process nova-compute started at:" + processTimeStamp.toString();
        return infoString;
    }

    /**
     * method which copies a remote file from compute to local
     *
     * @param path - the remote file path
     * @return a File represents the copied local file
     */
    public File getRemoteFile(String remotePath, String fileName) {
        mEcsFuel.getFileFromComputeBlade(this, remotePath + fileName, "/root/");
        File file = mEcsFuel.getRemoteFile("/root/", fileName);
        mEcsFuel.deleteFile("/root/", fileName);
        return file;
    }

    /**
     * Method that gets physical cpus that are allocated for each vcpu
     * For Example: Command ("virsh vcpupin instance-00000093|sed 1,2d") gives the output as below,
     *          0: 10,12,14,16
     *          1: 34,36,38,40
     * Method will return a Map that stores vcpu number and the list of allocated physical
     * cpus for that particular vcpu as shown below,
     *     {0 : [10 12 14 16]}, (1 : [34 36 38 40]}
     *
     * So from the result map we can say that
     * 1st vCPU will be selected from one of the physical CPUs 10 12 14 16
     * 2nd vCPU will be selected from one of the physical CPUs 34 36 38 40
     *
     * @param instanceId - For which cpu pinning info to be found
     * @return Map<Integer, List<Integer>> - A collection that stores vcpu  and the list of allocated physical cpus
     *         for that particular vcpu.
     */
    public Map<Integer, List<Integer>> getvCpuPinningInfo(String instanceId) {
        String cpuPinningInfoString = sendCommand("virsh vcpupin " + instanceId + "|sed 1,2d");
        Map<Integer, List<Integer>> cpuPinningInfoMap = new HashMap<Integer, List<Integer>>();
        Matcher m = Pattern.compile("(\\d+):\\s((\\d+)(,\\d+)*)").matcher(cpuPinningInfoString);
        while (m.find()) {
            List<Integer> cpuList = new ArrayList<Integer>();
            Matcher matcher = Pattern.compile("(\\d+)").matcher(m.group(2));
            while (matcher.find()) {
                cpuList.add(Integer.parseInt(matcher.group()));
            }
            cpuPinningInfoMap.put(Integer.parseInt(m.group(1)), cpuList);
        }
        if (!cpuPinningInfoMap.isEmpty()) {
            return cpuPinningInfoMap;
        } else {
            throw new EcsOpenStackException("Failed to get CPU Pinning info of instance" + instanceId);
        }
    }

    public boolean isConnectedViaFuel() {
        return mIsConnectedViaFuel;
    }

    /**
     * Checks if fuel_master is running on the current blade
     *
     * @return true if fuel_master is running, false if it is in shut off state
     */
    public boolean isFuelVmActive() {
        String result = sendCommand("sudo virsh list --all|grep fuel_master");
        if (!result.contains("fuel_master")) {
            throw new EcsTargetException(getHostname() + " does not host fuel vm");
        }
        return result.contains("running");
    }

    /**
     * Checks if multipath is available or not
     *
     * @return
     */
    public boolean multipathInstalled() {
        String cmdResult = sendCommand(CMD_CHECK_MULTIPATH_CONNECTIONS);
        return !cmdResult.contains("currently not installed")
                && (cmdResult.contains("active") || cmdResult.contains("ready"));
    }

    public void restoreLink(String networkInterface) {
        sendCommand(String.format(CMD_RESTORE_LINK_FORMAT, networkInterface));
    }

    @Override
    public String sendCommand(String command) {
        initializeSession();
        return mSshSession.send(command);
    }

    /**
     * Sets the connection to compute blade via fuel
     */
    public void setConnectionViaFuel() {
        if (!mIsConnectedViaFuel) {
            mIsConnectedViaFuel = true;
            if (mSshSession != null) {
                mSshSession.disconnect();
            }
            mSshSession = mComputeBladeSessionFactory.create(mIpAddress, mFuelIpAddress, mFuelPortFwd);
            mSshSession.setHostname(mHostName);
        }
    }

    /**
     * Sets the connection to compute blade via LXC
     */
    public void setConnectionViaLxc() {
        if (mIsConnectedViaFuel) {
            mIsConnectedViaFuel = false;
            String userName = getOpenstackAdminUser();
            String password = getOpenstackUsers().get(userName);
            if (mSshSession != null) {
                mSshSession.disconnect();
            }
            mSshSession = new ComputeBladeSession(mIpAddress, mFuelIpAddress, userName, password);
        }
    }

    /**
     * Manually set the isFailSafeEnabled boolean value
     *
     * @param boolean
     */
    public void setIsFailSafeEnabled(boolean isFailSafeEnabled) {
        initializeSession();
        ComputeBladeSession bladeSession = (ComputeBladeSession) mSshSession;
        bladeSession.setIsFailSafeEnabled(isFailSafeEnabled);
    }

    /**
     * Starts fuel_master on the current blade
     *
     * @return true if fuel is started, false otherwise
     */
    public boolean startFuelVm() {
        if (isFuelVmActive()) {
            throw new EcsTargetException(getHostname() + " already has active fuel vm");
        }
        if (mIsConnectedViaFuel) {
            setConnectionViaLxc();
        }
        String output = sendCommand("sudo virsh start fuel_master");
        boolean fuelStarted = output.contains("started");
        if (fuelStarted) {
            waitForFuelUp();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startScalabilityCollection() {
        if (runCheck(COMPUTE_SCALABILITY_DIRECTORY, "scalability_script.sh", 60) > 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startStabilityCollection() {
        int script1 = runCheck(COMPUTE_STABILITY_DIRECTORY, "stat_script.sh", 120);
        int script2 = runCheck(COMPUTE_STABILITY_DIRECTORY, "misc_script.sh", 120);
        if (script1 > 0 && script2 > 0) {
            return true;
        }
        return false;
    }

    public void takeDownLink(String networkInterface) {
        sendCommand(String.format(CMD_BREAK_LINK_FORMAT, networkInterface));
    }

    /**
     * Transfers specified file from current compute to specified compute host with ceeadm user.
     *
     * @param absoluteSrcFilePath - absolute path of file in current compute blade
     * @param targetHost - The destination compute blade where the file is to be copied
     * @return String - the absolute path of the copied file in destination compute blade.
     *                  (i.e., "/var/lib/nova/copiedfile/filename")
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public String transferLargeFileToCompute(String absoluteSrcFile, EcsComputeBlade targetHost)
            throws InterruptedException, ExecutionException {
        String targetFilePath = TEST_DIR;
        String fileName = absoluteSrcFile;
        if (absoluteSrcFile.contains("/")) {
            fileName = absoluteSrcFile.substring(absoluteSrcFile.lastIndexOf("/") + 1);
        }
        if (targetHost.doesFileExist(targetFilePath + fileName)) {
            targetHost.deleteFile(targetFilePath, fileName);
        }
        targetHost.createDirectory(targetFilePath, true);
        targetHost.changePermissions("777", targetFilePath);
        String adminUser = getOpenstackAdminUser();
        String adminUserPassword = getOpenstackUsers().get(adminUser);
        Long fileSize = getFileSize(absoluteSrcFile);
        boolean fileTransferred = transferLargeFileToCompute(absoluteSrcFile, targetHost, adminUser, adminUserPassword,
                targetFilePath, fileName, fileSize);
        if (!fileTransferred) {
            throw new EcsTargetException("Failed to transfer file between computes");
        }
        return targetFilePath + fileName;
    }

    /**
     * Transfers large file (with size upto 60GB) from current compute to specified compute host.
     * (When the file size is the largest, it takes around 15 minutes to copy file. So sendtimeout is set to
     * "Timeout.COPY_FUEL_IMAGE" which is 20 minutes, before sending the copy command. Once it is done, the
     * send timeout is again set back to default value which is 60 seconds).
     *
     * @param absoluteSrcFilePath - absolute path of file in current compute blade
     * @param targetHost - the destination compute blade where the file is to be copied
     * @param user - username for scp
     * @param password - password for scp
     * @param targetFilePath - the location in destination compute host where the file is to be copied
     * @param fileName - the name of the file that is to be copied
     * @param fileSize - the size of the file that is to be copied
     * @return boolean
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public boolean transferLargeFileToCompute(String absoluteSrcFilePath, EcsComputeBlade targetHost, String user,
            String password, String targetFilePath, String fileName, long fileSize)
            throws InterruptedException, ExecutionException {
        Runnable task1 = () -> {
            mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, EcsComputeBlade.class, "from compute "
                    + getIpAddress() + " to compute " + targetHost.getIpAddress() + " at location: " + targetFilePath);
            initializeSession();
            mSshSession.setNextPossiblePassword(password);
            mSshSession.setSendTimeoutMillis(Timeout.COPY_FUEL_IMAGE.getTimeoutInMilliSeconds());
            String result = sendCommand(
                    String.format(SCP_CMD, absoluteSrcFilePath, user, targetHost.getIpAddress(), targetFilePath));
            mSshSession.setSendTimeoutMillis(mSshSession.getUniversalSendTimeout());
            if (!result.contains("100%")) {
                throw new EcsTargetException(
                        "Was not able to transfer file between computes, got response : " + result);
            }
            mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, EcsComputeBlade.class, "from compute "
                    + getIpAddress() + " to compute " + targetHost.getIpAddress() + " at location: " + targetFilePath);
        };
        Runnable task2 = () -> {
            new LoopHelper<Boolean>(Timeout.COPY_FUEL_IMAGE, "copying file doesn't seem to finish in time",
                    Boolean.TRUE, () -> {
                        if (targetHost.doesFileExist(targetFilePath + fileName)) {
                            if (!targetHost.getFileSize(targetFilePath + fileName).equals(fileSize)) {
                                mLogger.info("File is still being copied");
                                return false;
                            }
                        } else {
                            mLogger.info("Copying file has not started yet");
                            return false;
                        }
                        return true;
                    }).setIterationDelay(60).run();
        };

        ParallelExecutionService service = new ParallelExecutionService();
        Map<Runnable, Result> tasks = new HashMap<Runnable, Result>();

        tasks.put(task1, Result.SUCCESS);
        tasks.put(task2, Result.SUCCESS);

        service.executeTasks(tasks);

        String taskResult1 = service.waitAndGetTaskResult(task1);
        String taskResult2 = service.waitAndGetTaskResult(task2);

        if (!(taskResult1 == null && taskResult2 == null)) {
            return false;
        }
        return true;
    }

    /**
     * Reboot the VM on compute blade
     * ex: root@compute-0-7:~# sudo virsh reboot instance-000000aa
     *     Domain instance-000000aa is being rebooted
     *
     * @param instanceName - Vm instance Name
     * @return
     */
    public boolean virshRebootVm(String instanceName) {
        String output = sendCommand("sudo virsh reboot " + instanceName);
        if (!output.contains("rebooted")) {
            throw new EcsTargetException("virsh reboot vm is not performed");
        }
        return true;
    }
}

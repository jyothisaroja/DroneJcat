package com.jcat.cloud.fw.components.model.compute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.NetworkCreate;
import org.openstack4j.model.compute.RebootType;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.model.target.EcsTarget;
import com.jcat.cloud.fw.components.system.cee.ecssession.VmSessionVirshFactory;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.EcsBlockDevice;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**
 * Class which collects parameters to build a VM
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-10-13 initial version
 * @author ethssce 2014-11-24 builder uses createName for generating names
 * @author ezhgyin 2014-11-27 added name option
 * @author epergat 2014-12-09 Moved methods from NovaController to this class to
 *         fit the new OO-architecture. Also added skeleton methods.
 * @author eqinann 2015-01-25 Rename to EcsVm and merged with EcsVirtualMachine
 * @author eedsla 2015-02-03 Added EcsVmBuilder option availabilityZone
 * @author ezhgyin 2015-02-13 add metadata option
 * @author ezhgyin 2015-02-24 add EcsCredentials
 * @auhtor epergat 2015-03-18 Added init of VmSession.
 * @author ezhgyin 2015-04-07 move EcsCredentials to separate class
 * @author eelimei 2015-06-22 Add implementation to reboot and add un-pause
 *         method
 * @author eelimei 2016-01-11 Add getName
 * @author eqinann 2016-09-22 Removed NovaServerCreateWithHint
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2016-11-16 Add personality option while booting VM
 * @author zdagjyo 2017-01-23 Add methods transferFileToVM and transferFileToVMWithMultiNic
 * @author zdagjyo 2017-03-07 Add methods generateLoad,
 *         isLoadGeneratorSupported and terminateLoadGeneration and overridden methods
 *         systemServiceAction and isServiceRunning
 * @author zdagjyo 2017-03-22 Add networkPort option while booting VM
 * @author zdagjyo 2017-03-31 Add methods fsfreeze and fsUnfreeze, modify existing fsfreeze method
 * @author zdagjyo 2017-04-01 Add methods isWatchdogEnabled and triggerWatchdog
 * @author zdagjyo 2017-04-20 Add methods bringInterfaceUp and isInterfaceUp
 * @author zdagjyo 2017-08-08 Add method makePartitionAndFilesystem
 * @author zdagjyo 2017-10-26 Add method getImage, modified private constructor to protected
 */
public class EcsVm extends EcsTarget {

    private static final String LOAD_CMD = "./loadCpu.py --noOfCpu %s &";

    public static class EcsVmBuilder {

        private boolean mIsBlockDeviceProvided = false;
        private boolean mIsNetworkIdProvided = false;
        private boolean mIsNetworkPortIdProvided = false;
        private final List<String> mNetworkIdList = Lists.newArrayList();
        private ServerCreateBuilder mServerCreateBuilder;

        /**
         * This method is only used for EcsVm.toBuilder()
         *
         * @param serverCreateBuilder
         * @param hints
         * @param credentials
         */
        private EcsVmBuilder(ServerCreateBuilder serverCreateBuilder) {
            mServerCreateBuilder = serverCreateBuilder;
        }

        protected EcsVmBuilder(String name, String imageId, String flavorId) {
            mServerCreateBuilder = Builders.server().name(name).image(imageId).flavor(flavorId);
        }

        public EcsVmBuilder addSchedulerHint(String hintKey, String hintValue) {
            mServerCreateBuilder = mServerCreateBuilder.addSchedulerHint(hintKey, hintValue);
            return this;
        }

        public EcsVmBuilder availabilityZone(String availabilityZone) {
            mServerCreateBuilder = mServerCreateBuilder.availabilityZone(availabilityZone);
            return this;
        }

        /**
         * Specify BlockDeviceMapping for VM. If this is provided by the user,
         * the VM will be booted from volume.
         *
         * @param ecsBlockDevice
         * @return
         */
        public EcsVmBuilder blockDevice(EcsBlockDevice ecsBlockDevice) {
            mServerCreateBuilder = mServerCreateBuilder.blockDevice(ecsBlockDevice.get());
            mIsBlockDeviceProvided = true;
            return this;
        }

        public EcsVm build() {
            if (!mNetworkIdList.isEmpty()) {
                mServerCreateBuilder = mServerCreateBuilder.networks(mNetworkIdList);
            }
            return new EcsVm(mServerCreateBuilder.build(), mIsNetworkIdProvided, mIsNetworkPortIdProvided,
                    mIsBlockDeviceProvided);
        }

        public EcsVmBuilder flavor(String flavorId) {
            mServerCreateBuilder = mServerCreateBuilder.flavor(flavorId);
            return this;
        }

        public EcsVmBuilder key(EcsKey key) {
            mServerCreateBuilder = mServerCreateBuilder.keypairName(key.getName());
            return this;
        }

        public EcsVmBuilder keypairName(String name) {
            mServerCreateBuilder = mServerCreateBuilder.keypairName(name);
            return this;
        }

        public EcsVmBuilder metaData(Map<String, String> metadata) {
            mServerCreateBuilder = mServerCreateBuilder.addMetadata(metadata);
            return this;
        }

        public EcsVmBuilder name(String name) {
            mServerCreateBuilder = mServerCreateBuilder.name(name);
            return this;
        }

        public EcsVmBuilder network(String networkId) {
            mIsNetworkIdProvided = true;
            mNetworkIdList.add(networkId);
            return this;
        }

        /**
         * Specify network port for the VM.
         *
         * @param portId - The id of the port
         *
         * @return
         */
        public EcsVmBuilder networkPort(String portId) {
            mIsNetworkIdProvided = true;
            mIsNetworkPortIdProvided = true;
            mServerCreateBuilder = mServerCreateBuilder.addNetworkPort(portId);
            return this;
        }

        /**
         * Specify personality for VM which is used to inject file while
         * creating VM.
         *
         * @param path
         *            Path of the file to be injected to VM
         *
         * @param contents
         *            contents of the file
         * @return
         */
        public EcsVmBuilder personality(String path, String contents) {
            mServerCreateBuilder = mServerCreateBuilder.addPersonality(path, contents);
            return this;
        }
    }

    private final EcsLogger mLogger = EcsLogger.getLogger(EcsVm.class);

    private Map<String, String> mHints = new HashMap<String, String>();
    private static Map<String, EcsVm> mVmWithNetwork = new HashMap<String, EcsVm>();
    private String mHypervisorHostname;
    private String mHypervisorInstanceId;
    private boolean mIsBlockDeviceProvided = false;
    private boolean mIsFrozen = false;
    private boolean mIsHintProvided = false;
    private boolean mIsNetworkIdProvided = false;
    private boolean mIsNetworkPortIdProvided = false;
    private String mNetworkId;
    private ServerCreate mServerCreate;
    private String mServerId;
    private EcsImage mEcsImage;
    protected String mVmIp;
    @Inject
    protected NovaController mNovaController;
    @Inject
    protected NeutronController mNeutronController;
    @Inject
    private VmSessionVirshFactory mVmSessionFactory;

    protected EcsVm(ServerCreate serverCreate, boolean isNetworkIdProvided, boolean isNetworkPortIdProvided,
            boolean isBlockDeviceProvided) {
        mServerCreate = serverCreate;
        mIsNetworkIdProvided = isNetworkIdProvided;
        mIsNetworkPortIdProvided = isNetworkPortIdProvided;
        mIsBlockDeviceProvided = isBlockDeviceProvided;
    }

    @Override
    protected String systemServiceAction(String serviceName, ServiceAction serviceAction) {
        String status = null;
        if (serviceAction == ServiceAction.STATUS) {
            if (serviceName.equals("qemu-guest-agent") && serviceAction.equals(ServiceAction.STATUS)) {
                status = sendCommand(String.format(SERVICE_CMD, serviceName, serviceAction) + "|grep running");
                if (status.contains("running")) {
                    return status;
                } else {
                    status = "not running";
                    return status;
                }
            }
        }
        return super.systemServiceAction(serviceName, serviceAction);
    }

    @Inject
    public EcsVm(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp) {
        mServerId = serverId;
        mEcsImage = ecsImage;
        mHypervisorHostname = hypervisorHostname;
        mHypervisorInstanceId = hypervisorInstanceId;
        mVmIp = vmIp;
        mNetworkId = networkId;
    }

    public static EcsVmBuilder builder(String imageId, String flavorId) {
        if (flavorId == null) {
            throw new EcsOpenStackException("Flavor id shoud not be null.");
        }
        return new EcsVmBuilder(ControllerUtil.createName(), imageId, flavorId);
    }

    /**
     * Brings the specified vlan interface up inside VM
     *
     * @param interfaceName - the name of the interface that is to
     *                        be brought up(as specified in /etc/networks/interfaces file)
     *
     */
    public void bringInterfaceUp(String interfaceName) {
        mLogger.info(EcsAction.ENABLING, "", "Network Interface", interfaceName + " on VM " + getHostname());
        sendCommand("ifup " + interfaceName);
        if (!isInterfaceUp(interfaceName)) {
            throw new EcsOpenStackException("Failed to enable interface " + interfaceName + " on VM " + getHostname());
        }
        mLogger.info(Verdict.ENABLED, "", "Network Interface", interfaceName + " on VM " + getHostname());
    }

    /**
     * At test-case end the cleanup method will be called to remove resources
     * made by this component.
     */
    @Override
    public Boolean deinitialize() {
        if (null != mSshSession) {
            mSshSession.disconnect();
        }
        return true;
    }

    /**
     * Sends fsfreeze command on specified directory.
     */
    public void fsfreeze(String directory) {
        String result = sendCommand("fsfreeze -f " + directory);
        if (result.contains("error") || result.contains("ERROR") || result.contains("failed to freeze")) {
            throw new EcsOpenStackException("fsfreeze command failed, command returned: " + result);
        }
        sync();
        mSshSession.disconnect();
    }

    /**
     * Sends a fsfreeze command on /mnt. This command freezes the root file
     * system. Therefore no command or operation can be done on the vm
     * afterwards. Trying to send any command to the Vm after will throw an
     * exception.
     */
    public void fsfreezeMnt() {
        String result = sendCommand("fsfreeze -f /mnt");
        if (result.contains("error") || result.contains("ERROR") || result.contains("failed to freeze")) {
            throw new EcsOpenStackException("fsfreeze command failed, command returned: " + result);
        }
        mIsFrozen = true;
        mSshSession.disconnect();
    }

    /**
     * Sends fsUnfreeze command on specified directory.
     */
    public void fsUnfreeze(String directory) {
        String result = sendCommand("fsfreeze -u " + directory);
        if (result.contains("error") || result.contains("ERROR") || result.contains("failed to unfreeze")) {
            throw new EcsOpenStackException("fs unfreeze command failed, command returned: " + result);
        }
    }

    /**
     * Starts load generation(loadCpu.py) program on a VM
     *
     * @param directory - the path to the file loadCpu.py
     * @param cpuCount - the no:of vCPUs (must be less than or equal to no:of vCPUs in the VM's flavor)
     *
     */
    public void generateLoad(String directory, int cpuCount) {
        if (!isLoadGeneratorSupported()) {
            throw new EcsOpenStackException(
                    "Load generation is not supported for the current VM as the VM image is not TC_STR6_491_IMAGE");
        }
        mLogger.info(EcsAction.STARTING, "", "Load generation", "on VM " + getHostname());
        changeDirectory(directory);
        String result = sendCommand(String.format(LOAD_CMD, cpuCount));
        if (result.contains("noOfCpu is larger than existing cores")) {
            throw new EcsOpenStackException(
                    "Failed to start load generating program as the no:of cpus used is greater than existing cores in VM");
        }
        mLogger.info(Verdict.STARTED, "", "Load generation", "on VM " + getHostname());
    }

    public ServerCreate get() {
        return mServerCreate;
    }

    /**
     * Returns the compute blade id
     *
     * @return
     */
    public String getHost() {
        return mHypervisorHostname;
    }

    /**
     * Returns the compute host where this server is running
     *
     * @return String - host where the server is running
     */
    public String getHostName() {
        return mNovaController.getHostName(mServerId);
    }

    /**
     * returns the id that's used when using the hypervisor.
     *
     * @return
     */
    public String getHypervisorId() {
        return mHypervisorInstanceId;
    }

    /**
     * Gets the openstack ID associated for this Virtual machine to be used in
     * other openstack methods.
     *
     * @return
     */
    public String getId() {
        return mServerId;
    }

    /**
     * Getter for VM's image
     *
     * @return EcsImage
     */
    public EcsImage getImage() {
        return mEcsImage;
    }

    public String getImageId() {
        return mServerCreate.getImageRef();
    }

    /**
     * * Get list of IP addresses the server has in a given network
     *
     * @return List<String> IPs the server has in a given networkName
     */
    public List<String> getIPs(String networkName) {
        return mNovaController.getIPs(mServerId, networkName);
    }

    /**
     * Get Metadata of VM
     *
     * @return Metadata of the VM
     */
    public String getMetadata() {
        return mNovaController.getMetadata(mServerId);
    }

    /**
     * Get VM's name from OpenStack
     *
     * @return name of the VM
     */
    public String getName() {
        return mNovaController.getVmName(mServerId);
    }

    public List<String> getNetworkNames() {
        return mNovaController.getNetworkNames(mServerId);
    }

    public List<? extends NetworkCreate> getNetworks() {
        return mServerCreate.getNetworks();
    }

    /**
     * Getter for VM's Password
     *
     * @return Password
     */
    public String getPassword() {
        return mEcsImage.getPassword();
    }

    @Override
    public String getProcessId(String processName) {
        // TODO: investigate: getProcessId does not work, why?
        throw new EcsNotImplementedException(
                "The method currently does not function properly for target vm, please contact the JCAT team");
    }

    @Override
    public String getProcessIdOfService(String serviceName) {
        // TODO: investigate: this method calls getProcessId which does not
        // work, see above.
        throw new EcsNotImplementedException(
                "The method currently does not function properly for target vm, please contact the JCAT team");
    }

    @Override
    public List<String> getServicesSupervisedByUpstart() {
        // TODO: investigate: when implemented, method returns "grep: * " as a
        // service. why?
        throw new EcsNotImplementedException(
                "The method currently does not function properly for target vm, please contact the JCAT team");
    }

    public boolean isBlockDeviceProvided() {
        return mIsBlockDeviceProvided;
    }

    public boolean isHintProvided() {
        return mIsHintProvided;
    }

    /**
     * Checks if the specified vlan interface of VM is up
     *
     * @return boolean
     */
    public boolean isInterfaceUp(String interfaceName) {
        String result = sendCommand("ip a");
        if (result.contains(interfaceName)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the VM supports load generation.(VM supports load generation only when it is booted from image TC_STR6_491_image
     * as this image has hw_qemu_guest_agent installed in it.)
     *
     * @return boolean
     */
    public boolean isLoadGeneratorSupported() {
        boolean supported = false;
        if (mEcsImage.getImage().getName().equals("TC_stR6_491_image")
                && mEcsImage.getImage().getAdditionalPropertyValue("hw_qemu_guest_agent") != null
                && mEcsImage.getImage().getAdditionalPropertyValue("hw_qemu_guest_agent").equals("yes")) {
            supported = true;
        }
        return supported;
    }

    public boolean isNetworkIdProvided() {
        return mIsNetworkIdProvided;
    }

    public boolean isNetworkPortIdProvided() {
        return mIsNetworkPortIdProvided;
    }

    @Override
    public boolean isServiceRunning(String service) {
        String status = systemServiceAction(service, ServiceAction.STATUS);
        return (status.contains("running") || status.contains("start/running") || status.contains("ACTIVE OK")
                || status.contains("is running") || status.equals(UNSUPPORTED_SERVICE_STATUS));
    }

    /**
     * Checks if the VM is reachable on all of its networks
     */
    public boolean isVmReachableOnAllNetworks() {
        boolean successfulPing = false;
        List<String> networkNames = getNetworkNames();
        for (String networkName : networkNames) {
            mLogger.info(EcsAction.PINGING, getName(), networkName, "on " + getHostname());
            successfulPing = isVmReachableOnNetwork(networkName);
            if (!successfulPing) {
                break;
            }
        }
        return successfulPing;
    }

    /**
     * Checks if the VM is reachable on any of its networks
     */
    public boolean isVmReachableOnAnyNetwork() {
        List<String> networkNames = getNetworkNames();
        boolean successfulPing = false;

        if (networkNames.size() == 0) {
            throw new EcsTargetException(
                    "Tried to ping a VM (id='" + getId() + "') which hasn't any networks attached to it");
        }
        // select the first found networkName
        mLogger.info(EcsAction.PINGING, getName(), networkNames.get(0), "on " + getHostname());
        successfulPing = isVmReachableOnNetwork(networkNames.get(0));
        if (successfulPing) {
            mLogger.info(Verdict.PINGED, getName(), networkNames.get(0), "on " + getHostname());
        }
        return successfulPing;
    }

    /**
     * Checks if the VM is reachable on the specified network
     *
     * @param networkName
     */
    public boolean isVmReachableOnNetwork(String networkName) {
        String networkId = mNeutronController.getNetworkIdByName(networkName);
        EcsVm testVmForPingCheck = mVmWithNetwork.get(networkId);
        if (testVmForPingCheck == null) {
            EcsVm testVm = EcsVm.builder(mEcsImage.getImage().getId(), mNovaController.getVmFlavorId(mServerId))
                    .network(networkId).build();
            testVmForPingCheck = mNovaController.createVm(testVm);
            mVmWithNetwork.put(networkId, testVmForPingCheck);
        }
        return testVmForPingCheck.pingIp(mVmIp);
    }

    /**
     * Checks if watchdog is enabled in a VM
     */
    public boolean isWatchdogEnabled() {
        return sendCommand("find /sys/bus/pci/ |grep 6300").contains("pci")
                && sendCommand("dmesg | grep 6300").contains("6300")
                && sendCommand("ls -la /dev/watchdog").contains("/dev/watchdog");
    }

    @Override
    public void kernelPanic() {
        throw new EcsNotImplementedException(
                "Kernel panic has not been verified for target VM, please contact the JCAT team");
    }

    /**
     * Makes partition and creates a file system inside the volume attached to the VM.
     * (Invoke this method only after attaching a volume to VM)
     * @param disk - String - The volume attached to the VM that is to be partitioned (eg: /dev/vdb)
     * @param device - String - The file system (eg: /dev/vdb1 if the attached volume is /dev/vdb)
     * @param directory - String - The absolute path of the directory to be created inside attached volume
     * @param fileName - String - The name of the file to be created in the directory
     * @param fileContent - String - The content of the file
     */
    public void makePartitionAndFilesystem(String disk, String device, String directory, String fileName,
            String fileContent) {
        changeUser("root");
        diskPartition(null, disk);
        buildFileSystem(FileSystemType.EXT4.type(), null, device);
        createDirectory(directory);
        String result = mountFileSystem(device, directory);
        if (!diskUsage().contains(directory)) {
            throw new EcsOpenStackException("Mounting of volume failed, mount command returned: " + result);
        }
        createFile(directory, fileContent, fileName);
        sync();
    }

    /**
     * Pauses the virtual machine
     *
     */
    public void pause() {
        mNovaController.pauseVm(mServerId);
    }

    /**
     * Ping an ip address without specifying the network namespace. This will
     * only be successful for a public ip address or for a private ip when
     * pinging from inside the private network.
     *
     * @param String
     *            ipAddress - ip address to ping
     * @return Boolean - true if ping result is 0% packet loss, otherwise false
     */
    public boolean pingIp(String ipAddress) {
        return pingIp(ipAddress, null);
    }

    /**
     *
     * Performs a vm SOFT reboot
     */
    @Override
    public void reboot() {
        mNovaController.rebootVm(mServerId, RebootType.SOFT);
    }

    /**
     * Resumes the virtual machine from suspended state.
     *
     * @return true if the VM could be started, otherwise false.
     */
    public void resume() {
        mNovaController.resumeVm(mServerId);
    }

    @Override
    public String sendCommand(String command) {
        if (mIsFrozen) {
            throw new EcsTargetException("The vm: " + mServerId + " has been frozed and no commands can be sent");
        }
        if (mSshSession == null) {
            mSshSession = mVmSessionFactory.create(mHypervisorHostname, mHypervisorInstanceId, mEcsImage.getUserName(),
                    mEcsImage.getPassword(), mEcsImage.getRegexPrompt());
        }
        return mSshSession.send(command);
    }

    @Override
    public boolean startScalabilityCollection() {
        throw new EcsNotImplementedException("Scalability collection is not done on vm.");
    }

    @Override
    public int startService(String service) {
        throw new EcsNotImplementedException("start service is not supported on VMs");
    }

    @Override
    public boolean startStabilityCollection() {
        throw new EcsNotImplementedException("Stability collection is not done on vm.");
    }

    @Override
    public void stopService(String service) {
        throw new EcsNotImplementedException("stop service is not supported on VMs");
    }

    /**
     * Suspends the virtual machine.
     *
     * @return
     */
    public void suspend() {
        mNovaController.suspendVm(mServerId);
    }

    /**
     * synchronize data on disk with memory
     *
     * returns any result string received.
     *
     */
    @Override
    public String sync() {
        return sendCommand("sync");
    }

    /**
     * Stops load generation(loadCpu.py) program on a VM
     */
    public void terminateLoadGeneration() {
        if (!isLoadGeneratorSupported()) {
            throw new EcsOpenStackException(
                    "Load generation is not supported for the current VM as the VM image is not TC_STR6_491_IMAGE");
        }
        mLogger.info(EcsAction.STOPPING, "", "Load generation", "on VM " + getHostname());
        String result1 = sendCommand("jobs");
        if (result1.contains("Running") && result1.contains("loadCpu.py")) {
            sendCommand("kill %1");
        }
        String result2 = sendCommand("jobs");
        if (result2.contains("Running") && result2.contains("loadCpu.py")) {
            LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.KILL_LOAD_GENERATION,
                    "failed to kill load generation as the 'jobs' command returned " + result2, Boolean.TRUE, () -> {
                        String output = sendCommand("jobs");
                        return !(output.contains("Running") && result2.contains("loadCpu.py"));
                    });
            loopHelper.setIterationDelay(10);
            loopHelper.run();
        }
        mLogger.info(Verdict.STOPPED, "", "Load generation", "on VM " + getHostname());
    }

    public EcsVmBuilder toBuilder() {
        return new EcsVmBuilder(mServerCreate.toBuilder());
    }

    @Override
    public String toString() {
        return String.format("EcsVm{id=%s, Image=%s, NetworkId=%s, Hints=%s, BlockDevice=%s}", mServerId, mEcsImage,
                mNetworkId, mHints, mIsBlockDeviceProvided);
    }

    /**
     * Copy file from one VM to another
     *
     * @param targetVM - EcsVm - VM where file is copied to
     * @param srcFilePath - String - location of the file in source VM
     * @param destFilePath - String - location of the file in destination VM
     * @param fineName - String - name of the file to be copied
     * @return - boolean
     */
    public boolean transferFileToVM(EcsVm targetVM, String srcFilePath, String destFilePath, String fileName) {
        String targetIp = targetVM.getIPs(targetVM.getNetworkNames().get(0)).get(0);
        return transferFileToVMWithMultiNic(targetVM, targetIp, srcFilePath, destFilePath, fileName);
    }

    /**
     * Copy file from one VM to another VM with multiple networks
     *
     * @param targetVM - EcsVm - VM where file is copied to
     * @param targetVMIp - String - IP Address of target VM
     * @param srcFilePath - String - location of the file in source VM
     * @param destFilePath - String - location of the file in destination VM
     * @param fineName - String - name of the file to be copied
     * @return - boolean
     */
    public boolean transferFileToVMWithMultiNic(EcsVm targetVM, String targetVMIp, String srcFilePath,
            String destFilePath, String fileName) {
        mLogger.info(EcsAction.TRANSFERING, srcFilePath + "/" + fileName,
                "from " + mNovaController.getVmName(mServerId), "to " + targetVM.getName());
        String result = sendCommand("scp " + srcFilePath + "/" + fileName + " " + targetVM.getUser() + "@" + targetVMIp
                + ":" + destFilePath);
        if (result.contains("Error") || result.contains("Connection refused") || result.contains("No route to host")) {
            throw new EcsOpenStackException("copying file to VM failed");
        }
        List<String> files = targetVM.listFiles("");
        if (files.isEmpty()) {
            return false;
        }
        mLogger.info(Verdict.TRANSFERED, srcFilePath + "/" + fileName, "from " + mNovaController.getVmName(mServerId),
                "to " + targetVM.getName());
        return true;
    }

    /**
     * Triggers watchdog in VM
     */
    public void triggerWatchdog() {
        mLogger.info(EcsAction.TRIGGERING, "", "Watchdog", "in VM " + getHostname());
        if (!isWatchdogEnabled()) {
            throw new EcsOpenStackException("Failed to trigger Watchdog as it is not enabled in VM " + getHostname());
        }
        String result = sendCommand("more /dev/watchdog");
        if (!result.contains("Permission denied")) {
            mLogger.info(Verdict.TRIGGERED, "", "Watchdog", "in VM " + getHostname());
        } else {
            throw new EcsOpenStackException(
                    "Failed to trigger Watchdog in VM " + getHostname() + ", command output is " + result);
        }
    }

    /**
     * Un-pause the vm
     */
    public void unPause() {
        mNovaController.unpauseVm(mServerId);
    }

    @Override
    public boolean verifyAtdIsRunning() {
        throw new EcsNotImplementedException("Service atd currently does not run on target Vm");
    }

    @Override
    public boolean waitForSshConnection() {
        throw new EcsNotImplementedException("Ssh processes restart not fully implemeted/tested on vm.");
    }
}

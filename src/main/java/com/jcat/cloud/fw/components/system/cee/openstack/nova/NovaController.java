package com.jcat.cloud.fw.components.system.cee.openstack.nova;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.api.compute.HostService;
import org.openstack4j.api.compute.ServerGroupService;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.api.compute.ext.ServicesService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.HostResource;
import org.openstack4j.model.compute.InstanceAction;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.NetworkCreate;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.QuotaSetUpdate;
import org.openstack4j.model.compute.RebootType;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.model.compute.VolumeAttachment;
import org.openstack4j.model.compute.actions.EvacuateOptions;
import org.openstack4j.model.compute.actions.RebuildOptions;
import org.openstack4j.model.compute.ext.Service;
import org.openstack4j.model.storage.block.Volume;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopInterruptedException;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVm;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasBatVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVm;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsBatVmFactory;
import com.jcat.cloud.fw.components.model.compute.EcsKey;
import com.jcat.cloud.fw.components.model.compute.EcsVm;
import com.jcat.cloud.fw.components.model.compute.EcsVm.EcsVmBuilder;
import com.jcat.cloud.fw.components.model.compute.EcsVmFactory;
import com.jcat.cloud.fw.components.model.identity.EcsCredentials;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.model.network.EcsNetwork;
import com.jcat.cloud.fw.components.model.network.EcsSubnet;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolume;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade.NumaNode;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.glance.GlanceController;
import com.jcat.cloud.fw.components.system.cee.openstack.keystone.KeystoneController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.EcsFlavor.PredefinedFlavor;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration.VmBootMode;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

import se.ericsson.jcat.fw.logging.JcatLoggingApi;

/**
 * This class contains procedures related with openstack nova.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-10-08 initial version
 * @author eelimei 2014-11-03 added methods for attach and detach volume from server
 * @author epergat 2014-12-01 Added EcsVirtalMachineFactory to be used in createVm.
 * @author ethssce 2015-01-16 added injection of GlanceController and methods createSnapshot() getServerStatus()
 * @author epergat 2015-01-20 Added more methods regarding actions regarding VM.
 * @author eedann 2015-01-27 Modified above mentioned methods regarding VM, added methods to start and stop a VM
 * @author eedann 2015-01-29 Added methods to reboot, rebuild and resize/confirm/reject a VM
 * @author ethssce 2015-01-27 modifications to fix waitForServerStatus
 * @author ehosmol 2015-02-03 Replace assertions with {@link EcsOpenStackException}
 * @author ezhgyin 2015-02-13 add updateVmMetadata method
 * @author ezhgyin 2015-02-24 modify createVm to support boot a VM with non admin tenant/user
 * @author ezhgyin 2015-03-04 modify createVm to create test network when networkId is not provided
 * @author uabjlm 2015-03-13 change checkIfServerExists from protected to public
 * @author ezhgyin 2015-03-17 add servicesList()
 * @author epergat 2015-03-18 Bugfix of Nullpointer exception when running loophelper.
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author epergat 2015-03-26 Added method doesVmExist()
 * @author eedsla 2015-05-05 Added methods waitForNovaServiceListUpdate, checkAllServicesStateUpStatusEnabled,
 *         waitForNovaServiceStatus and temporarily createVmNoCheckActive
 * @author epergat, ehosmol 2015-05-08 updated createSnapshot() method to copy image metadata.
 * @author eqinann 2015-05-22 Change create flavor to use EcsFlavor class
 * @author eedann 2015-05-27 added deleteVMwithoutCheck and getUserId methods
 * @author eedann 2015-06-25 added getServerFaultMessage, getNovaQuotaMemoryForTenant and updateNovaQuotaMemoryForTenant
 *         methods
 * @author epergat 2015-06-27 made cleanup more robust
 * @author emagnbr 2015-12-14 exchanged DstType and SrcType for new org.openstack4j.model.compute BDMDestType and
 *         BDMSourceType
 * @author eelimei 2016-01-11 Add getAllVmsForAllTenants, getServerName
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author xsrabal 2016-10-20 Added method forceMoveOfVm
 * @author zdagjyo 2016-12-23 Modify method checkAllServicesStateUpStatusEnabled
 * @author zdagjyo 2016-12-14 Add methods generateX509RootCertificate and waitForNovaServiceState
 * @author zdagjyo 2017-01-12 Added methods hostService,getCurrentlyUsedMemoryOnHost,getTotalCPUsOnHost,
 *         getTotalMemoryOnHost and setExtraSpecsForCpuPolicy
 * @author zpralak 2017-02-01 Added overloaded method of forceMoveOfVm
 * @author zdagjyo 2017-02-09 Modified methods buildAvailabilityZone and waitForNovaServiceStatus
 * @author zdagjyo 2017-03-06 Added method getVmImageId, Modified method createSnapshot to add deletionlevel
 * @author zdagjyo 2017-03-20 Added methods getNovaQuotaCoresForTenant, getNovaQuotaInstancesForTenant,
 *         updateNovaQuotaCoresForTenant and updateNovaQuotaInstancesForTenant and modified method updateNovaQuotaMemoryForTenant
 * @author zdagjyo 2017-03-22 Added enums CpuListValue, NumMemoryValue and method setExtraSpecs
 * @author zdagjyo 2017-03-22 Modified method createVm to support VM created with port id instead of network id
 * @author zdagjyo 2017-04-01 Added enum WatchdogAction, methods rebootStoppedVm and setExtraSpecsForWatchdogAction
 * @author zdagjyo 2017-05-09 Added methods createServerGroup, deleteServerGroup, migrateVm and serverGroupService
 *         and modified cleanup to include servergroup cleanup
 * @author zpralak 2017-05-05 Modified setExtraSpecs method and added overloaded method,
 *         Added generateCpuListValue method and modified NumaMemoryValue enum
 * @author zdagjyo 2017-08-18 Added methods getCurrentlyUsedDiskOnHost, getCurrentlyUsedVcpusOnHost and getFlavorById
 * @author zdagjyo 2017-10-10 Added methods getAtlasVm, getVmIdByName, modified method deleteVm to support atlasVm deletion
 *         and modified getEcsVm method to support atlasVm
 * @author zdagjyo 2017-11-15 Added methods getAtlasBatVm, getNovaQuotaInjectedFileContentForTenant and
 *         updateNovaQuotaInjectedFileContentForTenant
 * @author zdagjyo 2017-06-12 Added method getInstanceName
 * @author zdagjyo 2018-01-09 Added methods evacuateVm and waitAndVerifyAllServicesStateUpStatusEnabled
 * @author zmousar 2018-01-30 Added method getRebootTimestamps, virshRebootVm
 */
public final class NovaController extends EcsComponent {

    /**
     * Describes various values for the CpuList specification of a flavor
     */
    public enum CpuListValue {
        CpuFloat01("node1:{17,19,21,23}/node1:{39,41,43,45}"), CpuPin01("node1:{9}/node1:{33}"), CpuPin02(
                "node1:{11}/node1:{35}"), CpuPin03("node0:16/node0:40");

        private final String mCpuListValue;

        CpuListValue(String cpuListValue) {
            mCpuListValue = cpuListValue;
        }

        public String cpuListValue() {
            return mCpuListValue;
        }
    }

    /**
     * ForceMove Options
     */
    public static enum ForceMoveOption {
        IGNORE_BROKEN_DEPENDENCIES("--ignore-broken-dependencies"), IGNORE_HINTS("--ignore-hints");

        private final String mForceMoveOption;

        ForceMoveOption(String forceMoveOption) {
            mForceMoveOption = forceMoveOption;
        }

        public String forceMoveOption() {
            return mForceMoveOption;
        }
    }

    /**
     * Describes various values for the NumaMemory specification of a flavor
     */
    public enum NumaMemoryValue {
        Value2GB("node0:2097152"), Value4GB("node1:4194304"), Value16GB("node0:16777216"), Value4GBNode0(
                "node0:4194304");

        private final String mNumaMemoryValue;

        NumaMemoryValue(String numaMemoryValue) {
            mNumaMemoryValue = numaMemoryValue;
        }

        public String numaMemoryValue() {
            return mNumaMemoryValue;
        }
    }

    /**
     * Describes various watchdog actions for a flavor
     */
    public enum WatchdogAction {

        Pause("pause"), Poweroff("poweroff"), Reset("reset"), None("none");

        private final String mAction;

        WatchdogAction(String action) {
            mAction = action;
        }

        public String action() {
            return mAction;
        }
    }

    private static OpenStack4jEcs mOpenStack4jEcs;
    /**
     * List to keep track of created volume attachments
     */
    private final ConcurrentHashMap<String, VolumeAttachment> mAttachedVolumesByAttachmentId = new ConcurrentHashMap<String, VolumeAttachment>();
    /**
     * List to keep track of created flavors
     */
    private List<String> mCreatedFlavorIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created server groups
     */
    private List<String> mCreatedServerGroupIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created active servers
     */
    private final List<EcsVm> mSuccessfullyCreatedVms = new CopyOnWriteArrayList<EcsVm>();

    /**
     * List to keep track of servers which were created (but didn't reach status active)
     */
    private final List<String> mInitialAndUnsuccessfullyCreatedVmIds = new CopyOnWriteArrayList<String>();

    @Inject
    private EcsBatVmFactory mEcsBatVmFactory;

    @Inject
    private EcsVmFactory mEcsVmFactory;
    @Inject
    private EcsAtlasVmFactory mEcsAtlasVmFactory;
    @Inject
    private EcsAtlasBatVmFactory mEcsAtlasBatVmFactory;
    @Inject
    private GlanceController mGlanceController;
    @Inject
    private KeystoneController mKeystoneController;
    @Inject
    private NeutronController mNeutronController;
    @Inject
    private CinderController mCinderController;
    @Inject
    private EcsComputeBladeList mEcsComputeBladeList;
    private final TestConfiguration mTestConfiguration;
    @Inject
    protected EcsCicList mEcsCicList;
    private final EcsLogger mLogger = EcsLogger.getLogger(NovaController.class);

    private static final String ATLAS_VM_NETWORK_NAME = "tenant_3583";
    private static final String ATLAS_VM_NETWORK_NAME_BSP = "provider_51";
    private static final String BAT_ATLAS_VM_NETWORK_NAME = "tenant_3582";
    private static final String CPU_LIST = "hw:cpu_list";
    private static final String CPU_POLICY = "hw:cpu_policy";
    private static final String CPU_POLICY_VALUE = "dedicated";
    private static final String HUGE_PAGE = "hw:mem_page_size";
    private static final String HUGE_PAGE_VALUE = "1048576";
    /** HA_POLICY is used to specify the ha-policy for the VM. */
    private static final String HA_POLICY = "ha-policy";
    private static final String NUMA_MEMORY = "hw:numa_memory";
    private static final String WATCHDOG_ACTION = "hw:watchdog_action";
    Map<String, String> mExtraSpecMap = new HashMap<String, String>();

    /**
     * Main Constructor
     */
    @Inject
    private NovaController(OpenStack4jEcs openStack4jEcs, TestConfiguration testConfiguration) {
        mOpenStack4jEcs = openStack4jEcs;
        mTestConfiguration = testConfiguration;
    }

    /**
     * Use admin user to collect info for created VM
     *
     * @param serverId
     * @param imageId
     * @param networkId
     * @return
     */
    private EcsVm buildCreatedVm(String serverId, String imageId, String networkId) {
        // switch to admin user if necessary
        EcsCredentials previousCredentials = null;
        if (!mOpenStack4jEcs.currentCredentialsIsAdmin()) {
            previousCredentials = mOpenStack4jEcs.getCurrentCredentials();
            mOpenStack4jEcs.getClientForAdminUser(Facing.PUBLIC);
        }
        Server serverInstance = serverService().get(serverId);
        String hypervisorHostname = serverInstance.getHost();
        String hypervisorInstanceId = serverInstance.getInstanceName();
        EcsImage ecsImage = mGlanceController.getImage(imageId);
        String networkName = mNeutronController.getNetworkNameById(networkId);
        String vmIp = getIPs(serverId, networkName).get(0);
        EcsVm vm;
        if (serverInstance.getName().equals("atlas_vm")) {
            vm = mEcsAtlasVmFactory.create(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId,
                    vmIp);
        } else if (serverInstance.getName().contains("BAT")) {
            vm = mEcsBatVmFactory.create(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId,
                    vmIp);
        } else {
            vm = mEcsVmFactory.create(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId, vmIp);
            mSuccessfullyCreatedVms.add(vm);
        }
        // When we finally have created a vm we can remove it from the temporary List containing Server IDs
        // since we now know that it's not faulty.
        Iterator<String> iter = mInitialAndUnsuccessfullyCreatedVmIds.iterator();
        while (iter.hasNext()) {
            String currServerId = iter.next();
            if (currServerId.equals(serverId)) {
                mInitialAndUnsuccessfullyCreatedVmIds.remove(serverId);
            }
        }
        // switch to non-admin user if necessary
        if (null != previousCredentials) {
            mOpenStack4jEcs.getClient(Facing.PUBLIC, previousCredentials);
        }
        return vm;
    }

    private ComputeService computeService() {
        return ControllerUtil.checkRestServiceNotNull(getClient().compute(), ComputeService.class);
    }

    private String createTestNetwork() {
        EcsNetwork ecsNetwork = EcsNetwork.builder().build();
        String networkId = mNeutronController.createNetwork(ecsNetwork);
        try {
            EcsSubnet ecsSubnet = EcsSubnet.builder(networkId).build();
            mNeutronController.createSubnet(ecsSubnet);
        } catch (UnknownHostException e) {
            throw new EcsOpenStackException("Unable to create subnet in network for booting a VM");
        }
        return networkId;
    }

    /**
     * Delete all the VMs from list
     *
     * @param vmList - List - A list of VMs, can be a list of VM's IDs or a list of VM instances
     */
    private <T> void deleteAllVmsFromList(List<T> vmList) {
        if (vmList != null) {
            mLogger.debug("Try to delete all VMs from list: " + vmList);
            for (T item : vmList) {
                try {
                    deleteVm(item);
                } catch (Exception ex) {
                    String vmId = item.toString();
                    if (item instanceof EcsVm) {
                        vmId = ((EcsVm) item).getId();
                    }
                    mLogger.error(String.format("Failed to delete VM(id:%s), exception was: %s\n", vmId, ex)
                            + ex.getStackTrace());
                }
            }
        }
    }

    private void deleteVmHelper(String id) {
        serverService().delete(id);
        // check if server has been removed
        waitForServerDeleted(id);
    }

    private FlavorService flavorService() {
        return computeService().flavors();
    }

    private OSClientV3 getClient() {
        return mOpenStack4jEcs.getClient(Facing.PUBLIC);
    }

    private HostService hostService() {
        return computeService().host();
    }

    private ServerGroupService serverGroupService() {
        return computeService().serverGroups();
    }

    private ServerService serverService() {
        return computeService().servers();
    }

    private ServicesService servicesService() {
        return computeService().services();
    }

    private void setExtraSpecsForHugePage(String flavorId) {
        // set huge page spec
        String hugePageValue = flavorService().getSpec(flavorId, HUGE_PAGE);
        if (hugePageValue == null || Integer.parseInt(hugePageValue) < Integer.parseInt(HUGE_PAGE_VALUE)) {
            Map<String, String> spec = new HashMap<String, String>();
            spec.put(HUGE_PAGE, HUGE_PAGE_VALUE);
            flavorService().createAndUpdateExtraSpecs(flavorId, spec);
        }
    }

    private void waitForServerDeleted(final String serverId) {
        mLogger.info(EcsAction.DELETING, "", EcsVm.class,
                serverId + " in " + Timeout.SERVER_DELETE.getTimeoutInSeconds() + " seconds");
        new LoopHelper<Boolean>(Timeout.SERVER_DELETE, "Server " + serverId + " was still found after deletion",
                Boolean.TRUE, () -> {
                    Server serverToBeDeleted = serverService().get(serverId);
                    if (null != serverToBeDeleted) {
                        mLogger.debug("Server '" + serverToBeDeleted.getId() + "' still exists.");
                        mLogger.debug("Server Detail: '" + serverToBeDeleted);
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.DELETED, EcsVm.class, serverId);
    }

    /**
     * Checks if server instance reaches specified status within default timeout limit. Exception will be thrown if the
     * server hasn't reached expected status within that timeout.
     * The time used to check that the server has reached desired status are:
     * timeout = {@value #Timeout.SERVER_IMMEDIATE_STATUS_CHANGE} and iteration delay = {@value #ITERATION_DELAY}.
     *
     * @param serverId - String - id of the server
     * @param desiredStatus - {@link Status} - desired status of the server
     */
    private void waitForServerImmediateStatusChange(final String serverId, final Status desiredStatus) {
        String desiredStatusStr = desiredStatus.toString();
        mLogger.info(String.format("Wait for server %s to reach status %s within %s seconds.", serverId,
                desiredStatusStr, Timeout.SERVER_IMMEDIATE_STATUS_CHANGE.getTimeoutInSeconds()));
        String errorMessage = String.format("Server %s did not reach status: %s", serverId, desiredStatusStr);
        new LoopHelper<Status>(Timeout.SERVER_IMMEDIATE_STATUS_CHANGE, errorMessage, desiredStatus, () -> {
            Server.Status currentServerState = serverService().get(serverId).getStatus();
            mLogger.debug(String.format("Current server state:" + currentServerState.toString()));
            return currentServerState;
        }).run();
        mLogger.info(String.format("Server %s has reached status %s.", serverId, desiredStatusStr));
    }

    /**
     * Check if server with specified id exists within a specific amount of time.
     * The time used to check that the server exists are:
     * timeout = {@value #Timeout.SERVER_CREATE} seconds and iteration delay = {@value #ITERATION_DELAY} seconds.
     *
     * @param serverId - String - ID of the server
     */
    protected void checkIfServerExists(final String serverId) {
        mLogger.info(EcsAction.FINDING, "Exists", EcsVm.class,
                serverId + ", timeout= " + Timeout.SERVER_CREATE.getTimeoutInSeconds());
        String errorMessage = String.format("Could not find server %s", serverId);
        new LoopHelper<Boolean>(Timeout.SERVER_CREATE, errorMessage, Boolean.TRUE, () -> doesServerExist(serverId))
                .run();
        mLogger.info(Verdict.EXISTED, EcsVm.class, serverId);
    }

    /**
     * Check if a flavor already exists in the system.
     *
     * @param flavorId - String - id of the flavor
     * @return true if the flavor can be found
     */
    protected boolean doesFlavorExistWithId(String flavorId) {
        // Note: flavorService().get() does NOT work since even if a flavor is deleted, it will still be there
        List<? extends Flavor> flavors = flavorService().list();
        for (Flavor currentFlavor : flavors) {
            if (currentFlavor.getId().equals(flavorId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attaches a volume to a server. Returns the volumeAttachmentId. Throws exception if the volume
     * attachment was not properly created or if status available is not reached within time limit for volume change.
     *
     * @param volumeId - String - id of the volume to attach
     * @param serverId - String - id of the server to attach to
     * @param volume - String - device name
     * @return String - id of this volume attachment
     */
    public String attachVolumeToServer(final String volumeId, final String serverId, String device) {
        mLogger.info(EcsAction.ATTACHING, EcsVolume.class, EcsVm.class,
                serverId + ", volumeId= " + volumeId + ", device= " + device);
        VolumeAttachment volumeAttachment = serverService().attachVolume(serverId, volumeId, device);
        if (volumeAttachment == null) {
            throw new EcsOpenStackException("The volume could not be attached to the server");
        }
        mAttachedVolumesByAttachmentId.put(volumeAttachment.getId(), volumeAttachment);
        mCinderController.waitForVolumeStatus(volumeId, Volume.Status.IN_USE);
        mLogger.info(Verdict.ATTACHED, EcsVolume.class, EcsVm.class,
                serverId + ", volumeId= " + volumeId + ", device= " + device);
        return volumeAttachment.getId();
    }

    /**
     * Build the correct availability zone string for hostname
     *
     * @param hostname
     * @return
     */
    public String buildAvailabilityZone(String hostname) {
        String availabilityZone = "nova:" + hostname;
        if (!hostname.contains(".")) {
            availabilityZone += mEcsComputeBladeList.getComputeHostDomainName();
        }
        return availabilityZone;
    }

    /**
     * Check that all nova services have state == up and status == enabled
     *
     * @return boolean - true: all nova services have state == up and status == enabled
     */
    public boolean checkAllServicesStateUpStatusEnabled() {
        mLogger.info(EcsAction.VALIDATING, "", "Nova Services", "if nova services are up and enabled");
        List<EcsNovaService> novaServices = servicesList();
        boolean result = false;
        // check that state == up and status == enabled
        for (EcsNovaService service : novaServices) {
            if (service.getState().equals(EcsNovaService.State.UP)
                    && service.getStatus().equals(EcsNovaService.Status.ENABLED)) {
                result = true;
                mLogger.info(Verdict.VALIDATED, "", "Nova Service",
                        service.getBinary() + " is up and enabled on host " + service.getHost());
            } else {
                result = false;
                mLogger.error(String.format("nova service %s on host %s has status %s and state %s !!!",
                        service.getBinary(), service.getHost(), service.getStatus(), service.getState()));
                break;
            }
        }
        return result;
    }

    /**
     * Clean up resources created in this controller instance
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", NovaController.class, "");
        if (mAttachedVolumesByAttachmentId != null) {
            mLogger.debug("Try to delete all created volumes.");
            Iterator<Entry<String, VolumeAttachment>> iterator = mAttachedVolumesByAttachmentId.entrySet().iterator();
            while (iterator.hasNext()) {
                VolumeAttachment volumeAttachment = iterator.next().getValue();
                try {
                    detachVolumeFromServer(volumeAttachment.getId(), volumeAttachment.getVolumeId(),
                            volumeAttachment.getServerId());
                } catch (Exception ex) {
                    mLogger.error(String.format("Failed to remove volume(id:%s) from Server(id:%s)",
                            volumeAttachment.getId(), volumeAttachment.getServerId()));
                }
            }
        }

        deleteAllVmsFromList(mSuccessfullyCreatedVms);
        deleteAllVmsFromList(mInitialAndUnsuccessfullyCreatedVmIds);

        if (mCreatedFlavorIds != null) {
            mLogger.debug("Try to delete all created flavors.");
            Iterator<String> iterator = mCreatedFlavorIds.iterator();
            while (iterator.hasNext()) {
                String flavorId = iterator.next();
                try {
                    deleteFlavor(flavorId);
                } catch (Exception e) {
                    mLogger.error(String.format("Failed to delete flavor(id:%s), exception was: %s", flavorId, e));
                }
            }
        }
        if (mCreatedServerGroupIds != null) {
            mLogger.debug("Try to delete all created server groups.");
            Iterator<String> iterator = mCreatedServerGroupIds.iterator();
            while (iterator.hasNext()) {
                String groupId = iterator.next();
                try {
                    deleteServerGroup(groupId);
                } catch (Exception e) {
                    mLogger.error(String.format("Failed to delete server group(id:%s), exception was: %s", groupId, e));
                }
            }
        }
        // Clean up other controller instance created by NovaController
        mGlanceController.cleanup();
        mNeutronController.cleanup();
        mLogger.info(Verdict.DONE, "Clean up", NovaController.class, "");
    }

    /**
     * Confirm resize of a VM.
     * Throws an exception if the status of the VM is not VERIFY_RESIZE
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be resized.
     */
    public void confirmResizeVm(String serverId) {
        mLogger.info(EcsAction.RESIZING, EcsVm.class, serverId);
        if (!getVmStatus(serverId).equals(Status.VERIFY_RESIZE)) {
            throw new EcsOpenStackException("Server is not in status VERIFY_RESIZE, no resize-confirm can be done");
        }
        serverService().confirmResize(serverId);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
        mLogger.info(Verdict.RESIZED, EcsVm.class, serverId);
    }

    /**
     * Creates a flavor, return the flavor if a flavor with the same name already exists
     *
     * @param - EcsFlavor - flavor object to create
     * @return - String - ID of the flavor
     */
    public String createFlavor(EcsFlavor flavor) {
        List<? extends Flavor> flavors = flavorService().list();
        for (Flavor currentFlavor : flavors) {
            if (currentFlavor.getName().equals(flavor.get().getName())) {
                mLogger.debug(String.format("Flavor with name %s already exist, will not create a new one",
                        flavor.get().getName()));
                return currentFlavor.getId();
            }
        }
        String flavorId = flavorService().create(flavor.get()).getId();
        Flavor createdFlavor = flavorService().get(flavorId);
        if (null == createdFlavor) {
            throw new EcsOpenStackException("Flavor creation failed: ID{" + flavorId + "} " + flavor.get());
        }
        setExtraSpecsForHugePage(flavorId);
        mCreatedFlavorIds.add(flavorId);
        mLogger.info(Verdict.CREATED, EcsFlavor.class, createdFlavor);
        return flavorId;
    }

    /**
     * Creates a flavor with predefined Flavor ENUM
     *
     * @param flavor Enum which is predefined
     * @return - String - ID of the flavor
     */
    public String createFlavor(PredefinedFlavor flavor) {
        return createFlavor(EcsFlavor.builder(flavor).build());
    }

    public EcsKey createKey() {
        String keyname = ControllerUtil.createName();
        mLogger.info(EcsAction.CREATING, EcsKey.class, keyname);
        Keypair k = computeService().keypairs().create(keyname, null);
        EcsKey kp = new EcsKey(k);
        mLogger.info(Verdict.CREATED, EcsKey.class,
                "PrivateKey= " + kp.getPrivateKey() + ", PublicKey= " + kp.getPublicKey());
        return kp;
    }

    /**
     * Creates server group with specified name and group policy.
     *
     * @param name - The name of the server group to be created
     * @param policy - the group policy
     *
     * @return String - the id of the created server group
     */
    public String createServerGroup(String name, String policy) {
        mLogger.info(EcsAction.CREATING, "", "Server Group", name + " with policy " + policy);
        String groupId = serverGroupService().create(name, policy).getId();
        ServerGroup createdGroup = serverGroupService().get(groupId);
        if (null == createdGroup) {
            throw new EcsOpenStackException("Server group creation failed: ID{" + groupId + "} " + name);
        }
        mCreatedServerGroupIds.add(groupId);
        mLogger.info(Verdict.CREATED, "", "Server Group", name + ",id:" + groupId + " with policy " + policy);
        return groupId;
    }

    /**
     * creates a snapshot of the given VM and stores it as image. Will wait for the image to become active. Will throw
     * exception otherwise.
     *
     * @param serverId - id of the server to take the snapshot from
     * @return String - id of the snapshot image
     */
    public String createSnapshot(String serverId) {
        /*
         * TODO: nova creates a snapshot (IMAGE) here that is not tracked by our Glance Controller and will not be
         * cleaned up by it. We injected the glance controller here to use its wait...() method. Watch if this causes
         * trouble...
         */
        mLogger.info(EcsAction.CREATING, "Snapshot", EcsImage.class, "from server: " + serverId);
        String snapshotImageId = serverService().createSnapshot(serverId, "Snapshot-" + ControllerUtil.createName());
        mGlanceController.waitForImageToBecomeActive(snapshotImageId);
        mGlanceController.updateImage(EcsImage.imageWithId(snapshotImageId).deletionLevel(DeletionLevel.TEST_CASE).build());
        mLogger.info(Verdict.CREATED, "Snapshot", EcsImage.class, snapshotImageId + ", from server: " + serverId);
        return snapshotImageId;
    }

    /**
     * Method for creating a VM and wait until it reaches ACTIVE state. Exception will be thrown if the VM is not
     * correctly created.
     * The time used to check that the server is ACTIVE are:
     * timeout = {@value #Timeout.SERVER_READY} seconds and iteration delay = {@value #ITERATION_DELAY} seconds.
     *
     * Test network will be created and used if the user does not specify any network id in EcsVm.
     * If user has provide BlockDevice in EcsVm, the VM will be booted from volume.
     * If no BlockDevice was provided, vm argument "ecs.VmBootMode" will be used to decide whether VM will be booted
     * from image or volume,
     * the default value is "image" which will boot VM from image
     *
     * @param EcsVm - server - object containing information for creating the server
     * @return String - ID of the newly create server
     */
    public EcsVm createVm(EcsVm server) {
        mLogger.info(EcsAction.CREATING, server);
        EcsVmBuilder vmBuilder = server.toBuilder();
        String imageId = server.getImageId();
        if (!server.isNetworkIdProvided()) {
            // no network provided for creating server, create a test network (with a subnet) on the node
            String networkId = createTestNetwork();
            vmBuilder.network(networkId);
        }
        // if BlockDevice was already provide by the user, VM will be booted from volume
        // otherwise, check the VmBootingMode to decide whether to boot VM from image or volume
        if (!server.isBlockDeviceProvided()) {
            VmBootMode mode = mTestConfiguration.getVmBootingMode();
            mLogger.info(String.format("BlockDevice not provided, boot server from %s according to VmBootMode", mode));
            if (mode.equals(VmBootMode.VOLUME)) {
                EcsBlockDevice blockDevice = EcsBlockDevice.builder(BDMSourceType.IMAGE, imageId, BDMDestType.VOLUME)
                        .bootIndex(0).deleteOnTermination(true).build();
                vmBuilder.blockDevice(blockDevice);
            }
        } else {
            mLogger.info("BlockDevice provided by user, boot server from volume.");
        }
        EcsVm ecsVm = vmBuilder.build();
        Server serverInstance = null;
        serverInstance = serverService().boot(ecsVm.get());
        if (serverInstance == null) {
            throw new EcsOpenStackException("Server was not created");
        }
        mLogger.info(Verdict.CREATED, EcsVm.class, "Enclosing openstack4j object: " + serverInstance);
        serverInstance.getAdminPass();

        // check if server has actually been created
        String serverId = serverInstance.getId();
        checkIfServerExists(serverId);
        // We assume that it might be faulty, i.e. that the VM
        // doesn't get into active status,
        mInitialAndUnsuccessfullyCreatedVmIds.add(serverId);
        waitForServerStatus(serverId, Server.Status.ACTIVE);

        String networkId = null;
        List<? extends NetworkCreate> networks = ecsVm.getNetworks();
        if (networks.isEmpty()) {
            throw new EcsOpenStackException("VM " + serverInstance.getName() + ", id: " + serverInstance.getId()
                    + " should have at least one network");
        }
        // check if network port is provided for VM instead of network. If yes, get network id from port, else get it
        // from network
        if (server.isNetworkPortIdProvided()) {
            // get the port id of the first network as the first network will always have a port.
            // Then get the network id of the port ( getNetworks() will only have the portId. networkId would be
            // null in this case. So we cannot get network id directly from getNetworks().
            // We first need to get the portId and then using the portId ,we need to get its network id)
            String portId = networks.get(0).getPort();
            networkId = mNeutronController.getNetworkIdOfPort(portId);
        } else {
            networkId = networks.get(0).getId();
        }

        // get necessary info from server instance and return a EcsVm
        return buildCreatedVm(serverId, imageId, networkId);
    }

    /**
     * Delete flavor
     *
     * @param flavorId
     */
    public void deleteFlavor(String flavorId) {
        mLogger.info(EcsAction.DELETING, "", EcsFlavor.class, flavorId);
        if (!doesFlavorExistWithId(flavorId)) {
            mLogger.warn(String.format("Flavor %s cannot be found. Skipping deletion.", flavorId));
            return;
        }
        flavorService().delete(flavorId);
        if (doesFlavorExistWithId(flavorId)) {
            throw new EcsOpenStackException(
                    "Flavor still exist after deletion. If you see this text, add a loop helper here.");
        }
        mCreatedFlavorIds.remove(flavorId);
    }

    /**
     * Deletes the specified server group.
     *
     * @param groupId - The id of the server group to be deleted
     */
    public void deleteServerGroup(String groupId) {
        mLogger.info(EcsAction.DELETING, "", "Server Group", groupId);
        ActionResponse response = serverGroupService().delete(groupId);
        if (response.isSuccess()) {
            mLogger.info(Verdict.DELETED, "", "Server Group", groupId);
        } else {
            mLogger.warn("failed to delete server group " + groupId);
        }
        mCreatedServerGroupIds.remove(groupId);
    }

    /**
     * Delete a VM. Exception will be thrown if the VM was not correctly removed.
     *
     * @param server - T - String (server id) or EcsVm (server instance)
     */
    public <T> void deleteVm(T server) {
        String vmId = null;
        if (server instanceof EcsAtlasVm) {
            vmId = getVmIdByName("atlas_vm");
            deleteVmHelper(vmId);
        } else if (server instanceof EcsVm) {
            EcsVm vmInstance = (EcsVm) server;
            vmInstance.deinitialize();
            vmId = vmInstance.getId();
            if (mSuccessfullyCreatedVms.contains(vmInstance)) {
                mSuccessfullyCreatedVms.remove(vmInstance);
            } else {
                mLogger.warn("Underlining VM \"" + getVmName(vmId) + "\"(" + vmId + ") is not created by this run");
            }
            deleteVmHelper(vmId);
        } else if (server instanceof String) {
            vmId = (String) server;
            if (mInitialAndUnsuccessfullyCreatedVmIds.contains(vmId)) {
                mInitialAndUnsuccessfullyCreatedVmIds.remove(vmId);
            } else {
                mLogger.warn("Underlining VM \"" + getVmName(vmId) + "\"(" + vmId + ") is not created by this run");
            }
            deleteVmHelper(vmId);
        } else {
            throw new EcsOpenStackException(
                    String.format("Type %s is not supported in deleteVm, only instance of EcsVm or String are allowed.",
                            server.getClass().getSimpleName()));
        }
    }

    /**
     * Try to delete a VM with a specified ID.
     * Returns if deletion was successful or not.
     * It's not waiting for the VM to be deleted.
     * Warning: this method should only be used in negative test condition!
     * e.g. when it should not be able to delete the VM.
     *
     * @param serverId - String - ID of the server to delete
     * @return boolean - return true if VM could be deleted, otherwise return false
     */
    public boolean deleteVmWithoutCheck(final String serverId) {
        mLogger.info(String.format("Try to delete server : %s", serverId));
        ActionResponse result = serverService().delete(serverId);
        return result.isSuccess();
    }

    /**
     * Detaches an specific volume from an specific server. Exception will be thrown if
     * the volume can't be detached from the server.
     *
     * @param volumeAttachmentId - String - the volumeAttachmentId
     * @param volumeId - String - the volume id to detach
     * @param serverId - String - server id to detach from
     */
    public void detachVolumeFromServer(String volumeAttachmentId, String volumeId, String serverId) {
        mLogger.info(EcsAction.DETACHING, EcsVolume.class, EcsVm.class,
                serverId + ", volumeId= " + volumeId + ", volumeAttachmentId= " + volumeAttachmentId);
        serverService().detachVolume(serverId, volumeAttachmentId);
        mAttachedVolumesByAttachmentId.remove(volumeAttachmentId);
        mCinderController.waitForVolumeStatus(volumeId, Volume.Status.AVAILABLE);
        mLogger.info(Verdict.DETACHED, EcsVolume.class, EcsVm.class, serverId + ", volumeId= " + volumeId);
    }

    /**
     * Method for checking if a server with a specific id exists.
     *
     * @param serverId String - ID of the server.
     * @return true if the server exists
     */
    public boolean doesServerExist(String serverId) {
        return (serverService().get(serverId) != null);
    }

    /**
     * Method will start a VM without checking for active status.
     * DO NOT USE THIS METHOD ANYMORE !!! WILL BE REPLACED WITH A BUILDER PARAMETER !!!
     *
     * @param EcsVm - server - object containing information for creating the server
     * @return String - ID of the newly create server
     * @deprecated
     */
    @Deprecated
    public String doNotUseThisMethodcreateVmNoCheck(EcsVm server) {
        EcsVmBuilder vmBuilder = server.toBuilder();
        if (!server.isNetworkIdProvided()) {
            // user has not provided any network for creating server,
            // create a network (with a subnet) on the node
            String networkId = createTestNetwork();
            vmBuilder.network(networkId);
        }
        VmBootMode mode = mTestConfiguration.getVmBootingMode();
        mLogger.info("VmBootMode is " + mode);
        if (mode.equals(VmBootMode.VOLUME)) {
            mLogger.info("Boot server from volume.");
            EcsBlockDevice blockDevice = EcsBlockDevice
                    .builder(BDMSourceType.IMAGE, server.getImageId(), BDMDestType.VOLUME).bootIndex(0)
                    .deleteOnTermination(true).build();
            vmBuilder.blockDevice(blockDevice);
        } else {
            mLogger.info("Boot server from image.");
        }
        EcsVm ecsVm = vmBuilder.build();
        Server serverInstance = null;
        serverInstance = serverService().boot(ecsVm.get());
        if (serverInstance == null) {
            throw new EcsOpenStackException("Server was not created");
        }
        mLogger.info("Created server: " + serverInstance);

        // check if server has actually been created
        String serverId = serverInstance.getId();
        checkIfServerExists(serverId);

        return serverId;
    }

    /**
     * Evacuates a VM.
     * Throws an exception if the status ACTIVE
     * is not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be evacuated
     * @param hostname - name of the compute host where the server is to be evacuated to
     */
    public void evacuateVm(String serverId, String hostname) {
        EvacuateOptions options = EvacuateOptions.create().host(hostname);
        serverService().evacuate(serverId, options);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Performs forcemove of VM Without Any Option.
     *
     * @param vm
     *            The VM to be force moved
     */
    public void forceMoveVm(EcsVm vm) {
        forceMoveVm(vm, null);
    }

    /**
     * Performs forcemove of VM with Options.
     *
     * @param vm
     *            The VM to be force moved
     * @param providedOption
     *            The Option to be added while doing forcemove
     */
    public void forceMoveVm(EcsVm vm, ForceMoveOption providedOption) {
        String host_before_migration = vm.getHostName();
        EcsCic cic = mEcsCicList.getRandomCic();
        if (!cic.getUser().equals("root")) {
            throw new EcsOpenStackException(
                    "Accessing root certificate requires root access. Current access level: " + cic.getUser());
        }
        String vmHasHa_Policy;
        String optionProvided = (providedOption != null) ? (providedOption.forceMoveOption() + " ") : "";
        vmHasHa_Policy = cic
                .sendCommand("nova forcemove " + optionProvided + vm.getName() + "| sed -n 4p|awk '{print $4}'");
        // check if VM has ha-policy and if yes,migrate the VM
        mLogger.info(EcsAction.FINDING, vm.getName(), "", String.format("if %s has ha-policy", vm.getName()));
        if (vmHasHa_Policy.equals("True")) {
            // check if VM's ha-policy is ha-offline
            if (getVmStatus(vm.getId()).equals(Server.Status.RESIZE)) {
                mLogger.info(Verdict.FOUND, vm.getName(), "",
                        String.format("%s has ha-policy 'ha-offline'", vm.getName()));
                mLogger.info(EcsAction.MIGRATING, vm.getName(), "", String.format("from:%s", host_before_migration));
                waitForServerStatus(vm.getId(), Server.Status.VERIFY_RESIZE);
                mLogger.info(Verdict.MIGRATED, vm.getName(), String.format("to:%s", vm.getHostName()),
                        String.format("from:%s", host_before_migration));
            }
            // check if VM's ha-policy is managed-on-host
            else if (getVmStatus(vm.getId()).equals(Server.Status.ACTIVE)) {
                mLogger.info(Verdict.FOUND, vm.getName(), "",
                        String.format("%s has ha-policy 'managed-on-host'", vm.getName()));
                mLogger.info(EcsAction.MIGRATING, vm.getName(), "", String.format("from:%s", host_before_migration));
                waitForServerStatus(vm.getId(), Server.Status.SHUTOFF);
                mLogger.info(Verdict.MIGRATED, vm.getName(), String.format("to:%s", vm.getHostName()),
                        String.format("from:%s", host_before_migration));
            }
        } else {
            mLogger.info(Verdict.FOUND, vm.getName(), "",
                    String.format("%s has no ha-policy.So it can not be migrated", vm.getName()));
        }
    }

    /**
     * Generates CPU list value string based on vCpus
     *
     * @param vcpus - number of vcpus used for flavor.
     * @param numaNode - physical numa node where the virtual cpus are pinned on
     * @return cpuListValue string
     */
    public String generateCpuListValue(int vcpus, NumaNode numaNode) {
        mLogger.info(EcsAction.PREPARING, "", "CPUListvalue", "to set extraspecs for flavor");
        EcsComputeBlade blade = mEcsComputeBladeList.getRandomComputeBlade();
        String cpuListValue = null;
        List<Integer> availableCpuList = blade.getAvailableCpu(numaNode);
        for (int i = 0; i < vcpus; i++) {
            if (cpuListValue == null) {
                cpuListValue = numaNode.getName() + ":" + availableCpuList.get(i) + "/";
            } else if (i == vcpus - 1) {
                cpuListValue = cpuListValue + numaNode.getName() + ":" + availableCpuList.get(i);
            } else {
                cpuListValue = cpuListValue + numaNode.getName() + ":" + availableCpuList.get(i) + "/";
            }
        }
        mLogger.info(Verdict.PREPARED, "", "CPUListvalue", "as " + cpuListValue + " to set extraspecs for flavor");
        return cpuListValue;
    }

    /**
     * Fetches the x509 root certificate and writes it to cacert.pem
     *
     * @return boolean - true if the certificate has been fetched/written
     */
    public boolean generateX509RootCertificate() {
        EcsCic cic = mEcsCicList.getRandomCic();
        if (!cic.getUser().equals("root")) {
            throw new EcsOpenStackException(
                    "Accessing root certificate requires root access. Current access level: " + cic.getUser());
        }
        String result = cic.sendCommand("nova x509-get-root-cert");
        if (result.contains("ERROR")) {
            throw new EcsOpenStackException("Could not fetch x509 root certificate!!!");
        } else if (!result.contains("Wrote")) {
            return false;
        }
        return true;
    }

    /**
     * Gets all VMs existing on the node
     *
     * @return List of EcsVm objects represents all VMs on the node
     */
    public List<EcsVm> getAllVMsForAllTenants() {
        List<? extends Server> servers = serverService().listAll(true);
        List<EcsVm> allVms = new ArrayList<EcsVm>();
        for (Server server : servers) {
            if (server.getStatus() != Status.ACTIVE) {
                continue;
            }
            String hypervisorHostname = server.getHost();
            String hypervisorInstanceId = server.getInstanceName();
            EcsImage ecsImage = null;
            if (server.getImage() == null && !server.getOsExtendedVolumesAttached().isEmpty()) {
                // The VM was booted from volume and we need to get the image id from the volume attached.
                String volumeId = server.getOsExtendedVolumesAttached().get(0);
                EcsVolume volume = mCinderController.getVolume(volumeId);
                String imageRef = volume.getImageRef();
                ecsImage = mGlanceController.getImage(imageRef);
            } else {
                try {
                    ecsImage = mGlanceController.getImage(server.getImageId());
                } catch (EcsOpenStackException e) {
                    JcatLoggingApi.setTestWarning("EcsVM object could not be created for vm: " + server.getId()
                            + " The JCAT FW could not recognize the vm image: " + server.getImageId()
                            + " An enum for this image needs to be added to the FW");
                    continue;
                }
            }
            List<String> networkNames = getNetworkNames(server.getId());
            String networkId = "";
            String vmIp = "";
            if (!networkNames.isEmpty()) {
                String networkName = getNetworkNames(server.getId()).get(0);
                networkId = mNeutronController.getNetworkIdByName(networkName);
                vmIp = getIPs(server.getId(), networkName).get(0);
            }
            try {
                EcsVm vm = mEcsVmFactory.create(server.getId(), ecsImage, hypervisorHostname, hypervisorInstanceId,
                        networkId, vmIp);
                allVms.add(vm);
            } catch (Exception e) {
                JcatLoggingApi.setTestWarning("While creating vm object from serverid: " + server.getId()
                        + " something went wrong, maybe it has already been deleted, skipping this server in the list");
                continue;
            }

        }
        return allVms;
    }

    /**
     * Returns atlas vm instance on node
     *
     * @return EcsAtlasVm
     */
    public EcsAtlasVm getAtlasVm() {
        String serverId = getVmIdByName("atlas_vm");
        if (serverId == null) {
            throw new EcsOpenStackException("Atlas VM is not deployed on node, please get it deployed first");
        }
        EcsAtlasVm atlasVm = (EcsAtlasVm) getEcsVm(serverId);
        atlasVm.lazyInitialize();
        return atlasVm;
    }

    /**
     * Returns atlas bat vm instance on node
     *
     * @param serverId - The id of the Bat VM
     * @return EcsAtlasBatVm
     */
    public EcsAtlasBatVm getAtlasBatVm(String serverId) {
        EcsAtlasBatVm atlasBatVm = (EcsAtlasBatVm) getEcsVm(serverId);
        atlasBatVm.lazyInitialize();
        return atlasBatVm;
    }

    /**
     * Gets currently used disk (in GB) on the specified compute host
     * @return int
     */
    public int getCurrentlyUsedDiskOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(1).getDiskInGb();
    }

    /**
     * Gets currently used memory (in MB) on the specified compute host
     * @return int
     */
    public int getCurrentlyUsedMemoryOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(1).getMemoryInMb();
    }

    /**
     * Gets currently used vcpus on the specified compute host
     * @return int
     */
    public int getCurrentlyUsedVcpusOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(1).getCpu();
    }

    /**
     * Gets VM details form CEE.
     *
     * @param vmId VM UUID
     * @return {@link EcsVm}
     */
    public EcsVm getEcsVm(String vmId) {
        mLogger.info(EcsAction.CREATING, EcsVm.class, "Get ECS VM data.");
        Server server = serverService().get(vmId);
        List<? extends InterfaceAttachment> list = serverService().interfaces().list(server.getId());
        for (InterfaceAttachment interfaceAttachment : list) {
            mLogger.info(Verdict.FOUND, EcsVm.class, "NW Id: " + interfaceAttachment.getNetId());
        }
        String netId = null;
        String atlasPublicNetworkId = mNeutronController.getNetworkIdByName(ATLAS_VM_NETWORK_NAME);
        if (atlasPublicNetworkId == null) {
            atlasPublicNetworkId = mNeutronController.getNetworkIdByName(ATLAS_VM_NETWORK_NAME_BSP);
        }
        String batAtlasPublicNetworkId = mNeutronController.getNetworkIdByName(BAT_ATLAS_VM_NETWORK_NAME);
        for (InterfaceAttachment interfaceAttachment : list) {
            if (server.getName().equals("atlas_vm")) {
                if (interfaceAttachment.getNetId().equals(atlasPublicNetworkId)) {
                    netId = interfaceAttachment.getNetId();
                    break;
                }
            } else if (server.getName().contains("BAT-A")) {
                if (interfaceAttachment.getNetId().equals(batAtlasPublicNetworkId)) {
                    netId = interfaceAttachment.getNetId();
                    break;
                }
            } else {
                netId = interfaceAttachment.getNetId();
                break;
            }
        }
        mLogger.info(Verdict.CREATED, EcsVm.class, "Using the first Network id: " + netId);
        return buildCreatedVm(server.getId(), server.getImageId(), netId);
    }

    /**
     * Get detailed info about a flavor
     *
     * @param flavorId - String - id of the flavor
     * @return Flavor, detailed info about the flavor, null if the flavor was not found
     */
    public Flavor getFlavorById(String flavorId) {
        return flavorService().get(flavorId);
    }

    /**
     * Method which returns ID of a predefined flavor, if the predefined flavor does not exist, create it and return the
     * id
     *
     * @param PredefinedFlavor
     * @return String - id of the flavor
     */
    public String getFlavorId(PredefinedFlavor flavor) {
        String flavorId = getFlavorIdByName(flavor.flavorName());
        if (flavorId == null) {
            flavorId = createFlavor(flavor);
        }
        return flavorId;
    }

    /**
     * Method which takes flavor name and returns its ID.
     *
     * @param name - String - name of the flavor
     * @return String - id of the flavor, returns null if the flavor does not exists
     */
    public String getFlavorIdByName(String name) {
        String flavorId = null;
        List<? extends Flavor> flavorList = flavorService().list();
        for (Flavor flavor : flavorList) {
            if (name.equals(flavor.getName())) {
                flavorId = flavor.getId();
                break;
            }
        }
        if (flavorId == null) {
            mLogger.info("Flavor \"" + name + "\" not found on node");
        } else {
            mLogger.debug("Flavor \"" + name + "\" has id \"" + flavorId + "\"");
            setExtraSpecsForHugePage(flavorId);
        }
        return flavorId;
    }

    /**
     * Find the compute blade host name of a VM
     *
     * @param serverId of a VM to query
     * @return Host name of the compute blade the VM is running on
     */
    public String getHostName(String serverId) {
        Server server = serverService().get(serverId);
        if (null != server) {
            return server.getHost();
        } else {
            mLogger.warn(String.format(
                    "Could not get server instance with id %s, thus could not get host name for this server.",
                    serverId));
            return null;
        }
    }

    /**
     * Retrieves the instance name of the VM with specified id
     *
     * @param vmId - String - ID of the VM
     * @return String - the instance name of the specified VM
     */
    public String getInstanceName(String vmId) {
        return serverService().get(vmId).getInstanceName();
    }

    /**
     * gets all IP addresses connected to a specific network on a specific server id.
     *
     * @param serverId - Server id
     * @param networkName - Name of the network to fetch IP addresses from
     * @return
     */
    public List<String> getIPs(String serverId, String networkName) {
        List<? extends Address> ipAddresses = serverService().get(serverId).getAddresses().getAddresses(networkName);
        List<String> ipAddressList = new ArrayList<String>();
        for (Address ipAddress : ipAddresses) {
            ipAddressList.add(ipAddress.getAddr());
            mLogger.info("Server has IP address: " + ipAddress.getAddr() + " in network: " + networkName);
        }
        return ipAddressList;
    }

    /**
     * Returns the Metadata of the given server
     *
     * @param String serverId - id of the server for which the host is requested
     * @return Sring - ha-policy of server
     */
    public String getMetadata(String serverId) {
        Map<String, String> Metadata = serverService().get(serverId).getMetadata();
        String hapolicy = Metadata.get(HA_POLICY);
        return hapolicy;
    }

    /**
     * Returns the network names assigned to the given server
     *
     * @param String serverId - id of the server for which the host is requested
     * @return List<Sring> - network names
     */
    public List<String> getNetworkNames(String serverId) {
        // Time to iterate through the addresses.
        Map<String, List<? extends Address>> addresses = serverService().get(serverId).getAddresses().getAddresses();

        List<String> networkNames = new ArrayList<String>();
        for (String networkName : addresses.keySet()) {
            networkNames.add(networkName);
        }
        return networkNames;
    }

    /**
     * Gets the nova cores quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the cores quota for
     *
     * @return int - the nova cores quota for the specified tenant
     */
    public int getNovaQuotaCoresForTenant(String tenantId) {
        int quotaCores = computeService().quotaSets().get(tenantId).getCores();
        return quotaCores;
    }

    /**
     * Gets the nova injected file content bytes quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the injected file content bytes quota for
     *
     * @return int - the nova injected file content bytes quota for the specified tenant
     */
    public int getNovaQuotaInjectedFileContentForTenant(String tenantId) {
        int quotaInjectedFileContentBytes = computeService().quotaSets().get(tenantId).getInjectedFileContentBytes();
        return quotaInjectedFileContentBytes;
    }

    /**
     * Gets the nova instances quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the instances quota for
     *
     * @return int - the nova instances quota for the specified tenant
     */
    public int getNovaQuotaInstancesForTenant(String tenantId) {
        int quotaInstances = computeService().quotaSets().get(tenantId).getInstances();
        return quotaInstances;
    }

    /**
     * Gets the nova quota memory (in mb) for a given tenant
     *
     * @param tenantId - String - id of the tenant to get the memory for
     * @return int - the nova quota memory
     */
    public int getNovaQuotaMemoryForTenant(String tenantId) {
        int quotaMemory = computeService().quotaSets().get(tenantId).getRam();
        return quotaMemory;
    }

    /**
     * Gets the power state for a server.
     *
     * @param serverId - id of the server to be queried.
     * @return
     */
    public String getPowerState(String serverId) {
        Server server = serverService().get(serverId);
        return server.getPowerState();
    }

    /**
     * Returns the time Stamps for all reboot actions on specified Server
     * ex: root@cic-2:~# nova instance-action-list  db45cfc0-8b0c-4662-9560-5b265ab18fc8
     *     +--------+------------------------------------------+---------+----------------------------+
     *     | Action | Request_ID                               | Message | Start_Time                 |
     *     +--------+------------------------------------------+---------+----------------------------+
     *      create | req-67c0cc31-8b71-4fcd-83a9-66f62f84cb2f | -       | 2018-01-16T09:14:51.000000
     *      reboot | req-67c0cc31-8b71-4fcd-83a9-66f62f84cb2f | -       | 2018-01-16T09:17:39.000000
     *      reboot | req-67c0cc31-8b71-4fcd-83a9-66f62f84cb2f | -       | 2018-01-16T10:01:28.000000
     *
     * return reboot Timestamp list - [2018-01-16T09:17:39.000000, 2018-01-16T10:01:28.000000]
     *
     * @param serverId - vm instance-id
     * @return list of date objects for reboot action
     */
    public List<Date> getRebootTimestamps(String serverId) {
        List<Date> instanceActionsStartTime = new ArrayList<Date>();
        List<? extends InstanceAction> instanceActions = serverService().instanceActions().list(serverId);
        for (InstanceAction instanceAction : instanceActions) {
            if (instanceAction.getAction().equals("reboot")) {
                instanceActionsStartTime.add(instanceAction.getStartTime());
            }
        }
        if (instanceActionsStartTime.isEmpty()) {
            throw new EcsOpenStackException("No reboot action is performed on instance");
        }
        return instanceActionsStartTime;
    }

    /**
     *
     * @param mServerId
     * @return The server name
     */
    public String getServerName(String mServerId) {
        return serverService().get(mServerId).getName();
    }

    /**
     * Gets total number of vCPUs available on the specified compute host
     *
     * @return int
     */
    public int getTotalCPUsOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(0).getCpu();
    }

    /**
     * Gets total disk (in GB) on the specified compute host
     *
     * @return int
     */
    public int getTotalDiskOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(0).getDiskInGb();
    }

    /**
     * Gets total memory (in MB) on the specified compute host
     *
     * @return int
     */
    public int getTotalMemoryOnHost(String hostName) {
        List<? extends HostResource> hostResources = hostService().hostDescribe(hostName);
        return hostResources.get(0).getMemoryInMb();
    }

    /**
     * Returns the user id of the VM.
     *
     * @param serverId - String - id of the server to get the user id
     * @return userId
     */
    public String getUserIdOfVm(String serverId) {
        return serverService().get(serverId).getUserId();
    }

    /**
     * Gets the fault message displayed at nova show <server>
     *
     * @param vmId - String - id of the server to be queried
     * @return String - fault message displayed at nova show
     */
    public String getVmFaultMessage(String vmId) {
        return serverService().get(vmId).getFault().getMessage();
    }

    /**
     * Get flavor ID for given VM
     *
     * @param vmId VM UUID
     * @return {@link String} ID of the flavor
     */
    public String getVmFlavorId(String vmId) {
        return serverService().get(vmId).getFlavor().getId();
    }

    /**
     * Get flavor name for given VM
     *
     * @param vmId VM UUID
     * @return {@link String} name of the flavor
     */
    public String getVmFlavorName(String vmId) {
        return serverService().get(vmId).getFlavor().getName();
    }

    /**
     * Get the id of the VM with given name. Null will be returned if no VM was found with specified name.
     * Throws an exception if VMs with the same name was found due to ambiguity.
     *
     * @param vmName - String - name of the VM
     * @return - String - id of the VM, null if not found
     */
    public String getVmIdByName(String vmName) {
        int nrOfVmsFound = 0;
        String serverId = null;
        List<? extends Server> serversList = serverService().listAll(false);
        for (Server server : serversList) {
            if (server.getName().equals(vmName)) {
                nrOfVmsFound++;
                serverId = server.getId();
            }
        }
        if (nrOfVmsFound > 1) {
            throw new EcsOpenStackException("Found duplicate VMs with the same name.");
        }
        return serverId;
    }

    /**
     * Gets the id of the image from which VM is booted.
     *
     * @param serverId - The id of the VM
     *
     * @return String - id of the image used by VM
     */
    public String getVmImageId(String serverId) {
        return serverService().get(serverId).getImageId();
    }

    /**
     * Get the name of a VM
     *
     * @param vmId
     * @return String - Name of the VM
     */
    public String getVmName(String vmId) {
        return serverService().get(vmId).getName();
    }

    /**
     * Get the status of a VM
     *
     * @param vmId
     * @return Status - the status of the VM (ACTIVE, BUILD, ERROR, ...)
     */
    public Status getVmStatus(String vmId) {
        return serverService().get(vmId).getStatus();
    }

    /**
     * Get a list of VMs from all tenants
     *
     * @return list of VMs
     */
    public List<String> listVMsAllTenant() {
        List<String> vms = new ArrayList<String>();
        for (Server vm : serverService().listAll(false)) {
            vms.add(vm.getId());
        }
        return vms;
    }

    /**
     * Get a list of VMs from current tenant
     *
     * @return list of VMs
     */
    public List<String> listVMsCurrentTenant() {
        List<String> vms = new ArrayList<String>();
        for (Server vm : serverService().list()) {
            vms.add(vm.getId());
        }
        return vms;
    }

    /**
     * Locks the VM
     * Throws an exception if the status is not
     * ACTIVE after locking.
     *
     * @param serverId - id of the server to be locked.
     */
    public void lockVm(String serverId) {
        mLogger.info("Try to lock the server " + serverId);
        serverService().action(serverId, Action.LOCK);
        if (!getVmStatus(serverId).equals(Status.ACTIVE)) {
            throw new EcsOpenStackException("Status of VM is not ACTIVE after locking");
        }
    }

    /**
     * Performs migration of VM from one host to another.
     *
     * @param vm - The VM to be migrated
     */
    public void migrateVm(EcsVm vm) {
        String host_before_migration = vm.getHostName();
        EcsCic cic = mEcsCicList.getRandomCic();
        if (!cic.getUser().equals("root")) {
            throw new EcsOpenStackException(
                    "Accessing root certificate requires root access. Current access level: " + cic.getUser());
        }
        mLogger.info(EcsAction.MIGRATING, EcsVm.class, vm.getName() + " from " + host_before_migration);
        cic.sendCommand("nova migrate " + vm.getName());
        waitForServerStatus(vm.getId(), Server.Status.VERIFY_RESIZE);
        // resize confirm
        confirmResizeVm(vm.getId());
        waitForServerStatus(vm.getId(), Server.Status.ACTIVE);
        mLogger.info(Verdict.MIGRATED, EcsVm.class,
                vm.getName() + " to:" + vm.getHostName() + " from:" + host_before_migration);
    }

    /**
     * Pauses the VM.
     * Throws an exception if the status PAUSED is not
     * reached within the time limit for status change.
     *
     * @param serverId - id of the server to be paused.
     */
    public void pauseVm(String serverId) {
        mLogger.info("Try to pause the server " + serverId);
        serverService().action(serverId, Action.PAUSE);
        waitForServerStatus(serverId, Status.PAUSED);
    }

    /**
     * Hard Reboot a VM that is in SHUTOFF state.
     * Throws an exception if status ACTIVE is
     * not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be rebooted.
     */
    public void rebootStoppedVm(String serverId) {
        mLogger.info(EcsAction.REBOOTING, EcsVm.class, serverId);
        serverService().reboot(serverId, RebootType.HARD);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
        mLogger.info(Verdict.REBOOTED, EcsVm.class, serverId);
    }

    /**
     * Reboot a VM. Reboot type can be SOFT or HARD.
     * Throws an exception if the status of the VM is not HARD_REBOOT
     * or REBOOT immediately after the reboot, or if status ACTIVE is
     * not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be rebooted.
     * @param rebootType - reboot type SOFT or HARD
     */
    public void rebootVm(String serverId, RebootType rebootType) {
        mLogger.info("Try to do a " + rebootType + " reboot of the server " + serverId);
        serverService().reboot(serverId, rebootType);
        Status desiredStatus = Status.HARD_REBOOT;
        if (rebootType == RebootType.SOFT) {
            desiredStatus = Status.REBOOT;
        }
        waitForServerImmediateStatusChange(serverId, desiredStatus);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Rebuild a VM.
     * Throws an exception if the status of the VM is not REBUILD
     * immediately after the rebuild, or if status ACTIVE is
     * not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be rebuild.
     * @param rebuildOptions - rebuild options (admin pass, name and/or image)
     */
    public void rebuildVm(String serverId, RebuildOptions rebuildOptions) {
        mLogger.info("Try to do a rebuild of the server " + serverId);
        serverService().rebuild(serverId, rebuildOptions);
        waitForServerImmediateStatusChange(serverId, Server.Status.REBUILD);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Resize a VM.
     * Throws an exception if the status of the VM is not RESIZE
     * immediately after the reboot, or if status VERIFY_RESIZE is
     * not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be resized.
     * @param flavorId - new flavorId for the VM
     */
    public void resizeVm(String serverId, String flavorId) {
        mLogger.info("Try to do a resize of the server " + serverId + " with new flavor " + flavorId);
        serverService().resize(serverId, flavorId);
        waitForServerImmediateStatusChange(serverId, Server.Status.RESIZE);
        waitForServerStatus(serverId, Server.Status.VERIFY_RESIZE);
    }

    /**
     * Resumes a suspended VM.
     * Throws an exception if the status of the VM is not SUSPENDED
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be resumed.
     */
    public void resumeVm(String serverId) {
        mLogger.info("Try to resume the server " + serverId);
        if (!getVmStatus(serverId).equals(Status.SUSPENDED)) {
            throw new EcsOpenStackException("Server is not in status SUSPENDED, can't be resumed");
        }
        serverService().action(serverId, Action.RESUME);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Revert resize of a VM.
     * Throws an exception if the status of the VM is not VERIFY_RESIZE
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be resized.
     */
    public void revertResizeVm(String serverId) {
        mLogger.info("Try to revert the resize of the server " + serverId);
        if (!getVmStatus(serverId).equals(Status.VERIFY_RESIZE)) {
            throw new EcsOpenStackException("Server is not in status VERIFY_RESIZE, no resize-revert can be done");
        }
        serverService().revertResize(serverId);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Get a list of availabe nova services from node
     *
     * @return list of nova services
     */
    public List<EcsNovaService> servicesList() {
        List<? extends Service> services = servicesService().list();
        List<EcsNovaService> ecsServices = new ArrayList<EcsNovaService>();
        for (Service service : services) {
            ecsServices.add(new EcsNovaService(service));
        }
        if (ecsServices.isEmpty()) {
            throw new EcsOpenStackException("Ecs Nova Service List is empty, which it shouldn't.");
        }
        return ecsServices;
    }

    /**
     * Sets the numa memory and/or cpu list specifications for the flavor specified by id.
     *
     * @param numaMemoryValue - enum - the value for NumaMemory specification for the flavor(can be null if this specification is not needed)
     * @param cpuListValue - enum - the value for CpuList specification for the flavor(can be null if this specification is not needed)
     *
     * @return - boolean
     */
    public boolean setExtraSpecs(String flavorId, NumaMemoryValue numaMemoryValue, CpuListValue cpuListValue) {
        if (numaMemoryValue != null) {
            mExtraSpecMap.put(NUMA_MEMORY, numaMemoryValue.numaMemoryValue());
        }
        if (cpuListValue != null) {
            mExtraSpecMap.put(CPU_LIST, cpuListValue.cpuListValue());
        }
        flavorService().createAndUpdateExtraSpecs(flavorId, mExtraSpecMap);
        if (flavorService().getSpec(flavorId, NUMA_MEMORY) != null
                || flavorService().getSpec(flavorId, CPU_LIST) != null) {
            return true;
        }
        return false;
    }

    /**
     * Sets the numa memory and/or cpu list specifications for the flavor specified by id.
     *
     * @param flavorId - String Id of flavor
     * @param numaMemoryValue - enum - the value for NumaMemory specification for the flavor(can be null if this specification is not needed)
     * @param cpuListValue - enum - the value for CpuList specification for the flavor(can be null if this specification is not needed)
     * @param cpuPolicyList -String - the value for CpuList specification for the flavor when vcpus are dynamic
     *
     * @return - boolean
     */
    public boolean setExtraSpecs(String flavorId, NumaMemoryValue numaMemoryValue, CpuListValue cpuListValue,
            String cpuPolicyValue) {
        if (cpuListValue == null && cpuPolicyValue != null) {
            mExtraSpecMap.put(CPU_LIST, cpuPolicyValue);
        }
        return setExtraSpecs(flavorId, numaMemoryValue, null);
    }

    /**
     * Set dedicated cpu policy to the specified flavor
     *
     * @param flavorId - id of the flavor to add cpu policy
     *
     * @return boolean
     */
    public boolean setExtraSpecsForCpuPolicy(String flavorId) {
        boolean setCpuPolicy = false;
        // set cpu policy spec
        String cpuPolicyValue = flavorService().getSpec(flavorId, CPU_POLICY);
        if (cpuPolicyValue == null) {
            Map<String, String> spec = new HashMap<String, String>();
            spec.put(CPU_POLICY, CPU_POLICY_VALUE);
            flavorService().createAndUpdateExtraSpecs(flavorId, spec);
            setCpuPolicy = true;
        } else if (cpuPolicyValue.equals(CPU_POLICY_VALUE)) {
            setCpuPolicy = true;
        }
        return setCpuPolicy;
    }

    /**
     * Set watchdog action to the specified flavor
     *
     * @param flavorId - id of the flavor to add watchdog action
     *
     * @return boolean
     */
    public boolean setExtraSpecsForWatchdogAction(String flavorId, WatchdogAction action) {
        boolean setWatchdogAction = false;
        String watchdogAction = flavorService().getSpec(flavorId, WATCHDOG_ACTION);
        if (watchdogAction == null) {
            Map<String, String> spec = new HashMap<String, String>();
            spec.put(WATCHDOG_ACTION, action.action());
            flavorService().createAndUpdateExtraSpecs(flavorId, spec);
            setWatchdogAction = true;
        } else if (watchdogAction.equals(action.action())) {
            setWatchdogAction = true;
        }
        return setWatchdogAction;
    }

    /**
     * Starts a VM.
     * Throws an exception if the status of the VM is not SHUTOFF
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be started
     */
    public void startVm(String serverId) {
        mLogger.info("Try to start the server " + serverId);
        if (!serverService().get(serverId).getStatus().equals(Status.SHUTOFF)) {
            throw new EcsOpenStackException("Server is not in status SHUTOFF, can't be started");
        }
        serverService().action(serverId, Action.START);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Stops a VM.
     * Throws an exception if the status SHUTOFF
     * is not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be stopped
     */
    public void stopVm(String serverId) {
        mLogger.info("Try to stop the server " + serverId);
        serverService().action(serverId, Action.STOP);
        waitForServerStatus(serverId, Server.Status.SHUTOFF);
    }

    /**
     * Suspends a VM
     * Throws an exception if the status SUSPENDED
     * is not reached within the time limit for status change.
     *
     * @param serverId - id of the server to be suspended.
     */
    public void suspendVm(String serverId) {
        mLogger.info("Try to suspend the server " + serverId);
        serverService().action(serverId, Action.SUSPEND);
        waitForServerStatus(serverId, Status.SUSPENDED);
    }

    /**
     * Unlocks a VM
     * Throws an exception if the status of the VM is not ACTIVE
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be unlocked.
     */
    public void unlockVm(String serverId) {
        mLogger.info("Try to unlock the server " + serverId);
        if (!serverService().get(serverId).getStatus().equals(Status.ACTIVE)) {
            throw new EcsOpenStackException("Server is not in status ACTIVE, can't be unlocked");
        }
        serverService().action(serverId, Action.UNLOCK);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Unpause a VM.
     * Throws an exception if the status of the VM is not PAUSED
     * in the beginning, and if status ACTIVE is not reached within
     * the time limit for status change.
     *
     * @param serverId - id of the server to be unpaused.
     */
    public void unpauseVm(String serverId) {
        mLogger.info("Try to unpause the server " + serverId);
        if (!serverService().get(serverId).getStatus().equals(Status.PAUSED)) {
            throw new EcsOpenStackException("Server is not in status PAUSED, can't be unpaused");
        }
        serverService().action(serverId, Action.UNPAUSE);
        waitForServerStatus(serverId, Server.Status.ACTIVE);
    }

    /**
     * Update the nova cores quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to update the cores for
     * @param cores
     *            - int - new quota value for cores
     */
    public void updateNovaQuotaCoresForTenant(String tenantId, int cores) {
        QuotaSetUpdate qs = Builders.quotaSet().cores(cores).build();
        getClient().compute().quotaSets().updateForTenant(tenantId, qs);
    }

    /**
     * Update the nova injected file content bytes quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to update the injected file content bytes for
     * @param cores
     *            - int - new quota value for injected file content bytes
     * @return boolean
     */
    public boolean updateNovaQuotaInjectedFileContentForTenant(String tenantId, int injectedFileContentBytes) {
        mLogger.info(EcsAction.UPDATING, NovaController.class, "injected file content quota");
        QuotaSetUpdate qs = Builders.quotaSet().injectedFileContentBytes(injectedFileContentBytes).build();
        QuotaSet quotaSet = getClient().compute().quotaSets().updateForTenant(tenantId, qs);
        if (quotaSet.getInjectedFileContentBytes() == injectedFileContentBytes) {
            mLogger.info(Verdict.UPDATED, NovaController.class, "injected file content quota");
            return true;
        }
        return false;
    }

    /**
     * Update the nova instances quota for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to update the instances for
     * @param instances
     *            - int - new quota value for instances
     */
    public void updateNovaQuotaInstancesForTenant(String tenantId, int instances) {
        QuotaSetUpdate qs = Builders.quotaSet().instances(instances).build();
        getClient().compute().quotaSets().updateForTenant(tenantId, qs);
    }

    /**
     * Update the nova quota memory (in mb) for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to update the memory for
     * @param ram
     *            - int - new memory value
     */
    public void updateNovaQuotaMemoryForTenant(String tenantId, int ram) {
        QuotaSetUpdate qs = Builders.quotaSet().ram(ram).build();
        getClient().compute().quotaSets().updateForTenant(tenantId, qs);
    }

    /**
     * Update the metadata for a VM
     *
     * @param serverId - String - id of the server whose metadata will be updated
     * @param metadata - String - new metadata for the VM
     */
    public void updateVmMetadata(String serverId, Map<String, String> metadata) {
        mLogger.info("Try to update metadata for the server " + serverId);
        Map<String, String> updatedMetadata = serverService().updateMetadata(serverId, metadata);
        if (!updatedMetadata.equals(metadata)) {
            throw new EcsOpenStackException("Metadata was not successfully updated for server " + serverId);
        }
    }

    /**
     * virsh reboot the server from compute blade
     *
     * @param vmId - Server id
     * @param blade - on which server is launched
     */
    public void virshRebootVm(String vmId, EcsComputeBlade blade) {
        mLogger.info(EcsAction.REBOOTING, EcsVm.class, vmId);
        if (blade.virshRebootVm(getInstanceName(vmId))) {
            waitForServerStatus(vmId, Server.Status.ACTIVE);
        }
        mLogger.info(Verdict.REBOOTED, EcsVm.class, vmId);
    }

    /**
     * Waits and verifies that all nova services have state == up and status == enabled
     */
    public void waitAndVerifyAllServicesStateUpStatusEnabled() {
        new LoopHelper<Boolean>(Timeout.NOVA_SERVICE_STATE_CHANGE_AFTER_HOST_RESTART,
                "Failed to verify that all nova services are up", Boolean.TRUE, () -> {
                    return checkAllServicesStateUpStatusEnabled();
                }).run();
    }

    /**
     * Wait until nova service-list changes value for status, state or updated_at timestamp
     *
     * @param nodeName - String - name of controller or compute node
     * @param novaService - String - novaService
     *
     */
    public void waitForNovaServiceListUpdate(final String nodeName, final String novaService) {
        mLogger.info(
                "Waiting until service " + novaService + " changes state, status or timestamp on node " + nodeName);
        List<EcsNovaService> novaServices = servicesList();
        int indexInList = 0;
        // find out index of service in service list
        for (EcsNovaService tempService : novaServices) {
            if (tempService.getHost().equals(nodeName) && tempService.getBinary().equals(novaService)) {
                break;
            }
            indexInList++;
        }
        if (indexInList >= novaServices.size()) {
            throw new EcsOpenStackException(
                    "nova service is not running on specified node according to nova service list !!!");
        }
        // initialize original values of service
        final EcsNovaService.State originalState = novaServices.get(indexInList).getState();
        final EcsNovaService.Status originalStatus = novaServices.get(indexInList).getStatus();
        final Date originalTimeStamp = novaServices.get(indexInList).getUpdatedAt();
        mLogger.debug(String.format("Original status: %s | original state: %s | original timestamp: %s", originalStatus,
                originalState, originalTimeStamp));
        String errorMessage = String.format("Nova service %s did not change its status, status or updated_at timestamp",
                novaService);
        // get nova service list until entry for nova service has changed (time stamp or state or status)
        new LoopHelper<Boolean>(Timeout.PROCESS_KILL, errorMessage, Boolean.TRUE, () -> {
            List<EcsNovaService> updatedNovaServices = servicesList();
            EcsNovaService.State updatedState = null;
            EcsNovaService.Status updatedStatus = null;
            Date updatedTimeStamp = null;
            for (EcsNovaService updatedTempService : updatedNovaServices) {
                if (updatedTempService.getHost().equals(nodeName)
                        && updatedTempService.getBinary().equals(novaService)) {
                    updatedState = updatedTempService.getState();
                    updatedStatus = updatedTempService.getStatus();
                    updatedTimeStamp = updatedTempService.getUpdatedAt();
                    break;
                }
            }
            mLogger.debug(String.format("Current status: %s | current state: %s | current timestamp: %s", updatedStatus,
                    updatedState, updatedTimeStamp));
            boolean serviceChanged = (!updatedState.equals(originalState)) || (!updatedStatus.equals(originalStatus))
                    || (updatedTimeStamp.after(originalTimeStamp));
            return (serviceChanged);
        }).run();
    }

    /**
     * Wait until nova service-list shows specified state for nova service on specified host
     *
     * @param hostName - String - name of controller or compute host
     * @param novaService - EcsNovaService.Binary - nova service
     * @param desiredState - String - desired state
     */
    public void waitForNovaServiceState(final String hostname, final EcsNovaService.Binary novaService,
            final EcsNovaService.State desiredState) {

        final String serviceName = novaService.value();
        final String host;
        if (!hostname.contains(".")) {
            host = hostname + mEcsComputeBladeList.getComputeHostDomainName();
        } else {
            host = hostname;
        }
        final String errorMessage = "service " + serviceName + " did not reach state " + desiredState + " on host "
                + host;

        mLogger.info(EcsAction.VALIDATING, "", "EcsNovaService.class",
                "if the nova service " + serviceName + " has reached state " + desiredState + " on host " + host);

        List<EcsNovaService> novaServices = servicesList();
        int indexInList = 0;
        // check if specified nova service is running on specified host
        for (EcsNovaService tempService : novaServices) {
            if (tempService.getHost().equals(host) && tempService.getBinary().equals(serviceName)) {
                break;
            }
            indexInList++;
        }
        if (indexInList >= novaServices.size()) {
            throw new EcsOpenStackException(
                    "nova service is not running on specified node according to nova service list !!!");
        }

        // get nova service list until state for nova service has reached desired state

        new LoopHelper<Boolean>(Timeout.NOVA_SERVICE_STATUS_CHANGE, errorMessage, Boolean.TRUE, () -> {
            List<EcsNovaService> updatedNovaServices = servicesList();
            EcsNovaService.State updatedState = null;
            for (EcsNovaService updatedTempService : updatedNovaServices) {
                if (updatedTempService.getHost().equals(host) && updatedTempService.getBinary().equals(serviceName)) {
                    updatedState = updatedTempService.getState();
                    if (updatedState == desiredState) {
                        return true;
                    }
                }
            }
            mLogger.debug(String.format("Current state: %s", updatedState));
            return false;
        }).setIterationDelay(60).run();

        mLogger.info(Verdict.VALIDATED, "", "Nova Service",
                "service " + serviceName + " reached state " + desiredState + " on host " + host);
    }

    /**
     * Wait until nova service-list shows specified status for nova service on specified host
     *
     * @param hostName - String - name of controller or compute host
     * @param novaService - EcsNovaService.Binary - nova service
     * @param desiredStatus - String - desired status
     */
    public void waitForNovaServiceStatus(final String hostname, final EcsNovaService.Binary novaService,
            final EcsNovaService.Status desiredStatus) {

        final String serviceName = novaService.value();
        final String host;
        if (!hostname.contains(".")) {
            host = hostname + mEcsComputeBladeList.getComputeHostDomainName();
        } else {
            host = hostname;
        }
        final String errorMessage = "service " + serviceName + " did not reach status " + desiredStatus + " on host "
                + host;

        mLogger.info(EcsAction.VALIDATING, "", "Nova Service",
                "if the nova service " + serviceName + " has reached status " + desiredStatus + " on host " + host);

        List<EcsNovaService> novaServices = servicesList();
        int indexInList = 0;
        // check if specified nova service is running on specified host
        for (EcsNovaService tempService : novaServices) {
            if (tempService.getHost().equals(host) && tempService.getBinary().equals(serviceName)) {
                break;
            }
            indexInList++;
        }
        if (indexInList >= novaServices.size()) {
            throw new EcsOpenStackException(
                    "nova service is not running on specified node according to nova service list !!!");
        }

        // get nova service list until status for nova service has reached desired status

        new LoopHelper<Boolean>(Timeout.NOVA_SERVICE_STATUS_CHANGE, errorMessage, Boolean.TRUE, () -> {
            List<EcsNovaService> updatedNovaServices = servicesList();
            EcsNovaService.Status updatedStatus = null;
            for (EcsNovaService updatedTempService : updatedNovaServices) {
                if (updatedTempService.getHost().equals(host) && updatedTempService.getBinary().equals(serviceName)) {
                    updatedStatus = updatedTempService.getStatus();
                    if (updatedStatus == desiredStatus) {
                        return true;
                    }
                }
            }
            mLogger.debug(String.format("Current status: %s", updatedStatus));
            return false;
        }).setIterationDelay(60).run();

        mLogger.info(Verdict.VALIDATED, "", "Nova Service",
                "service " + serviceName + " reached status " + desiredStatus + " on host " + host);
    }

    /**
     * Checks if server instance reaches specified status within default timeout limit. Exception will be thrown if the
     * server hasn't reached expected status within that timeout.
     * The time used to check that the server has reached desired status are:
     * timeout = {@value #Timeout.SERVER_READY} and iteration delay = {@value #ITERATION_DELAY}.
     *
     * @param serverId - String - id of the server
     * @param desiredStatus - {@link Status} - desired status of the server
     * @throws LoopTimeoutException
     * @throws LoopInterruptedException
     */
    public void waitForServerStatus(final String serverId, final Status desiredStatus) {
        mLogger.info(EcsAction.STATUS_CHANGING, EcsVm.class, serverId + ", Timeout: "
                + Timeout.SERVER_READY.getTimeoutInSeconds() + "seconds. Target status: " + desiredStatus);
        final String errorMessage = String.format("Server %s did not reach status: %s", serverId, desiredStatus);
        LoopHelper<Server.Status> loopHelper = new LoopHelper<Server.Status>(Timeout.SERVER_READY, errorMessage,
                desiredStatus, () -> {
                    Server server = serverService().get(serverId);
                    // this check is to prevent
                    if (null == server) {
                        throw new EcsOpenStackException(String.format(
                                "Failed to retrieve info for server %s while checking server status.", serverId));
                    }
                    Server.Status currentServerState = server.getStatus();
                    mLogger.debug(String.format("Current server state:" + currentServerState.toString()));
                    if (currentServerState.equals(Server.Status.ERROR)) {
                        mLogger.error("Server went into status ERROR");
                        mLogger.error("Current server info:" + serverService().get(serverId));
                        mLogger.error("Fault message from the server: " + getVmFaultMessage(serverId));
                    }
                    return currentServerState;

                });
        loopHelper.setErrorState(Server.Status.ERROR);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, desiredStatus, EcsVm.class, serverId);
    }
}

package com.jcat.cloud.fw.common.utils;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import se.ericsson.jcat.fw.assertion.JcatAssertApi;

import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.components.model.compute.EcsVm;
import com.jcat.cloud.fw.components.model.network.EcsNetwork;
import com.jcat.cloud.fw.components.model.network.EcsSubnet;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolume;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolumeType;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;

/**
 * This class contains sanity check procedures.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eedsla 2015-05-05 Created this class by copying original SanityCheck and adapting to FW 2.0
 *
 */

public final class SanityCheck4J {

    /**
     * Logger instance
     */
    private static final Logger LOGGER = Logger.getLogger(SanityCheck4J.class);

    /**
     * name of network
     */
    private static final String NETWORK_NAME = "SanityCheckNetwork";

    /**
     * volume size in GB
     */
    private static final int SC_VOLUME_SIZE = 10;

    /**
     * name of subnetwork
     */
    private static final String SUBNETWORK_NAME = "SanityCheckSubnetwork";

    /**
     * Hide Utility Class Constructor:
     * Utility classes should not have a public or default constructor.
     */
    private SanityCheck4J() {
    }

    /**
     * Boots a VM either on specified blade or on any blade
     *
     * @param novaController - NovaController - NovaController class
     * @param networkId - String - ID of network
     * @param computeBladeList - EcsComputeBladeList - list of Compute blades
     * @param computeHost - String - host name of compute host ("any" if no specific host required)
     * @param imageId - String - ID of glance image
     * @param flavorId - String - ID of flavor
     *
     * @return String - ID of sanity check VM
     */
    private static String bootVm(NovaController novaController, String networkId, EcsComputeBladeList computeBladeList,
            String computeHost, String imageId, String flavorId) {

        EcsVm scServer;

        if (computeHost.equalsIgnoreCase(CommonParametersValues.ANY_COMPUTE_HOST)) {
            LOGGER.info("Booting VM");
            scServer = EcsVm.builder(imageId, flavorId).network(networkId).build();

        } else {
            LOGGER.info("Booting VM on host " + computeHost);
            scServer = EcsVm.builder(imageId, flavorId).network(networkId)
                    .availabilityZone(CommonParametersValues.AVAILABILITYZONE + ":" + computeHost).build();

        }
        EcsVm scVM = novaController.createVm(scServer);

        // check if server is started on correct host (if compute host was specified)
        if (!(computeHost.equalsIgnoreCase(CommonParametersValues.ANY_COMPUTE_HOST))) {
            JcatAssertApi.assertTrue("VM did not start on specified compute host",
                    computeBladeList.getComputeBlade(computeHost).doesVmExist(scVM.getHypervisorId()));
        }

        return scVM.getId();
    }

    /**
     * Run a complete sanity check consisting of creating network and subnetwork, booting VM, creating
     * volume, attaching volume, detaching volume, deleting volume, deleting VM, deleting network (and
     * with it implicitly subnetwork)
     *
     * @param cicNode - EcsCic - CIC instance
     * @param neutronController - NeutronController - Neutron Controller
     * @param novaController - NovaController - Nova Controller
     * @param cinderController - CinderController - Cinder Controller
     * @param computeBladeList - EcsComputeBladeList - list of Compute blades
     * @param computeHost - String - host name of compute host ("any" if no specific host required)
     * @param imageId - String - ID of glance image
     * @param flavorId - String - ID of flavor
     *
     * @return void
     * @throws UnknownHostException
     */
    public static void runSanityCheck(EcsCic cicNode, NeutronController neutronController,
            NovaController novaController, CinderController cinderController, EcsComputeBladeList computeBladeList,
            String computeHost, String imageId, String flavorId) throws UnknownHostException {

        LOGGER.info("SANITY CHECK: Checking nova services and neutron agents");
        JcatAssertApi.assertTrue("Nova services are not in a correct state/status !!",
                novaController.checkAllServicesStateUpStatusEnabled());
        JcatAssertApi.assertTrue("Not all Neutron services are alive", neutronController.areAllNeutronAgentsAlive());

        LOGGER.info("SANITY CHECK: Start creating and deleting Openstack resources");
        LOGGER.info("Create NW, SubNw, VM, Volume -->  Attach and detach volume  --> Delete Volume, VM, NW.");

        // create network and subnetwork
        String scNetworkId = neutronController.createNetwork(EcsNetwork.builder().name(NETWORK_NAME).build());
        neutronController.createSubnet(EcsSubnet.builder(scNetworkId).name(SUBNETWORK_NAME).build());

        // boot VM
        String scServerId = bootVm(novaController, scNetworkId, computeBladeList, computeHost, imageId, flavorId);

        // create Volume
        String scVolumeTypeId = cinderController.createVolumeType(EcsVolumeType.builder("SC_TYPE").build());
        String scVolumeId = cinderController.createVolume(EcsVolume.builder(scVolumeTypeId, SC_VOLUME_SIZE).build());

        // attach volume
        String scVolumeAttachId = novaController.attachVolumeToServer(scVolumeId, scServerId, null);

        // detach volume
        novaController.detachVolumeFromServer(scVolumeAttachId, scVolumeId, scServerId);

        // delete volume
        cinderController.deleteVolume(scVolumeId);
        cinderController.deleteVolumeType(scVolumeTypeId);

        // delete VM
        novaController.deleteVm(scServerId);

        // delete network (and with it subnetwork)
        neutronController.deleteNetwork(scNetworkId);

    }
}

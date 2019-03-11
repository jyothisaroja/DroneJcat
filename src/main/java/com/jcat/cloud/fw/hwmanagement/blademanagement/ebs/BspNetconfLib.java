package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.hwmanagement.blademanagement.EquipmentControllerException;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.BridgeNumber;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.BridgePortNumber;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.CommonArpTarget;
import com.jcat.cloud.fw.infrastructure.resources.DmxResource;

import bspEricssonComTop.ManagedElementDocument.ManagedElement.Transport.Bridge;
import bspEricssonComTop.ManagedElementDocument.ManagedElement.Transport.Bridge.BridgePort;
import bspEricssonComTop.ManagedElementDocument.ManagedElement.Transport.Bridge.Vlan;
import bspEricssonDMXCTransportLibrary.AdminState;
import bspEricssonECIMCommonLibrary.AdmState;
import se.ericsson.jcat.netconf.bsp.api.dmxcfunction.BladeApi;
import se.ericsson.jcat.netconf.bsp.api.dmxcfunction.IPv4NetworkApi;
import se.ericsson.jcat.netconf.bsp.api.dmxcfunction.SystemInformationApi;
import se.ericsson.jcat.netconf.ebs.api.NetconfApi;
import se.ericsson.jcat.netconf.ecim.api.transport.BridgeApi;
import se.ericsson.jcat.netconf.ecim.api.transport.BridgePortApi;

/**
 * Implementation of {@link IEquipmentController} plus extra functionalities for managing EBS(Ericsson blade system)
 * using BSP (Ericsson blade server platform) NetConf library
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce - 2014-06-04 - first version (migrated from eladand's code)
 * @author ehosmol - 2014-10-12 - Adapted to Bridge and Bridge port enums from {@link EbsCommonTypesLib}, Removed
 *         unnecessary functions
 * @author zdagjyo 2017-14-03 modify powerOff and powerOn methods to imclude ecscomputeblade
 * @author zdagjyo 2017-09-22 Implemented powerOn and powerOff methods, added methods getBladeId, locakBlade,
 *         unlockBlade and waitForBladeAdminStatus
 * @author zdagjyo 2018-02-20 Added methods lockSwitchBlade, unlockSwitchBlade and
 *         waitForBridgeConfigurationReady
 */
public class BspNetconfLib implements IEquipmentController {

    // enum which represents cmx and scx slots of a BSP node
    public enum SwitchBlade {

         CMX_0_LEFT("0-26"), CMX_0_RIGHT("0-28"),
         CMX_1_LEFT("1-26"), CMX_1_RIGHT("1-28"),
         CMX_2_LEFT("2-26"), CMX_2_RIGHT("2-28"),
         CMX_3_LEFT("3-26"), CMX_3_RIGHT("3-28"),
         SCX_0_LEFT("0-0"),  SCX_0_RIGHT("0-25"),
         SCX_1_LEFT("1-0"),  SCX_1_RIGHT("1-25"),
         SCX_2_LEFT("2-0"),  SCX_2_RIGHT("2-25"),
         SCX_3_LEFT("3-0"),  SCX_3_RIGHT("3-25");

        private String mSubrackSlot;

        SwitchBlade(String subrackSlot) {
            mSubrackSlot = subrackSlot;
        }

        public String getSubrackSlot() {
            return mSubrackSlot;
        }
    }

    /**
     * Ideal bridge configuration status ready value (100) of BSP
     */
    private static final int BRIDGE_CONFIGURATION_STATUS_READY = 100;
    /**
     * Iteration delay for blade state checks
     */
    private static final int WAIT_ADMIN_STATE_LOOP_DELAY_SEC = 1;

    private static final String BSP_TENANT = "BSP";
    private static final String TENANT = "CEE";

    /**
     * resource data for BSP
     * TODO: change to BspResource if decided to have one of those
     */
    private final DmxResource mBspConfiguration;

    private int mNetconfInstanceId = 0;

    private final Logger mLogger = Logger.getLogger(BspNetconfLib.class);

    private int mWaitAdminStateLoopDelaySec = WAIT_ADMIN_STATE_LOOP_DELAY_SEC;

    private Timeout mWaitAdminStateLoopTimeout = Timeout.BLADE_ADMIN_STATE;

    /**
     *
     * @param bspResources - DmxResourceGroup
     */
    @Inject
    public BspNetconfLib(DmxResource bspResource) {
        mBspConfiguration = bspResource;
    }

    /**
     * Gets the blade id of a compute blade in the format 'subrack-baynumber'
     * for example, the blade id of compute-0-5 would be 0-9 ( baynumber in dmx = actual baynumber * 2 - 1)
     *
     * @param blade
     * @return
     */
    private String getBladeId(EcsComputeBlade blade) {
        int rack = blade.getBaySubrack();
        int bay = blade.getBayNumber() * 2 - 1;
        return rack + "-" + bay;
    }

    /**
     * Lock a specified blade. This will power off a blade via DMX. The blade can be GEP or SCX
     *
     * @param blade
     * @return
     */
    private boolean lockBlade(EcsComputeBlade blade) {
        return BladeApi.setAdministrativeState(mNetconfInstanceId, TENANT, getBladeId(blade), AdmState.LOCKED);
    }

    /**
     * Unlock a specified blade. This will power on a blade via DMX. The blade can be GEP or SCX
     *
     * @param blade
     * @return
     */
    private boolean unlockBlade(EcsComputeBlade blade) {
        return BladeApi.setAdministrativeState(mNetconfInstanceId, TENANT, getBladeId(blade), AdmState.UNLOCKED);
    }

    /**
     * Checks a blades admin status to make sure it's in the desired state.
     *
     * @param blade
     * @param desiredState
     */
    private void waitForBladeAdminStatus(final EcsComputeBlade blade, final AdmState.Enum desiredState) {
        LoopHelper<AdmState.Enum> loopHelper = new LoopHelper<AdmState.Enum>(mWaitAdminStateLoopTimeout, null,
                desiredState, () -> {
                    mLogger.debug("Checking blade state, waiting for state " + desiredState.toString().toLowerCase());
                    AdmState.Enum currentState = BladeApi.getAdministrativeState(mNetconfInstanceId, TENANT,
                            getBladeId(blade));
                    mLogger.info("Current blade state is " + currentState);
                    return currentState;
                });
        loopHelper.setIterationDelay(mWaitAdminStateLoopDelaySec);
        loopHelper.run();
        mLogger.info("Blade " + blade.getHostname() + " reached desired state:" + desiredState);
    }

    /**
     * Waits for the bridge configuration to be ready.
     * It is said to be ready when the bridge configuration status value is 100
     * (when the system is normal, the value of bridge configuration status is 100)
     */
    private void waitForBridgeConfigurationReady() {
        new LoopHelper<Boolean>(Timeout.BRIDGE_CONFIGURATION_STATUS_CHNAGE, null, true, () -> {
            mLogger.info("Waiting for bridge configuration status value to be 100");
            int bridgeConfigurationStatus = SystemInformationApi.getBridgeConfigurationStatus(mNetconfInstanceId);
            return bridgeConfigurationStatus == BRIDGE_CONFIGURATION_STATUS_READY;
        }).setIterationDelay(10).run();
        mLogger.info("Bridge configuration status value is now 100");
    }

    /**
     * common function for testing if a port is in a vLan
     *
     * @param bridgeNr - String
     * @param portNr - String
     * @param vlanNr - int
     * @param tagged - boolean
     * @return boolean
     */
    protected boolean isPortInVlanCommon(String bridgeNr, String portNr, int vlanNr, boolean tagged) {
        Bridge bridge = BridgeApi.getBridge(mNetconfInstanceId, bridgeNr);
        Vlan vlan = null;
        Vlan[] vlans = bridge.getVlanArray();
        for (Vlan vl : vlans) {
            if (vl.getVlanId().equals(Integer.toString(vlanNr))) {
                vlan = vl;
            }
        }
        if (vlan == null) {
            return false;
        }
        String[] ports = null;
        if (tagged) {
            ports = vlan.getTaggedBridgePortsArray();
        } else {
            ports = vlan.getUntaggedBridgePortsArray();
        }
        if (ports == null) {
            return false;
        }
        for (String port : ports) {
            int eqSignIndex = port.lastIndexOf("=");
            if (eqSignIndex != -1) {
                port = port.substring(eqSignIndex + 1);
                if (port.equals(portNr)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Closes the established Netconf session.
     * This API needs to be invoked after any other API in this class is invoked.
     */
    @Override
    public void closeConnection() {
        BridgePortApi.closeSession(mNetconfInstanceId);
    }

    /**
     * get the arp responder of a given id
     *
     * @param id arp responder id
     * @return {@link CommonArpTarget}
     */
    public CommonArpTarget getArpResponder(String id) {
        return CommonArpTarget.createCommonArpTarget(IPv4NetworkApi.getIpv4Network(mNetconfInstanceId, id));
    }

    /**
     * @return {@link DmxResource}
     */
    @Override
    public DmxResource getConfiguration() {
        return mBspConfiguration;
    }

    /**
     * gets the Administrative state of a bridge port
     *
     * @param bridgeNr - String
     * @param portNr - String
     * @return {@link PortAdminState}
     */
    public PortAdminState getPortAdminState(String bridgeNr, String portNr) {
        AdminState.Enum adminState = BridgePortApi.getAdministrativeState(mNetconfInstanceId, bridgeNr, portNr);
        switch (adminState.intValue()) {
        case 1:
            return PortAdminState.ENABLED;
        case 2:
            return PortAdminState.DISABLED;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPortEnabled(int bridgeNr, int portNr) {
        return PortAdminState.ENABLED == getPortAdminState(BridgeNumber.getValueForIndex(bridgeNr),
                BridgePortNumber.getValueForIndex(portNr));
    }

    /**
     * check vlan presence on a specific port of a bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @param vlanNr
     * @return boolean - true if port is in vlan
     */
    public boolean isPortInVlan(int bridgeNr, int portNr, int vlanNr) {
        return isUntaggedPortInVlan(bridgeNr, portNr, vlanNr) || isTaggedPortInVlan(bridgeNr, portNr, vlanNr);
    }

    /**
     *
     * @param bridgeNr - {@link BridgeNumber}
     * @param portNr - {@link BridgePortNumber}
     * @param vlanNr - int
     * @return boolean
     */
    public boolean isTaggedPortInVlan(int bridgeNr, int portNr, int vlanNr) {
        return isPortInVlanCommon(BridgeNumber.getValueForIndex(bridgeNr), BridgePortNumber.getValueForIndex(portNr),
                vlanNr, true);
    }

    /**
     * check if vlan is present as untagged on a specific port of a bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @param vlanNr
     * @return true if port is untagged, otherwise false
     */
    public boolean isUntaggedPortInVlan(int bridgeNr, int portNr, int vlanNr) {
        return isPortInVlanCommon(BridgeNumber.getValueForIndex(bridgeNr), BridgePortNumber.getValueForIndex(portNr),
                vlanNr, false);
    }

    /**
     * Locks the specified switch
     *
     * @param switchToLock - enum SwitchBlade - the CMX/SCX switch to be locked
     * @return boolean
     */
    public boolean lockSwitchBlade(SwitchBlade switchToLock) {
        waitForBridgeConfigurationReady();
        return BladeApi.setAdministrativeState(mNetconfInstanceId, BSP_TENANT, switchToLock.getSubrackSlot(),
                AdmState.LOCKED);
    }

    /**
     * Establishes a Netconf session.
     * This API needs to be invoked before any other API in this class is invoked.
     *
     * @throws EquipmentControllerException
     */
    @Override
    public void openConnection() throws EquipmentControllerException {
        BridgePortApi.setLog4jProtocolLoggers(Logger.getLogger(BspNetconfLib.class),
                Logger.getLogger(BspNetconfLib.class));
        mNetconfInstanceId = BridgePortApi.startSession(mBspConfiguration.getIp(),
                // UserNameExpert == "advanced"
                mBspConfiguration.getUserNameExpert(),
                // UserPasswordExpert == "ett,30"
                mBspConfiguration.getUserPasswordExpert(),
                // PortNetconfSsh == 22012
                mBspConfiguration.getPortNetconfSsh());
    }

    /**
     * Powers off the specified compute blade.
     * Connection will have to be maintained by test case
     * (using openConnection()) but not a part of this FW API.
     *
     * @param blade - the compute blade to power off
     * @return boolean
     * @throws EquipmentControllerException
     */
    @Override
    public boolean powerOff(EcsComputeBlade blade) throws EquipmentControllerException {
        boolean result = lockBlade(blade);
        waitForBladeAdminStatus(blade, AdmState.LOCKED);
        return result;
    }

    /**
     * Powers on the specified compute blade.
     * Connection will have to be maintained by test case
     * (using openConnection()) but not a part of this FW API.
     *
     * @param blade - the compute blade to power on
     * @return boolean
     * @throws EquipmentControllerException
     */
    @Override
    public boolean powerOn(EcsComputeBlade blade) throws EquipmentControllerException {
        boolean result = unlockBlade(blade);
        waitForBladeAdminStatus(blade, AdmState.UNLOCKED);
        return result;
    }

    /**
     * Attention: not implemented yet, therefore throws new NotImplementedException
     */
    @Override
    public boolean powerReset(EcsComputeBlade blade) throws EquipmentControllerException {
        // TODO implement if possible
        throw new NotImplementedException("The method \"powerReset()\" is not implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPortState(int bridgeNr, int portNr, PortAdminState portState) {
        BridgePortApi.setAdministrativeState(mNetconfInstanceId, BridgeNumber.getValueForIndex(bridgeNr),
                BridgePortNumber.getValueForIndex(portNr), portState.getBspAdminStateEnum());
        NetconfApi.commit(mNetconfInstanceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showDefaultVlan(int bridgeNr, int portNr) {
        BridgePort[] bridgePorts = BridgeApi.getBridgePortArray(mNetconfInstanceId,
                BridgeNumber.getValueForIndex(bridgeNr));
        for (BridgePort bridgePort : bridgePorts) {
            if (bridgePort.getBridgePortId().equals(BridgePortNumber.getValueForIndex(portNr))) {
                return bridgePort.getDefaultVlanId();
            }
        }
        // should not happen
        return -1;
    }

    /**
     * Unlocks the specified switch
     *
     * @param switchToUnlock - enum SwitchBlade - the CMX/SCX switch to be unlocked
     * @return boolean
     */
    public boolean unlockSwitchBlade(SwitchBlade switchToUnlock) {
        boolean switchUnlocked = BladeApi.setAdministrativeState(mNetconfInstanceId, BSP_TENANT,
                switchToUnlock.getSubrackSlot(), AdmState.UNLOCKED);
        waitForBridgeConfigurationReady();
        return switchUnlocked;
    }
}

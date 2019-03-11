package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import se.ericsson.jcat.dmx.tcapi.NetconfDmxAsc;
import se.ericsson.jcat.dmx.tcapi.NetconfDmxBGM;
import se.ericsson.jcat.dmx.utils.NetConf;

import com.ericsson.dmx.ManagedElementDocument;
import com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.BladeGroupManagement.Group.ShelfSlot.Blade;
import com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.DmxSysM.Network;
import com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.DmxSysM.Network.ArpTargets;
import com.ericsson.dmxTypes.Opstate;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.hwmanagement.blademanagement.EquipmentControllerException;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.BridgeNumber;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.BridgePortNumber;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EbsCommonTypesLib.CommonArpTarget;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.BladeAdminState;
import com.jcat.cloud.fw.infrastructure.resources.DmxResource;

/**
 * Implementation of {@link IEquipmentController} using DMX through NetConf
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2013-01-17 Initial version. Combined DMXCLib with NetconfDmxLib and formed EquipmentController class
 * @author esauali 2013-05-08 Remove unused method, change waitForBladeAdminStatus()
 * @author eqinann 2013-06-07 Added method to get first MAC address (dmx lib minimal 0.0.2)
 * @author esauali 2013-08-02 add getBlaseInfo() to interface
 * @author eladand 2013-11-14 arp responder get/set added
 * @author ethssce 2014-06-13 introduced BladeAdminState and PortAdminState
 * @author eqinann 2014-08-19 Changed to new TimeOut enum for LoopHelper
 * @author ehosmol 2014-10-12 removed bridge and bridge port array and adapted to new enums from
 *         {@link EbsCommonTypesLib},
 *         Update javadoc
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author zdagjyo 2017-14-03 modify powerOff and powerOn methods to include ecscomputeblade
 */
public class DmxNetconfLib implements IEquipmentController {

    /**
     * Blade Group Type is defined in DMX
     */
    protected enum BladeSoftwareGroup {
        Applications, DMX
    }

    /**
     * Iteration delay for blade state checks
     */
    private static final int WAIT_ADMIN_STATE_LOOP_DELAY_SEC = 1;

    /**
     * The selected DMX resource that this DmxNetconfLib will connect to
     */
    private final DmxResource mDmxConfiguration;

    private EcsBladeBuilder mEcsBladeBuilder;

    private final Logger mLogger = Logger.getLogger(DmxNetconfLib.class);

    private NetConf mNetconf;

    /**
     * DMX NETCONF method library for BladeGroupManagement command tree
     */
    private NetconfDmxBGM mNetconfBladeGroupManagement;

    /**
     * DMX NETCONF method library for Software management command tree
     */
    private NetconfDmxAsc mNetconfSoftwareControl;

    private int mWaitAdminStateLoopDelaySec = WAIT_ADMIN_STATE_LOOP_DELAY_SEC;

    private Timeout mWaitAdminStateLoopTimeout = Timeout.BLADE_ADMIN_STATE;

    /**
     * Main constructor
     *
     * @param dmxResources - DmxResourceGroup - all available DMX resources
     * @param ecsBladeBuilder
     */
    @Inject
    public DmxNetconfLib(DmxResource dmxResource) {
        mDmxConfiguration = dmxResource;
    }

    private EcsBladeBuilder getEcsBladeBuilder() {
        if (null == mEcsBladeBuilder) {
            return new EcsBladeBuilder();
        }
        return mEcsBladeBuilder;
    }

    /**
     * Get {@link #mNetconfBladeGroupManagement} library object
     *
     * @return
     */
    private NetconfDmxBGM getNetconfDmxBGMDriver() {
        if (null == mNetconfBladeGroupManagement) {
            mNetconfBladeGroupManagement = new NetconfDmxBGM(getNetconfDriver());
        }
        return mNetconfBladeGroupManagement;
    }

    /**
     * Get {@link #mNetconf} library object
     *
     * @return
     */
    private NetConf getNetconfDriver() {
        if (null == mNetconf) {
            mNetconf = new NetConf();
        }
        return mNetconf;
    }

    /**
     * Lock a specified blade. This will power off a blade via DMX. The blade can be GEP or SCX
     *
     * @param group
     * @param subrack
     * @param slot
     * @return True if blade was locked
     * @throws EquipmentControllerException
     */
    private boolean lockBlade(String group, int subrack, int slot) throws EquipmentControllerException {
        try {
            return getNetconfDmxBGMDriver().lockBlade(group, subrack, slot);
        } catch (Exception e) {
            throw new EquipmentControllerException("Cannot lock blade", e);
        }
    }

    /**
     * Starts a NETCONF session using one of the switch NB IP addresses
     *
     * @param subrack
     * @return IP address of the active DMX
     * @throws EquipmentControllerException
     */
    private void openNetconfConnection(int subrack) throws EquipmentControllerException {
        mLogger.debug("Openning connection to DMX subrack:" + subrack);
        // TODO: for now all DMX's are configured to to have one subrack that is 0
        if (subrack != 0) {
            throw new EquipmentControllerException("Currently only subrack 0 is supported");
        }
        getNetconfDriver().startNetConfSession(mDmxConfiguration.getIp(), mDmxConfiguration.getPortNetconfSsh(),
                mDmxConfiguration.getUserNameExpert(), mDmxConfiguration.getUserPasswordExpert());
    }

    /**
     * Perform a power cycle on a blade via DMX. The blade can be GEP or SCX.
     *
     * @param group
     * @param subrack
     * @param slot
     * @return
     * @throws EquipmentControllerException
     */
    private boolean resetBlade(String group, int subrack, int slot) throws EquipmentControllerException {
        try {
            return getNetconfDmxBGMDriver().resetBlade(group, subrack, slot);
        } catch (Exception e) {
            throw new EquipmentControllerException("Cannot reset blade", e);
        }
    }

    /**
     * Unlock a specified blade. This will power on a blade via DMX. The blade can be GEP or SCX
     *
     * @param group
     * @param subrack
     * @param slot
     * @return
     * @throws EquipmentControllerException
     */
    private boolean unlockBlade(String group, int subrack, int slot) throws EquipmentControllerException {
        try {
            return getNetconfDmxBGMDriver().unlockBlade(group, subrack, slot);
        } catch (Exception e) {
            throw new EquipmentControllerException("Cannot unlock blade", e);
        }
    }

    /**
     * Checks a blades admin status to make sure it's in the desired state.
     *
     * @param group
     * @param subrack
     * @param slot
     * @param desiredState
     * @param maxTime
     *            the maximum waiting time in seconds
     * @throws EquipmentControllerException
     */
    private void waitForBladeAdminStatus(final String group, final int subrack, final int slot,
            final BladeAdminState desiredState) throws EquipmentControllerException {
        LoopHelper<BladeAdminState> loopHelper = new LoopHelper<BladeAdminState>(mWaitAdminStateLoopTimeout, null,
                desiredState, new LoopHelper.ICheck<BladeAdminState>() {
                    @Override
                    public BladeAdminState getCurrentState() {
                        mLogger.debug("Checking blade state, waiting for state "
                                + desiredState.toString().toLowerCase());
                        try {
                            BladeAdminState currentState = getBladeAdminState(group, subrack, slot);
                            mLogger.info("Current blade state is " + currentState);
                            return currentState;
                        } catch (EquipmentControllerException e) {
                            mLogger.error("Could not get blade administrative state", e);
                            return null;
                        }
                    }
                });
        loopHelper.setIterationDelay(mWaitAdminStateLoopDelaySec);
        try {
            loopHelper.run();
        } catch (Exception e) {
            mLogger.error(e.getMessage());
            throw new EquipmentControllerException("Blade did not reach state: " + desiredState);
        }
        mLogger.info("Blade " + subrack + ":" + slot + " reached desired state:" + desiredState);
    }

    /**
     * Get or create a default {@link #mNetconfSoftwareControl}
     *
     * @param library
     */
    protected NetconfDmxAsc getNetconfSoftwareControl() {
        if (mNetconfSoftwareControl == null) {
            NetconfDmxAsc netconfDmxAsc = new NetconfDmxAsc(getNetconfDriver());
            setLibSoftwareManagement(netconfDmxAsc);
        }
        return mNetconfSoftwareControl;
    }

    /**
     * @return the waitAdminStateLoopDelaySec
     */
    protected int getWaitAdminStateLoopDelaySec() {
        return mWaitAdminStateLoopDelaySec;
    }

    /**
     * @return the waitAdminStateLoopTimeout
     */
    protected Timeout getWaitAdminStateLoopTimeout() {
        return mWaitAdminStateLoopTimeout;
    }

    /**
     * Set {@link #mNetconfBladeGroupManagement} for UT purposes.
     *
     * @param library
     */
    protected void setLibBladeGroupControl(NetconfDmxBGM library) {
        mNetconfBladeGroupManagement = library;
    }

    /**
     * Set {@link #mNetconf} for UT purposes.
     */
    protected void setLibNetconfMain(NetConf library) {
        mNetconf = library;
    }

    /**
     * Set {@link #mNetconfSoftwareControl} for UT purposes.
     *
     * @param library
     */
    protected void setLibSoftwareManagement(NetconfDmxAsc library) {
        mNetconfSoftwareControl = library;
    }

    /**
     * Set {@link #mWaitAdminStateLoopDelaySec} for UT only
     *
     * @param
     */
    protected void setWaitAdminStateLoopDelaySec(int waitAdminStateLoopDelaySec) {
        mWaitAdminStateLoopDelaySec = waitAdminStateLoopDelaySec;
    }

    /**
     * Set {@link #mWaitAdminStateLoopTimeoutc} for UT only
     *
     * @param
     */
    protected void setWaitAdminStateLoopTimeout(Timeout waitAdminStateLoopTimeout) {
        mWaitAdminStateLoopTimeout = waitAdminStateLoopTimeout;
    }

    /**
     * add an arp responder with the properties given.
     *
     * The arp responder ips will be the .1 and .2 in the specified network
     *
     * @param id arp responder id
     * @param subnet
     * @param netmask
     * @param vlan
     * @return boolean - true if successful
     */
    public boolean addArpResponder(String id, String subnet, String netmask, int vlan) {

        // TODO: check if this implementations really works

        ManagedElementDocument meDoc = getNetconfSoftwareControl().mkMED();

        Network network = (Network) getNetconfSoftwareControl().mkElements("ME 1 DmxFunctions 1 DmxSysM 1 Network 1",
                meDoc);
        ArpTargets arpTarget = network.addNewArpTargets();
        arpTarget.setArpTargetId(id);
        arpTarget.setSubnet(subnet);
        arpTarget.setNetmask(netmask);
        arpTarget.setVlanid(vlan);

        return getNetconfSoftwareControl().sendNetconfEditConfMessage(meDoc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection() {
        if (null != mNetconf) {
            mNetconf.closeNetconfSession();
        }
    }

    /**
     * get the arp responder of a given id
     *
     * @param id arp responder id
     * @return {@link CommonArpTarget}
     */
    public CommonArpTarget getArpResponder(String id) {
        ManagedElementDocument meDoc = getNetconfSoftwareControl().mkMED();
        getNetconfSoftwareControl().mkElements("ME 1 DmxFunctions 1 DmxSysM 1 Network 1", meDoc);
        ManagedElementDocument meReply = getNetconfSoftwareControl().getMeDocFromReply(
                getNetconfSoftwareControl().sendNetconfGetMessage(meDoc));
        ArpTargets[] arpTargets = meReply.getManagedElement().getDmxFunctions().getDmxSysM().getNetwork()
                .getArpTargetsArray();
        for (int i = 0; i < arpTargets.length; i++) {
            if (arpTargets[i].getArpTargetId().equals(id)) {
                return CommonArpTarget.createCommonArpTarget(arpTargets[i]);
            }
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Get administrative state of specified blade
     *
     * @param group
     * @param subrack
     * @param slot
     * @return
     * @throws EquipmentControllerException
     */
    public BladeAdminState getBladeAdminState(String group, int subrack, int slot) throws EquipmentControllerException {
        try {
            return BladeAdminState.getEnumFromInt(getNetconfDmxBGMDriver().getBladeAdminState(group, subrack, slot)
                    .intValue());
        } catch (Exception e) {
            throw new EquipmentControllerException("Cannot get blade administrative state", e);
        }
    }

    /**
     * Get blade information
     *
     * @param subrack
     * @param slot
     * @return {@link EcsBlade}
     * @throws EquipmentControllerException
     */
    public EcsBlade getBladeInfo(int subrack, int slot) throws EquipmentControllerException {
        try {
            Blade gepBlade = getNetconfDmxBGMDriver().getBladeInfo(BladeSoftwareGroup.Applications.toString(), subrack,
                    slot);
            return getEcsBladeBuilder().build(gepBlade);
        } catch (Exception e) {
            throw new EquipmentControllerException("Cannot get blade information of blade: subrack " + subrack
                    + " slot " + slot, e);
        }
    }

    @Override
    public DmxResource getConfiguration() {
        return mDmxConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPortEnabled(int bridgeInstance, int portInstance) {
        return Opstate.ENABLED == getNetconfSoftwareControl().getPortOperationalState(
                BridgeNumber.getValueForIndex(bridgeInstance), BridgePortNumber.getValueForIndex(portInstance));
    }

    /**
     * check vlan presence on a specific port of a bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @param vlan
     * @return boolean - true if port is in vlan
     */
    public boolean isPortInVlan(int bridgeInstance, int portInstance, int vlan) {
        List<String> portList = Arrays.asList(getNetconfSoftwareControl().getMemberPortArray(
                BridgeNumber.getValueForIndex(bridgeInstance), vlan));
        String portToFind = BridgePortNumber.getValueForIndex(portInstance);
        return portList.contains(portToFind);
    }

    /**
     * Fetch power status for a blade
     *
     * @param subrack
     * @param blade
     * @return boolean - true if power is on
     * @throws EquipmentControllerException
     */
    public boolean isPowerOn(int subrack, int blade) throws EquipmentControllerException {
        return getBladeAdminState(BladeSoftwareGroup.Applications.toString(), subrack, blade).equals(
                BladeAdminState.UNLOCKED);
    }

    /**
     * check if vlan is present as untagged on a specific port of a bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @param vlan
     * @return true if port is untagged, otherwise false
     */
    public boolean isUntaggedPortInVlan(int bridgeInstance, int portInstance, int vlan) {
        List<String> portList = Arrays.asList(getNetconfSoftwareControl().getUntaggedMemberPortArray(
                BridgeNumber.getValueForIndex(bridgeInstance), vlan));
        String portToFind = BridgePortNumber.getValueForIndex(portInstance);
        return portList.contains(portToFind);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void openConnection() throws EquipmentControllerException {
        openConnection(0);
    }

    /**
     * Open connection on specific subrack
     *
     * @param subrack
     * @throws EquipmentControllerException
     */
    public void openConnection(int subrack) throws EquipmentControllerException {
        openNetconfConnection(subrack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean powerOff(EcsComputeBlade blade) throws EquipmentControllerException {
        boolean result = lockBlade(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(), blade.getBayNumber());
        waitForBladeAdminStatus(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(), blade.getBayNumber(),
                BladeAdminState.LOCKED);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean powerOn(EcsComputeBlade blade) throws EquipmentControllerException {
        boolean result = unlockBlade(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(), blade.getBayNumber());
        waitForBladeAdminStatus(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(), blade.getBayNumber(),
                BladeAdminState.UNLOCKED);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean powerReset(EcsComputeBlade blade) throws EquipmentControllerException {
        boolean result = resetBlade(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(),
                blade.getBayNumber());
        waitForBladeAdminStatus(BladeSoftwareGroup.Applications.toString(), blade.getBaySubrack(), blade.getBayNumber(),
                BladeAdminState.UNLOCKED);
        return result;
    }

    public void setEcsBladeBuilder(EcsBladeBuilder ecsBladeBuilder) {
        this.mEcsBladeBuilder = ecsBladeBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPortState(int bridgeInstance, int portInstance, PortAdminState state) {
        getNetconfSoftwareControl().setPortState(BridgeNumber.getValueForIndex(bridgeInstance),
                BridgePortNumber.getValueForIndex(portInstance), state.getDmxAdminStateEnum());
    }

    @Override
    public int showDefaultVlan(int bridgeInstance, int portInstance) {
        return getNetconfSoftwareControl().showDefaultVlan(BridgeNumber.getValueForIndex(bridgeInstance),
                BridgePortNumber.getValueForIndex(portInstance));
    }
}

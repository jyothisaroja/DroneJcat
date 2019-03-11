package com.jcat.cloud.fw.hwmanagement.switches.extremeswitch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.xml.rpc.ServiceException;

import org.apache.axis.types.UnsignedInt;
import org.apache.axis.types.UnsignedShort;

import se.ericsson.jcat.fw.assertion.JcatAssertApi;
import xapi.FdbFdbEntry;
import xapi.XosLocator;
import xapi.XosPortType;

import com.extremenetworks.www.XMLSchema.xos._switch.CreateRequest;
import com.extremenetworks.www.XMLSchema.xos._switch.CreateResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.DeleteRequest;
import com.extremenetworks.www.XMLSchema.xos._switch.DeleteResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.GetRequest;
import com.extremenetworks.www.XMLSchema.xos._switch.GetResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.SetRequest;
import com.extremenetworks.www.XMLSchema.xos._switch.SetResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.SwitchPortType;
import com.extremenetworks.www.XMLSchema.xos._switch._switchLocator;
import com.extremenetworks.www.XMLSchema.xos.common.ClientHeader;
import com.extremenetworks.www.XMLSchema.xos.common.CloseSessionReply;
import com.extremenetworks.www.XMLSchema.xos.common.CloseSessionRequest;
import com.extremenetworks.www.XMLSchema.xos.common.ExosBase;
import com.extremenetworks.www.XMLSchema.xos.common.IPAddress;
import com.extremenetworks.www.XMLSchema.xos.common.IPAddressNetMask;
import com.extremenetworks.www.XMLSchema.xos.common.OpenSessionReply;
import com.extremenetworks.www.XMLSchema.xos.common.OpenSessionRequest;
import com.extremenetworks.www.XMLSchema.xos.common.Session;
import com.extremenetworks.www.XMLSchema.xos.common.TrueFalse;
import com.extremenetworks.www.XMLSchema.xos.common.holders.ClientHeaderHolder;
import com.extremenetworks.www.XMLSchema.xos.port.AdminStates;
import com.extremenetworks.www.XMLSchema.xos.port.PortConfig;
import com.extremenetworks.www.XMLSchema.xos.system.SwitchInfo;
import com.extremenetworks.www.XMLSchema.xos.vlan.VirtualRouter;
import com.extremenetworks.www.XMLSchema.xos.vlan.VlanConfig;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.network.EcsPort;
import com.jcat.cloud.fw.components.model.network.EcsRouter;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResource;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup.ExtremeSwitchName;

/**
 * Extreme Switch library implementation. </br>
 * NOTE! This library implements basic requests towards a specific extreme switch device. In order to use
 * this library and get the information desired you must apply filtering. See examples on the class ExtremeExample
 * located on the jcat-telcodc-tests project.
 *
 * Extreme Switch reference API:
 * <a href="https://axe-jcat.rnd.ki.sw.ericsson.se/ecs/EXOS-15.1.2/doc/api/index.html">https://axe-jcat.rnd.ki.sw.
 * ericsson.se/ecs/EXOS-15.1.2/doc/api/index.html</a>
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-05-13 Initial version
 * @author ezhgyin 2014-05-16 enable ExtremeSwitchLib to select specific extreme switch resource
 * @author zdagjyo 2017-06-14 Added methods cleanup, createRouter, createVlans, deleteRouter,
 *         deleteVlans, doesRouterExist, getFdbEntries, getRouterVlanCount, getVlanId,
 *         getVlanState, rebootSwitch, sendCliCommand and waitForVrrpState
 * @author zdagjyo 2017-09-19 Modified methods configurePortsAdminState, disablePorts and enablePorts
 */
public class ExtremeSwitchLib implements IExtremeSwitchLib {

    /**
     * format of time info returned from SwitchInfo object
     */
    private static final String CURRENT_TIME_FORMAT = "MMM dd HH:mm:ss yyyy";
    /**
     * format of date info returned from SwitchInfo object
     */
    private static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss yyyy";
    /**
     * time between reading current time and reboot (in seconds)
     */
    private static final int REBOOT_DELAY = 3;
    /**
     * time format required when scheduling the reboot
     */
    private static final String REBOOT_TIME_FORMAT = "MM dd yyyy HH mm ss";
    /**
     * position in String, where date and time value starts (position of Aug in example below)
     * Thu Aug 14 13:31:56 2017
     */
    private static final int START_POSITION_DATETIME = 4;

    private static final ExtremeSwitchName DEFAULT_EXTREMESWITCH = ExtremeSwitchName.EXTREMESWITCH_1;

    /**
     * List to keep track of created routers
     */
    private final List<VirtualRouter> mSuccessfullyCreatedRouters = new CopyOnWriteArrayList<VirtualRouter>();

    /**
     * List to keep track of created vlans
     */
    private final List<VlanConfig> mSuccessfullyCreatedVlans = new CopyOnWriteArrayList<VlanConfig>();

    /**
     * user selected extreme switch resource
     */
    private ExtremeSwitchResource mExtremeSwitch;

    /**
     * Group of all the extreme switch resources
     */
    private final ExtremeSwitchResourceGroup mExtremeSwitchGroup;

    private final EcsLogger mLogger = EcsLogger.getLogger(ExtremeSwitchLib.class);

    /**
     * Contains the session with the extreme switch device.
     */
    private Session mSession;

    /**
     * Switch locator
     */
    private _switchLocator mSwitchLocator;

    /**
     * Constructor
     *
     * @param extremeSwitchGroup - ExtremeSwitchResourceGroup - all extreme switch resources of an STP
     */
    @Inject
    public ExtremeSwitchLib(ExtremeSwitchResourceGroup extremeSwitchGroup) {
        mExtremeSwitchGroup = extremeSwitchGroup;
        mExtremeSwitch = extremeSwitchGroup.getExtremeSwitchResource(DEFAULT_EXTREMESWITCH);
    }

    /**
     * Internal helper to create the header holder used in the requests
     *
     * @param mSession
     * @return ClientHeaderHolder
     */
    private ClientHeaderHolder createHeaderHolder() {
        ClientHeader header = new ClientHeader();
        header.setReqId(new UnsignedInt(1));
        header.setSessionId(mSession.getSessionId());
        return new ClientHeaderHolder(header);
    }

    /**
     * Getter of switch locator
     *
     * @return
     */
    private _switchLocator getLocator() {
        if (mSwitchLocator == null) {
            mSwitchLocator = new _switchLocator();
        }
        return mSwitchLocator;
    }

    /**
     * Getter session
     *
     * @param sessionName
     * @return
     */
    private Session getSession(String sessionName) {
        if (mSession == null) {
            mSession = new Session();
            mSession.setUsername(mExtremeSwitch.getUserName());
            mSession.setPassword(mExtremeSwitch.getPassword());
            mSession.setAppName(sessionName);
        }
        return mSession;
    }

    /**
     * Get object of type SwitchInfo (includes current time, SW version,...)
     *
     * @return SwitchInfo - information about switch
     */
    private SwitchInfo getSwitchInformation() {
        SwitchInfo switchInformation = new SwitchInfo();
        try {
            openSessionRequest("Getting Switch Info");
            GetResponse response = sendGetRequest(switchInformation);
            switchInformation = (SwitchInfo) response.getObjects().getObject(0);
            closeSessionRequest();
        } catch (MalformedURLException | ServiceException | RemoteException e) {
            mLogger.error("Could not complete session to switch", e);
        }
        return switchInformation;
    }

    /**
     * Constructs swtich soap interface url
     *
     * @return url
     */
    private String getSwitchUrl() {
        return "http://" + mExtremeSwitch.getIp() + ":" + mExtremeSwitch.getHttpPort() + "/xmlService";
    }

    /**
     * @param switchLocator
     */
    protected void setLocator(_switchLocator switchLocator) {
        mSwitchLocator = switchLocator;
    }

    /**
     * Setter for the session field
     *
     * @param session
     */
    protected void setSession(Session session) {
        mSession = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", "Extreme Switch", "");
        if (mSuccessfullyCreatedVlans != null) {
            mLogger.debug("Try to delete all created vlans.");
            try {
                deleteVlans(mSuccessfullyCreatedVlans);
            } catch (RemoteException e) {
                mLogger.warn("Failed to delete created vlans due to RemoteException");
            } catch (MalformedURLException e) {
                mLogger.warn("Failed to delete created vlans due to MalformedURLException");
            } catch (ServiceException e) {
                mLogger.warn("Failed to delete created vlans due to ServiceException");
            }
        }
        if (mSuccessfullyCreatedRouters != null) {
            mLogger.debug("Try to delete all created routers.");
            Iterator<VirtualRouter> iterator = mSuccessfullyCreatedRouters.iterator();
            while (iterator.hasNext()) {
                VirtualRouter router = iterator.next();
                try {
                    deleteRouter(router);
                } catch (RemoteException e) {
                    mLogger.warn("Failed to delete router " + router.getVrName() + " due to RemoteException");
                } catch (MalformedURLException e) {
                    mLogger.warn("Failed to delete router " + router.getVrName() + " due to MalformedURLException");
                } catch (ServiceException e) {
                    mLogger.warn("Failed to delete router " + router.getVrName() + " due to ServiceException");
                }
            }
        }
        mLogger.info(Verdict.DONE, "Clean up", "Extreme Switch", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloseSessionReply closeSessionRequest() throws RemoteException, MalformedURLException, ServiceException {
        mLogger.debug("Closing session: " + mSession.getAppName() + " and sessionID: " + mSession.getSessionId());
        CloseSessionRequest request = new CloseSessionRequest();
        request.setSessionId(mSession.getSessionId());
        // Close existing session
        setSession(null);
        return getSwitchPort().closeSession(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configurePortsAdminState(String portList, AdminStates adminState, String sessionName)
            throws RemoteException, MalformedURLException, ServiceException {
        openSessionRequest(sessionName);
        PortConfig filter = new PortConfig();
        filter.setPortList(portList);
        filter.setAdminState(adminState);
        sendSetRequest(filter);
        if (portList.contains("53")) {
            mLogger.warn("sendGetRequest doesn't work for port 53, hence returning");
            closeSessionRequest();
            return;
        }
        // double check that the port admin state has been successfully changed
        ExosBase[] items = sendGetRequest(filter).getObjects().getObject();
        for (ExosBase item : items) {
            PortConfig port = (PortConfig) item;
            JcatAssertApi.assertEquals(String.format("Port %s has wrong state after config", port.getPortList()),
                    adminState, port.getAdminState());
        }
        closeSessionRequest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VirtualRouter createRouter(String vrName) throws RemoteException, MalformedURLException, ServiceException {
        mLogger.info(EcsAction.CREATING, EcsRouter.class, vrName);
        VirtualRouter router = new VirtualRouter();
        router.setVrName(vrName);
        sendCreateRequest(router);
        if (!doesRouterExist(vrName)) {
            throw new EcsOpenStackException("Router " + vrName + " creation failed");
        }
        mSuccessfullyCreatedRouters.add(router);
        mLogger.info(Verdict.CREATED, EcsRouter.class, vrName);
        return router;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VlanConfig> createVlans(String vrName, int vlanCount, List<String> providerSegIdList,
            List<String> ipAddressList) throws RemoteException, MalformedURLException, ServiceException {
        List<VlanConfig> vlans = new ArrayList<VlanConfig>();
        String vlanName = null;
        for (int i = 0; i < vlanCount; i++) {
            vlanName = String.format("TEST_VLAN_%s", (i + 1));
            mLogger.info(EcsAction.CREATING, "", "vlan", vlanName + ", VR Name= " + vrName);
            VlanConfig filter = new VlanConfig();
            filter.setName(vlanName);
            filter.setVrName(vrName);
            UnsignedShort id = new UnsignedShort(providerSegIdList.get(i));
            filter.setTagValue(id);
            filter.setVlanTaggedPorts("1 2");
            IPAddressNetMask ipMask = new IPAddressNetMask();
            ipMask.setIpAddress(new IPAddress(ipAddressList.get(i)));
            filter.setPrimaryIpv4Address(ipMask);
            TrueFalse tf = TrueFalse.fromString("true");
            filter.setIpv4ForwardingEnabled(tf);
            sendCreateRequest(filter);
            vlans.add(filter);
            mSuccessfullyCreatedVlans.add(filter);
            mLogger.info(Verdict.CREATED, "", "vlan", vlanName + ", VR Name= " + vrName);
        }
        if (getRouterVlanCount(vrName) != vlanCount) {
            throw new EcsOpenStackException("Failed to create " + vlanCount + " vlans for the router " + vrName);
        }
        return vlans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRouter(VirtualRouter vr) throws RemoteException, MalformedURLException, ServiceException {
        mLogger.info(EcsAction.DELETING, EcsRouter.class, vr.getVrName());
        String command = "delete vr " + vr.getVrName();
        String result = sendCliCommand(command);
        if (result.contains("Error")) {
            mLogger.warn("Failed to delete router " + vr.getVrName());
        } else {
            mLogger.info(Verdict.DELETED, EcsRouter.class, vr.getVrName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVlans(List<VlanConfig> vlans) throws RemoteException, MalformedURLException, ServiceException {
        for (VlanConfig vlan : vlans) {
            mLogger.info(EcsAction.DELETING, "", "vlan", vlan.getName() + ", VR Name= " + vlan.getVrName());
            String command = "delete vlan " + vlan.getName();
            String result = sendCliCommand(command);
            if (result.contains("Invalid")) {
                mLogger.warn("Failed to delete vlan " + vlan.getName());
            } else {
                mLogger.info(Verdict.DELETED, "", "vlan", vlan.getName() + ", VR Name= " + vlan.getVrName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disablePorts(String portList) throws RemoteException, MalformedURLException, ServiceException {
        mLogger.info(EcsAction.DISABLING, EcsPort.class,
                portList + " on switch ip:" + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort());
        configurePortsAdminState(portList, AdminStates.Disabled, "disablePort");
        mLogger.info(Verdict.DISABLED, EcsPort.class,
                portList + " on switch ip:" + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesRouterExist(String vrName) throws RemoteException, MalformedURLException, ServiceException {
        for (VirtualRouter vr : getAllVrouters()) {
            if (vr.getVrName().equals(vrName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enablePorts(String portList) throws RemoteException, MalformedURLException, ServiceException {
        mLogger.info(EcsAction.ENABLING, EcsPort.class,
                portList + " on switch ip:" + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort());
        configurePortsAdminState(portList, AdminStates.Enabled, "enablePort");
        mLogger.info(Verdict.ENABLED, EcsPort.class,
                portList + " on switch ip:" + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VlanConfig> getAllVlans() throws RemoteException, MalformedURLException, ServiceException {
        openSessionRequest("getVlan");
        List<VlanConfig> vlanList = new ArrayList<VlanConfig>();
        GetResponse response = sendGetRequest(new VlanConfig());
        ExosBase[] objects = response.getObjects().getObject();
        for (ExosBase object : objects) {
            vlanList.add((VlanConfig) object);
        }
        closeSessionRequest();
        return vlanList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VirtualRouter> getAllVrouters() throws RemoteException, MalformedURLException, ServiceException {
        openSessionRequest("Get all virtual-routers");
        List<VirtualRouter> vRouterList = new ArrayList<VirtualRouter>();
        VirtualRouter filter = new VirtualRouter();
        // First get request
        GetResponse response = sendGetRequest(filter);
        // actual number of objects returned
        int numberOfObjectsReturned = response.getObjects().getObject().length;
        // Response is an array of ExosBase objects
        for (int i = 0; i < numberOfObjectsReturned; i++) {
            vRouterList.add((VirtualRouter) response.getObjects().getObject(i));
        }
        closeSessionRequest();
        return vRouterList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCurrentTimeOnSwitch() throws ParseException {
        Date currentTime = null;
        // "Mon Dec 4 05:47:56 2017";
        String time = getSwitchInformation().getCurrentTime();
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        currentTime = dateFormat.parse(time);
        return currentTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtremeSwitchResource getExtremeSwitchResource() {
        return mExtremeSwitch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FdbFdbEntry[] getFdbEntries() throws FileNotFoundException, IOException, ServiceException {
        return getXosPort().getAllFdb().getFdb();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminStates getPortAdminState(String portList) throws RemoteException, MalformedURLException,
            ServiceException {
        AdminStates result = null;
        openSessionRequest("GetPortInfo");
        PortConfig filter = new PortConfig();
        filter.setPortList(portList);
        ExosBase[] items = sendGetRequest(filter).getObjects().getObject();
        for (ExosBase item : items) {
            result = ((PortConfig) item).getAdminState();
        }
        closeSessionRequest();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRouterVlanCount(String vrName) throws RemoteException, MalformedURLException, ServiceException {
        int count = 0;
        for (VlanConfig vc : getAllVlans()) {
            if (vc.getVrName().equals(vrName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SwitchPortType getSwitchPort() throws MalformedURLException, ServiceException {
        return getLocator().getswitchPort(new URL(getSwitchUrl()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVlanId(String vlanName) throws RemoteException, MalformedURLException, ServiceException {
        UnsignedShort vid = null;
        List<VlanConfig> vlans = getAllVlans();
        for (VlanConfig vlan : vlans) {
            if (vlan.getName().equalsIgnoreCase(vlanName)) {
                vid = vlan.getTagValue();
            }
        }
        if (vid == null) {
            throw new EcsOpenStackException("Could not find VID for vlan " + vlanName);
        }
        return vid.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VlanState getVlanState(String vlanName) {
        String state = null;
        VlanState vlanState = null;
        String command = "show vrrp " + vlanName + " | include State:";
        String response = sendCliCommand(command);
        if (response != null) {
            state = response.substring(response.lastIndexOf(':') + 1).trim();
            try {
                vlanState = VlanState.valueOf(state);
            } catch (IllegalArgumentException e) {
                mLogger.error("No matching state " + e);
            }
        }
        return vlanState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XosPortType getXosPort() throws ServiceException, MalformedURLException {
        URL url = new URL(getSwitchUrl());
        XosPortType stub = new XosLocator().getXosPort(url);
        if (mExtremeSwitch.getUserName() != null) {
            ((org.apache.axis.client.Stub) stub).setUsername(mExtremeSwitch.getUserName());
        } else {
            throw new IllegalArgumentException("Extreme switch user name is missing");
        }

        if (mExtremeSwitch.getPassword() != null) {
            ((org.apache.axis.client.Stub) stub).setPassword(mExtremeSwitch.getPassword());
        } else {
            throw new IllegalArgumentException("Extreme switch password is missing");
        }

        return stub;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenSessionReply openSessionRequest(String sessionName) throws RemoteException, MalformedURLException,
            ServiceException {
        getSession(sessionName);
        OpenSessionRequest request = new OpenSessionRequest();
        request.setSession(mSession);
        OpenSessionReply reply = getSwitchPort().openSession(request);
        setSession(reply.getSession());
        mLogger.debug("Using session: " + mSession.getAppName() + " with sessionID: " + mSession.getSessionId());
        return reply;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rebootSwitch() throws ParseException {
        // get current time on switch
        String result = getSwitchInformation().getCurrentTime();
        // cut out time from answer printout string
        String dateTime = result.substring(START_POSITION_DATETIME).trim();
        mLogger.debug("Current Time on Switch: " + dateTime);
        // add 3 seconds and re-format time format as needed by reboot command
        SimpleDateFormat df = new SimpleDateFormat(CURRENT_TIME_FORMAT);
        Date d = df.parse(dateTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.add(Calendar.SECOND, REBOOT_DELAY);
        SimpleDateFormat newDf = new SimpleDateFormat(REBOOT_TIME_FORMAT);
        String newTime = newDf.format(cal.getTime());
        String rebootCommand = String.format("reboot time %s", newTime);
        // schedule reboot of switch
        // (rebooting extreme directly without scheduling would lead to hanging CLI session)
        mLogger.info(EcsAction.REBOOTING, "", "extreme switch",
                "ip:" + mExtremeSwitch.getIp() + ",http port:" + mExtremeSwitch.getHttpPort());
        sendCliCommand(rebootCommand);
        mLogger.info(Verdict.REBOOTED, "", "extreme switch",
                "ip:" + mExtremeSwitch.getIp() + ",http port:" + mExtremeSwitch.getHttpPort());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectExtremeSwitch(ExtremeSwitchName name) {
        mExtremeSwitch = mExtremeSwitchGroup.getExtremeSwitchResource(name);
        JcatAssertApi.assertNotNull("Selected extreme switch resource " + name.getName() + " does not exist.",
                mExtremeSwitch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String sendCliCommand(String command) {
        String response = null;
        try {
            openSessionRequest("Connect to switch");
            XosPortType switchPort = getXosPort();
            response = switchPort.execCli(command);
            closeSessionRequest();
        } catch (MalformedURLException | ServiceException | RemoteException e) {
            mLogger.error("Could not complete session to switch", e);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateResponse sendCreateRequest(ExosBase exosBase) throws RemoteException, MalformedURLException,
            ServiceException {
        mLogger.debug("Sending CREATE request");
        openSessionRequest("SendCreateRequest");
        CreateRequest request = new CreateRequest();
        request.setFilter(exosBase);
        return getSwitchPort().create(createHeaderHolder(), request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteResponse sendDeleteRequest(ExosBase exosBase) throws RemoteException, MalformedURLException,
            ServiceException {
        mLogger.debug("Sending DELETE request");
        DeleteRequest request = new DeleteRequest();
        request.setFilter(exosBase);
        return getSwitchPort().delete(createHeaderHolder(), request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetResponse sendGetRequest(ExosBase exosBase) throws MalformedURLException, ServiceException,
            RemoteException {
        mLogger.debug("Sending GET request");
        GetRequest request = new GetRequest();
        request.setFilter(exosBase);
        return getSwitchPort().get(createHeaderHolder(), request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SetResponse sendSetRequest(ExosBase exosBase) throws RemoteException, MalformedURLException,
            ServiceException {
        mLogger.debug("Sending SET request");
        SetRequest request = new SetRequest();
        request.setFilter(exosBase);
        return getSwitchPort().set(createHeaderHolder(), request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitForVrrpState(final VlanState desiredState, final String vlanName) {
        mLogger.info(EcsAction.STATUS_CHANGING, desiredState.toString(), EcsRouter.class, "on switch (ip:"
                + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort() + ") , Timeout:"
                + Timeout.EXTREME_RESTORE_MASTER_STATE.getTimeoutInSeconds() + "seconds");
        LoopHelper<VlanState> loopHelper = new LoopHelper<VlanState>(Timeout.EXTREME_RESTORE_MASTER_STATE,
                "Virtual Router did not reach State " + desiredState, desiredState, () -> {
                    VlanState currentState = getVlanState(vlanName);
                    mLogger.debug("Current VRRP state " + currentState);
                    return currentState;
                });
        loopHelper.setIterationDelay(30);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, desiredState.toString(), EcsRouter.class, "on switch (ip:"
                + mExtremeSwitch.getIp() + ", http port:" + mExtremeSwitch.getHttpPort() + ")");
    }
}

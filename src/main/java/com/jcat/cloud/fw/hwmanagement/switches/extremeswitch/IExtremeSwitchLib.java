package com.jcat.cloud.fw.hwmanagement.switches.extremeswitch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.xml.rpc.ServiceException;

import com.extremenetworks.www.XMLSchema.xos._switch.CreateResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.DeleteResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.GetResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.SetResponse;
import com.extremenetworks.www.XMLSchema.xos._switch.SwitchPortType;
import com.extremenetworks.www.XMLSchema.xos.common.CloseSessionReply;
import com.extremenetworks.www.XMLSchema.xos.common.ExosBase;
import com.extremenetworks.www.XMLSchema.xos.common.OpenSessionReply;
import com.extremenetworks.www.XMLSchema.xos.port.AdminStates;
import com.extremenetworks.www.XMLSchema.xos.vlan.VirtualRouter;
import com.extremenetworks.www.XMLSchema.xos.vlan.VlanConfig;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResource;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup.ExtremeSwitchName;

import xapi.FdbFdbEntry;
import xapi.XosPortType;

/**
 * Interface to handle the Extreme Switch device.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-05-13 Initial version
 * @author ezhgyin 2014-05-16 add method selectExtremeSwitch
 * @author zdagjyo 2017-06-14 Added methods cleanup, createRouter, createVlans, deleteRouter,
 *         deleteVlans, doesRouterExist, getFdbEntries, getRouterVlanCount, getVlanId,
 *         getVlanState, rebootSwitch, sendCliCommand and waitForVrrpState
 * @author zdagjyo 2017-06-14 Added method getCurrentTimeOnSwitch
 */
public interface IExtremeSwitchLib {

    /**
     * Vlan states.
     *
     */
    public enum VlanState {

        BACKUP("BACKUP"), MASTER("MASTER");

        private final String mState;

        /**
         * Initiates each enum.
         *
         * @param state - String - string value for enum.
         */
        private VlanState(String state) {
            mState = state;
        }

        /**
         * Get string value for enum.
         *
         * @return String - string value for enum.
         */
        public String getState() {
            return mState;
        }
    }

    /**
     * Clean up resources created in this extreme switch instance
     */
    void cleanup();

    /**
     * Close a webservice session on the switch.
     *
     * @return response with confirmation of close session
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    CloseSessionReply closeSessionRequest() throws RemoteException, MalformedURLException, ServiceException;

    void configurePortsAdminState(String portList, AdminStates adminState, String sessionName)
            throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Create virtual router
     * @param vrName - the name of the virtual router
     * @return VirtualRouter - the create virtual router
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    VirtualRouter createRouter(String vrName) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Create vlans for the specified virtual router
     * @param vrName - the name of the virtual router
     * @param vlanCount - the number of vlans to be created for the specified router
     * @param providerSegIdList - list of provider segmentation IDs of the networks corresponding to vlans being created
     * @param ipAddressList - list of IP addresses of the vlans to be created.
     *                        The IP address of vlan is based on the CIDR of its network
     *                        (For example, if the CIDR of a network is 10.10.0.0, then
     *                        the IP address of its vlan should be 10.10.0.1)
     * @return List<VlanConfig> - list of created vlans for the specified router
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    List<VlanConfig> createVlans(String vrName, int vlanCount, List<String> providerSegIdList,
            List<String> ipAddressList) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Delete the created router
     * @param vr - the router to be deleted
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    void deleteRouter(VirtualRouter vr) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Delete the created vlans
     * @param vlans - list of vlans to be deleted
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    void deleteVlans(List<VlanConfig> vlans) throws RemoteException, MalformedURLException, ServiceException;

    void disablePorts(String portList) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Check if the specified router exists
     * @param vrName - the name of the virtual router
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     * @return boolean
     */
    boolean doesRouterExist(String vrName) throws RemoteException, MalformedURLException, ServiceException;

    void enablePorts(String portList) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Fetch all VLANs.
     *
     * @return List of {@link VlanConfig} objects
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    List<VlanConfig> getAllVlans() throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Fetch all virtual routers defined in Extreme switch.
     *
     * @return List of {@link VirtualRouter} objects
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    List<VirtualRouter> getAllVrouters() throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Retrieve current time on extreme switch
     *
     * @return current time on switch
     * @throws ParseException
     */
    Date getCurrentTimeOnSwitch() throws ParseException;

    /**
     * Getter for the extreme device information. {@link ExtremeSwitchResource}
     *
     * @return ExtremeSwitchResource instance
     */
    ExtremeSwitchResource getExtremeSwitchResource();

    /**
     * Retrieve all FDB entries of extreme switch
     *
     * @return FdbFdbEntry[] - an array of FDB entries of extreme switch
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    FdbFdbEntry[] getFdbEntries() throws FileNotFoundException, IOException, ServiceException;

    AdminStates getPortAdminState(String portList) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Get the number of vlans added to the specified router
     *
     * @param vrName - the name of the virtual router
     * @return int - the number of vlans added to the router specified
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    int getRouterVlanCount(String vrName) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Get a handle to the webservice on the switch.
     *
     * @return handle to the webservice on the switch.
     *
     * @throws MalformedURLException
     * @throws ServiceException
     */
    SwitchPortType getSwitchPort() throws MalformedURLException, ServiceException;

    /**
     * get the id of vlan
     *
     * @param String - the name of the vlan
     * @return int - the id of vlan
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    int getVlanId(String vlanName) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Get the state for a VLAN in the Extreme switch.
     *
     * @param vlanName - String - VLAN name.
     * @return VlanState - state of the VLAN (or null if no matching state was found).
     */
    VlanState getVlanState(String vlanName);

    /**
     * Get a handle to the webservice with the XOS on the switch.
     *
     * @return handle to the webservice on the switch.
     * @throws ServiceException
     * @throws MalformedURLException
     */
    XosPortType getXosPort() throws ServiceException, MalformedURLException;

    /**
     * Open a webservice session on the webservice.
     *
     * @param sessionName
     *
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    OpenSessionReply openSessionRequest(String sessionName)
            throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Reboot the region switch
     * @throws ParseException
     */
    void rebootSwitch() throws ParseException;

    /**
     * Select specific extreme switch resource
     *
     * @param name - ExtremeSwitchName - name of the extreme switch resource
     */
    void selectExtremeSwitch(ExtremeSwitchName name);

    /**
     * Send a CLI command to the Extreme switch.
     *
     * @param command - String - command to send.
     * @return String - response from the Extreme switch.
     */
    String sendCliCommand(String command);

    /**
     * Send create request
     *
     * @param exosBase object for the create request
     *
     * @return response with data for the create request
     * @throws RemoteException
     * @throws ServiceException
     * @throws MalformedURLException
     */
    CreateResponse sendCreateRequest(ExosBase exosBase) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Send delete request
     *
     * @param exosBase object for the delete request
     *
     * @return response with data for the delete request
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    DeleteResponse sendDeleteRequest(ExosBase exosBase) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Send get request. This method will return all objects.
     *
     * @param exosBase filter for the get request
     *
     * @return response with data for the get request
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    GetResponse sendGetRequest(ExosBase exosBase) throws MalformedURLException, ServiceException, RemoteException;

    /**
     * Send set request
     *
     * @param exosBase object for the set request
     *
     * @return response with data for the get request
     * @throws ServiceException
     * @throws MalformedURLException
     * @throws RemoteException
     */
    SetResponse sendSetRequest(ExosBase exosBase) throws RemoteException, MalformedURLException, ServiceException;

    /**
     * Wait until the VRRP state of the switch for the specified VLAN has reached the desired state
     *
     * @param desiredState - VlanState - desired VRRP state (MASTER or BACKUP)
     * @param vlanName - String - name of VLAN
     */
    void waitForVrrpState(final VlanState desiredState, final String vlanName);

}

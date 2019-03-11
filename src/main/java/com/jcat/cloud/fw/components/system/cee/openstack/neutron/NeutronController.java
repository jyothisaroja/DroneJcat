package com.jcat.cloud.fw.components.system.cee.openstack.neutron;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ServerResponseException;
import org.openstack4j.api.networking.NetworkService;
import org.openstack4j.api.networking.NetworkingService;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.api.networking.RouterService;
import org.openstack4j.api.networking.SecurityGroupRuleService;
import org.openstack4j.api.networking.SecurityGroupService;
import org.openstack4j.api.networking.SubnetService;
import org.openstack4j.api.networking.ext.AgentService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Agent;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.NetSecurityGroupRuleBuilder;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.NeutronException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.network.EcsNetwork;
import com.jcat.cloud.fw.components.model.network.EcsPort;
import com.jcat.cloud.fw.components.model.network.EcsRouter;
import com.jcat.cloud.fw.components.model.network.EcsSubnet;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * Neutron Controller that contains major operations of Neutron APIs
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2014-10-22 initial version
 * @author ehosmol 2014-11-20 Added list, delete, create staticRoutes functions
 * @author ehosmol Add {@link #getDhcpActiveCicHostName()}
 * @author eedelk 2015-01-30 Added: getSubnetList, getSubnetCidr, check deletion level
 *         in createNetwork and createSubnet
 * @author epergat 2015-03-15 Added error checks to minimize risk of errors in test-cases.
 * @author ezhgyin 2015-03-20 Add agentList
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author eedsla 2015-05-05 Added method areAllNeutronAgentsAlive
 * @author eqinann 2015-06-01 Use new exception in creating router
 * @author eelimei 2015-06-12 Remove asserts and add waitForPortStatus
 * @author ehosmol 2015-06-16 Add loophelper for delete and wait functions
 * @author eelimei 2016-01-18 Add loophelper for agent-list if the rest request throws a ServerResponseException,
 *                 could be temporary disturbance if changes have been made in the system.
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2017-01-05 added methods addRouterInterface,getRouterById,getSubnetById and removeRouterInterface
 * @author zdagjyo 2017-02-08 Added methods verifyNeutronAgentsAlive, waitAndVerifyNeutronAgentsAlive and modified method agentList
 * @author zdagjyo 2017-04-17 Added method getNetworkIdOfPort
 * @author zdagjyo 2017-06-06 Added method getProviderSegID
 * @author zdagjyo 2017-11-15 Added methods getSubnetIdByName and updateSubnet
 */
public class NeutronController extends EcsComponent {

    /**
     * Maximum number of c in an IP address (a.b.c.d)
     */
    private static final int CIDR_C_MAX_NUMBER = 255;

    /**
     * CIDR_DEFAULT_PREFIX_SIZE (24) gives 256 host IP addresses in the defined subnetwork
     */
    private static final int CIDR_DEFAULT_PREFIX_SIZE = 24;
    private static final int CIDR_HIGHEST_VALUE = 32;
    // TODO : Update to support values between 0 and 32, see also CIDR_DEFAULT_PREFIX_SIZE
    private static final int CIDR_LOWEST_VALUE = 24;
    private static final String DEFAULT_NETWORK_PREFIX = "10.0.";

    /**
     * List to keep track of created networks
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedNetworkIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created ports
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedPortIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created routers
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedRouterIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created router interfaces
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<RouterInterface> mCreatedRouterInterfaces = new CopyOnWriteArrayList<RouterInterface>();

    /**
     * List to keep track of created subnets
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedSubnetIds = new CopyOnWriteArrayList<String>();

    /**
     * Logger instance
     */
    private final EcsLogger mLogger = EcsLogger.getLogger(NeutronController.class);

    private final Map<String, List<String>> mNetworkSubnetMap = new HashMap<String, List<String>>();

    private final OpenStack4jEcs mOpenStack4jEcs;

    private final Map<String, List<String>> mTrunkPortSubportMap = new HashMap<String, List<String>>();

    /**
     * List to keep track of created security groups
     * use CopyOnWriteArrayList due to concurrent access to the list(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedSecurityGroupIds = new CopyOnWriteArrayList<String>();

    /**
     * Injectable interface for Neutron Controller
     */
    @Inject
    private NeutronController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    private AgentService agentService() {
        return networkingService().agent();
    }

    private void deleteAnyPortInUseOnNetwork(String networkId) {
        List<? extends Port> portList = portService().list();
        mLogger.info(EcsAction.DELETING, "All", EcsPort.class, "Used in network: " + networkId);
        for (Port port : portList) {
            String portId = port.getId();
            Port portInfo = portService().get(portId);
            if (portInfo != null && networkId.equals(portInfo.getNetworkId())) {
                deletePort(portId);
            }
        }
        mLogger.info(Verdict.DELETED, "All", EcsPort.class, "Used in network: " + networkId);
    }

    private String getAvaliableCidrInNetwork(String networkId, int cidrPrefixSize) {
        List<? extends Subnet> subnetList = subnetService().list();
        boolean cidrAvailable = true;
        int newCNumber = 0;
        String networkPrefix = DEFAULT_NETWORK_PREFIX + newCNumber + ".";
        while (newCNumber <= CIDR_C_MAX_NUMBER) {
            cidrAvailable = true;
            for (Subnet subnet : subnetList) {
                if (subnet.getCidr().startsWith(networkPrefix)) {
                    // if the cidr already exists, increment C number to get a new CIDR
                    // and check if the new one already exists in the subnet list.
                    mLogger.debug("CIDR exists: " + networkPrefix + ".0");
                    cidrAvailable = false;
                    newCNumber++;
                    networkPrefix = DEFAULT_NETWORK_PREFIX + newCNumber + ".";
                    break;
                } else {
                    // continue to check if the new cidr already exists in the rest of the subnet list
                    continue;
                }
            }
            if (cidrAvailable) {
                break;
            }
        }
        if (!cidrAvailable) {
            throw new EcsOpenStackException("Can not find an avaliable cidr in network" + networkId);
        }
        String cidr = networkPrefix + "0/" + cidrPrefixSize;
        mLogger.debug("Get an available CIDR: " + cidr);
        return cidr;
    }

    private OSClientV3 getClient() {
        return mOpenStack4jEcs.getClient(Facing.PUBLIC);
    }

    /**
     * Returns the first ip-address in subnet for supplied ip-address/netmask written in CIDR notation
     *
     * @param cidr - String - Classless Inter-Domain Routing notation (e.g. "10.80.247.101/27")
     * @return String
     */
    private String getDefaultGatewayIp(String cidr) {
        SubnetUtils subnetUtil = new SubnetUtils(cidr);
        return subnetUtil.getInfo().getLowAddress();
    }

    /**
     * Returns the last ip-address before the broadcast address
     *
     * @param cidr - String - Classless Inter-Domain Routing notation (e.g. "10.80.247.101/27")
     * @return String
     */
    private String getSubnetEndIp(String cidr) {
        SubnetUtils subnetUtil = new SubnetUtils(cidr);
        return subnetUtil.getInfo().getHighAddress();
    }

    /**
     * Returns the first ip-address after the gateway IP
     *
     * @param cidr - String - Classless Inter-Domain Routing notation (e.g. "10.80.247.101/27")
     * @return String
     * @throws UnknownHostException
     */
    private String getSubnetStartIp(String cidr) throws UnknownHostException {
        String gatewayIp = getDefaultGatewayIp(cidr);
        Pattern pattern = Pattern.compile("(.+)(\\.)(\\d+)");
        Matcher matcher = pattern.matcher(gatewayIp);
        if (!matcher.find()) {
            throw new EcsOpenStackException("Could not get Subnet start IP from cidr " + cidr);
        }
        int newDNumber = Integer.parseInt(matcher.group(3)) + 1;
        return matcher.group(1) + matcher.group(2) + (newDNumber);
    }

    /**
     * Checks if cidr matches pattern "a.b.c.d/e"
     *
     * @param cidr
     * @return
     */
    private boolean isCidrValid(String cidr) {
        Pattern pattern = Pattern.compile("(\\d+)(\\.)(\\d+)(\\.)(\\d+)(\\.)(\\d+)/(\\d+)");
        Matcher matcher = pattern.matcher(cidr);
        return matcher.find();
    }

    private NetworkingService networkingService() {
        return ControllerUtil.checkRestServiceNotNull(getClient().networking(), NetworkingService.class);
    }

    private NetworkService networkService() {
        return networkingService().network();
    }

    private PortService portService() {
        return networkingService().port();
    }

    private SecurityGroupRuleService securityGroupRuleService() {
        return networkingService().securityrule();
    }

    private SecurityGroupService securityGroupService() {
        return networkingService().securitygroup();
    }

    /**
     * Local helper method which removes any associated trunksubports from CreatedPortIds list
     *
     * @param portId - ID of the parent port
     */
    private void removeSubnets(String networkId) {
        List<String> subnetList = mNetworkSubnetMap.get(networkId);
        if (null != subnetList) {
            mCreatedSubnetIds.removeAll(subnetList);
        }
    }

    private RouterService routerService() {
        return networkingService().router();
    }

    private SubnetService subnetService() {
        return networkingService().subnet();
    }

    /**
     * Local helper method which waits until the network was really deleted. Exception will thrown if the network was
     * still not deleted after Timeout.SERVER_DELETE period.
     *
     * @param networkId - ID of the network
     */
    private void waitForNetworkDeleted(final String networkId) {
        new LoopHelper<Boolean>(Timeout.NETWORK_DELETE,
                "Network with id " + networkId + " was still found after deletion", Boolean.TRUE,
                () -> null == networkService().get(networkId)).run();
    }

    /**
     * Wait until port is actually deleted
     *
     * @param portId
     */
    private void waitForPortDeleted(final String portId) {
        new LoopHelper<Boolean>(Timeout.PORT_DELETE, "Port with id " + portId + " was still found after deletion",
                Boolean.TRUE, () -> null == portService().get(portId)).run();
    }

    /**
     * Local helper method which waits until the port reaches desired status.
     * Exception will thrown if the port still has not reach the status or if reaches error status.
     *
     * @param networkId - ID of the port
     */
    private void waitForPortReady(final String portId, Timeout timeout) {
        // wait until the network is actually deleted
        LoopHelper<org.openstack4j.model.network.State> loopHelper = new LoopHelper<org.openstack4j.model.network.State>(
                timeout, "Port with id " + portId + " did not reach desired status: DOWN orACTIVE(UP)",
                org.openstack4j.model.network.State.ACTIVE, () -> {
                    if (portService().get(portId) == null) {
                        throw new EcsOpenStackException(
                                "Port with id: " + portId + "was not found in the system so cannot wait for status");
                    }
                    org.openstack4j.model.network.State state = portService().get(portId).getState();
                    if (state == org.openstack4j.model.network.State.DOWN) {
                        // This is a workaround because we can only give loophelper one desired state and
                        // appearently when you create a port it is sometimes DOWN and sometimes ACTIVE
                        return org.openstack4j.model.network.State.ACTIVE;
                    }
                    return state;
                });
        loopHelper.setErrorState(org.openstack4j.model.network.State.ERROR);
        loopHelper.run();
    }

    /**
     * Wait until router is actually deleted
     * @param routerId
     */
    private void waitForRouterDeleted(final String routerId) {
        new LoopHelper<Boolean>(Timeout.ROUTER_DELETE, "Router with id " + routerId + " was still found after deletion",
                Boolean.TRUE, () -> null == routerService().get(routerId)).run();
    }

    /**
     * Wait until router goes to active state
     * @param routerId
     */
    private void waitForRouterToBecomeActive(final String routerId) {
        LoopHelper<org.openstack4j.model.network.State> loopHelper = new LoopHelper<org.openstack4j.model.network.State>(
                Timeout.ROUTER_CREATE, "Router with id " + routerId + " did not reach desired status : ACTIVE",
                org.openstack4j.model.network.State.ACTIVE, () -> routerService().get(routerId).getStatus());
        loopHelper.setErrorState(org.openstack4j.model.network.State.ERROR);
        loopHelper.run();
    }

    /**
     * Wait until subnet is actually deleted
     * @param subnetId
     */
    private void waitForSubnetDeleted(final String subnetId) {
        new LoopHelper<Boolean>(Timeout.SUBNET_DELETE, "Subnet with id " + subnetId + " was still found after deletion",
                Boolean.TRUE, () -> null == routerService().get(subnetId)).run();
    }

    /**
     * Add router interface to a subnet
     *
     * @param routerId - ID of the router
     * @param subnetId - ID of the subnet
     *
     * @return id of the router interface
     */
    public String addRouterInterface(String routerId, String subnetId) {
        mLogger.info(EcsAction.CREATING, "", "Router Interface", "for subnet " + getSubnetById(subnetId).getName());
        RouterInterface routerInterface = routerService().attachInterface(routerId, AttachInterfaceType.SUBNET,
                subnetId);
        if (routerInterface == null) {
            throw new NeutronException(
                    "Router interface was not created for the subnet: " + getSubnetById(subnetId).getName());
        }
        mLogger.info(Verdict.CREATED, "", "Router Interface",
                "for subnet " + getSubnetById(subnetId).getName() + ", id= " + routerInterface.getId());
        mCreatedRouterInterfaces.add(routerInterface);
        return routerInterface.getId();
    }

    /**
     * List neutron agents
     *
     * @return List of neutron agents
     */
    public List<EcsAgent> agentList() {
        List<? extends Agent> neutronAgents = agentService().list();
        List<EcsAgent> ecsNeutronAgents = new ArrayList<EcsAgent>();
        for (Agent agent : neutronAgents) {
            ecsNeutronAgents.add(new EcsAgent(agent));
        }
        return ecsNeutronAgents;
    }

    /**
     * Check if all neutron agents are alive
     *
     * @return boolean - true: all agents are alive
     */
    public boolean areAllNeutronAgentsAlive() {

        boolean allAgentsAlive = true;
        for (EcsAgent neutronAgent : agentList()) {

            if (!(neutronAgent.getAlive())) {
                mLogger.warn("Neutron agent " + neutronAgent.getAgentType() + " on host " + neutronAgent.getHost()
                        + " is not alive !");
                allAgentsAlive = false;
            }

        }
        return allAgentsAlive;
    }

    /**
     * Cleans up method, deletes whatever resource created by this controller instance.
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", NeutronController.class, "");
        for (String portId : mCreatedPortIds) {
            try {
                deletePort(portId);
            } catch (Exception ex) {
                mLogger.debug(String.format("Failed to delete port(id:%s), exception was: %s", portId, ex));
            }
        }

        for (RouterInterface routerInterface : mCreatedRouterInterfaces) {
            for (String subnetId : mCreatedSubnetIds) {
                if (routerInterface.getSubnetId().equals(subnetId)) {
                    try {
                        removeRouterInterface(routerInterface.getId(), subnetId);
                    } catch (Exception ex) {
                        mLogger.debug(String.format("Failed to remove router interface(id:%s), exception was: %s",
                                routerInterface.getId(), ex));
                    }
                }
            }
        }

        for (String routerId : mCreatedRouterIds) {
            try {
                deleteRouter(routerId);
            } catch (Exception ex) {
                mLogger.debug(String.format("Failed to delete router(id:%s), exception was: %s", routerId, ex));
            }
        }

        if (mCreatedSecurityGroupIds != null) {
            mLogger.debug("Try to delete all created security groups.");
            Iterator<String> iterator = mCreatedSecurityGroupIds.iterator();
            while (iterator.hasNext()) {
                String groupId = iterator.next();
                try {
                    deleteSecurityGroup(groupId);
                } catch (Exception e) {
                    mLogger.error(String.format("Failed to delete server group(id:%s), exception was: %s", groupId, e));
                }
            }
        }

        for (String subnetId : mCreatedSubnetIds) {
            try {
                deleteSubnet(subnetId);
            } catch (Exception ex) {
                mLogger.debug(String.format("Failed to delete subnet(id:%s), exception was: %s", subnetId, ex));
            }
        }

        for (String networkId : mCreatedNetworkIds) {
            try {
                deleteNetwork(networkId);
            } catch (Exception ex) {
                mLogger.debug(String.format("Failed to delete network(id:%s), exception was: %s", networkId, ex));
            }
        }
        mLogger.info(Verdict.DONE, "Clean up", NeutronController.class, "");
    }

    /**
     * Create a network and wait until it reaches active state
     *
     * @param ecsNetwork - EcsNetwork
     * @return String - ID of the created network
     */
    public String createNetwork(EcsNetwork ecsNetwork) {
        String networkName = ecsNetwork.getName();
        mLogger.info(EcsAction.CREATING, "", EcsNetwork.class, networkName);
        Network createdNetwork = networkService().create(ecsNetwork.get());
        if (createdNetwork == null) {
            throw new NeutronException("Network could not be created : " + networkName);
        }
        String networkId = createdNetwork.getId();
        Network network = networkService().get(networkId);

        if (network == null) {
            throw new NeutronException("Network was not created : " + networkName);
        }

        if (ecsNetwork.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedNetworkIds.add(networkId);
        }
        mLogger.info(Verdict.CREATED, EcsNetwork.class, networkName + ", id=" + networkId);
        if (!network.isAdminStateUp()) {
            throw new NeutronException("Network admin state is not up: " + networkName);
        }
        return networkId;
    }

    /**
     * Create a port in a network
     *
     * @param ecsPort - EcsPort - ecsPort to be created
     * @return String - ID of the created port
     */
    public String createPort(EcsPort ecsPort) {
        String portName = ecsPort.getName();
        mLogger.info(EcsAction.CREATING, EcsPort.class, portName);
        Port createdPort = portService().create(ecsPort.get());

        if (createdPort == null) {
            throw new NeutronException("Port could not be created :" + portName);
        }
        String portId = createdPort.getId();
        Port port = portService().get(portId);
        if (port == null) {
            throw new NeutronException("Port was not created :" + portName);
        }
        mCreatedPortIds.add(portId);
        waitForPortReady(portId, Timeout.PORT_READY);
        mLogger.info(Verdict.CREATED, EcsPort.class, portName + ", id=" + portId);
        return portId;
    }

    /**
     * Create a virtual router. Exceptions will be thrown if the router was not successfully
     * created.
     *
     * @param ecsRouter - EcsRouter - The router to be created
     * @return Router ID of the created router
     */
    public String createRouter(EcsRouter ecsRouter) {
        String routerName = ecsRouter.getName();
        String externalGatewayNetworkId = ecsRouter.getNetworkIdOfExternalGateway();
        if (externalGatewayNetworkId != null) {
            if (!getNetwork(externalGatewayNetworkId).isRouterExternal()) {
                throw new EcsOpenStackException(
                        "Network provided in external gateway for creating router must be an external network!",
                        Network.class);
            }
        }
        mLogger.info(EcsAction.CREATING, EcsRouter.class,
                routerName + ", ExternalGatewayNetworkId= " + externalGatewayNetworkId);
        Router createdRouter = routerService().create(ecsRouter.get());
        if (createdRouter == null) {
            throw new NeutronException("Router could not be created : " + routerName);
        }
        String routerId = createdRouter.getId();
        Router router = routerService().get(routerId);
        if (router == null) {
            throw new NeutronException("Router was not created : " + routerName, router);
        }
        waitForRouterToBecomeActive(routerId);
        mCreatedRouterIds.add(routerId);
        mLogger.info(Verdict.CREATED, EcsRouter.class,
                routerName + ", id= " + routerId + " in tenant, id= " + ecsRouter.getTenantId());
        return routerId;
    }

    /**
     * Create a security group
     *
     * @param name - the name of the security group
     * @param description - the description for security group
     * @return String - ID of the created security group
     */
    public String createSecurityGroup(String name, String description) {
        mLogger.info(EcsAction.CREATING, "", "Security Group", name);
        SecurityGroup groupToCreate = Builders.securityGroup().name(name).build();
        SecurityGroup createdGroup = securityGroupService().create(groupToCreate);
        if (null == createdGroup) {
            throw new EcsOpenStackException("Security group creation failed: " + name);
        }
        String securityGroupId = createdGroup.getId();
        mCreatedSecurityGroupIds.add(securityGroupId);
        mLogger.info(Verdict.CREATED, "", "Security Group", name + ",id:" + securityGroupId);
        return securityGroupId;
    }

    /**
     * Create security group rule and associate to existed security group.
     *
     * @param securityGroupId - Security Group Id, to which rule is going to be created
     * @param protocol - name of the protocol for security group
     * @param direction - direction of rule [ingress / egress]
     * @param remoteIp - remoteIp for security group
     * @param etherType - IPv4 or IPv6
     */
    public void createSecurityGroupRule(String securityGroupId, String protocol, String direction, String remoteIp, String etherType) {
        NetSecurityGroupRuleBuilder ruleBuilder = Builders.securityGroupRule().remoteIpPrefix(remoteIp)
                .direction(direction).protocol(protocol).ethertype(etherType).securityGroupId(securityGroupId);
        SecurityGroupRule ruleToCreate = ruleBuilder.build();
        SecurityGroupRule createdRule = securityGroupRuleService().create(ruleToCreate);
        if (createdRule == null) {
            throw new EcsOpenStackException("Security group rule creation failed for group:" + securityGroupId + " with protocol: " + protocol);
        }
    }


    /**
     * Create a subnet.
     *
     * @param ecsSubnet - EcsSubnet
     * @return String - ID of the created subnet
     * @throws UnknownHostException
     */
    public String createSubnet(EcsSubnet ecsSubnet) throws UnknownHostException {
        String subnetName = ecsSubnet.getName();
        String cidr = ecsSubnet.getCidr();
        if (cidr == null) {
            int cidrPrefixSize = CIDR_DEFAULT_PREFIX_SIZE;
            if (ecsSubnet.getCidrPrefixSize() != 0) {
                if (CIDR_LOWEST_VALUE <= cidrPrefixSize && cidrPrefixSize < CIDR_HIGHEST_VALUE) {
                    cidrPrefixSize = ecsSubnet.getCidrPrefixSize();
                } else {
                    throw new NeutronException(
                            String.format("Wrong CIDR value is provided, the value should be within range [%s, %s).",
                                    CIDR_LOWEST_VALUE, CIDR_HIGHEST_VALUE));
                }
            }
            cidr = getAvaliableCidrInNetwork(ecsSubnet.getNetworkId(), cidrPrefixSize);
        } else {
            if (!isCidrValid(cidr)) {
                throw new EcsOpenStackException("Cidr is not valid!");
            }
        }

        /*
         * a.b.c.0 is network address a.b.c.1 is gateway IP address a.b.c.lastIp
         * is broadcast IP address
         * The number of addresses of a subnetwork defined by the mask or prefix
         * can be calculated as 2 sqr(address size - prefix size), in which the
         * address size is 128 for IPv6 and 32 for IPv4. For example, in IPv4, a
         * prefix size of /29 gives: 2 sqr(32-29) = 8 addresses.
         */
        // current implementation does not allow user to specify gateway ip and allocation pool, can be added later.
        Subnet subnetToCreate = ecsSubnet.toBuilder().gateway(getDefaultGatewayIp(cidr))
                .addPool(getSubnetStartIp(cidr), getSubnetEndIp(cidr)).cidr(cidr).build().get();
        mLogger.info(EcsAction.CREATING, "", EcsSubnet.class, subnetName + ", CIDR=" + cidr);
        Subnet createdSubnet = subnetService().create(subnetToCreate);
        String subnetId = createdSubnet.getId();
        String networkId = ecsSubnet.getNetworkId();
        List<String> subnetList = mNetworkSubnetMap.get(networkId);
        if (null == subnetList) {
            subnetList = new ArrayList<String>();
        }
        subnetList.add(subnetId);
        mNetworkSubnetMap.put(networkId, subnetList);
        if (ecsSubnet.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedSubnetIds.add(subnetId);
        }
        mLogger.info(Verdict.CREATED, EcsSubnet.class, subnetName + ", id=" + subnetId);
        return subnetId;
    }

    /**
     * Delete a network. Exception will be thrown if the network was not deleted. Deletes any port in use on network.
     *
     * @param networkId - String - ID of the network
     */
    public void deleteNetwork(String networkId) {
        if (networkService().get(networkId) == null) {
            mLogger.warn(String.format("Can not delete network with id %s because it does not exist.", networkId));
        } else {
            mLogger.info(EcsAction.DELETING, EcsNetwork.class, networkId);
            deleteAnyPortInUseOnNetwork(networkId);
            networkService().delete(networkId);
            waitForNetworkDeleted(networkId);
            removeSubnets(networkId);
            mCreatedNetworkIds.remove(networkId);
            mLogger.info(Verdict.DELETED, EcsNetwork.class, networkId);
        }
    }

    /**
     * Deletes specified port
     *
     * @param portId
     */
    public void deletePort(String portId) {
        if (portService().get(portId) == null) {
            mLogger.warn(String.format("Can not delete port with id %s because it does not exist.", portId));
        } else {
            mLogger.info(EcsAction.DELETING, EcsPort.class, portId);
            portService().delete(portId);
            waitForPortDeleted(portId);
            mCreatedPortIds.remove(portId);
            mLogger.info(Verdict.DELETED, EcsPort.class, portId);
        }
    }

    /**
     * Delete a router. Exception will be thrown if the router was not deleted.
     *
     * @param routerId - String - ID of the router
     */
    public void deleteRouter(String routerId) {
        if (routerService().get(routerId) == null) {
            mLogger.warn(String.format("Can not delete router with id %s because it does not exist.", routerId));
        } else {
            mLogger.info(EcsAction.DELETING, EcsRouter.class, routerId);
            routerService().delete(routerId);
            waitForRouterDeleted(routerId);
            mCreatedRouterIds.remove(routerId);
            mLogger.info(Verdict.DELETED, EcsRouter.class, routerId);
        }
    }

    /**
     * Delete the specified security group.
     *
     * @param groupId - String - id of the security group
     */
    public void deleteSecurityGroup(String groupId) {
        mLogger.info(EcsAction.DELETING, "", "Security Group", groupId);
        ActionResponse response = securityGroupService().delete(groupId);
        if (response.isSuccess()) {
            mLogger.info(Verdict.DELETED, "", "Security Group", groupId);
        } else {
            mLogger.warn("failed to delete security group " + groupId);
        }
        mCreatedSecurityGroupIds.remove(groupId);
    }

    /**
     * Delete a subnet. Exception will be thrown if the subnet was not deleted.
     *
     * @param subnetId - String - ID of the subnet
     */
    public void deleteSubnet(String subnetId) {
        if (subnetService().get(subnetId) == null) {
            mLogger.warn(String.format("Can not delete subnet with id %s because it does not exist.", subnetId));
        } else {
            mLogger.info(EcsAction.DELETING, EcsSubnet.class, subnetId);
            deleteSubnetPorts(subnetId);
            subnetService().delete(subnetId);
            waitForSubnetDeleted(subnetId);
            mCreatedSubnetIds.remove(subnetId);
            mLogger.info(Verdict.DELETED, EcsSubnet.class, subnetId);
        }
    }

    /**
     * Delete ports on specified subnet, if any. Exception will be thrown if any port belonging to that subnet could not
     * be deleted.
     *
     * @param subnetId - String - ID of the subnet
     */
    public void deleteSubnetPorts(String subnetId) {
        mLogger.info(EcsAction.DELETING, "All", EcsPort.class, "Used in subnet: " + subnetId);
        List<? extends Port> ports = portService().list();
        for (Port port : ports) {
            Set<? extends IP> fixedIps = port.getFixedIps();
            for (IP ip : fixedIps) {
                if (ip.getSubnetId().equals(subnetId)) {
                    deletePort(port.getId());
                }
            }
        }
        mLogger.info(Verdict.DELETED, "All", EcsPort.class, "Used in subnet: " + subnetId);
    }

    /**
     * Get detailed info about a network
     *
     * @param networkId - String - id of the network
     * @return Network, detailed info about the network, null if the network was not found
     */
    public Network getNetwork(String networkId) {
        return networkService().get(networkId);
    }

    /**
     * Get the id of the network with given name. Null will be returned if no network was found with specified name.
     * Throws an exception if networks with the same name was found due to ambiguity.
     *
     * @param networkName - String - name of the network
     * @return - String - id of the network, null if not found
     */
    public String getNetworkIdByName(String networkName) {
        int nrOfNetworksFound = 0;
        String networkId = null;
        List<? extends Network> networkList = networkService().list();
        for (Network network : networkList) {
            if (networkName.equals(network.getName())) {
                nrOfNetworksFound++;
                networkId = network.getId();
            }
        }

        if (nrOfNetworksFound > 1) {
            throw new EcsOpenStackException("Found duplicate networks with the same name. This is not allowed.");
        }
        return networkId;
    }

    /**
     * Get the network id of the port specified by id.
     *
     * @param portId - String - id of the port
     * @return - String - id of the port's network, null if not found
     */
    public String getNetworkIdOfPort(String portId) {
        return portService().get(portId).getNetworkId();
    }

    /**
     * Get the name of the network with given id. Null will be returned if no network was found with specified id.
     *
     * @param networkId - String - id of the network
     * @return - String - name of the network, null if not found
     */
    public String getNetworkNameById(String networkId) {
        int nrOfNetworksFound = 0;
        String networkName = null;
        List<? extends Network> networkList = networkService().list();
        for (Network network : networkList) {
            if (networkId.equals(network.getId())) {
                nrOfNetworksFound++;
                networkName = network.getName();
            }
        }

        if (nrOfNetworksFound > 1) {
            throw new EcsOpenStackException(
                    "There was multiple networks found with the same ID (" + networkId + "). This is not allowed");
        }
        return networkName;
    }

    /**
     * Gets the provider segmentation id for the specified network.
     *
     * @param networkId - String - id of the network
     * @return String - the provider segmentation id for the specified network
     */
    public String getProviderSegID(String networkId) {
        return networkService().get(networkId).getProviderSegID();
    }

    /**
     * Get the router object with given id. Null will be returned if no router was found with specified id.
     *
     * @param routerId - String - id of the router
     * @return - Router - router object, null if not found
     */
    public Router getRouterById(String routerId) {
        return routerService().get(routerId);
    }

    /**
     * Get detailed info about a subnet
     *
     * @param subnetId - String - id of the subnet
     * @return Subnet, detailed info about the subnet, null if the subnet was not found
     */
    public Subnet getSubnetById(String subnetId) {
        return subnetService().get(subnetId);
    }

    /**
     * Get the cidr of the subnet with given id. Null will be returned if no subnet
     * was found with specified id.
     *
     * @param subnetId - String - id of the subnet
     * @return - String - cidr of the subnet
     */
    public String getSubnetCidr(String subnetId) {
        Subnet subnet = subnetService().get(subnetId);
        return subnet.getCidr();
    }

    /**
     * Get the id of the subnet with given name. Null will be returned if no subnet was found with specified name.
     * Throws an exception if subnets with the same name was found due to ambiguity.
     *
     * @param subnetName - String - name of the subnet
     * @return - String - id of the subnet, null if not found
     */
    public String getSubnetIdByName(String subnetName) {
        int nrOfSubnetsFound = 0;
        String subnetId = null;
        List<? extends Subnet> subnetList = subnetService().list();
        for (Subnet subnet : subnetList) {
            if (subnetName.equals(subnet.getName())) {
                nrOfSubnetsFound++;
                subnetId = subnet.getId();
            }
        }
        if (nrOfSubnetsFound > 1) {
            throw new EcsOpenStackException("Found duplicate subnets with the same name. This is not allowed.");
        }
        return subnetId;
    }

    /**
     * Get a list of subnets of a network with given id.
     * Null will be returned if no subnet was found for the given network id.
     *
     * @param networkId - String - id of the network
     * @return - List<String> - list of subnet ids, null if no subnet was found
     */
    public List<String> getSubnetList(String networkId) {
        return networkService().get(networkId).getSubnets();
    }

    public List<String> listNetworks() {
        List<String> networks = new ArrayList<String>();
        for (Network network : networkingService().network().list()) {
            networks.add(network.getId());
        }
        return networks;
    }

    public List<String> listPorts() {
        List<String> ports = new ArrayList<String>();
        for (Port port : networkingService().port().list()) {
            ports.add(port.getId());
        }
        return ports;
    }

    public Map<String, String> listRouters() {
        Map<String, String> routers = new HashMap<>();
        for (Router router : networkingService().router().list()) {
            routers.put(router.getName(), router.getId());
        }
        return routers;
    }

    public List<String> listSubnets() {
        List<String> subnets = new ArrayList<String>();
        for (Subnet subnet : networkingService().subnet().list()) {
            subnets.add(subnet.getId());
        }
        return subnets;
    }

    /**
     * Remove router interface added to a subnet
     *
     * @param routerId - ID of the router
     * @param subnetId - ID of the subnet
     *
     * @return id of the router interface
     */
    public String removeRouterInterface(String routerId, String subnetId) {
        mLogger.info(EcsAction.DELETING, "", "Router Interface", "for subnet " + getSubnetById(subnetId).getName());
        RouterInterface routerInterface = routerService().detachInterface(routerId, subnetId, null);
        if (routerInterface == null) {
            throw new NeutronException(
                    "Router interface was not removed for the subnet: " + getSubnetById(subnetId).getName());
        }
        mCreatedRouterInterfaces.remove(routerInterface);
        mLogger.info(Verdict.DELETED, "", "Router Interface",
                "for subnet " + getSubnetById(subnetId).getName() + ", id= " + routerInterface.getId());
        return routerInterface.getId();
    }

    /**
     * Updates the specified subnet with specified gatewayIp and enableDhcp options.
     *
     * @param subnetId - the id of the subnet to be updated
     * @param gatewayIp - the gateway ip of the subnet
     * @param enableDhcp - boolean - true if DHCP is to enabled for the subnet, else false
     * @return Subnet - the updated subnet
     */
    public Subnet updateSubnet(String subnetId, String gatewayIp, boolean enableDhcp) {
        mLogger.info(EcsAction.UPDATING, EcsSubnet.class, subnetId);
        Subnet subnet = subnetService().get(subnetId).toBuilder().gateway(gatewayIp).enableDHCP(true).build();
        Subnet updatedSubnet = subnetService().update(subnet);
        mLogger.info(Verdict.UPDATED, EcsSubnet.class, subnetId);
        return updatedSubnet;
    }

    /**
     * Verifies that the neutron agents are alive
     */
    public void verifyNeutronAgentsAlive() {
        if (!areAllNeutronAgentsAlive()) {
            LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.NEUTRON_READY,
                    "could not verify that neutron agents are alive", Boolean.TRUE, () -> {
                        return areAllNeutronAgentsAlive();
                    });
            loopHelper.setIterationDelay(10);
            loopHelper.run();
        }
    }

    /**
     * Waits for the neutron service to be available and verifies that the neutron agents are all alive
     */
    public void waitAndVerifyNeutronAgentsAlive() {
        new LoopHelper<Boolean>(Timeout.NEUTRON_READY,
                "Was not able to complete request neutron agent list via rest api due to service unavailability",
                Boolean.TRUE, () -> {
                    try {
                        agentService().list();
                        return true;
                    } catch (ServerResponseException e) {
                        return false;
                    }
                }).run();
        verifyNeutronAgentsAlive();
    }
}

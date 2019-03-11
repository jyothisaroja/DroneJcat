package com.jcat.cloud.fw.components.system.cee.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsComputeBladeException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.EcsComputeBladeFactory;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSessionFactory;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelNode;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelNode.NodeRole;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;

/**
 * Class that represents all the compute blades in the list, there is functionality for retreiving
 * all compute blades, individual compute blades and more.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat - 2014-12-15 - Initial version
 * @author zdagjyo - 2017-02-07 - Add constructor for EcsComputeBladeList, modify the way EcsComputeBlade is created using EcsComputeBladeFactory
 * @author zdagjyo - 2017-02-09 - Added methods getComputeHostDomainName,getComputeHostFullName and modified method initializeComputeBlades
 * @author zdagjyo - 2017-02-17 - Added method isComputeTimeSynchronized
 * @author zdagjyo - 2018-01-09 - Added method getBladesHostingCic
 **/
public class EcsComputeBladeList extends EcsComponent {

    private String mComputeDomain;
    private final Map<String, String> mComputeBladeIpsByHostName = new HashMap<String, String>();
    private final List<EcsComputeBlade> mCreatedBladeInstancesForUser = new ArrayList<EcsComputeBlade>();
    @Inject
    private EcsComputeBladeFactory mComputeBladeFactory;
    @Inject
    private ComputeBladeSessionFactory mComputeBladeSessionFactory;
    @Inject
    private FuelResource mFuelResource;
    @Inject
    private EcsFuel mFuel;

    private boolean mInitialized = false;

    private final EcsLogger mLogger = EcsLogger.getLogger(EcsComputeBladeList.class);

    /**
     * Method to get the full host name of a compute blade
     *
     * @param fuelNode - The node(compute host) whose host name is to be found.
     * @return String - The full host name of the specified compute node.
     */
    private String getComputeHostFullName(FuelNode fuelNode) {
        String hostName = null;
        if (fuelNode.getRoles().contains(FuelNode.NodeRole.COMPUTE)) {
            hostName = mComputeBladeSessionFactory
                    .create(fuelNode.getAddress(), mFuelResource.getIpPublic(), mFuelResource.getFuelPublicSshPort())
                    .getHostname();
        } else {
            mLogger.error("Not a valid compute host");
        }
        return hostName;
    }

    private void initializeComputeBlades() {
        if (!mInitialized) {
            // TODO: Uncomment these code when FuelLib functions normal
            // try {
            // for (FuelNode fuelNode : mFuelLib.getNodes()) {
            getComputeHostDomainName();
            for (FuelNode fuelNode : getNodes()) {
                if (fuelNode.getRoles().contains(FuelNode.NodeRole.COMPUTE)) {
                    String address = fuelNode.getAddress();
                    String hostName = getComputeHostFullName(fuelNode);
                    mComputeBladeIpsByHostName.put(hostName, address);
                }
            }
            // } catch (IOException | JSONException | FuelAuthorizationException e) {
            // Assert.fail("Could not instantiate computeblades with info from fuel/n" + e.getMessage());
            // }
            mInitialized = true;
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.jcat.ecs.lib.controllers.EcsComponent#initialize()
     */
    @Override
    public Boolean deinitialize() {
        mLogger.info(EcsAction.STARTING, "Clean up", EcsComputeBladeList.class, "");
        for (EcsComputeBlade blade : mCreatedBladeInstancesForUser) {
            blade.deinitialize();
        }
        mLogger.info(Verdict.DONE, "Clean up", EcsComputeBladeList.class, "");
        return true;
    }

    /**
     * returns a list of all compute blades
     *
     * @return
     */
    public List<EcsComputeBlade> getAllComputeBlades() {
        initializeComputeBlades();
        List<EcsComputeBlade> bladeList = new ArrayList<EcsComputeBlade>();
        for (String hostName : mComputeBladeIpsByHostName.keySet()) {
            String ipAddress = mComputeBladeIpsByHostName.get(hostName);
            EcsComputeBlade personalBladeInstanceForUser = mComputeBladeFactory.create(hostName, ipAddress,
                    mFuelResource.getIpPublic(), mFuelResource.getFuelPublicSshPort());
            mCreatedBladeInstancesForUser.add(personalBladeInstanceForUser);
            bladeList.add(personalBladeInstanceForUser);
        }
        return bladeList;
    }

    /**
     * Returns the names of all blades hosting vCics.
     *
     * @return a list of names of blades that host vCics
     */
    public List<String> getBladesHostingCic() {
        List<String> bladesWithCic = new ArrayList<String>();
        for (EcsComputeBlade computeBlade : getAllComputeBlades()) {
            if (computeBlade.doesAnyCicExist()) {
                bladesWithCic.add(computeBlade.getHostname());
            }
        }
        return bladesWithCic;
    }

    /**
     *
     * @param hostName of compute blade, must be in format of "XXXX.domain.tld"
     * @return EcsBlade object for this blade host name or throws exception if not found
     */
    public EcsComputeBlade getComputeBlade(String hostName) {
        initializeComputeBlades();
        String ipAddress = mComputeBladeIpsByHostName.get(hostName);
        if (ipAddress == null) {
            throw new RuntimeException("The requested Compute Blade with name '" + hostName + "' could not be found.");
        } else {
            EcsComputeBlade personalBladeInstanceForUser = mComputeBladeFactory.create(hostName, ipAddress,
                    mFuelResource.getIpPublic(), mFuelResource.getFuelPublicSshPort());
            mCreatedBladeInstancesForUser.add(personalBladeInstanceForUser);
            return personalBladeInstanceForUser;
        }
    }

    /**
     *
     * @return a set of host names of the current compute blades
     */
    public Set<String> getComputeBladeDestinationIds() {
        initializeComputeBlades();
        return mComputeBladeIpsByHostName.keySet();
    }

    /**
     *
     * @return the hostname postfix of a compute blade(eg:".domain.tld")
     */
    public String getComputeHostDomainName() {
        if (mComputeDomain == null) {
            for (FuelNode fuelNode : getNodes()) {
                if (fuelNode.getRoles().contains(FuelNode.NodeRole.COMPUTE)) {
                    String hostName = getComputeHostFullName(fuelNode);
                    mComputeDomain = hostName.substring(hostName.indexOf("."));
                    break;
                }
            }
        }
        if (mComputeDomain == null) {
            throw new EcsComputeBladeException("Something went wrong..could not find any compute blade in the node.");
        }
        return mComputeDomain;
    }

    /**
     * Fetch the IP Address of compute blade specified by hostname.
     *
     * @param hostname
     * @return IP Address
     */
    public String getComputeIpByName(String hostname) {
        initializeComputeBlades();
        for (Map.Entry<String, String> entry : mComputeBladeIpsByHostName.entrySet()) {
            if (entry.getKey().contains(hostname)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Temporary workaround to get compute blade list from fuel
     */
    public List<FuelNode> getNodes() {
        List<FuelNode> nodes = new ArrayList<FuelNode>();
        String response = mFuel.sendCommand("fuel node | grep \"ready\"");
        String[] lines = response.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\\|");
            if (isInteger(fields[0].trim())) {
                Set<NodeRole> roles = new HashSet<NodeRole>();
                String[] roleStrs = fields[6].trim().split(",");
                for (String role : roleStrs) {
                    roles.add(FuelNode.NodeRole.fromNodeRoleString(role.trim()));
                }
                nodes.add(new FuelNode(fields[0].trim(), fields[2].trim(), fields[3].trim(), fields[4].trim(), roles,
                        Boolean.valueOf(fields[8].trim())));
            }
        }
        return nodes;
    }

    /**
     * returns a Compute blade instance at random.
     *
     * @return
     */
    public EcsComputeBlade getRandomComputeBlade() {
        return getRandomComputeBlade(null);
    }

    /**
     * Method to get a random computeBlade, the user can specify which blades should be excluded
     *
     * Example of Usage:
     * Let's say you have created a VM and want to see the host id via method
     * EcsVirtualMachine.getComputeBladeId(). Then you want to transfer it to another
     * blade, it doesn't matter which blade, just as long it's a another blade than the
     * current one, use this method.
     *
     * @param ComputeBladesToExclude - String[] - A list of Compute Blades to exclude.
     * @return A random Compute Blade
     */
    public EcsComputeBlade getRandomComputeBlade(List<String> computeBladesToExclude) {
        initializeComputeBlades();

        Map<String, String> mapCopy = new HashMap<String, String>();
        mapCopy.putAll(mComputeBladeIpsByHostName);
        if (computeBladesToExclude != null && !computeBladesToExclude.isEmpty()) {
            for (String blade : computeBladesToExclude) {
                mapCopy.remove(blade);
            }
        }
        if (mapCopy.isEmpty()) {
            throw new EcsTargetException(
                    "Could not select any random compute blade, there is either no compute blade or all blades are excluded.");
        }

        int index = new Random().nextInt(mapCopy.size());
        String hostName = new ArrayList<String>(mapCopy.keySet()).get(index);
        String ipAddress = mComputeBladeIpsByHostName.get(hostName);
        EcsComputeBlade personalBladeInstanceForUser = mComputeBladeFactory.create(hostName, ipAddress,
                mFuelResource.getIpPublic(), mFuelResource.getFuelPublicSshPort());
        mCreatedBladeInstancesForUser.add(personalBladeInstanceForUser);
        return personalBladeInstanceForUser;
    }

    /**
     * Method to check if the system time is synchronized on all compute blades
     * (The system time is said to be synchronized when the ntp offset value
     * on each compute blade lies between -50.0 and +50.0)
     *
     * @return  boolean
     */
    public boolean isComputeTimeSynchronized() {
        boolean isSynchronized = false;
        int bladeCount = 0;
        for (EcsComputeBlade blade : getAllComputeBlades()) {
            Float offset = blade.getNtpOffset();
            if (offset > -50F && offset < 50F) {
                bladeCount++;
            }
        }
        if (bladeCount == getAllComputeBlades().size()) {
            isSynchronized = true;
        }
        return isSynchronized;
    }
}

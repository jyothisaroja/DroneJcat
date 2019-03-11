package com.jcat.cloud.fw.components.system.cee.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.EcsScaleIoBlade;
import com.jcat.cloud.fw.components.model.target.EcsScaleIoBladeFactory;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelNode;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelNode.NodeRole;

/**
 * Class that represents all the scaleio blades in fuel, there is functionality for retreiving
 * all scaleio blades, individual random scaleio blade, verify scli commands are working in specified blade.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zmousar - 2018-02-13 - Initial version
 */
public class EcsScaleIoBladeList extends EcsComponent {

    private static final String CMD_SCALEIO_LIST = "fuel node | grep \"ready\" | grep \"scaleio\"";
    private final List<EcsScaleIoBlade> mScaleIoBladeList = new ArrayList<EcsScaleIoBlade>();
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsScaleIoBladeList.class);
    private List<FuelNode> mScaleIoNodes = new ArrayList<FuelNode>();
    private boolean mFlag = false;
    @Inject
    private EcsFuel mFuel;
    @Inject
    private EcsScaleIoBladeFactory mEcsScaleIoBladeFactory;

    /**
     * Returns EcsScaleIoBlade Object of the specific fuel node(scaleio)
     *
     * @param fuelNode - the scaleio node
     * @return
     */
    private EcsScaleIoBlade getEcsScaleIoBlade(FuelNode fuelNode) {
        EcsScaleIoBlade ecsScaleIoBlade = mEcsScaleIoBladeFactory.create(fuelNode.getAddress());
        mScaleIoBladeList.add(ecsScaleIoBlade);
        return ecsScaleIoBlade;
    }

    /**
     * Get ScaleIo Nodes from fuel command
     * ex: [root@fuel ~]# fuel node | grep "ready" | grep "scaleio"
     *      1 | ready  | scaleio-0-3  |       1 | 192.168.0.24 | 04:4e:06:cf:42:3a | base-os, scaleio  |               |      1 |        1
     *      2 | ready  | scaleio-0-11 |       1 | 192.168.0.22 | a4:a1:c2:ea:cf:06 | base-os, scaleio  |               |      1 |        1
     *      4 | ready  | scaleio-0-10 |       1 | 192.168.0.21 | a4:a1:c2:ea:c6:5e | base-os, scaleio  |               |      1 |        1
     * If scaleio blades are not present in DC
     * ex: [root@fuel ~]# fuel node | grep "ready" | grep "scaleio"
     *     [root@fuel ~]#
     *
     * @return - scaleio blade list if present otherwise throws exception
     */
    private List<FuelNode> getScaleIoNodesFromFuel() {
        List<FuelNode> scaleIoNodes = new ArrayList<FuelNode>();
        String result = mFuel.sendCommand(CMD_SCALEIO_LIST);
        if (!result.contains(FuelNode.NodeRole.SCALEIO.toString())) {
            throw new EcsTargetException("ScaleIo blades are not present in DC");
        }
        String[] blades = result.split("\n");
        for (String blade : blades) {
            blade = blade.replaceAll(" ", "");
            // ex: 10|ready|scaleio-0-16|1|192.168.0.35|24:6e:96:0f:4b:b8|base-os,scaleio||1|1
            Matcher matcher = Pattern.compile(
                    "^(\\d+)\\|(.*)\\|(.*-\\d+-\\d+)\\|(\\d+)\\|(\\d+\\.\\d+\\.\\d+\\.\\d+)\\|(.*)\\|(.*,scaleio)\\|\\|(\\d+)\\|(\\d+)")
                    .matcher(blade);
            if (matcher.find()) {
                Set<NodeRole> nodeRoles = new HashSet<NodeRole>();
                for (String role : Arrays.asList(matcher.group(7).trim())) {
                    nodeRoles.add(FuelNode.NodeRole.fromNodeRoleString(role.trim()));
                }
                scaleIoNodes.add(new FuelNode(matcher.group(1).trim(), matcher.group(3).trim(), matcher.group(4).trim(),
                        matcher.group(5).trim(), nodeRoles, Boolean.valueOf(matcher.group().trim())));
            }
        }
        return scaleIoNodes;
    }

    private void initializeScaleIoBlades() {
        if (!mFlag) {
            mScaleIoNodes = getScaleIoNodesFromFuel();
            mFlag = true;
        }
    }

    /**
     * checks if "scli commands are working" in the specified scaleio-blade
     *
     * @param ecsScaleIoBlade - EcsScaleIoBlade to verify scli commands
     * @return
     */
    private boolean isScliSupported(EcsScaleIoBlade ecsScaleIoBlade) {
        return ecsScaleIoBlade.sendScliCommand().contains("Logged in.");
    }

    /**
     * Disconnects from the cli host[SSH server]
     */
    @Override
    public Boolean deinitialize() {
        mLogger.info(EcsAction.STARTING, EcsScaleIoBladeList.class, "Clean up");
        for (EcsScaleIoBlade blade : mScaleIoBladeList) {
            blade.deinitialize();
        }
        mLogger.info(Verdict.DONE, EcsScaleIoBladeList.class, "Clean up");
        return true;
    }

    /**
     * Get the list of all scli supported scaleio blades
     *
     * @return List of EcsScaleIoBlade objects for the scaleio blades
     */
    public List<EcsScaleIoBlade> getAllScaleIoBlades() {
        initializeScaleIoBlades();
        List<EcsScaleIoBlade> scaleIoBladeList = new ArrayList<EcsScaleIoBlade>();
        for (FuelNode node : mScaleIoNodes) {
            EcsScaleIoBlade ecsScaleIoBlade = getEcsScaleIoBlade(node);
            // verify scli commands are working in blade
            if (isScliSupported(ecsScaleIoBlade)) {
                scaleIoBladeList.add(ecsScaleIoBlade);
            }
        }
        return scaleIoBladeList;
    }

    /**
     * Get the random scli supported blade from the available list of scaleio blades
     *
     * @return EcsScaleIoBalde object for the scaleio blade otherwise throws Exception
     */
    public EcsScaleIoBlade getRandomScaleIoBlade() {
        initializeScaleIoBlades();
        List<EcsScaleIoBlade> scaleIoBlades = getAllScaleIoBlades();
        if (scaleIoBlades != null) {
            int random = new Random().nextInt(scaleIoBlades.size());
            return scaleIoBlades.get(random);
        }
        throw new EcsTargetException("Could not find any scaleio blade which supports scli");
    }
}

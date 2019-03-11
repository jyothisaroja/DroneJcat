package com.jcat.cloud.fw.components.model.compute;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.system.cee.ecssession.VmSessionVirshFactory;

/**
 * Class which represents BAT VM launched from Atlas VM (from heat stack) and handles operations performed on it.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2018-18-04 initial version
 */
public class EcsAtlasBatVm extends EcsVm {

    private static final String BAT_ATLAS_VM_NETWORK_NAME = "tenant_3582";
    private static final String BAT_SETUP_SCRIPT = "/home/batman/bat_mini_setup.sh";
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsAtlasBatVm.class);
    private EcsVm mEcsVm;
    private String mVmIp;

    @Inject
    private VmSessionVirshFactory mVmSessionFactory;

    @Inject
    public EcsAtlasBatVm(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp) {
        super(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId, vmIp);
    }

    /**
     * Returns the IP of Atlas Bat VM.
     *
     * @return String
     */
    public String getVmIP() {
        return mVmIp;
    }

    /*
     * {@inheritDoc}
     * @see ecs.jcat.lib.controllers.EcsComponent#initialize
     */
    @Override
    public Boolean lazyInitialize() {
        mVmIp = getIPs(BAT_ATLAS_VM_NETWORK_NAME).get(0);
        EcsImage image = getImage();
        mSshSession = mVmSessionFactory.create(" ", mVmIp, image.getUserName(), image.getPassword(),
                image.getRegexPrompt());
        return true;
    }

    @Override
    public String sendCommand(String command) {
        return super.sendCommand(command);
    }

    /**
     * Starts and monitors BAT traffic inside VM.
     *
     * @return boolean
     */
    public boolean startAndVerifyBatTraffic() {
        mLogger.info(EcsAction.STARTING, EcsAtlasBatVm.class, "Atlas BAT traffic");
        if (doesFileExist(BAT_SETUP_SCRIPT)) {
            changePermissions("777", BAT_SETUP_SCRIPT);
            mSshSession.send(BAT_SETUP_SCRIPT + " --interface");
            mSshSession.send(BAT_SETUP_SCRIPT + " --copy");
            mSshSession.send(BAT_SETUP_SCRIPT + " --setup_remote");
            mLogger.info(Verdict.STARTED, EcsAtlasBatVm.class, "Atlas BAT traffic");
            return verifyBatTraffic();
        }
        throw new EcsOpenStackException(
                "Bat script file \"bat_mini_setup.sh\" does not exist in VM " + mEcsVm.getName());
    }

    /**
     * Stops that BAT traffic inside VM.
     *
     * @return boolean
     */
    public boolean stopBatTraffic() {
        mLogger.info(EcsAction.STOPPING, EcsAtlasBatVm.class, "Atlas BAT traffic");
        String result = mSshSession.send(BAT_SETUP_SCRIPT + " --stop_remote");
        if (result.contains("stopping")) {
            mLogger.info(Verdict.STOPPED, EcsAtlasBatVm.class, "Atlas BAT traffic");
            return true;
        }
        return false;
    }

    /**
     * Verifies that BAT traffic is running inside VM.
     *
     * @return boolean
     */
    public boolean verifyBatTraffic() {
        mLogger.info(EcsAction.VALIDATING, EcsAtlasBatVm.class, "Atlas BAT traffic");
        if (doesFileExist(BAT_SETUP_SCRIPT)) {
            String trafficResult = mSshSession.send(BAT_SETUP_SCRIPT + " --report");
            if (!(trafficResult.contains("send") && trafficResult.contains("recv"))) {
                return false;
            }
        } else {
            throw new EcsOpenStackException(
                    "Bat script file \"bat_mini_setup.sh\" does not exist in VM " + mEcsVm.getName());
        }
        mLogger.info(Verdict.VALIDATED, EcsAtlasBatVm.class, "Atlas BAT traffic");
        return true;
    }
}

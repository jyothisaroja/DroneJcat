package com.jcat.cloud.fw.components.system.cee.target.fuel;

import org.testng.internal.Nullable;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.EcsTarget;
import com.jcat.cloud.fw.components.system.cee.ecssession.LxcSessionFactory;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.BspNetconfLib;
import com.jcat.cloud.fw.infrastructure.resources.LxcResource;

/**
 * Handles LXC
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017- initial version
 * @author zdagjyo - 2017-11-29 - Add methods getFileFromComputeBlade and transferFileToComputeBlade
 */
public class EcsLxc extends EcsTarget {
    private LxcResource mLxc;
    private String mIpAddress;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsLxc.class);
    @Inject
    private LxcSessionFactory mLxcSessionFactory;
    @Inject
    private IEquipmentController mEquipmentController;

    @Inject
    public EcsLxc(@Nullable LxcResource lxc) {
        mLxc = lxc;
        if (mLxc != null) {
            mIpAddress = mLxc.getIp();
        }
    }

    /**
     * Initializes the mSshSession variable of lxc
     */
    private void initializeSession() {
        if (mSshSession == null) {
            mSshSession = mLxcSessionFactory.create(mIpAddress, mLxc.getSshPort());
        }
    }

    @Override
    public boolean changeUser(String newUser) {
        throw new EcsNotImplementedException("Change user functionality is not supported for target Lxc");
    }

    @Override
    public Boolean deinitialize() {
        if (mSshSession != null) {
            mSshSession.disconnect();
        }
        return true;
    }

    /**
     * Transfers file from specified compute blade to LXC.
     *
     * @param blade - EcsComputeBlade - the compute blade from where the file is to be copied
     * @param absoluteSrcFilePath - String - absolute file path in compute blade
     * @param destinationFilePath - String - the location in LXC where the file is to be copied
     * @return boolean
     */
    public boolean getFileFromComputeBlade(EcsComputeBlade blade, String absoluteSrcFilePath,
            String destinationFilePath) {
        mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, "from " + blade.getHostname(),
                "to LXC :" + destinationFilePath);
        String bladeIp = blade.getBridgeIp();
        if (mEquipmentController instanceof BspNetconfLib) {
            bladeIp = blade.getAdminIp();
        }
        String result = sendCommand("scp root@" + bladeIp + ":" + absoluteSrcFilePath + " " + destinationFilePath);
        if (!result.contains("100%")) {
            throw new EcsTargetException("copying file from compute blade failed");
        }
        mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, "from " + blade.getHostname(),
                "to LXC :" + destinationFilePath);
        return true;
    }

    /**
     * Returns the lxc ip address
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    @Override
    public String sendCommand(String command) {
        initializeSession();
        return mSshSession.send(command);
    }

    @Override
    public boolean startScalabilityCollection() {
        throw new EcsNotImplementedException("Scalability collection is not done on LXC.");
    }

    @Override
    public boolean startStabilityCollection() {
        throw new EcsNotImplementedException("Stability collection is not done on LXC.");
    }

    /**
     * Transfers file from LXC to specified compute blade.
     *
     * @param blade - EcsComputeBlade - the compute blade where the file is to be copied
     * @param absoluteSrcFilePath - String - absolute file path in LXC
     * @param destinationFilePath - String - the location in blade where the file is to be copied
     * @return boolean
     */
    public boolean transferFileToComputeBlade(EcsComputeBlade blade, String absoluteSrcFilePath,
            String destinationFilePath) {
        mLogger.info(EcsAction.TRANSFERING, absoluteSrcFilePath, "from LXC",
                "to " + blade.getHostname() + " :" + destinationFilePath);
        String bladeIp = blade.getBridgeIp();
        if (mEquipmentController instanceof BspNetconfLib) {
            bladeIp = blade.getAdminIp();
        }
        String result = sendCommand("scp " + absoluteSrcFilePath + " root@" + bladeIp + ":" + destinationFilePath);
        if (!result.contains("100%")) {
            throw new EcsTargetException("copying file to compute blade failed");
        }
        mLogger.info(Verdict.TRANSFERED, absoluteSrcFilePath, "from LXC",
                "to " + blade.getHostname() + " :" + destinationFilePath);
        return true;
    }
}

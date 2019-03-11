package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.ericsson.commonlibrary.remotecli.exceptions.AuthenticationException;
import com.ericsson.commonlibrary.remotecli.exceptions.ConnectionToServerException;
import com.ericsson.commonlibrary.remotecli.exceptions.ReadTimeoutException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionTarget;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionType;
import com.jcat.cloud.fw.common.exceptions.EcsSessionException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsLxc;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015-02-17 initial version
 * @author epergat 2015-03-18 refactored to extend EcsSession
 * @author eelimei 2015-05-04 Restructure and adoption to automatic functionality in EcsSession for prompts.
 * @author zdagjyo 2017-09-11 Modified connect method to support connection to blade via both LXC and Fuel
 * @author zdagjyo 2018-01-25 Add method setIsFailSafeEnabled
 */
public class ComputeBladeSession extends EcsSession {

    private static final String FUEL_PASSWORD = "r00tme";
    private static final String FUEL_USERNAME = "root";
    private static final String LXC_PASSWORD = "Jumpstart";
    private static final String LXC_USERNAME = "lxcpxe";

    private final String mIpAddress;
    private final EcsLogger mLogger = EcsLogger.getLogger(ComputeBladeSession.class);
    private String mLxcIpAddress = null;
    private String mFuelIpAddress = null;
    private final int mLxcPortFwd;
    private String mAdmUser = null;
    private String mAdmPwd = null;
    protected boolean mIsFailSafeEnabled = true;

    @Inject
    public ComputeBladeSession(@Assisted("ipAddress") String ipAddress, @Assisted("fuelIpAddress") String fuelIpAddress,
            @Assisted("lxcPortFwd") int lxcPortFwd) {
        super(EcsConnectionTarget.COMPUTE_BLADE);
        mIpAddress = ipAddress;
        mFuelIpAddress = fuelIpAddress;
        mLxcPortFwd = lxcPortFwd;
    }

    // To connect via lxc
    public ComputeBladeSession(String ipAddress, String lxcIpAddress, String userName, String password) {
        super(EcsConnectionTarget.COMPUTE_BLADE);
        mIpAddress = ipAddress;
        mLxcIpAddress = lxcIpAddress;
        mLxcPortFwd = 22;
        mAdmUser = userName;
        mAdmPwd = password;
    }

    @Override
    protected void connect() throws EcsConnectionException {
        boolean connectViaLxc = false;
        if (mLxcIpAddress == null) {
            mLogger.info(EcsAction.CONNECTING, EcsComputeBlade.class, mHostname + " (" + mIpAddress + ") via fuel ("
                    + mFuelIpAddress + ":" + mLxcPortFwd + "), LXC port forwarding required");
            try {
                connect(mFuelIpAddress, FUEL_USERNAME, FUEL_PASSWORD, mLxcPortFwd,
                        REGEX_BASIC_PROMPTS + "|" + FUEL_PROMPT);
                mLogger.info(Verdict.CONNECTED, EcsFuel.class, "now ssh to Compute Blade");

            } catch (EcsSessionException | ReadTimeoutException | ConnectionToServerException
                    | AuthenticationException exception) {
                if (mIsFailSafeEnabled) {
                    mLogger.warn("Connection to blade " + mHostname
                            + " is supposed to be via fuel, but it failed, hence trying to connect to " + mHostname
                            + " via LXC");
                    mLogger.warn(
                            "If you are reading this warning but you don't know anything about it, it means something "
                                    + "is wrong with Fuel and we strongly suggest you to stop the test and find out why Fuel"
                                    + " is not responding");
                    mLxcIpAddress = mFuelIpAddress;
                    connectViaLxc = true;
                }
            }
        }
        if (connectViaLxc || !(mLxcIpAddress == null)) {
            mLogger.info(EcsAction.CONNECTING, EcsComputeBlade.class,
                    mHostname + " (" + mIpAddress + ") via lxc (" + mLxcIpAddress + ")");
            try {
                connect(mLxcIpAddress, LXC_USERNAME, LXC_PASSWORD, REGEX_BASIC_PROMPTS + "|" + LXC_PROMPT);
                mLogger.info(Verdict.CONNECTED, EcsLxc.class, "now ssh to Compute Blade");
            } catch (EcsSessionException | ReadTimeoutException | ConnectionToServerException
                    | AuthenticationException exception) {
                throw new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.LXC,
                        EcsConnectionTarget.LXC, mLxcIpAddress, 22, exception);
            }
        }
        mCli.setExpectedRegexPrompt(REGEX_BASIC_PROMPTS + "|" + CIC_COMPUTE_PROMPT + "|" + getPersonalUserPrompt());
        String connectToBlade = null;
        EcsConnectionException connectionException;
        if (mLxcIpAddress == null) {
            connectionException = new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.COMPUTE_BLADE,
                    EcsConnectionTarget.FUEL, mFuelIpAddress, mLxcPortFwd);
            connectToBlade = mCli.send("ssh -o ConnectTimeout=30 " + mIpAddress);
        } else {
            connectionException = new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.COMPUTE_BLADE,
                    EcsConnectionTarget.LXC, mLxcIpAddress, 22);
            setNextPossiblePassword(mAdmPwd);
            connectToBlade = mCli.send("ssh -o ConnectTimeout=30 " + mAdmUser + "@" + mIpAddress);
        }
        String tempString = checkForPossiblePasswordOrContinuePrompt();
        if (tempString != null) {
            connectToBlade = tempString;
        }
        if (connectToBlade.contains(" No route to host")) {
            mCli.disconnect();
            throw connectionException;
        }
        if (mCli.getMatchedPrompt().matches(FUEL_PROMPT)) {
            mLogger.fatal("Fail to connect to Compute Blade (*" + mHostname + "): prompt matches fuel prompt");
            throw connectionException;
        } else if (mCli.getMatchedPrompt().matches(LXC_PROMPT)) {
            mLogger.fatal("Fail to connect to Compute Blade (*" + mHostname + "): prompt matches lxc prompt");
            throw connectionException;
        }
        // Set the host name to compute blade's instead of fuel's/lxc's
        mHostname = mCli.send("hostname");
        mLogger.info(Verdict.STARTED, "session", EcsComputeBlade.class, mHostname);
    }

    /**
     * Manually set the isFailSafeEnabled boolean value
     *
     * @param boolean
     */
    public void setIsFailSafeEnabled(boolean isFailSafeEnabled) {
        mIsFailSafeEnabled = isFailSafeEnabled;
    }
}

package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionTarget;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionType;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsUser;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsLxc;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2017-08-28 initial version
 */
public class LxcSession extends EcsSession {
    private static final String SSH_PASSWORD = "Jumpstart";
    private static final String SSH_USERNAME = "lxcpxe";

    private final String mLxcIpAddress;
    private final int mLxcPortFwd;

    /**
     * @param lxcIpAddress
     *            public address where the Lxc can be reached
     * @param lxcPortFwd
     *            port which forwards to the Lxc SSH port
     */
    @Inject
    public LxcSession(@Assisted("lxcIpAddress") String lxcIpAddress, @Assisted("lxcPortFwd") int lxcPortFwd) {
        super(EcsConnectionTarget.LXC);
        mLxcIpAddress = lxcIpAddress;
        mLxcPortFwd = lxcPortFwd;
        mCurrentUser = new EcsUser(SSH_USERNAME, SSH_PASSWORD, false);
    }

    @Override
    protected void connect() throws EcsConnectionException {
        mLogger.info(EcsAction.CONNECTING, EcsLxc.class, "via LXC port fwd");
        try {
            connect(mLxcIpAddress, SSH_USERNAME, SSH_PASSWORD, mLxcPortFwd, REGEX_BASIC_PROMPTS + "|" + LXC_PROMPT);
        } catch (Exception ex) {
            throw new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.LXC, EcsConnectionTarget.LXC,
                    mLxcIpAddress, mLxcPortFwd, ex);
        }
        mLogger.info(Verdict.CONNECTED, EcsLxc.class, "");
        mHostname = mCli.send("hostname");
    }
}
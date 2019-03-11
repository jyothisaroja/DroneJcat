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
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eolnans 2015-02-17 initial version
 * @author epergat 2015-03-18 refactored to extend EcsSession
 * @author eelimei 2015-05-04 Restructure and adoption to automatic functionality in EcsSession for prompts.
 * @author ehosmol 2015-05-06 adopt to {@link EcsUser}
 * @author zdagjyo 2017-09-20 Add overridden method getDefaultPrompt
 *
 */
public class FuelSession extends EcsSession {
    private static final String SSH_PASSWORD = "r00tme";
    private static final String SSH_USERNAME = "root";

    private final String mLxcIpAddress;
    private final int mLxcPortFwd;

    /**
     *
     * @param lxcIpAddress public address where the Fuel Master can be reached
     * @param lxcPortFwd port which forwards to the Fuel Master SSH port
     */
    @Inject
    public FuelSession(@Assisted("lxcIpAddress") String lxcIpAddress, @Assisted("lxcPortFwd") int lxcPortFwd) {
        super(EcsConnectionTarget.FUEL);
        mLxcIpAddress = lxcIpAddress;
        mLxcPortFwd = lxcPortFwd;
        mCurrentUser = new EcsUser(SSH_USERNAME, SSH_PASSWORD, false);
    }

    @Override
    protected void connect() throws EcsConnectionException {
        mLogger.info(EcsAction.CONNECTING, EcsFuel.class, "via LXC port fwd");
        try {
            connect(mLxcIpAddress, SSH_USERNAME, SSH_PASSWORD, mLxcPortFwd, REGEX_BASIC_PROMPTS + "|" + FUEL_PROMPT);
        } catch (Exception ex) {
            throw new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.FUEL, EcsConnectionTarget.LXC,
                    mLxcIpAddress, mLxcPortFwd, ex);
        }
        mLogger.info(Verdict.CONNECTED, EcsFuel.class, "");
        mHostname = mCli.send("hostname");
    }
}
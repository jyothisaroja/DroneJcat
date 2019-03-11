/**
 *
 */
package com.jcat.cloud.fw.components.system.cee.services.fee;

import org.apache.log4j.Logger;
import com.jcat.cloud.fw.common.exceptions.FeeServiceException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.EcsTarget;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015-03-18 initial version
 * @author eelimei 2015-05-04 Removed workaround for regex prompt matching
 * @author ehosmol 2015-05-18 adopt to {@link EcsTarget}
 * @author eqinann 2016-03-11 Removed from Integration test, proposed to be deleted
 */
public class EcsFeeService extends EcsComponent {

    public static enum Status {
        ACTIVE_OK, PASSIVE;
    }

    private final Logger mLogger = Logger.getLogger(EcsFeeService.class);

    private final EcsSession mSshSession;

    public EcsFeeService(EcsSession sshSession) {
        mSshSession = sshSession;
    }

    private void verifyCurrentStatus(Status status) {
        if (getStatus() != status) {
            throw new FeeServiceException("The fee service could not verify that the current status is: "
                    + status.toString());
        }
        mLogger.info("Fee service verified that the current status is: " + status.toString());
    }

    /**
     *
     * @return current fee status
     */
    public EcsFeeService.Status getStatus() {
        mLogger.info("Checking the fee service status");
        String result = mSshSession.send("service fee status");
        if (result.contains("ACTIVE OK")) {
            mLogger.info("The fee service status is: ACTIVE_OK");
            return Status.ACTIVE_OK;
        } else if (result.contains("PASSIVE")) {
            mLogger.info("The fee service status is: PASSIVE");
            return Status.PASSIVE;
        } else {
            throw new FeeServiceException("The fee service status could not be determined, command output unknown");
        }
    }

    /**
     * Method to re-set fee counter by removing /var/run/fee. This will stop the fee service.
     * Throws exception if the status does not become PASSIVE
     */
    public void resetCounter() {
        mLogger.info("Re-setting the fee service counter");
        mSshSession.send("rm -rf /var/run/fee");
        verifyCurrentStatus(Status.PASSIVE);
    }

    /**
     * Resets the fee counter for this particular process in /var/run/fee/<processName>
     * @param processName
     */
    public void resetCounter(String processName) {
        mLogger.info("Re-setting the fee service counter for process: " + processName);
        String allProcesses = mSshSession.send("service --status-all");
        if (!allProcesses.contains(" " + processName + "\r\n")) {
            throw new FeeServiceException("The fee service was not able to verify the existance of a process named: "
                    + processName);
        }
        mSshSession.send("rm -rf /var/run/fee/" + processName);
    }

    /**
     * Resets the fee counter and then starts the fee service.
     * Throws exception if the status does not become ACTIVE_OK
     */
    public void startFeeService() {
        mLogger.info("starting the fee service but will reset the counter first.");
        resetCounter();
        mSshSession.send("service fee start");
        mLogger.info("The fee service was started");
        verifyCurrentStatus(Status.ACTIVE_OK);
    }

    /**
     * Stops the fee service
     * Throws exception if the status does not become PASSIVE
     */
    public void stopFeeService() {
        mLogger.info("Stopping the fee service");
        mSshSession.send("service fee stop");
        verifyCurrentStatus(Status.PASSIVE);

    }
}

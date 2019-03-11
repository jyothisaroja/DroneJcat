package com.jcat.cloud.fw.fwservices.traffic.model;

import org.apache.log4j.Logger;
import com.jcat.cloud.fw.components.model.EcsComponent;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * Data structure for TestApp traffic
 *
 * @author eqinann 2015- initial version
 *
 */
public class TestAppResult extends EcsComponent {
    private final Logger mLogger = Logger.getLogger(TestAppResult.class);
    private String batVmName;
    private int sent;
    private int received;
    private int sendFailed;
    private int failed;
    private int timeout;
    private int unknown;

    public TestAppResult(String name, int sent, int received, int sendFailed, int failed, int timeout, int unknown) {
        this.batVmName = name;
        this.sent = sent;
        this.received = received;
        this.sendFailed = sendFailed;
        this.failed = failed;
        this.timeout = timeout;
        this.unknown = unknown;
    }

    public int failed() {
        return failed;
    }

    /**
     * Similar to equals(). Comparing two TestAppResults to find out if anything is failing
     *
     * @param otherResult
     * @return true if there is no failures
     */
    public boolean hasNoMoreFailureThan(TestAppResult otherResult) {
        if (this.sendFailed != otherResult.sendFailed) {
            mLogger.error("Send failed packegs has changed: " + (this.sendFailed - otherResult.sendFailed) + " on "
                    + batVmName);
            return false;
        }
        if (this.failed != otherResult.failed) {
            mLogger.error("Failed packegs has changed: " + (this.failed - otherResult.failed) + " on " + batVmName);
            return false;
        }
        if (this.timeout != otherResult.timeout) {
            mLogger.error("Timeout packegs has changed: " + (this.timeout - otherResult.timeout) + " on " + batVmName);
            return false;
        }
        if (this.unknown != otherResult.unknown) {
            mLogger.error("Unknown packegs has changed: " + (this.unknown - otherResult.unknown) + " on " + batVmName);
            return false;
        }
        return true;
    }

    public String name() {
        return batVmName;
    }

    public int received() {
        return received;
    }

    public int sent() {
        return sent;
    }

    public int timeout() {
        return timeout;
    }
}

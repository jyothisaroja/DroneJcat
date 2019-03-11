package com.jcat.cloud.fw.common.exceptions;

import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlException;

/**
 * Exception thrown in case of SSH connection failure in Traffic plugin
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-01-22 initial version
 *
 */
public class TrafficSSHException extends TrafficControlException {

    /**
     * Generated serial version UID
     */
    private static final long serialVersionUID = -8544107467539914515L;

    /**
     * Default exception constructor
     */
    public TrafficSSHException() {
        super();
    }

    /**
     * Exception constructor with the custom message
     *
     * @param message
     *            String
     */
    public TrafficSSHException(String message) {
        super(message);
    }

    /**
     * Exception constructor with the custom message and throwable cause
     *
     * @param message
     *            String
     */
    public TrafficSSHException(String message, Throwable cause) {
        super(message, cause);
    }

}

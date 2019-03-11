package com.jcat.cloud.fw.common.exceptions;

import se.ericsson.jcat.fw.ng.traffic.AbstractTrafficPlugin;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-09-09 initial version
 *
 */
public class EcsTrafficException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 5697695158460275715L;
    private Class<? extends AbstractTrafficPlugin> trafficType;

    /**
     * Default exception constructor
     */
    @SuppressWarnings("unused")
    private EcsTrafficException() {
    }

    /**
     * Exception constructor with the custom message
     *
     * @param message
     *            String
     */
    public EcsTrafficException(Class<? extends AbstractTrafficPlugin> trafficType, String message) {
        super("Traffic " + trafficType.getName().substring(trafficType.getName().lastIndexOf('.') + 1) + ": " + message);
        this.trafficType = trafficType;
    }

    /**
     * Exception constructor with the custom message and throwable cause
     *
     * @param message
     *            String
     */
    public EcsTrafficException(Class<? extends AbstractTrafficPlugin> trafficType, String message, Throwable cause) {
        super(
                "Traffic " + trafficType.getName().substring(trafficType.getName().lastIndexOf('.') + 1) + ": "
                + message, cause);
        this.trafficType = trafficType;
    }

    public Class<? extends AbstractTrafficPlugin> getTrafficType() {
        return trafficType;
    }
}

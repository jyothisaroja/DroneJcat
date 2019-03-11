package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception which should be thrown when an Openstack neutron problem happens
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-06-27 initial version
 *
 */
public class NeutronException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    protected NeutronException(String message, Throwable cause) {
        super(message, cause);
    }

    public NeutronException(String message) {
        super(message);
    }

    /**
     * Usage: EcsOpenStackException("message body", this)
     *
     * @param message
     * @param openstackComponent
     */
    public NeutronException(String message, Object openstackComponent) {
        super("\nFrom component: "
                + openstackComponent.getClass().getName()
                        .substring(openstackComponent.getClass().getName().lastIndexOf('.') + 1) + "\nError message: "
                + message + openstackComponent);
    }
}

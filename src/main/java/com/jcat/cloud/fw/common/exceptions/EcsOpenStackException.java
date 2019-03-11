package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception which should be thrown when an OpenStack resource problem happens
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehosmol 2015-02-03 initial version
 * @author eqinann 2015-06-01 added one new constructor
 *
 */
public class EcsOpenStackException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    protected EcsOpenStackException(String message, Throwable cause) {
        super(message, cause);
    }

    public EcsOpenStackException(String message) {
        super(message);
    }

    /**
     * Create an exception when the cause is not the current class
     * e.g. When Flavor has problems when creating a vm in nova controller
     * we would like to show in the error message clearly that Flavor is the
     * source, then put EcsFlavor as the openstackComponent
     *
     * @param message
     * @param openstackComponent
     */
    public EcsOpenStackException(String message, Class<?> openstackComponent) {
        super("From component: "
                + openstackComponent.getName().substring(openstackComponent.getName().lastIndexOf('.') + 1)
                + "\nError message: " + message);
    }

}

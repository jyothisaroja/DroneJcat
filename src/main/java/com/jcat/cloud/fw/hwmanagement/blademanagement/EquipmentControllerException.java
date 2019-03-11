package com.jcat.cloud.fw.hwmanagement.blademanagement;

/**
 * Thrown by an TdcExt implementation when an API method error occurs.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2013-01-17 Initial version
 *
 */
public class EquipmentControllerException extends Exception {

    /**
     * Generated <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 8546766677966938195L;

    /**
     *
     * Creates a new instance of <code>TdcExtException</code> with a specified detail message.
     *
     * @param message the detail message
     */
    public EquipmentControllerException(String message) {
        super(message);
    }

    /**
     *
     * Creates a new instance of <code>TdcExtException</code> with a specified detail message and an throwable object
     *
     * @param message the detail message
     */
    public EquipmentControllerException(String message, Throwable t) {
        super(message, t);
    }
}

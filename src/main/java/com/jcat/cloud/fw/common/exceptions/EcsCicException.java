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
 *
 */
public class EcsCicException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    public EcsCicException(String message) {
        super(message);
    }
}

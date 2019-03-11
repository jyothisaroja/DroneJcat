package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception which should be thrown when an OpenStack resource problem happens
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2017-02-24 initial version
 *
 */
public class EcsComputeBladeException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    public EcsComputeBladeException(String message) {
        super(message);
    }
}

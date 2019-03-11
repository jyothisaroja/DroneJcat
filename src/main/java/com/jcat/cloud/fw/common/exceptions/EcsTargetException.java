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
 * @author ethssce 2015-06-11 initial version
 *
 */
public class EcsTargetException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    public EcsTargetException(String message) {
        super(message);
    }
}

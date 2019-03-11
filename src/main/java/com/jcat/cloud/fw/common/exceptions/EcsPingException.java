package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception which should be thrown when an ping fails
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
public class EcsPingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EcsPingException(String reason) {
        super(reason);
    }
}

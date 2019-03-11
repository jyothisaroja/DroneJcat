package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception which will thrown when error happens during file processing
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2015-03-23 initial version
 *
 */
public class EcsFileException extends RuntimeException {

    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    protected EcsFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public EcsFileException(String message) {
        super(message);
    }

}

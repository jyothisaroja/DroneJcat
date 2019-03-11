package com.jcat.cloud.fw.common.exceptions;

import org.testng.SkipException;

/**
 * Exception which should be thrown when a test should be skipped
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-01-09 initial version
 *
 */
public class EcsSkipException extends SkipException {
    /**
     * Default serial version ID
     */
    private static final long serialVersionUID = 1L;

    protected EcsSkipException(String skipMessage, Throwable cause) {
        super(skipMessage, cause);
    }

    public EcsSkipException(String skipMessage) {
        super(skipMessage);
    }

}

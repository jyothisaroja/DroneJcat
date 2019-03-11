package com.jcat.cloud.fw.common.exceptions;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015- initial version
 *
 */
public class EcsWatchmenException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 4579670931314154208L;

    public EcsWatchmenException(String msg) {
        super(msg);
    }
}

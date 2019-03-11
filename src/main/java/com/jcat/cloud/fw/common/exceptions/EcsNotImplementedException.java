/**
 *
 */
package com.jcat.cloud.fw.common.exceptions;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015- initial version
 *
 */
public class EcsNotImplementedException extends RuntimeException {
    /**
     * Default Serial version ID
     */
    private static final long serialVersionUID = 1L;

    public EcsNotImplementedException(String message) {
        super(message);
    }
}

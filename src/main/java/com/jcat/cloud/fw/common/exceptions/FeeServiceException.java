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
 * @author eelimei 2015-03-18 initial version
 *
 */
public class FeeServiceException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param msg
     */
    public FeeServiceException(String msg) {
        super(msg);
    }
}

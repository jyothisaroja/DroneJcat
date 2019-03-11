package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

/**
 * Serial console interface
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eladand 2013-11-04 Initial version
 *
 */
public interface IConsoleConnector {

    /**
     * Open connection to the equipment.
     */
    void connect(int shelf, int slot);

    /**
     * Close connection to the equipment.
     */
    void disconnect();

    /**
     * send a message to the equipment
     *
     * @return the response from the equipment
     */
    String send(String command);
}

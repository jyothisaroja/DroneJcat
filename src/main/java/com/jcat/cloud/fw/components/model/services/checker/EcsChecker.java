/**
 *
 */
package com.jcat.cloud.fw.components.model.services.checker;

/**
 * This class is an interface for open-stack checking
 * related services such as background checker.
 *
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author efikayd - 2015-03-31 - Initial version
 * @author efikayd - 2015-04-01 - Added setEnable and setDisable methods
 * @author efikayd - 2015-04-14 - Added restart method
 */
public interface EcsChecker {

    public static final int EXIT_SUCCESS = 0;

    /**
     * Tests if the checker is enabled
     * @return if enabled, returns true.
     *         Otherwise, returns false.
     */
    public boolean isEnabled();

    /**
     * Sets the parameter that is used to
     * enable the checker. After invoking
     * this method, the method restart()
     * MUST be invoked successively in order
     * that the checker enabling is taken
     * into action.
     * @return On Success returns 0.
     *         Otherwise, returns a negative
     *         error code.
     */
    public int setDisable();

    /**
     * Sets the parameter that is used to
     * disable the checker. After invoking
     * this method, the method restart()
     * MUST be invoked successively in order
     * that the checker disabling is taken
     * into action.
     * @return On Success returns 0.
     *         Otherwise, returns a negative
     *         error code.
     */
    public int setEnable();

    /**
     * Restart the checker
     * @return On Success returns 0.
     *         Otherwise, returns an error code.
     */
    public int restart();
}

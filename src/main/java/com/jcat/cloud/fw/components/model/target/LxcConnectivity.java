package com.jcat.cloud.fw.components.model.target;

/**
 * Interface to handle code that requires
 * connectivity to LXC from ecs targets
 * like EcsCic and EcsFuel.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017-12-12 - Initial version
 */
public interface LxcConnectivity {

    /**
     * Checks if the current target has connectivity to LXC.
     *
     * @return boolean - false by default if the implementing class doesn't implement this method
     */
    default boolean hasLxcConnectivity() {
        return false;
    }
}

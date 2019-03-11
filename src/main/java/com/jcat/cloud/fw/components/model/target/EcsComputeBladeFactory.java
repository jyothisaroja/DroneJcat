package com.jcat.cloud.fw.components.model.target;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the ComputeBlade class.
 *
 * To understand the behaviour and functionality of this class, read about
 * Guice and factories.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 * @author zdagjyo 2017-02-06 initial version
 */
public interface EcsComputeBladeFactory {
    /**
     * Boilerplate code for creating an EcsVirtualMachine.
     * @param hostName - name of the compute blade
     * @param ipAddress - IP Address of the compute blade
     * @param fuelIpAddress - IP Address of Fuel
     * @param fuelPortFwd - Fuel Port Number
     * @return
     */
    EcsComputeBlade create(@Assisted("hostname") String hostName, @Assisted("ipAddress") String ipAddress,
            @Assisted("fuelIpAddress") String fuelIpAddress, @Assisted("fuelPortFwd") int fuelPortFwd);

}

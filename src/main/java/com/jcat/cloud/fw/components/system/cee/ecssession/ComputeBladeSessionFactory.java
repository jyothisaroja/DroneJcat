package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the ComputeBladeSession class.
 *
 * To understand the behaviour and functionality of this class, read about
 * Guice and factories.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 * @author zdagjyo 2018-02-07 initial version
 */
public interface ComputeBladeSessionFactory {
    /**
     * Guice factory creation of ComputeBladeSession Object
     *
     * @param ipAddress
     * @param fuelIpAddress
     * @param lxcPortFwd
     * @return
     */
    ComputeBladeSession create(@Assisted("ipAddress") String ipAddress, @Assisted("fuelIpAddress") String fuelIpAddress,
            @Assisted("lxcPortFwd") int lxcPortFwd);
}
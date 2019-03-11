package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the FuelSession class.
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
public interface FuelSessionFactory {
    /**
     * Guice factory creation of FuelSession Object
     *
     * @param lxcIpAddress
     * @param lxcPortFwd
     * @return
     */
    FuelSession create(@Assisted("lxcIpAddress") String lxcIpAddress, @Assisted("lxcPortFwd") int lxcPortFwd);
}
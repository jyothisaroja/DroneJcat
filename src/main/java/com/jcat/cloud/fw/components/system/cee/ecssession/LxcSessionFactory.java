package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the LxcSession class.
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
public interface LxcSessionFactory {
    /**
     * Guice factory creation of LxcSession Object
     *
     * @param lxcIpAddress
     * @param lxcPortFwd
     * @return
     */
    LxcSession create(@Assisted("lxcIpAddress") String lxcIpAddress, @Assisted("lxcPortFwd") int lxcPortFwd);
}
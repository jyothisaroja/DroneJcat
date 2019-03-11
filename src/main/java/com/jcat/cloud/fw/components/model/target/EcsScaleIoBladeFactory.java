package com.jcat.cloud.fw.components.model.target;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the EcsScaleIoBlade class.
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
 * @author zmousar
 */
public interface EcsScaleIoBladeFactory {

    /**
     * Boilerplate code for creating an EcsVirtualMachine.
     *
     * @param hostName - name of the scaleIo blade
     * @param ipAddress - IP Address of the scaleIo blade
     * @return
     */
    EcsScaleIoBlade create(@Assisted("ipAddress") String ipAddress);
}

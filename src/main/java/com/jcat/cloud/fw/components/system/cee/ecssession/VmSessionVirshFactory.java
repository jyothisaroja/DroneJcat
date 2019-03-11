package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the VmSessionVirsh class.
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
 * @author zdagjyo 2018-07-06 initial version
 */
public interface VmSessionVirshFactory {
    /**
     * Guice factory creation of VmSessionVirsh Object
     */
    VmSessionVirsh create(@Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("vmUserName") String vmUserName,
            @Assisted("vmPassword") String vmPassword, @Assisted("prompt") String prompt);
}

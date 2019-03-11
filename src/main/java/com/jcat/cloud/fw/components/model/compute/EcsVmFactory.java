package com.jcat.cloud.fw.components.model.compute;

import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.components.model.image.EcsImage;

/**
 * Guice factory for creating instances of the EcsVm class.
 *
 * To understand the behaviour and functionality of this class, read about
 * Guice and factories.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 * @author epergat 2014-12-15 initial version
 * @author eqinann 2015-01-26 Added admin pass
 * @auhtor epergat 2015-03-16 Added networkId
 */
public interface EcsVmFactory {
    /**
     * Guice factory creation of EcsVm Object
     *
     * @param serverId - Openstack Server ID for the VM
     * @param adminPass - Admin Password assigned for the newly created VM
     * @param hypervisorHostname - Hypervisor's host name, from where VM is running
     * @param hypervisorInstanceId - VM's instance ID on Hypervisor
     * @return
     */
    EcsVm create(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp);
}

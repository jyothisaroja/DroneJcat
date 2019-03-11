package com.jcat.cloud.fw.components.model.compute;

import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.components.model.image.EcsImage;

/**
 * Guice factory for creating instances of the EcsBatVm class.
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
 * @author zmousar 2018-08-20 initial version
 */
public interface EcsBatVmFactory  {

    /**
     * Guice factory creation of EcaBatVm Object
     *
     * @return
     */

    EcsBatVm create(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp);

}

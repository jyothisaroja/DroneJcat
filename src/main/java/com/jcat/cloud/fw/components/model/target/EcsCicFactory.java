package com.jcat.cloud.fw.components.model.target;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the CIC class.
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
 * @author eqinann 2015-09-09 Added EcsBatCic
 */
public interface EcsCicFactory {
    /**
     * Boilerplate code for creating an EcsVirtualMachine.
     * @param name - name of the cic
     * @param sessionProvider - provider of sessions for the Cic
     * @return
     */
    EcsCic create(@Assisted("username") String userName, @Assisted("password") String password, @Assisted("ipAddress") String ipAddress,
            @Assisted("port") int port);
}

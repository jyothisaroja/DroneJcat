package com.jcat.cloud.fw.components.system.cee.ecssession;

import com.google.inject.assistedinject.Assisted;

/**
 * Guice factory for creating instances of the CicSession class.
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
public interface CicSessionFactory {
    /**
     * Guice factory creation of CicSession Object
     *
     * @param userName
     * @param password
     * @param ipAddress
     * @param port
     * @return
     */
    CicSession create(@Assisted("userName") String userName, @Assisted("password") String password,
            @Assisted("ipAddress") String ipAddress, @Assisted("port") int port);
}
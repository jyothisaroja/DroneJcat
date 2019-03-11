package com.jcat.cloud.fw.components.model.target;

import com.google.inject.assistedinject.Assisted;

/**
 * Created by eelimei on 2/13/18.
 */
public interface EcsBatCicFactory {
    EcsBatCic create(@Assisted("username") String userName, @Assisted("password") String password, @Assisted("ipAddress") String ipAddress,
                     @Assisted("port") int port);
}

package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * RabbitMQ Server representation used as a resource of a cloud node.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ewubnhn 2013-02-19 First steps
 * @author emulign 2013-03-15 Added TASS constructor and constants {@link #IP}, {@link #PORT}, {@link #PASSWORD},
 *         {@link #USERNAME} and moved {@link #RABBIT}
 * @author esauali 2013-03-29 Moved properties from interface which was removed
 * @author ezhgyin 2013-06-27 update toString() method to use Objects.toStringHelper() instead of
 *         com.jcat.ecs.etc.Logging
 * @author esauali 2013-10-14 change port to be Integer
 * @author ezhgyin 2013-10-09 Updated the constructor used by TASS adapter
 */
public class RabbitMqServerResource implements ConfigurationData {
    /**
     * ID of the resource
     */
    private static final String ID = "id";

    /**
     * Endpoint field defined in the TASS server and/or XML file
     */
    private static final String IP = "ip";

    /**
     * Password field defined in the TASS server and/or XML file
     */
    private static final String PASSWORD = "password";

    /**
     * Port field defined in the TASS server and/or XML file
     */
    private static final String PORT = "port";

    /**
     * User name field defined in the TASS server and/or XML file
     */
    private static final String USERNAME = "username";

    /**
     * Equipment type
     */
    public static final String RABBIT = "Rabbit";

    /**
     * {@link #getId()}
     */
    private String mId;

    /**
     * {@link #get()}
     */
    private String mIp;

    /**
     * {@link #getPassword()}
     */
    private String mPassword;

    /**
     * {@link #getPort()}
     */
    private int mPort;

    /**
     * {@link #getIdentity()}
     */
    private String mUsername;

    /**
     * Constructor used by ConfigurationFacadeAdapter in XML adapter
     *
     * @param xmlConfiguration
     */
    public RabbitMqServerResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getProperty(IP), xmlConfiguration.getProperty(USERNAME), xmlConfiguration
                .getProperty(PASSWORD), parsePort(xmlConfiguration.getProperty(PORT)));
    }

    /**
     * Constructor used by ConfigurationFacadeAdapter in TASS adapter
     *
     * @param id
     * @param ip
     * @param username
     * @param password
     * @param port
     */
    public RabbitMqServerResource(@JsonProperty("ip") String ip, @JsonProperty("username") String username,
            @JsonProperty("password") String password, @JsonProperty("port") int port) {
        Preconditions.checkArgument(!(ip == null || ip.isEmpty()));
        mIp = ip;
        mUsername = username;
        mPassword = password;
        mPort = port;
    }

    /**
     * Helper method to parse port from String
     *
     * @param portString
     * @return
     */
    private static int parsePort(String portString) {
        return Integer.parseInt(portString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * Get ip address eg. "1.2.3.4" *
     *
     * @return
     */
    public String getIp() {
        return mIp;
    }

    /**
     * Get password
     *
     * @return
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Get port
     */
    public int getPort() {
        return mPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * Get username
     *
     * @return
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(IP, mIp).add(PORT, mPort)
                .add(USERNAME, mUsername).add(PASSWORD, mPassword).toString();
    }
}

package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * VcFlex representation used as a resource of a cloud node.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-04-14 initial version
 *
 */
public class VcFlexResource implements ConfigurationData {

    /**
     * ID of the resource
     */
    private static final String ID = "id";

    /**
     * IP field defined in the TASS server and/or XML file
     */
    private static final String IP = "ip";

    /**
     * CLI Port
     */
    private static final String PORT_CLI = "port_cli";

    /**
     * Name of the user with expert permissions
     */
    private static final String USER_NAME = "username";

    /**
     * Password of the user with expert permissions
     */
    private static final String USER_PASSWORD = "password";

    /**
     * Equipment type
     */
    public static final String VCFLEX = "VcFlex";

    private String mId;
    private String mIp;
    private int mPortCli;
    private String mUserName;
    private String mUserPassword;

    /**
     * Constructor used by XML adapter
     *
     * @param xmlConfiguration
     */
    public VcFlexResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(IP), Integer.valueOf(xmlConfiguration
                .getProperty(PORT_CLI)), xmlConfiguration.getProperty(USER_NAME), xmlConfiguration
                .getProperty(USER_PASSWORD));
    }

    /**
     * Constructor used by TASS adapter
     *
     * @param ip
     * @param portCli
     * @param userName
     * @param userPassword
     */
    public VcFlexResource(@JsonProperty("ip") String ip, @JsonProperty("port_cli") int portCli,
            @JsonProperty("username") String userName, @JsonProperty("password") String userPassword) {
        Preconditions.checkArgument(!(ip == null || ip.isEmpty()));
        mIp = ip;
        mPortCli = portCli;
        mUserName = userName;
        mUserPassword = userPassword;
    }

    /**
     * Constructor
     *
     * @param id
     * @param ip
     * @param portCli
     * @param userName
     * @param userPassword
     */
    public VcFlexResource(String id, String ip, int portCli, String userName, String userPassword) {
        mId = id;
        mIp = ip;
        mPortCli = portCli;
        mUserName = userName;
        mUserPassword = userPassword;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * Get IP address
     *
     * @return
     */
    public String getIp() {
        return mIp;
    }

    /**
     * Get Port of CLI
     *
     * @return
     */
    public int getPortCli() {
        return mPortCli;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * Get user name
     *
     * @return
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * Get user password
     *
     * @return
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(IP, mIp).add(PORT_CLI, mPortCli)
                .add(USER_NAME, mUserName).add(USER_PASSWORD, mUserPassword).toString();
    }
}

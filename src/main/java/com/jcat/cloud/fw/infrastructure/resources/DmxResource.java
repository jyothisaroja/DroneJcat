package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * DMX board representation used as a resource of a cloud node.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-01-23 Initial version
 * @author emulign 2013-02-20 Added TASS and modified XML constructors, and created property IP and port_cli
 * @author esauali 2013-05-20 Add expert user credential handling
 * @author ezhgyin 2013-07-03 add toString() method
 * @author ezhgyin 2013-10-09 Updated the constructor used by TASS adapter
 *
 */
public class DmxResource implements ConfigurationData {

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
     * SSH Port
     */
    private static final String PORT_NECONF_SSH = "port_netconf_ssh";

    /**
     * Name of the user with expert permissions
     */
    private static final String USER_NAME_EXPERT = "username_expert";

    /**
     * Password of the user with expert permissions
     */
    private static final String USER_PASSWORD_EXPERT = "password_expert";

    /**
     * Equipment type
     */
    public static final String DMX = "Dmx";

    private String mId;
    private String mIp;
    private int mPortCli;
    private int mPortNetconfSsh;
    private String mUserNameExpert;
    private String mUserPasswordExpert;

    /**
     * Constructor used by XML adapter
     *
     * @param xmlConfiguration
     */
    public DmxResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(IP), Integer.valueOf(xmlConfiguration
                .getProperty(PORT_CLI)), Integer.valueOf(xmlConfiguration.getProperty(PORT_NECONF_SSH)),
                xmlConfiguration.getProperty(USER_NAME_EXPERT), xmlConfiguration.getProperty(USER_PASSWORD_EXPERT));
    }

    /**
     * Constructor used by TASS adapter
     *
     * @param ip
     * @param portCli
     * @param portNetconfSsh
     * @param userNameExpert
     * @param userPasswordExpert
     */
    public DmxResource(@JsonProperty("ip") String ip, @JsonProperty("port_cli") int portCli,
            @JsonProperty("port_netconf_ssh") int portNetconfSsh,
            @JsonProperty("username_expert") String userNameExpert,
            @JsonProperty("password_expert") String userPasswordExpert) {
        Preconditions.checkArgument(!(ip == null || ip.isEmpty()));
        mIp = ip;
        mPortCli = portCli;
        mPortNetconfSsh = portNetconfSsh;
        mUserNameExpert = userNameExpert;
        mUserPasswordExpert = userPasswordExpert;
    }

    /**
     * Constructor
     *
     * @param id
     * @param ip
     * @param portCli
     * @param portNetconfSsh
     * @param userNameExpert
     * @param userPasswordExpert
     */
    public DmxResource(String id, String ip, int portCli, int portNetconfSsh, String userNameExpert,
            String userPasswordExpert) {
        mId = id;
        mIp = ip;
        mPortCli = portCli;
        mPortNetconfSsh = portNetconfSsh;
        mUserNameExpert = userNameExpert;
        mUserPasswordExpert = userPasswordExpert;
    }

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
     * Get Netconf SSH port
     *
     * @return
     */
    public int getPortNetconfSsh() {
        return mPortNetconfSsh;
    }

    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * Get Expert user name
     *
     * @return
     */
    public String getUserNameExpert() {
        return mUserNameExpert;
    }

    /**
     * Get Expert user password
     *
     * @return
     */
    public String getUserPasswordExpert() {
        return mUserPasswordExpert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(IP, mIp).add(PORT_CLI, mPortCli)
                .add(PORT_NECONF_SSH, mPortNetconfSsh).add(USER_NAME_EXPERT, mUserNameExpert)
                .add(USER_PASSWORD_EXPERT, mUserPasswordExpert).toString();
    }
}

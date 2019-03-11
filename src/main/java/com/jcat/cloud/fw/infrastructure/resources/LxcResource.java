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
 * @author esauali 2013-11-18 Initial version
 *
 */
public class LxcResource implements ConfigurationData {
    private static final String IP = "ip";
    private static final String SSH_PASSWORD = "ssh_password";
    private static final String SSH_PORT = "ssh_port";
    private static final String SSH_USERNAME = "ssh_user";
    public static final String RESOURCE_ID = "Lxc";
    @JsonProperty(IP)
    private String mIp;
    @JsonProperty(SSH_PASSWORD)
    private String mSshPassword;
    @JsonProperty(SSH_PORT)
    private int mSshPort;
    @JsonProperty(SSH_USERNAME)
    private String mSshUserName;

    /**
     * Constructor for XMLAdapter
     *
     * @param xmlConfiguration
     */
    public LxcResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getProperty(IP), xmlConfiguration.getProperty(SSH_PORT), xmlConfiguration
                .getProperty(SSH_USERNAME), xmlConfiguration.getProperty(SSH_PASSWORD));
    }

    /**
     * Constructor for TASS
     *
     * @param consolesJson
     */
    public LxcResource(@JsonProperty(IP) String sshIp, @JsonProperty(SSH_PORT) String sshPort,
            @JsonProperty(SSH_USERNAME) String sshUserName, @JsonProperty(SSH_PASSWORD) String sshPassword) {
        Preconditions.checkArgument(!(sshIp == null || sshIp.isEmpty()));
        Preconditions.checkArgument(!(sshPort == null || sshPort.isEmpty()));
        Preconditions.checkArgument(!(sshUserName == null || sshUserName.isEmpty()));
        Preconditions.checkArgument(!(sshPassword == null || sshPassword.isEmpty()));
        mIp = sshIp;
        mSshPort = Integer.parseInt(sshPort);
        mSshUserName = sshUserName;
        mSshPassword = sshPassword;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * Get SSH IP
     *
     * @return
     */
    public String getIp() {
        return mIp;
    }

    /**
     * Get SSH Password
     *
     * @return
     */
    public String getSshPassword() {
        return mSshPassword;
    }

    /**
     * Get SSH port number
     *
     * @return
     */
    public int getSshPort() {
        return mSshPort;
    }

    /**
     * Get SSH user name
     *
     * @return
     */
    public String getSshUserName() {
        return mSshUserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(IP, mIp).add(SSH_PORT, mSshPort)
                .add(SSH_USERNAME, mSshUserName).toString();
    }
}

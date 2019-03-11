package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Resource describing EMC storage
 *
 * @author esauali 2013-11-18 Initial version
 *
 */
public class EmcResource implements ConfigurationData {
    private static final String PROPERTY_IP_SPA = "ip_spa";
    private static final String PROPERTY_IP_SPB = "ip_spb";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_USERNAME = "username";
    public static final String RESOURCE_ID = "Emc";
    @JsonProperty(PROPERTY_IP_SPA)
    private String mIpSpa;
    @JsonProperty(PROPERTY_IP_SPB)
    private String mIpSpb;
    @JsonProperty(PROPERTY_PASSWORD)
    private String mPassword;
    @JsonProperty(PROPERTY_USERNAME)
    private String mUsername;

    /**
     * Constructor for XMLAdapter
     *
     * @param xmlConfiguration
     */
    public EmcResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getProperty(PROPERTY_IP_SPA), xmlConfiguration.getProperty(PROPERTY_IP_SPB),
                xmlConfiguration.getProperty(PROPERTY_USERNAME), xmlConfiguration.getProperty(PROPERTY_PASSWORD));
    }

    /**
     * Constructor for TASS
     *
     * @param consolesJson
     */
    public EmcResource(@JsonProperty(PROPERTY_IP_SPA) String ipSpa, @JsonProperty(PROPERTY_IP_SPB) String ipSpb,
            @JsonProperty(PROPERTY_USERNAME) String username, @JsonProperty(PROPERTY_PASSWORD) String password) {
        Preconditions.checkArgument(!(ipSpa == null || ipSpa.isEmpty()));
        Preconditions.checkArgument(!(ipSpb == null || ipSpb.isEmpty()));
        Preconditions.checkArgument(!(username == null || username.isEmpty()));
        Preconditions.checkArgument(!(password == null || password.isEmpty()));
        mIpSpa = ipSpa;
        mIpSpb = ipSpb;
        mUsername = username;
        mPassword = password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * @return the ipSpa
     */
    public String getIpSpa() {
        return mIpSpa;
    }

    /**
     * @return the ipSpb
     */
    public String getIpSpb() {
        return mIpSpb;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(PROPERTY_IP_SPA, mIpSpa).add(PROPERTY_IP_SPB, mIpSpb)
                .add(PROPERTY_USERNAME, mUsername).toString();
    }

}

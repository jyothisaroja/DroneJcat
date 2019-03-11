package com.jcat.cloud.fw.infrastructure.resources;

import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;

/**
 * Represents the Fuel instance
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014,2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eolnans 2014-11-13 initial version
 */
public class FuelResource implements ConfigurationData {
    /**
     * Equipment type for resource manager use.
     */
    public static final String FUEL = "Fuel";
    public static final String IP_PUBLIC = "ip_public";
    public static final String KEYSTONE_PORT_PUBLIC = "keystone_port_public";
    public static final String FUEL_PORT_PUBLIC = "fuel_port_public";
    public static final String FUEL_SSH_PORT_PUBLIC = "ssh_port_public";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    private static final String TASS_REF = "TASS";
    private static final String KEYSTONE_SERVICE_ENDPOINT = "v3";
    private static final String FUEL_SERVICE_ENDPOINT = "api/v1";

    private final String mId;
    private final String mIp_public;
    private final int mKeystone_port;
    private final int mFuel_port;
    private final int mFuel_ssh_port;
    private final String mUser;
    private final String mPassword;
    private final URL mKeystoneUrl;
    private final URL mFuelUrl;

    /**
     * Main constructor
     *
     * @param id
     *            String Resource ID
     * @param ip_public
     *            String the public IP or hostname of Fuel
     * @param keystone_port
     *            String the port on the public IP that will reach Fuel's
     *            Keystone instance
     * @param fuel_port
     *            String the port on the public IP that will reach Fuel's API
     * @throws MalformedURLException
     */
    public FuelResource(String id, String ip_public, String keystone_port, String fuel_port, String fuel_ssh_port,
            String user, String password) throws MalformedURLException {
        mId = id;
        mIp_public = ip_public;
        mKeystone_port = Integer.parseInt(keystone_port);
        mFuel_port = Integer.parseInt(fuel_port);
        mFuel_ssh_port = Integer.parseInt(fuel_ssh_port);
        mUser = user;
        mPassword = password;
        mKeystoneUrl = new URL("http://" + mIp_public + ":" + mKeystone_port + "/" + KEYSTONE_SERVICE_ENDPOINT + "/");
        mFuelUrl = new URL("http://" + mIp_public + ":" + mFuel_port + "/" + FUEL_SERVICE_ENDPOINT + "/");
    }

    /**
     * Constructor used by ConfigurationFacadeAdapter in Xml adapter
     *
     * @param xmlConfiguration
     *            {@link ResourceElement}
     * @throws MalformedURLException
     */
    public FuelResource(ResourceElement xmlConfiguration) throws MalformedURLException {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(IP_PUBLIC), xmlConfiguration
                .getProperty(KEYSTONE_PORT_PUBLIC), xmlConfiguration.getProperty(FUEL_PORT_PUBLIC), xmlConfiguration
                .getProperty(FUEL_SSH_PORT_PUBLIC), xmlConfiguration.getProperty(USER), xmlConfiguration
                .getProperty(PASSWORD));
    }

    /**
     * TASS Constructor.
     *
     * @param id
     *            String Resource ID
     * @param ip_public
     *            String the public IP or hostname of Fuel
     * @param keystone_port
     *            String the port on the public IP that will reach Fuel's
     *            Keystone instance
     * @param fuel_port
     *            String the port on the public IP that will reach Fuel's API
     * @throws MalformedURLException
     */
    public FuelResource(@JsonProperty(IP_PUBLIC) String ip_public,
            @JsonProperty(KEYSTONE_PORT_PUBLIC) String keystone_port, @JsonProperty(FUEL_PORT_PUBLIC) String fuel_port,
            @JsonProperty(FUEL_SSH_PORT_PUBLIC) String fuel_ssh_port, @JsonProperty(USER) String user,
            @JsonProperty(PASSWORD) String password) throws MalformedURLException {
        this(TASS_REF, ip_public, keystone_port, fuel_port, fuel_ssh_port, user, password);
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * @return String the public IP or address of the Fuel node or forwarding
     *         towards the Fuel node
     */
    public String getIpPublic() {
        return mIp_public;
    }

    /**
     * @return int the port in the public IP/address where Fuel's Keystone
     *         instance can be reached
     */
    public int getKeystonePublicPort() {
        return mKeystone_port;
    }

    /**
     * @return int the port in the public IP/address where Fuel's API can be
     *         reached
     */
    public int getFuelPublicPort() {
        return mFuel_port;
    }

    /**
     * @return int the port in the public IP/address Fuel can be reached through
     *         SSH
     */
    public int getFuelPublicSshPort() {
        return mFuel_ssh_port;
    }

    public String getUser() {
        return mUser;
    }

    public String getPassword() {
        return mPassword;
    }

    /**
     * @return URL the combined endpoint URl of the Keystone sericve on Fuel
     */
    public URL getKeystoneUrl() {
        return mKeystoneUrl;
    }

    /**
     * @return URL the combined endpoint URl of the Fuel API
     */
    public URL getFuelUrl() {
        return mFuelUrl;
    }
}

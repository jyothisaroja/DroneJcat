package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.ericsson.tass.resources.MultipleEntryResource;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents the extreme switch device.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-05-13 initial version
 * @author ezhgyin 2013-07-03 add toString() method
 * @author ezhgyin 2013-10-09 Updated the constructor used by TASS adapter
 * @author ezhgyin 2014-05-16 implements MultipleExtryResource interface which indicates multiple
 *         extreme switch resources may present in an STP
 */
public class ExtremeSwitchResource implements ConfigurationData, MultipleEntryResource {
    /**
     * ID of the resource
     */
    private static final String ID = "id";
    /**
     * Represents the ip field
     */
    private static final String IP_ADDRESS = "ip";

    /**
     * Represents the password field
     */
    private static final String PASSWORD = "password";

    /**
     * Represents the user name field
     */
    private static final String USERNAME = "username";

    /**
     * Represents the http port field
     */
    public static final String HTTP_PORT = "http_port";

    /**
     * Represents the console server ip field
     */
    public static final String CONSOLE_SERVER_IP = "console_server_ip";

    /**
     * Represents the console server port field
     */
    public static final String CONSOLE_SERVER_PORT = "console_server_port";

    private String mConsoleServerIp;
    private int mConsoleServerPort;
    private String mId;
    private String mIp;
    private String mPassword;
    private String mUserName;
    private String mHttpPort;

    /**
     * Constructor used by ConfigurationFacadeAdapter in Xml adapter
     *
     * @param xmlConfiguration
     */
    public ExtremeSwitchResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(IP_ADDRESS),
                xmlConfiguration.getProperty(USERNAME), xmlConfiguration.getProperty(PASSWORD), xmlConfiguration
                        .getProperty(HTTP_PORT), xmlConfiguration.getProperty(CONSOLE_SERVER_IP), xmlConfiguration
                        .getProperty(CONSOLE_SERVER_PORT));
    }

    /**
     * Constructor used by ConfigurationFacadeAdapter in TASS adapter
     *
     * @param ip
     * @param userName
     * @param password
     * @param httpPort
     * @param consoleServerIp
     * @param consoleServerPort
     */
    public ExtremeSwitchResource(@JsonProperty("ip") String ip, @JsonProperty("username") String userName,
            @JsonProperty("password") String password, @JsonProperty("http_port") String httpPort,
            @JsonProperty("console_server_ip") String consoleServerIp,
            @JsonProperty("console_server_port") String consoleServerPort) {
        this(null, ip, userName, password, httpPort, consoleServerIp, consoleServerPort);
    }

    /**
     * Main constructor
     *
     * @param id
     * @param ip
     * @param userName
     * @param password
     * @param consoleServerIp
     * @param consoleServerPort
     */
    public ExtremeSwitchResource(String id, String ip, String userName, String password, String httpPort,
            String consoleServerIp, String consoleServerPort) {
        Preconditions.checkArgument(!(ip == null || ip.isEmpty()));
        mId = id;
        mIp = ip;
        mUserName = userName;
        mPassword = password;
        mHttpPort = httpPort;
        mConsoleServerIp = emptyToNull(consoleServerIp);
        if (null != consoleServerPort && !consoleServerPort.equals("")) {
            mConsoleServerPort = Integer.parseInt(consoleServerPort);
        }
    }

    /**
     * Helper method to convert empty strings to null
     *
     * @param value
     * @return
     */
    private String emptyToNull(String value) {
        return "".equals(value) ? null : value;
    }

    /**
     * @return mConsoleServerIp
     */
    public String getConsoleServerIp() {
        if ("".equals(mConsoleServerIp)) {
            mConsoleServerIp = null;
        }
        return mConsoleServerIp;
    }

    /**
     * @return mConsoleServerPort
     */
    public int getConsoleServerPort() {
        return mConsoleServerPort;
    }

    /**
     * @return mHttpPort
     */
    public String getHttpPort() {
        return mHttpPort;
    }

    /**
     * @return the mId
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return the mIp
     */
    public String getIp() {
        return mIp;
    }

    /**
     * @return mPassword
     */
    public String getPassword() {
        if (null == mPassword) {
            mPassword = "";
        }
        return mPassword;
    }

    /**
     * @return {@link ExtremeSwitchResource}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return ExtremeSwitchResource.class;
    }

    /**
     * @return the mUserName
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(IP_ADDRESS, mIp).add(USERNAME, mUserName)
                .add(PASSWORD, mPassword).add(HTTP_PORT, mHttpPort).add(CONSOLE_SERVER_IP, mConsoleServerIp)
                .add(CONSOLE_SERVER_PORT, mConsoleServerPort).toString();
    }
}

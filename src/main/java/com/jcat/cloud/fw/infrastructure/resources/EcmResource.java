package com.jcat.cloud.fw.infrastructure.resources;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

/**
 * ECM Resource class.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ewubnhn 2013-07-01 Initial version
 * @author ewubnhn 2013-07-10 Restructured ECM resource
 * @author ewubnhn 2013-08-09 Change from Tenant to EcmUser
 * @author ewubnhn 2013-09-30 Fixed bug introduced by Sonar fixes (GSON deserialization)
 * @author ewubnhn 2013-10-14 Fixed bug in tenant setter
 * @author ewubnhn 2013-10-17 Added @JsonIgnore to setters in EcmResource (confused JSON parser)
 * @author ewubnhn 2014-03-04 Added back end host and granite URL
 * @author ewubnhn 2014-07-17 Quick SSL solution
 */
public class EcmResource implements ConfigurationData {

    /**
     * Class to hold username/password/tenant triplets available on this resource.
     *
     */
    public class EcmUser {
        @SerializedName("identity")
        private String mIdentity;
        @SerializedName("password")
        private String mPassword;
        @SerializedName("type")
        private EcmUserType mType;

        /**
         * Get credentials string for this user
         *
         * @return String ("username:password")
         */
        public String getCredentials() {
            return mIdentity.split(":")[1] + ":" + mPassword;
        }

        /**
         * Get identity string for this user
         *
         * @return String ("tenantName:username")
         */
        public String getIdentity() {
            return mIdentity;
        }

        /**
         * Get user password.
         *
         * @return String tenant password
         */
        public String getPassword() {
            return mPassword;
        }

        /**
         * Get tenant name.
         *
         * @return String tenant name
         */
        public String getTenant() {
            return mIdentity.split(":")[0];
        }

        /**
         * Get username
         *
         * @return String username
         */
        public String getUsername() {
            return mIdentity.split(":")[1];
        }

        /**
         * Get user type
         *
         * @return {@link EcmUserType}
         */
        public EcmUserType getUserType() {
            return mType;
        }

        /**
         * Set password.
         *
         * @param password String password
         */
        public void setPassword(String password) {
            mPassword = password;
        }

        /**
         * Set tenant name
         *
         * @param tenantName String Tenant name
         */
        public void setTenantName(String tenantName) {
            if (null != mIdentity) {
                mIdentity = tenantName + ":" + mIdentity.split(":")[1];
            }
        }

        /**
         * Set username.
         *
         * @param username String username
         */
        public void setUsername(String username) {
            mIdentity = mIdentity.split(":")[0] + ":" + username;
        }

        /**
         * Set user type
         *
         * @param userType {@link EcmUserType} type to set
         */
        public void setUserType(EcmUserType userType) {
            mType = userType;
        }
    }

    /**
     * This ENUM contains the classes of users for ECM.
     *
     */
    public enum EcmUserType {
        ADMIN, TENANT_ADMIN, USER
    }

    private static final String ECM_SERVICE_ENDPOINT = "ecm_service";
    private static final String GRANITE_PORT = "7280";
    private static final String STANDARD_SSL_PORT = "443";
    private static final String TASS_REF = "TASS";
    public static final String BE_HOSTNAME = "backEndHost";

    /**
     * Equipment type for resource manager use.
     */
    public static final String ECM = "Ecm";
    public static final String ECM_USERS = "users";
    public static final String FE_HOSTNAME = "frontEndHost";
    public static final String REST_PORT = "restPort";

    private List<EcmUser> mAssociatedUsers = new ArrayList<EcmUser>();
    private String mBackEndHost;
    private URL mBaseUrl;
    private EcmUser mCurrentUser;
    private String mFrontEndHost;
    private URL mGraniteUrl;
    private String mId;
    private Logger mLogger = Logger.getLogger(EcmResource.class);
    private String mRestPort;

    /**
     * Constructor used by ConfigurationFacadeAdapter in Xml adapter
     *
     * @param xmlConfiguration {@link ResourceElement}
     * @throws MalformedURLException
     * @throws IllegalArgumentException - if the tenant list or controller information is null or empty
     */
    public EcmResource(ResourceElement xmlConfiguration) throws MalformedURLException {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(FE_HOSTNAME), xmlConfiguration
                .getProperty(BE_HOSTNAME), xmlConfiguration.getProperty(REST_PORT), xmlConfiguration
                .getProperty(ECM_USERS));
    }

    /**
     * TASS Constructor.
     *
     * @param feHost String Front end host name
     * @param beHost String Back end host name
     * @param restPort String Port for ECM REST service - SSL used w/ port 443
     * @param userList String JSON array containing user info
     *            [{"identity":"tenant:user","password":"thepassword"},...]
     * @throws MalformedURLException
     */
    public EcmResource(@JsonProperty(FE_HOSTNAME) String feHost, @JsonProperty(BE_HOSTNAME) String beHost,
            @JsonProperty(REST_PORT) String restPort, @JsonProperty(ECM_USERS) String userList)
            throws MalformedURLException {
        this(TASS_REF, feHost, beHost, restPort, userList);
    }

    /**
     * Main constructor.
     *
     * @param id String Resource ID
     * @param feHost String Host name - front end
     * @param beHost String Host name - back end
     * @param restPort String Port for ECM REST service - SSL used w/ port 443
     * @param userList String JSON array containing user info
     *            [{"identity":"tenant:user","password":"thepassword"},...]
     * @throws MalformedURLException
     */
    public EcmResource(String id, String feHost, String beHost, String restPort, String userList)
            throws MalformedURLException {
        mId = id;
        mFrontEndHost = feHost;
        mBackEndHost = beHost;
        mRestPort = restPort;
        String url_head;
        if (STANDARD_SSL_PORT.equals(mRestPort)) {
            url_head = "https://";
        } else {
            url_head = "http://";
        }
        initUsers(userList);
        setUserByType(EcmUserType.ADMIN);
        mBaseUrl = new URL(url_head + mFrontEndHost + ":" + mRestPort + "/" + ECM_SERVICE_ENDPOINT + "/");
        mGraniteUrl = new URL(url_head + mBackEndHost + ":" + GRANITE_PORT);
    }

    /**
     * Helper method to initialize the list of users
     *
     * @param userList String JSON array containing user info
     *            [{"identity":"tenant:user","password":"thepassword"},...]
     * @throws IllegalArgumentException - if user information is null or empty
     */
    private void initUsers(String userList) {
        // This will throw an IllegalArgumentException if the preconditions are not covered
        Preconditions.checkArgument(!(userList == null || userList.isEmpty()), "The ECM user list is null or empty");
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonArray arrayOfUsers = parser.parse(userList).getAsJsonArray();

        for (JsonElement user : arrayOfUsers) {
            mAssociatedUsers.add(gson.fromJson(user, EcmUser.class));
        }
    }

    /**
     * Set user by the user type (first matching).<br />
     * <b>Note:</b> null value will be set if no user with matching type is found.
     *
     * @param userType {@link EcmUserType} Desired type of user
     */
    private void setUserByType(EcmUserType userType) {
        // Reset user to null
        mCurrentUser = null;
        for (EcmUser user : mAssociatedUsers) {
            if (userType == user.getUserType()) {
                mCurrentUser = user;
                break;
            }
        }
        if (null == mCurrentUser) {
            mLogger.warn("No available ECM user matching type: " + userType);
        } else {
            mLogger.info("ECM user set as: " + mCurrentUser.getTenant());
        }
    }

    /**
     * Find a user by its username.
     *
     * @param String User name for desired {@link EcmUser}
     * @return {@link EcmUser} Requested user or null if not found
     */
    public EcmUser findUser(String userName) {
        EcmUser requestedUser = null;
        for (EcmUser user : mAssociatedUsers) {
            if (userName.equals(user.getUsername())) {
                requestedUser = user;
                break;
            }
        }
        return requestedUser;
    }

    /**
     * Get ECM back end hostname.
     *
     * @return String ECM hostname - back end
     */
    public String getBackEndHost() {
        return mBackEndHost;
    }

    /**
     * Gets the base URL for ECM (http://host:port/ecm_service/)
     *
     * @return {@link URL} The base URL for ECM service
     */
    public URL getBaseUrl() {
        return mBaseUrl;
    }

    /**
     * Returns the currently selected user.<br />
     * <b>Note:</b> By default, an {@link EcmUserType#ADMIN} type user is selected.
     *
     * @return {@link EcmUser}
     */
    public EcmUser getCurrentUser() {
        return mCurrentUser;
    }

    /**
     * Get ECM front end hostname.
     *
     * @return String ECM hostname - front end
     */
    public String getFrontEndHost() {
        return mFrontEndHost;
    }

    /**
     * Gets the Granite URL for ECM backend
     *
     * @return {@link URL} The URL for Granite
     */
    public URL getGraniteUrl() {
        return mGraniteUrl;
    }

    /**
     * Returns the ID for this resource.
     *
     * @return String ID for this resource
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * Get the ECM REST port.
     *
     * @return String ECM REST service port
     */
    public String getRestPort() {
        return mRestPort;
    }

    /**
     * Returns the type for this class.
     *
     * @return Class type
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * Set the current user. Use findUser(userName) to locate the user first.
     *
     * @param user {@link EcmUser} User to set
     * @throws IllegalArgumentException
     */
    @JsonIgnore
    public void setUser(EcmUser user) {
        mCurrentUser = user;
        if (null == mCurrentUser) {
            throw new IllegalArgumentException("Null value not allowed for setUser");
        }
    }

    /**
     * Set {@link EcmUser} by the user name.<br />
     * <b>Note:</b> null value will be set if no user with matching user name is found.<br />
     * <b>Deprecation:</b> Please use setUser(EcmUser) instead.
     *
     * @param userName String User name
     */
    @Deprecated
    @JsonIgnore
    public void setUser(String userName) {
        mCurrentUser = findUser(userName);
        if (null == mCurrentUser) {
            throw new IllegalArgumentException("No available ECM user matching user name: " + userName);
        }
    }
}

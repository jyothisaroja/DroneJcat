package com.jcat.cloud.fw.infrastructure.resources;

import java.io.IOException;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jcat.cloud.fw.infrastructure.modules.ResourceModule;

/**
 * Openstack representation used as a resource of a cloud node.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-01-15 First steps
 * @author emulign 2013-01-23 Initial version
 * @author emulign 2013-02-19 Added tass contructor and constants, {@link #OPENSTACK_IDENTITY},
 *         {@link #OPENSTACK_PASSWORD}
 * @author esauali 2013-03-29 Moved properties from interface which was removed
 * @author emulign 2013-04-16 Included controller information and updated fields to match to the new data
 *         structure. Removed notation as it is defined on the {@link ResourceModule}.
 * @author ezhgyin 2013-06-27 update toString() method to use Objects.toStringHelper() instead of
 *         com.jcat.ecs.etc.Logging
 * @author emulign 2013-08-26 created compute nodes, added more information about internal and public ports and ips.
 * @author emulign 2013-09-11 Fixed bug on getEndpoint()
 * @author esauali 2013-09-19 Fix port to be int and not string
 * @author ezhgyin 2013-10-09 Updated the constructor used by TASS adapter
 * @author egbonag 2013-11-20 ssh_port_public added to compute_nodes list
 * @author ehosmol 2014-02-12 change the ComputeNode implementation, Add {@link #IP_INTERNAL_CIC1},
 *         {@link #IP_INTERNAL_CIC2}
 *
 */
public class OpenStackResource implements ConfigurationData {

    /**
     * Computer node entity. It is generic and same for all the compute nodes (blades) exist.
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2013
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author emulign 2013-08-26 initial version
     * @author egbonag 2013-11-20 add ssh_port_public
     * @author ehosmol 2014-02-12 change the ComputeNode implementation
     * @author ezhgyin 2015-02-24 add getTenant and getUser
     *
     */
    public static class ComputeNode {

        /**
         * Compute node user name field defined in the TASS server and/or XML file
         */
        private static final String SSH_PASSWORD = "ssh_password";

        /**
         * Compute node user name field defined in the TASS server and/or XML file
         */
        private static final String SSH_USER = "ssh_user";

        /**
         * {@link #getComputeNodeSshPassword()}
         */
        @JsonProperty(SSH_PASSWORD)
        private String mSshPassword;

        /**
         * {@link #getComputeNodeSshUserName()}
         */
        @JsonProperty(SSH_USER)
        private String mSshUserName;

        /**
         * Compute node constructor
         */
        public ComputeNode() {
        }

        /**
         * @return the computeNodeSshPassword
         */
        public String getComputeNodeSshPassword() {
            return mSshPassword;
        }

        /**
         * @return the computeNodeSshUserName
         */
        public String getComputeNodeSshUserName() {
            return mSshUserName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Objects.toStringHelper(this).omitNullValues().add(SSH_USER, mSshUserName)
                    .add(SSH_PASSWORD, mSshPassword).toString();
        }
    }

    /**
     * Controller entity.
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2013
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author emulign 2013-08-26 initial version
     *
     */
    public static class Controller {

        /**
         * Controller's password field defined in the TASS server and/or XML file
         */
        private static final String CONTROLLER_PASSWORD = "password";

        /**
         * Controller's user name field defined in the TASS server and/or XML file
         */
        private static final String CONTROLLER_USERNAME = "username";

        /**
         * Contains the controller password
         */
        @JsonProperty(CONTROLLER_PASSWORD)
        private String mPassword;

        /**
         * Contains the controller user name
         */
        @JsonProperty(CONTROLLER_USERNAME)
        private String mUserName;

        /**
         * Constructor for the controller. This credentials will be used for opening a SSH connection.
         */
        public Controller() {
        }

        /**
         * @return {@link #mPassword}
         */
        public String getPassword() {
            return mPassword;
        }

        /**
         * @return {@link #mUserName}
         */
        public String getUserName() {
            return mUserName;
        }

        /**
         * Set {@link #mPassword}
         *
         * @param password
         */
        public void setPassword(String password) {
            mPassword = password;
        }

        /**
         * Set {@link #mUserName}
         *
         * @param userName
         */
        public void setUserName(String userName) {
            mUserName = userName;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).omitNullValues().add(CONTROLLER_USERNAME, mUserName)
                    .add(CONTROLLER_PASSWORD, mPassword).toString();
        }
    }

    /**
     * Compute node field defined in the TASS server and/or XML file
     */
    private static final String COMPUTE_NODE = "compute_node";

    /**
     * Controller field defined in the TASS server and/or XML file
     */
    private static final String CONTROLLER = "controller";

    /**
     * ID of the resource
     */
    private static final String ID = "id";

    /**
     * Internal IP Address field defined in the TASS server and/or XML file
     */
    private static final String IP_INTERNAL_CIC1 = "ip_internal_cic1";

    /**
     * Internal IP Address field defined in the TASS server and/or XML file
     */
    private static final String IP_INTERNAL_CIC2 = "ip_internal_cic2";

    /**
     * Public IP Address field defined in the TASS server and/or XML file (LXC container)
     */
    private static final String IP_PUBLIC = "ip_public";

    /**
     * Keystone internal port (inside LXC container)
     */
    private static final String KEYSTONE_PORT_INTERNAL = "keystone_port_internal";

    /**
     * Keystone public port (outside LXC container)
     */
    private static final String KEYSTONE_PORT_PUBLIC = "keystone_port_public";

    /**
     * Tenants list field defined in the TASS server and/or XML file
     */
    private static final String LIST_TENANTS = "tenants";

    /**
     * Identity field defined in the TASS server and/or XML file
     */
    private static final String OPENSTACK_IDENTITY = "identity";

    /**
     * Password field defined in the TASS server and/or XML file
     */
    private static final String OPENSTACK_PASSWORD = "password";

    /**
     * SSH internal port (inside LXC container)
     */
    private static final String SSH_PORT_INTERNAL = "ssh_port_internal";

    /**
     * SSH public port (outside LXC container)
     */
    private static final String SSH_PORT_PUBLIC = "ssh_port_public";

    /**
     * Equipment type
     */
    public static final String OPENSTACK = "OpenStack";

    /**
     * {@link #getComputeNodes()}
     */
    private ComputeNode mComputeNode;

    /**
     * {@link #getController()}
     */
    private Controller mController;

    /**
     * {@link #getId()}
     */
    private String mId;

    /**
     * {@link #getIdentity()}
     */
    private String mIdentity;

    /**
     * {@link #getTenant()}}
     */
    private String mTenant;

    /**
     * {@link #getUser()}}
     */
    private String mUser;

    /**
     * {@link #getIpInternalCic1()}
     */
    private final String mIpInternalCic1;

    /**
     * {@link #getIpInternalCic2()}
     */
    private final String mIpInternalCic2;

    /**
     * {@link #getIpPublic()}
     */
    private String mIpPublic;

    /**
     * {@link #getKeystonePortInternal()}
     */
    private final int mKeystonePortInternal;

    /**
     * {@link #getKeystonePortPublic()}
     */
    private int mKeystonePortPublic;

    /**
     * {@link #getPassword()}
     */
    private String mPassword;

    /**
     * {@link #getSshPortInternal()}
     */
    private final int mSshPortInternal;

    /**
     * {@link #getSshPortPublic()}
     */
    private final int mSshPortPublic;

    /**
     * Indicates the API version used - by default equals to "v3"
     */
    private String mVersion = "v3";

    /**
     * Constructor used by ConfigurationFacadeAdapter in Xml adapter
     *
     * @param xmlConfiguration
     * @throws IllegalArgumentException - if the tenant list or controller information is null or empty
     */
    public OpenStackResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(IP_PUBLIC), xmlConfiguration
                .getProperty(IP_INTERNAL_CIC1), xmlConfiguration.getProperty(IP_INTERNAL_CIC2), xmlConfiguration
                .getProperty(KEYSTONE_PORT_PUBLIC), xmlConfiguration.getProperty(KEYSTONE_PORT_INTERNAL),
                xmlConfiguration.getProperty(SSH_PORT_PUBLIC), xmlConfiguration.getProperty(SSH_PORT_INTERNAL),
                xmlConfiguration.getProperty(LIST_TENANTS), xmlConfiguration.getProperty(CONTROLLER), xmlConfiguration
                .getProperty(COMPUTE_NODE));
    }

    /**
     * Constructor used by TASS
     *
     * @param ipPublic
     * @param keystonePortPublic
     * @param keystonePortInternal
     * @param sshPortPublic
     * @param sshPortInternal
     * @param tenantList
     * @param controller
     * @param computeNode
     */
    public OpenStackResource(@JsonProperty(IP_PUBLIC) String ipPublic,
            @JsonProperty(IP_INTERNAL_CIC1) String ipInternalCic1,
            @JsonProperty(IP_INTERNAL_CIC2) String ipInternalCic2,
            @JsonProperty(KEYSTONE_PORT_PUBLIC) String keystonePortPublic,
            @JsonProperty(KEYSTONE_PORT_INTERNAL) String keystonePortInternal,
            @JsonProperty(SSH_PORT_PUBLIC) String sshPortPublic,
            @JsonProperty(SSH_PORT_INTERNAL) String sshPortInternal, @JsonProperty(LIST_TENANTS) String tenantList,
            @JsonProperty(CONTROLLER) String controller, @JsonProperty(COMPUTE_NODE) String computeNode) {
        Preconditions.checkArgument(!(ipPublic == null || ipPublic.isEmpty()));
        Preconditions.checkArgument(!(keystonePortPublic == null || keystonePortPublic.isEmpty()));
        mIpPublic = ipPublic;
        mIpInternalCic1 = ipInternalCic1;
        mIpInternalCic2 = ipInternalCic2;
        mKeystonePortPublic = Integer.parseInt(keystonePortPublic);
        mKeystonePortInternal = Integer.parseInt(keystonePortInternal);
        mSshPortPublic = Integer.parseInt(sshPortPublic);
        mSshPortInternal = Integer.parseInt(sshPortInternal);
        initTenants(tenantList);
        initController(controller);
        initComputeNodes(computeNode);
    }

    /**
     * Main constructor
     *
     * @param id
     * @param ipPublic - CIC ip address outside the LXC
     * @param keystonePortPublic - keystone port outside the LXC
     * @param keystonePortInternal - keystone port inside the LXC
     * @param tenantList
     * @param controller - Controller's information to ssh it (Json string)
     * @param computeNode - Compute node information to ssh it
     * @throws IllegalArgumentException - if the tenant list or controller information is null or empty
     */
    public OpenStackResource(String id, String ipPublic, String ipInternalCic1, String ipInternalCic2,
            String keystonePortPublic, String keystonePortInternal, String sshPortPublic, String sshPortInternal,
            String tenantList, String controller, String computeNode) {
        mId = id;
        mIpPublic = ipPublic;
        mIpInternalCic1 = ipInternalCic1;
        mIpInternalCic2 = ipInternalCic2;
        mKeystonePortPublic = Integer.parseInt(keystonePortPublic);
        mKeystonePortInternal = Integer.parseInt(keystonePortInternal);
        mSshPortPublic = Integer.parseInt(sshPortPublic);
        mSshPortInternal = Integer.parseInt(sshPortInternal);
        initTenants(tenantList);
        initController(controller);
        initComputeNodes(computeNode);
    }

    /**
     * Helper method to initialize the compute node instances.
     *
     * @param computeNodeJson
     * @throws IllegalArgumentException - if controller information is null or empty
     */
    private void initComputeNodes(String computeNodeJson) {
        // It will throw an IllegalArgumentException if the preconditions are not covered
        Preconditions.checkArgument(!(computeNodeJson == null || computeNodeJson.isEmpty()),
                "The compute node information is null or empty");
        try {
            mComputeNode = new ObjectMapper().readValue(computeNodeJson, OpenStackResource.ComputeNode.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("The compute node information is corrupted", e);
        }
    }

    /**
     * Helper method to initialize the controller's information
     *
     * @param controllerJson
     * @throws IllegalArgumentException - if controller information is null or empty
     */
    private void initController(String controllerJson) {
        // It will throw an IllegalArgumentException if the preconditions are not covered
        Preconditions.checkArgument(!(controllerJson == null || controllerJson.isEmpty()),
                "The controller is null or empty");
        try {
            mController = new ObjectMapper().readValue(controllerJson, OpenStackResource.Controller.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("The controller's information is corrupted", e);
        }
    }

    /**
     * Helper method to init the list of tenants
     *
     * @param tenants
     * @throws IllegalArgumentException - if tenant information is null or empty
     */
    private void initTenants(String tenants) {
        // It will throw an IllegalArgumentException if the preconditions are not covered
        Preconditions.checkArgument(!(tenants == null || tenants.isEmpty()), "The tenants list is null or empty");
        JsonObject tenantsJson = (JsonObject) new JsonParser().parse(tenants);
        mIdentity = tenantsJson.get(OPENSTACK_IDENTITY).getAsString();
        mPassword = tenantsJson.get(OPENSTACK_PASSWORD).getAsString();
        Integer separatorPos = mIdentity.indexOf(':');
        mTenant = mIdentity.substring(0, separatorPos);
        mUser = mIdentity.substring(separatorPos + 1, mIdentity.length());
    }

    protected void setComputeNode(ComputeNode computeNode) {
        mComputeNode = computeNode;
    }

    protected void setController(Controller controller) {
        mController = controller;
    }

    /**
     * @param ipPublic the ipPublic to set
     */
    protected void setIpPublic(String ipPublic) {
        mIpPublic = ipPublic;
    }

    /**
     * @param keystonePortPublic the keystonePortPublic to set
     */
    protected void setKeystonePortPublic(int keystonePortPublic) {
        mKeystonePortPublic = keystonePortPublic;
    }

    /**
     * @return the mComputeNode
     */
    public ComputeNode getComputeNode() {
        return mComputeNode;
    }

    /**
     * @return the mController
     */
    public Controller getController() {
        return mController;
    }

    /**
     * Get end point URL eg. "http://192.168.2.109:5000/v2.0/"
     *
     * @return
     */
    public String getEndpoint() {
        return "http://" + mIpPublic + ":" + mKeystonePortPublic + "/" + mVersion + "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * Get entity. In form of: "tenant_name:user_name"
     *
     * @return
     */
    public String getIdentity() {
        return mIdentity;
    }

    /**
     * @return the mIpInternalCic1
     */
    public String getIpInternalCic1() {
        return mIpInternalCic1;
    }

    /**
     * @return the mIpInternalCic2
     */
    public String getIpInternalCic2() {
        return mIpInternalCic2;
    }

    /**
     * @return the mIpPublic
     */
    public String getIpPublic() {
        return mIpPublic;
    }

    /**
     * @return the mKeystonePortInternal
     */
    public int getKeystonePortInternal() {
        return mKeystonePortInternal;
    }

    /**
     * @return the mKeystonePortPublic
     */
    public int getKeystonePortPublic() {
        return mKeystonePortPublic;
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
     * @return the mSshPortInternal
     */
    public int getSshPortInternal() {
        return mSshPortInternal;
    }

    /**
     * @return the mSshPortPublic
     */
    public int getSshPortPublic() {
        return mSshPortPublic;
    }

    /**
     * Get Tenant name
     *
     * @return
     */
    public String getTenant() {
        return mTenant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * Get User name
     *
     * @return
     */
    public String getUser() {
        return mUser;
    }

    /**
     * {@link #mVersion}
     *
     * @param versionApi the mVersion to set
     */
    public void setVersion(String versionApi) {
        mVersion = versionApi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(IP_PUBLIC, mIpPublic)
                .add(IP_INTERNAL_CIC1, mIpInternalCic1).add(IP_INTERNAL_CIC2, mIpInternalCic2)
                .add(KEYSTONE_PORT_PUBLIC, mKeystonePortPublic).add(KEYSTONE_PORT_INTERNAL, mKeystonePortInternal)
                .add(SSH_PORT_PUBLIC, mSshPortPublic).add(SSH_PORT_INTERNAL, mSshPortInternal)
                .add(OPENSTACK_IDENTITY, mIdentity).add(OPENSTACK_PASSWORD, mPassword).add(CONTROLLER, mController)
                .add(COMPUTE_NODE, mComputeNode).toString();
    }
}

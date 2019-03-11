package com.jcat.cloud.fw.components.system.cee.target.fuel;

/**
 * FuelNode represents the fuel node type
 *
 *
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eolnans - 2014-12-10 - Initial version
 * @author eelimei - 2014-12-17 - Support for multiple roles and addition of hostname
 */

import java.util.Set;

public class FuelNode {
    public enum NodeRole {
        CONTROLLER("controller"), COMPUTE("compute"), MONGO("mongo"), ZABBIX("zabbix"), CINDER("cinder"), VIRT("virt"), BASE_OS(
                "base-os"), SCALEIO("scaleio");

        private final String text;

        private NodeRole(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static NodeRole fromNodeRoleString(String nodeRoleString) {
            for (NodeRole nodeRole : NodeRole.values()) {
                if (nodeRole.toString().equals(nodeRoleString)) {
                    return nodeRole;
                }
            }
            return null;
        }
    }

    private final String mHostName;
    private final String mClusterId;
    private final String mAddress;
    private final Set<NodeRole> mRoles;
    private final String mId;
    private final Boolean mOnlineStatus;

    /**
     * Constructor
     *
     * @param hostName
     * @param clusterId the Fuel id of the cluster
     * @param address IP address of the Node
     * @param role The role (compute or controller) of the node
     */
    public FuelNode(String id, String hostName, String clusterId, String address, Set<NodeRole> roles,
            Boolean onlineStatus) {
        mId = id;
        mHostName = hostName;
        mClusterId = clusterId;
        mAddress = address;
        mRoles = roles;
        mOnlineStatus = onlineStatus;
    }

    /**
     * Get the address of the node
     *
     * @return address of the node
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * Get the Cluster ID for the node
     *
     * @return the ID of the corresponding Cluster
     */
    public String getClusterId() {
        return mClusterId;
    }

    /**
     * Get the Host Name for the node
     *
     * @return the name of the host
     */
    public String getHostName() {
        return mHostName;
    }

    /**
     * Get the id of the node
     *
     * @return id of the node
     */
    public String getId() {
        return mId;
    }

    /**
     * Get the Online status of the node
     *
     * @return Online status of the node
     */
    public Boolean getOnlineStatus() {
        return mOnlineStatus;
    }

    /**
     * Get the type of the node, could be dual
     *
     * @return value of the type
     */
    public Set<NodeRole> getRoles() {
        return mRoles;
    }
}

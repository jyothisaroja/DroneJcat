package com.jcat.cloud.fw.common.exceptions;

/**
 * Exception to be thrown when an connection problem occurs.
 * Since there are a lot of different connections that can be made and different targets
 * to be connected to, there are several constructors available.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015- initial version
 * @author epergat 2015- Created new constructors to create exception
 * message in a more formal way
 *
 */
public class EcsConnectionException extends RuntimeException {

    /**
     * Describes which target that is trying to be connected to.
     */
    public enum EcsConnectionTarget {
        FUEL("Fuel"), LXC("Lxc"), COMPUTE_BLADE("Compute Blade"), VM("VM"), CIC("CIC"), OPENSTACK(
                "Openstack"), ATLAS_VM("AtlasVm");

        private String mType;

        EcsConnectionTarget(String type) {
            mType = type;
        }

        public String getType() {
            return mType;
        }
    }

    /**
     * Describes the connection type that is trying to be established.
     */
    public enum EcsConnectionType {
        SSH("SSH"), SCP("SCP"), REST("REST");

        private String mType;

        private EcsConnectionType(String type) {
            mType = type;
        }

        public String getType() {
            return mType;
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param type
     * @param target
     * @param via
     * @param ip
     * @param port
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, EcsConnectionTarget via,
            String ip, int port) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "via " + via.getType()
                + "(IP = " + ip + ", port = " + port + ")");
    }

    /**
     * Exception that can be used connection
     *
     * @param type
     * @param target
     * @param via
     * @param ip
     * @param port
     * @param realException
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, EcsConnectionTarget via,
            String ip, int port, Exception realException) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "via " + via.getType()
                + "(IP = " + ip + ", port = " + port + ")");
    }

    /**
     *
     * @param type
     * @param target
     * @param via
     * @param ip
     * @param port
     * @param username
     * @param password
     * @param realException
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, EcsConnectionTarget via,
            String ip, String port, String username, String password, Exception realException) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip
                + ", port = " + port + ", username = " + username + ", password = " + password + ")");
    }

    /**
     * Use this exception if you only have an IP as troubleshooting information
     *
     * @param rest
     * @param target
     * @param ip
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, String ip) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip + ")");
    }

    /**
     *
     * @param type
     * @param target
     * @param ip
     * @param port
     * @param userName
     * @param realException
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, String ip, int port,
            String userName, Exception realException) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip
                + ", port = " + port + ", username = " + userName + "). \nReason: " + realException.getMessage());
    }

    /**
     *
     * @param type
     * @param target
     * @param ip
     * @param port
     * @param userName
     * @param password
     * @param namespace
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, String ip, int port,
            String userName, String password, String namespace) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip
                + ", port = " + port + ", username = " + userName + ", password = " + password + ", namespace = "
                + namespace + ")");
    }

    /**
     *
     * Exception to be used when creating a VM.
     *
     * @param type which connection type that is being established
     * @param target
     * @param ip
     * @param port
     * @param userName
     * @param password
     * @param namespace
     * @param ex
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, String ip, int port,
            String userName, String password, String namespace, Exception ex) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip
                + ", port = " + port + ", username = " + userName + ", password = " + password + ", namespace = "
                + namespace + ")");
    }

    /**
     *
     * @param type Type of connection that is trying to be established
     * @param target The final destination to be reached
     * @param ip
     * @param port
     * @param username
     * @param password
     * @param realException
     */
    public EcsConnectionException(EcsConnectionType type, EcsConnectionTarget target, String ip, String port,
            String username, String password, Exception realException) {
        super("Could not create a " + type.getType() + " connection to " + target.getType() + "(IP = " + ip
                + ", port = " + port + ", username = " + username + ", password = " + password + ")");
    }
}

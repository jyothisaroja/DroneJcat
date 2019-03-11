package com.jcat.cloud.fw.fwservices.monitoring.common;

/**
 * Zabbix API session properties
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-07-03 initial version
 */
public class ZabbixProperties {

    // The url to which the Zabbix API requests are sent
    private final String mUrl;

    // The content type of the requests
    private final String mContentType;

    // The name of the node
    private final String mNode;

    // The name of the Zabbix API user
    private final String mUsername;

    // The password for the user
    private final String mPassword;

    /**
     * The constructor.
     * @param url The url to which the Zabbix API requests are sent
     * @param contentType The content type of the requests
     * @param username The name of the Zabbix API user
     * @param password The password for the user
     */
    public ZabbixProperties(String url, String contentType, String node, String username, String password) {
        mUrl = url;
        mContentType = contentType;
        mNode = node;
        mUsername = username;
        mPassword = password;
    }

    /**
     * @return The content type of the Zabbix API requests
     */
    public String getContentType() {
        return mContentType;
    }

    /**
     * @return The name of the node
     */
    public String getNode() {
        return mNode;
    }

    /**
     * @return The Zabbix API password
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * @return The url of the Zabbix API
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * @return The Zabbix API username
     */
    public String getUsername() {
        return mUsername;
    }
}

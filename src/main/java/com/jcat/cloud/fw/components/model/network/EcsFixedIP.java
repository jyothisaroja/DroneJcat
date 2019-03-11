package com.jcat.cloud.fw.components.model.network;

/**
 * Class represents fixed IPs for port
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2014-11-14 initial version
 *
 */
public class EcsFixedIP {

    private final String mSubnetId;
    private final String mIpAddress;

    public EcsFixedIP(String subnetId) {
        mSubnetId = subnetId;
        mIpAddress = null;
    }

    public EcsFixedIP(String subnetId, String ipAddress) {
        mSubnetId = subnetId;
        mIpAddress = ipAddress;
    }

    public String getIpAddress() {
        return mIpAddress;
    }

    public String getSubnetId() {
        return mSubnetId;
    }

}

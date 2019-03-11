package com.jcat.cloud.fw.components.model.network;

import java.net.UnknownHostException;

import org.openstack4j.api.Builders;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.SubnetBuilder;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**
 * Class which represents a subnet
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author mob 2014-11-06 initial version
 * @author ethssce 2014-11-24 builder uses createName for generating names
 * @author ethssce 2014-11-27 added name option
 * @author eedelk 2015-01-29 added deletionLevel
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2017-01-05 added method getId
 */
public final class EcsSubnet extends EcsComponent {

    public static class Builder {

        private SubnetBuilder mSubnetBuilder;
        private DeletionLevel mDeletionLevel = DeletionLevel.TEST_CASE;

        private int mCidrPrefixSize;
        private boolean mIsIpVersionSet;

        private Builder(EcsSubnet ecsSubnet) {
            mSubnetBuilder = ecsSubnet.get().toBuilder();
        }

        private Builder(String subnetName, String networkId) {
            mSubnetBuilder = Builders.subnet().name(subnetName).networkId(networkId);
        }

        public Builder addPool(String startIp, String endIp) {
            this.mSubnetBuilder = mSubnetBuilder.addPool(startIp, endIp);
            return this;
        }

        public EcsSubnet build() throws UnknownHostException {
            if (!mIsIpVersionSet) {
                // if not ip version provided, by default it is V4
                mSubnetBuilder = mSubnetBuilder.ipVersion(IPVersionType.V4);
            }
            // Set DHCP by default
            mSubnetBuilder = mSubnetBuilder.enableDHCP(true);
            return new EcsSubnet(mSubnetBuilder.build(), mCidrPrefixSize, mDeletionLevel);
        }

        public Builder cidr(String cidr) {
            this.mSubnetBuilder = mSubnetBuilder.cidr(cidr);
            return this;
        }

        public Builder cidrPrefixSize(int cidrPrefixSize) {
            this.mCidrPrefixSize = cidrPrefixSize;
            return this;
        }

        public Builder enableDhcp(boolean enableDhcp) {
            this.mSubnetBuilder = mSubnetBuilder.enableDHCP(enableDhcp);
            return this;
        }

        public Builder gateway(String gatewayIp) {
            this.mSubnetBuilder = mSubnetBuilder.gateway(gatewayIp);
            return this;
        }

        public Builder ipVersion(IpVersion ipVersion) {
            mIsIpVersionSet = true;
            this.mSubnetBuilder = mSubnetBuilder.ipVersion(IPVersionType.valueOf(ipVersion.toString()));
            return this;
        }

        public Builder name(String name) {
            mSubnetBuilder = mSubnetBuilder.name(name);
            return this;
        }

        public Builder setDeletionLevel(DeletionLevel deletionLevel) {
            mDeletionLevel = deletionLevel;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.mSubnetBuilder = mSubnetBuilder.tenantId(tenantId);
            return this;
        }
    }

    public enum IpVersion {
        V4, V6;
    }

    private final DeletionLevel mDeletionLevel;

    private final Subnet mSubnet;

    private final int mCidrPrefixSize;

    private EcsSubnet(Subnet subnet, int cidrPrefixSize, DeletionLevel deletionLevel) {
        mCidrPrefixSize = cidrPrefixSize;
        mSubnet = subnet;
        mDeletionLevel = deletionLevel;
    }

    public static Builder builder(String networkId) {
        return new Builder(ControllerUtil.createName(), networkId);
    }

    public Subnet get() {
        return mSubnet;
    }

    /**
     * @return the cidr
     */
    public String getCidr() {
        return mSubnet.getCidr();
    }

    /**
     * @return the cidrPrefixSize
     */
    public int getCidrPrefixSize() {
        return mCidrPrefixSize;
    }

    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    /**
     * @return the id
     */
    public String getId() {
        return mSubnet.getId();
    }

    /**
     * @return the name
     */
    public String getName() {
        return mSubnet.getName();
    }

    /**
     * @return the networkId
     */
    public String getNetworkId() {
        return mSubnet.getNetworkId();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }
}

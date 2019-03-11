package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * BorderGateWay (BGW) representation used as a resource of a cloud node.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author elajkat 2014-08-08 initial version
 *
 */
public class BgwResource implements ConfigurationData {
    private static final String BGW_CIDR = "bgw_cidr";
    private static final String PUBLIC_BGW_IP = "public_bgw_ip";
    public static final String RESOURCE_ID = "Bgw";
    @JsonProperty(BGW_CIDR)
    private String mBgwCidr;
    @JsonProperty(PUBLIC_BGW_IP)
    private String mPublicBgwIp;

    /**
     * Constructor for XMLAdapter
     *
     * @param xmlConfiguration
     */
    public BgwResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getProperty(BGW_CIDR), xmlConfiguration.getProperty(PUBLIC_BGW_IP));
    }

    /**
     * Constructor for TASS
     *
     * @param consolesJson
     */
    public BgwResource(@JsonProperty(BGW_CIDR) String bgwCidr, @JsonProperty(PUBLIC_BGW_IP) String publicBgwIp) {
        Preconditions.checkArgument(!(bgwCidr == null || bgwCidr.isEmpty()));
        Preconditions.checkArgument(!(publicBgwIp == null || publicBgwIp.isEmpty()));
        mBgwCidr = bgwCidr;
        mPublicBgwIp = publicBgwIp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * Get BGW CIDR
     *
     * @return
     */
    public String getPublicBgwIp() {
        return mPublicBgwIp;
    }

    /**
     * Get BGW public IP
     *
     * @return
     */
    public String getBgwCidr() {
        return mBgwCidr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConfigurationData> getType() {
        return this.getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add(BGW_CIDR, mBgwCidr).add(PUBLIC_BGW_IP, mBgwCidr)
                .toString();
    }
}

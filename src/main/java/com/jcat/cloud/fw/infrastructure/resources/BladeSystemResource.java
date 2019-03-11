package com.jcat.cloud.fw.infrastructure.resources;

import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.cf.xml.adapter.ResourceElement;
import com.ericsson.tass.resources.MultipleEntryResource;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * <p>
 *
 * Represents the blade system
 *
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2014-12-15 initial version
 *
 */
public class BladeSystemResource implements ConfigurationData, MultipleEntryResource {
    /**
     * Equipment type
     */
    public static final String BladeSystem = "BladeSystem";

    /**
     * ID of the resource
     */
    private static final String ID = "id";
    private static final String CIC1_IP = "cic1_ip";
    private static final String CIC1_SSH_PORT = "cic1_ssh_port";
    private static final String CIC2_IP = "cic2_ip";
    private static final String CIC2_SSH_PORT = "cic2_ssh_port";
    private static final String CIC3_IP = "cic3_ip";
    private static final String CIC3_SSH_PORT = "cic3_ssh_port";
    private static final String COMPUTE_BLADES = "compute_blades";

    private String mId;
    private String mCic1Ip;
    private String mCic2Ip;
    private String mCic3Ip;
    private int mCic1Port;
    private int mCic2Port;
    private int mCic3Port;
    /**
     * Currently public information of compute blades is a string. But later this should stored as a structure.
     */
    private String mComputeBlades;

    /**
     * Constructor used by ConfigurationFacadeAdapter in Xml adapter
     *
     * @param xmlConfiguration
     */
    public BladeSystemResource(ResourceElement xmlConfiguration) {
        this(xmlConfiguration.getId(), xmlConfiguration.getProperty(CIC1_IP), Integer.valueOf(xmlConfiguration
                .getProperty(CIC1_SSH_PORT)), xmlConfiguration.getProperty(CIC2_IP), Integer.valueOf(xmlConfiguration
                .getProperty(CIC2_SSH_PORT)), xmlConfiguration.getProperty(CIC3_IP), Integer.valueOf(xmlConfiguration
                .getProperty(CIC3_SSH_PORT)), xmlConfiguration.getProperty(COMPUTE_BLADES));
    }

    /**
     * Constructor used by ConfigurationFacadeAdapter in TASS adapter
     *
     * @param cic1ip
     * @param cic1port
     * @param cic2ip
     * @param cic2port
     * @param cic3ip
     * @param cic3port
     * @param computeBlades
     */
    public BladeSystemResource(@JsonProperty(CIC1_IP) String cic1ip, @JsonProperty(CIC1_SSH_PORT) int cic1port,
            @JsonProperty(CIC2_IP) String cic2ip, @JsonProperty(CIC2_SSH_PORT) int cic2port,
            @JsonProperty(CIC3_IP) String cic3ip, @JsonProperty(CIC3_SSH_PORT) int cic3port,
            @JsonProperty(COMPUTE_BLADES) String computeBlades) {
        Preconditions.checkArgument(!(cic1ip == null || cic1ip.isEmpty()));
        mCic1Ip = cic1ip;
        mCic2Ip = cic2ip;
        mCic3Ip = cic3ip;
        mCic1Port = cic1port;
        mCic2Port = cic2port;
        mCic3Port = cic3port;
        mComputeBlades = computeBlades;
    }

    /**
     * Main constructor
     *
     * @param id
     * @param cic1ip
     * @param cic1port
     * @param cic2ip
     * @param cic2port
     * @param cic3ip
     * @param cic3port
     * @param computeBlades
     */
    public BladeSystemResource(String id, String cic1ip, int cic1port, String cic2ip, int cic2port, String cic3ip,
            int cic3port, String computeBlades) {
        Preconditions.checkArgument(!(cic1ip == null || cic1ip.isEmpty()));
        mId = id;
        mCic1Ip = cic1ip;
        mCic2Ip = cic2ip;
        mCic3Ip = cic3ip;
        mCic1Port = cic1port;
        mCic2Port = cic2port;
        mCic3Port = cic3port;
        mComputeBlades = computeBlades;
    }

    /**
     * @return The public IP address of CIC 1
     */
    public String getCic1Ip() {
        return mCic1Ip;
    }

    /**
     * @return The public port number of CIC 1
     */
    public int getCic1Port() {
        return mCic1Port;
    }

    public String getCic2Ip() {
        return mCic2Ip;
    }

    public int getCic2Port() {
        return mCic2Port;
    }

    public String getCic3Ip() {
        return mCic3Ip;
    }

    public int getCic3Port() {
        return mCic3Port;
    }

    public String getComputeBlades() {
        return mComputeBlades;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link BladeSystemResource}
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
        return Objects.toStringHelper(this).omitNullValues().add(ID, mId).add(CIC1_IP, mCic1Ip)
                .add(CIC1_SSH_PORT, mCic1Port).add(CIC2_IP, mCic2Ip).add(CIC2_SSH_PORT, mCic2Port)
                .add(CIC3_IP, mCic3Ip).add(CIC3_SSH_PORT, mCic3Port).add(COMPUTE_BLADES, mComputeBlades).toString();
    }

}

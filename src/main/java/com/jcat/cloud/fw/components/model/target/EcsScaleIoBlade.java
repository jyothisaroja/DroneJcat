package com.jcat.cloud.fw.components.model.target;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsNotImplementedException;
import com.jcat.cloud.fw.common.exceptions.EcsTargetException;
import com.jcat.cloud.fw.components.system.cee.ecssession.ComputeBladeSessionFactory;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;

/**
 * Class that represents all the scaleIo blades, there is functionality to retrieve available volume allocation, login into storage cli as admin,
 *  get the name of specified provisioning type from the config.yaml
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zmousar - initial version
 **/
public class EcsScaleIoBlade extends EcsTarget {

    /**
     * Represents the volume provisioning type in config.yaml
     */
    public enum VolumeProvisioningType {
        THICK("thick"), THIN("thin");

        private String mVolumeProvisioningType;

        VolumeProvisioningType(String type) {
            mVolumeProvisioningType = type;
        }

        @Override
        public String toString() {
            return mVolumeProvisioningType;
        }
    }

    private static final String LOGIN_AS_ADMIN_USER = "scli --login --username admin --password %s";
    private static final String STORAGE_CAPACITY = "scli --query_storage_pool --storage_pool_name %s --protection_domain_name %s | grep \" available\" | awk  -F'[()]' '{print $4}'";
    private final String mIpAddress;
    @Inject
    private ComputeBladeSessionFactory mComputeBladeSessionFactory;
    @Inject
    private FuelResource mFuelResource;

    /**
     * Constructor for GUICE
     *
     * @param hostName - hostname of scaleIo balde
     * @param ipAddress - IPv4 address of scaleIo balde
     */
    @Inject
    public EcsScaleIoBlade(@Assisted("ipAddress") String ipAddress) {
        mIpAddress = ipAddress;
    }

    @Override
    public Boolean deinitialize() {
        mSshSession.disconnect();
        return true;
    }

    /**
     * Retrieve available capacity of scaleio-blade to allocate for volumes
     * ex: root@scaleio-0-3:~# scli --query_storage_pool --storage_pool_name pool1 --protection_domain_name protection_domain1  | grep available | awk  -F'[()]' '{print $4}'
     *     106496 MB
     *
     * @return capacity in GB
     */
    public int getAvailableVolumeAllocationCapacity() {
        int capacityValue = 0;
        String storagePool = configYaml().getStoragePoolName();
        String protectionDomain = configYaml().getProtectionDomainName();
        if (storagePool == null) {
            throw new EcsTargetException("Storage pool is not configured in config.yaml");
        }
        if (protectionDomain == null) {
            throw new EcsTargetException("protection domain is not configured to scaleio balde in config.yaml");
        }
        String cmd = String.format(STORAGE_CAPACITY, storagePool, protectionDomain);
        String result = sendCommand(cmd);
        if (result.trim().length() > 0) {
            Matcher matcher = Pattern.compile("^(\\d+)\\s(GB|MB|TB)$").matcher(result);
            if (matcher.find()) {
                capacityValue = Integer.parseInt(matcher.group(1).trim());
                String capacityType = matcher.group(2).trim();
                if (capacityType.equals("MB")) {
                    capacityValue = capacityValue / 1024;
                } else if (capacityType.equals("TB")) {
                    capacityValue = capacityValue * 1024;
                }
            } else {
                throw new EcsTargetException("Does not have valid capacity for scaleio blade");
            }
        }
        return capacityValue;
    }

    /**
     * Get the name of specified provisioning_type from the config.yaml
     *
     * @param provisioningType - provisioning type [thick / thin]
     * @return
     */
    public String getProvisioningTypeName(VolumeProvisioningType provisioningType) {
        String provisioningTypeName = configYaml().getProvisioningTypeName(provisioningType.toString());
        return provisioningTypeName;
    }

    @Override
    public String sendCommand(String command) {
        if (mSshSession == null) {
            mSshSession = mComputeBladeSessionFactory.create(mIpAddress, mFuelResource.getIpPublic(),
                    mFuelResource.getFuelPublicSshPort());
        }
        return mSshSession.send(command);
    }

    /**
     * Execute scli commands in scaleio blade by logging into as 'admin'
     * ex: root@scaleio-0-6:~# scli --login --username admin --password Cluster1!
     *    Certificate info:
     *    subject: /GN=MDM/CN=scaleio-0-4.domain.tld/L=Hopkinton/ST=Massachusetts/C=US/O=E
     *    MC/OU=ASD
     *    issuer:  /GN=MDM/CN=scaleio-0-4.domain.tld/L=Hopkinton/ST=Massachusetts/C=US/O=E
     *    MC/OU=ASD
     *    Valid-From: Feb 26 18:52:57 2018 GMT
     *    Valid-To:   Feb 25 19:52:57 2028 GMT
     *    Thumbprint: D6:B2:D7:C6:BC:24:66:D3:4F:B7:E4:E5:62:06:67:01:BE:14:DA:D8
     *    Press 'y' to approve this certificate and add it to the truststore
     *    Logged in. User role is SuperUser. System ID is 58d7e0f3329b942d
     *
     * @return
     */
    public String sendScliCommand() {
        String password = configYaml().getStoragePassword();
        if (password == null) {
            throw new EcsTargetException("Storage cluster password is not configured in config.yaml");
        }
        return sendCommand(String.format(LOGIN_AS_ADMIN_USER, password));
    }

    @Override
    public boolean startScalabilityCollection() {
        throw new EcsNotImplementedException("Scalability collection is not done on EcsScaleIoBlade.");
    }

    @Override
    public boolean startStabilityCollection() {
        throw new EcsNotImplementedException("Stability collection is not done on EcsScaleIoBlade.");
    }
}

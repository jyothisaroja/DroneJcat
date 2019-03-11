package com.jcat.cloud.fw.components.system.cee.target.fuel;

import java.util.List;

/**
 * Representation of the Fuel Cluster type
 *
 */
public class FuelCluster {
    private String mName;
    private int mClusterId;
    private int mReleaseId;
    private int mPendingReleaseId;
    private String mStatus;

    /**
     * @param name the name of the cluster
     * @param clusterId the Fuel id of the cluster
     * @param releaseId the release currently on the nodes
     * @param pendingReleaseId the pending release, in case an upgrade is staged
     * @param status
     */
    public FuelCluster(String name, int clusterId, int releaseId, int pendingReleaseId, String status) {
        this.mName = name;
        this.mClusterId = clusterId;
        this.mReleaseId = releaseId;
        this.mPendingReleaseId = pendingReleaseId;
        this.mStatus = status;
    }

    public List<FuelNode> getNodes() {
        return null;
    }

    public String getName() {
        return mName;
    }

    public int getClusterId() {
        return mClusterId;
    }

    public int getReleaseId() {
        return mReleaseId;
    }

    public int getPendingReleaseId() {
        return mPendingReleaseId;
    }

    public String getStatus() {
        return mStatus;
    }
}

package com.jcat.cloud.fw.components.system.cee.openstack.cinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstack4j.api.Builders;
import org.openstack4j.api.storage.BlockQuotaSetService;
import org.openstack4j.api.storage.BlockVolumeService;
import org.openstack4j.api.storage.BlockVolumeSnapshotService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeUploadImage;
import org.openstack4j.model.storage.block.options.UploadImageData;

import se.ericsson.jcat.fw.assertion.JcatAssertApi;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolume;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolume.EcsVolumeBuilder;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolumeSnapshot;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolumeType;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.system.cee.openstack.glance.GlanceController;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains methods related with openstack cinder.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2014-10-03 initial version
 * @author ethssce 2014-11-24 createVolume uses createName for generating names
 * @author eelimei 2015-01-28 modify create volume/volumetype to use new ecs
 *         objects and only save id for cleanup if deletion level is TEST_CASE
 * @author eelimei 2015-01-28 Modify createVolumeType to not throw exception if
 *         volume type already exists, only return id of existing volume type.
 * @author efikayd 2015-03-16 Add the method getStatusListOfTheVolumes.
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author epergat 2015-06-27 made cleanup more robust
 * @author eelimei - 2015-09-02 - Add extendVolume
 * @author eelimei - 2016-01-18 - Fixed bug with getVolume, now all the info
 *         from the system is included, not only name and size
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2016-11-09 Add the method checkAllServicesStatusEnabled.
 * @author zpralak 2016-11-30 Add Method forceDeleteVolume.
 * @author zpralak 2016-12-09 Add the methods createVolumeSnapshot
 *         And waitForVolumeSnapshotStatus.
 * @author zpralak 2016-01-09 Add the methods deleteVolumeSnapshot
 *         And waitForVolumeSnapshotDeleted
 * @author zdagjyo 2017-01-23 Add method getVolumeStatusById
 * @author zdagjyo 2017-02-08 Add method verifyCinderServicesEnabled
 * @author zdagjyo 2017-03-04 Add methods createImageFromVolume and createUploadImageData
 * @author zdagjyo 2017-03-20 Add methods getCinderQuotaGigabytesForTenant,
 *         getCinderQuotaVolumesForTenant and updateCinderQuotaVolumesForTenant
 * @author zdagjyo 2017-03-31 Add method getVolumeIdFromSnapshot
 * @author zpralak 2017-04-13 Add method getCinderQuotaSnapshotsForTenant and
 *         modify updateCinderQuotaVolumesForTenant
 * @author zdagjyo 2017-04-27 Add method doesVolumeExist
 */
public final class CinderController extends EcsComponent {

    /**
     * Logger instance
     */
    private final EcsLogger mLogger = EcsLogger.getLogger(CinderController.class);

    /**
     * List to keep track of created volume types use CopyOnWriteArrayList due
     * to concurrent access to the list(iterate through it and remove item at
     * the same time)
     */
    private final List<String> mCreatedVolumeTypeIdsForTestCase = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created volumes
     */
    private final List<String> mCreatedVolumeIdsForTestCase = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of created volume snapshots
     */
    private final List<String> mCreatedVolumeSnapshotIdsForTestCase = new CopyOnWriteArrayList<String>();

    private final OpenStack4jEcs mOpenStack4jEcs;

    @Inject
    protected EcsCicList mEcsCicList;

    @Inject
    protected GlanceController mGlanceController;

    /**
     * Hide Utility Class Constructor: Utility classes should not have a public
     * or default constructor.
     */
    @Inject
    private CinderController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    private BlockQuotaSetService quotaSetService() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).blockStorage(),
                BlockQuotaSetService.class).quotaSets();
    }

    private BlockVolumeService volumeService() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).blockStorage(),
                BlockVolumeService.class).volumes();
    }

    private BlockVolumeSnapshotService volumeSnapshotService() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).blockStorage(),
                BlockVolumeSnapshotService.class).snapshots();
    }

    /**
     * Keep checking if volume has been removed within time limit for volume
     * status change. Throw exception if the volume still exists
     *
     * @param volumeId
     *            - String - Id of the volume to be removed
     */
    private void waitForVolumeDeleted(final String volumeId) {
        mLogger.info(EcsAction.STATUS_CHANGING, EcsVolume.class, volumeId + ", Target status: DELETED, Timeout="
                + Timeout.VOLUME_READY.getTimeoutInSeconds());
        new LoopHelper<Boolean>(Timeout.VOLUME_READY, "Volume with id " + volumeId + " was still found after deletion",
                Boolean.TRUE, () -> {
                    Volume volume = volumeService().get(volumeId);
                    if (volume != null) {
                        mLogger.info("Volume " + volume.getId() + " still exists");
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.STATUS_CHANGED, "DELETED", EcsVolume.class, volumeId);
    }

    /**
     * Keep checking if volume snapshot has been removed within time limit for volume
     * snapshot status change. Throw exception if the volume snapshot still exists
     *
     * @param snapshotId
     *            - String - Id of the volume snapshot to be removed
     */
    private void waitForVolumeSnapshotDeleted(final String snapshotId) {
        mLogger.info(EcsAction.STATUS_CHANGING, EcsVolumeSnapshot.class, snapshotId
                + ", Target status: DELETED, Timeout=" + Timeout.SNAPSHOT_FROM_VOLUME_READY.getTimeoutInSeconds());
        new LoopHelper<Boolean>(Timeout.SNAPSHOT_FROM_VOLUME_READY, "Volume with id " + snapshotId
                + " was still found after deletion", Boolean.TRUE, () -> {
                    VolumeSnapshot volumeSnapshot = volumeSnapshotService().get(snapshotId);
                    if (volumeSnapshot != null) {
                        mLogger.info("Volume Snapshot " + volumeSnapshot.getId() + " still exists");
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.STATUS_CHANGED, "DELETED", EcsVolumeSnapshot.class, snapshotId);
    }

    /**
     * Keep checking if VolumeSnapshot instance is in specified status within timeout
     * Throws exception if volume snapshot status not in desired Status if time
     * limit reached.
     *
     * @param snapshotId
     *            - String - the snapshotId
     * @param desiredStatus
     *            - String - Required status
     */
    private void waitForVolumeSnapshotStatus(String snapshotId, Volume.Status desiredStatus) {
        mLogger.info(EcsAction.STATUS_CHANGING, EcsVolumeSnapshot.class, snapshotId + ", Target status: "
                + desiredStatus + ", Timeout=" + Timeout.SNAPSHOT_FROM_VOLUME_READY);
        final String errorMessage = "Volume: " + snapshotId + " did not reach status: " + desiredStatus;
        LoopHelper<Volume.Status> loopHelper = new LoopHelper<Volume.Status>(Timeout.SNAPSHOT_FROM_VOLUME_READY,
                errorMessage, desiredStatus, () -> {
                    Volume.Status lastVolumeState = volumeSnapshotService().get(snapshotId).getStatus();
                    mLogger.debug("Volume: " + snapshotId + "Current volume state: " + lastVolumeState);
                    return lastVolumeState;
                });
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, volumeSnapshotService().get(snapshotId).getStatus(),
                EcsVolumeSnapshot.class, snapshotId);
    }

    /**
     * Check that all cinder services have status == enabled
     *
     * @return boolean - true: all cinder services have status == enabled
     */
    public boolean checkAllServicesStatusEnabled() {
        boolean result = false;
        EcsCic cic = mEcsCicList.getRandomCic();
        String executionOutput = cic.sendCommand("cinder service-list");
        // check that all cinder services are enabled
        String[] lines = executionOutput.split("\n");
        for (String line : lines) {
            if (line.contains("cinder")) {
                if (line.contains("enabled")) {
                    result = true;
                    continue;
                } else {
                    mLogger.error(String.format("cinder services are not enabled !!!"));
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * cleans up, i.e deletes all remaining VolumeTypes, Volumes that have been
     * created during the lifetime of this CinderController instance
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", CinderController.class, "");
        // remove all volume snapshots created in this instance.
        for (String snapshotId : mCreatedVolumeSnapshotIdsForTestCase) {
            try {
                deleteVolumeSnapshot(snapshotId);
            } catch (LoopTimeoutException timeoutException) {
                mLogger.error(String
                        .format("Failed to confirm deletion of volume snapshot (id:%s) within the given time limit, exception was",
                                snapshotId, timeoutException));
            } catch (Exception e) {
                mLogger.error(String.format(
                        "Got exception while trying to delete volume snapshot (id:%s), exception was: %s", snapshotId,
                        e));
            }
        }

        // remove all volumes created in this instance.
        for (String volumeId : mCreatedVolumeIdsForTestCase) {
            try {
                deleteVolume(volumeId);
            } catch (LoopTimeoutException timeoutException) {
                mLogger.error(String.format(
                        "Failed to confirm deletion of volume (id:%s) within the given time limit, exception was",
                        volumeId, timeoutException));
            } catch (Exception e) {
                mLogger.error(String.format("Got exception while trying to delete volume (id:%s), exception was: %s",
                        volumeId, e));
            }
        }

        // delete all volume types created in this instance.
        for (String volumeTypeId : mCreatedVolumeTypeIdsForTestCase) {
            try {
                deleteVolumeType(volumeTypeId);
            } catch (LoopTimeoutException timeoutException) {
                mLogger.error(String.format(
                        "Failed to confirm deletion of volume type (id:%s) within the given time limit, exception was",
                        volumeTypeId, timeoutException));
            } catch (Exception e) {
                mLogger.error(String.format(
                        "Got exception while trying to delete volume type(id:%s), exception was: %s", volumeTypeId, e));
            }
        }
        mLogger.info(Verdict.DONE, "Clean up", CinderController.class, "");
    }

    /**
     * Creates image from the volume specified by ID
     *
     * @param volumeId
     *            - String - id of the volume from which image is to be created
     *
     * @param imageName
     *            - String - the name of the image to be created
     *
     * @return String - id of the image created from volume
     */
    public String createImageFromVolume(String volumeId, String imageName) {
        UploadImageData data = createUploadImageData(imageName, true);
        mLogger.info(EcsAction.CREATING, EcsImage.class, "from volume: " + volumeId);
        VolumeUploadImage uploadImage = volumeService().uploadToImage(volumeId, data);
        if (uploadImage == null) {
            throw new EcsOpenStackException("Failed to create image from specified volume");
        }
        mGlanceController.waitForImageToBecomeActive(uploadImage.getImageId());
        mLogger.info(Verdict.CREATED, EcsImage.class, uploadImage.getImageId() + ", from volume: " + volumeId);
        return uploadImage.getImageId();
    }

    /**
     * Creates options that are needed to upload volume to image service as image
     *
     * @param imageName
     *            - String - name of the image to be created
     * @param isForce - boolean
     *
     * @return UploadImageData - data specifying options to upload volume as image
     */
    public UploadImageData createUploadImageData(String imageName, boolean isForce) {
        mLogger.info(EcsAction.CREATING, "", "Upload Image Data", "with image name: " + imageName);
        UploadImageData data = UploadImageData.create(imageName).diskFormat(DiskFormat.QCOW2).force(isForce);
        if (data == null) {
            throw new EcsOpenStackException("Failed to create Upload Image Data");
        }
        mLogger.info(Verdict.CREATED, "", "Upload Image Data", "with image name: " + imageName);
        return data;
    }

    /**
     * Creates a Volume and waits until it achieves the AVAILABLE status. If
     * volume is not created or available status is not reached within time
     * limit, exception will be thrown.
     *
     * @param EcsVolume
     *            - volume to be created
     * @return String - volumeId
     */
    public String createVolume(EcsVolume ecsVolume) {
        mLogger.info(EcsAction.CREATING, ecsVolume);
        Volume volume = volumeService().create(ecsVolume.get());
        JcatAssertApi.assertNotNull("Volume could not be created", volume);
        String volumeId = volume.getId();
        if (ecsVolume.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedVolumeIdsForTestCase.add(volumeId);
        }
        waitForVolumeStatus(volumeId, Volume.Status.AVAILABLE);
        mLogger.info(Verdict.CREATED, EcsVolume.class, "os4jVolume: " + volume);
        return volumeId;
    }

    /**
     * Creates a Volume snapshot and waits until it achieves the AVAILABLE status. If
     * volume is not created or available status is not reached within time
     * limit, exception will be thrown.
     *
     * @param EcsSnapshot
     *            - snapshot to be created
     * @return String - snapshotId
     */
    public String createVolumeSnapshot(EcsVolumeSnapshot ecsSnapshot) {
        mLogger.info(EcsAction.CREATING, "Snapshot", EcsVolumeSnapshot.class, "from server: " + ecsSnapshot);
        VolumeSnapshot snapshot = volumeSnapshotService().create(ecsSnapshot.get());
        String snapshotId = snapshot.getId();
        if (snapshotId == null) {
            throw new EcsOpenStackException("Failed to create snapshot of volume from openstack");
        }
        if (ecsSnapshot.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedVolumeSnapshotIdsForTestCase.add(snapshotId);
        }
        waitForVolumeSnapshotStatus(snapshotId, Volume.Status.AVAILABLE);
        mLogger.info(Verdict.CREATED, EcsVolumeSnapshot.class, "os4jVolume: " + snapshot);
        return snapshotId;
    }

    /**
     * Creates a volume type with the specified options.
     *
     * @param EcsVolumeType
     *            - volume type to be created
     * @return String - Id of created volume type or null if volume type could
     *         not be created successfully
     */
    public String createVolumeType(EcsVolumeType ecsVolumeType) {
        mLogger.info(EcsAction.CREATING, ecsVolumeType);
        VolumeType volumeType = volumeService().createVolumeType(ecsVolumeType.get());
        String volumeTypeId = null;
        JcatAssertApi.assertNotNull("Volume type could not be created", volumeType);
        volumeTypeId = volumeType.getId();
        if (ecsVolumeType.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedVolumeTypeIdsForTestCase.add(volumeTypeId);
        }
        mLogger.info(Verdict.CREATED, EcsVolumeType.class, "os4jVolume: " + volumeType);
        return volumeTypeId;
    }

    /**
     * This method will delete a Volume. Throws an error if it could not be
     * deleted or if the volume is still detected in the system before timeout
     *
     * @param volumeId
     *            - id of the volume to be removed
     */
    public void deleteVolume(final String volumeId) {
        mLogger.info(EcsAction.DELETING, EcsVolume.class, volumeId);
        ActionResponse result = volumeService().delete(volumeId);
        if (result.getCode() == 400) {
            throw new EcsOpenStackException(
                    "Failed to delete the volume from openstack.The response from openstack is " + result.getFault());
        } else if (result.getCode() != 200) {
            mLogger.warn("Got error message from openstack while trying to delete volume and the message is "
                    + result.getFault());
        }
        waitForVolumeDeleted(volumeId);
        mCreatedVolumeIdsForTestCase.remove(volumeId);
        mLogger.info(Verdict.DELETED, EcsVolume.class, volumeId);
    }

    /**
     * This method will delete a Volume Snapshot. Throws an error if it could not be
     * deleted or if the volume snapshot is still detected in the system before timeout
     *
     * @param snapshotId
     *            - id of the volume snapshot to be removed
     */
    public void deleteVolumeSnapshot(final String snapshotId) {
        mLogger.info(EcsAction.DELETING, EcsVolumeSnapshot.class, snapshotId);
        volumeSnapshotService().delete(snapshotId);
        waitForVolumeSnapshotDeleted(snapshotId);
        mCreatedVolumeSnapshotIdsForTestCase.remove(snapshotId);
        mLogger.info(Verdict.DELETED, EcsVolumeSnapshot.class, snapshotId);
    }

    /**
     * Deletes a volume type with a given name
     *
     * @param volumeTypeId
     *            - String - id of volume type to delete
     * @return boolean - returns true if the delete was successful
     */
    public boolean deleteVolumeType(final String volumeTypeId) {
        mLogger.info(EcsAction.DELETING, EcsVolumeType.class, volumeTypeId);
        volumeService().deleteVolumeType(volumeTypeId);
        if (!isVolumeTypeDefined(volumeTypeId)) {
            mCreatedVolumeTypeIdsForTestCase.remove(volumeTypeId);
            mLogger.info(Verdict.DELETED, EcsVolumeType.class, volumeTypeId);
            return true;
        } else {
            mLogger.error("Volume type is not defined therefore could not be deleted: " + volumeTypeId);
            return false;
        }
    }

    /**
     * Check if the volume exists in the ECS Cloud.
     *
     * @param volumeId - String - id of volume
     * @return true if volume exists, otherwise false
     */
    public boolean doesVolumeExist(String volumeId) {
        try {
            getVolume(volumeId);
        } catch (EcsOpenStackException e) {
            if (e.getMessage().contains("No volume") && e.getMessage().contains("can be found")) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

    /**
     * Extends the volume to the new size. The volume must be AVAILABLE
     *
     * @param volumeId
     * @param newSize
     *            in GB, must be greater than the old size
     */
    public void extend(final String volumeId, final int newSize) {
        Volume volume = volumeService().get(volumeId);
        if (volume == null) {
            throw new EcsOpenStackException("Failed to get the volume from openstack " + volumeId);
        }

        int currentSize = volume.getSize();
        if (newSize <= currentSize) {
            throw new RuntimeException("The new size must be grater than the old when extending volume. Current size: "
                    + currentSize + " newSize: " + newSize);
        }

        if (volume.getStatus() != Volume.Status.AVAILABLE) {
            throw new EcsOpenStackException(
                    "volume must be in available status when extending the size. Current status is: "
                            + volume.getStatus().toString());
        }
        mLogger.info(EcsAction.RESIZING, EcsVolume.class, " from " + currentSize + " to " + newSize);
        ActionResponse response = volumeService().extend(volumeId, newSize);
        if (!response.isSuccess()) {
            throw new EcsOpenStackException("The request to extend volume was not sucessful, message: "
                    + response.getFault());
        }

        new LoopHelper<Boolean>(Timeout.VOLUME_READY, "Volume with id " + volumeId
                + " did not extend size as requested within timelimit.", Boolean.TRUE, () -> {
                    Volume volume1 = volumeService().get(volumeId);
                    if (volume1 != null) {
                        if (volume1.getSize() == newSize) {
                            return true;
                        }
                    }
                    return false;
                }).run();
        mLogger.info(Verdict.RESIZED, EcsVolume.class, " from " + currentSize + " to " + newSize);
    }

    /**
     * This method will delete a Volume forcefully. Throws an error if it could not be
     * deleted or if the volume is still detected in the system before timeout
     *
     * @param volumeId
     *            - id of the volume to be removed
     */
    public void forceDeleteVolume(final String volumeId) {
        mLogger.info(EcsAction.DELETING, EcsVolume.class, volumeId);
        ActionResponse result = volumeService().forceDelete(volumeId);
        if (result.getCode() == 400) {
            throw new EcsOpenStackException(
                    "Failed to force delete the volume from openstack.The response from openstack is "
                            + result.getFault());
        } else if (result.getCode() != 200) {
            mLogger.warn("Got error message from openstack while trying to force delete volume and the message is "
                    + result.getFault());
        }
        waitForVolumeDeleted(volumeId);
        mCreatedVolumeIdsForTestCase.remove(volumeId);
        mLogger.info(Verdict.DELETED, EcsVolume.class, volumeId);
    }

    /**
     * Gets the cinder quota gigabytes for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the gigabytes quota for
     *
     * @return int - the cinder quota gigabytes
     */
    public int getCinderQuotaGigabytesForTenant(String tenantId) {
        return quotaSetService().get(tenantId).getGigabytes();
    }

    /**
     * Gets the cinder quota snapshots for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the snapshots quota for
     *
     * @return int - the cinder quota snapshots
     */
    public int getCinderQuotaSnapshotsForTenant(String tenantId) {
        return quotaSetService().get(tenantId).getSnapshots();
    }

    /**
     * Gets the cinder quota volumes for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to get the volumes quota for
     *
     * @return int - the cinder quota volumes
     */
    public int getCinderQuotaVolumesForTenant(String tenantId) {
        return quotaSetService().get(tenantId).getVolumes();
    }

    /**
     * This method is used primarily to get information on the status of each
     * volume that is known to cinder.
     *
     * @return An instance of java.util.Map<String, String> where the key of the
     *         map keeps the volume id of per known volume while the value keeps
     *         the corresponding status of the per known volume.
     */
    public Map<String, String> getStatusListOfVolumes() {
        mLogger.info(EcsAction.COLLECTING, "List of", EcsVolume.class, "Status");
        Map<String, String> volumeIdStatusMap = new HashMap<String, String>();
        List<? extends Volume> volumeList = volumeService().list();
        for (Volume volItem : volumeList) {
            volumeIdStatusMap.put(volItem.getId(), volItem.getStatus().toString());
        }
        mLogger.info(Verdict.COLLECTED, "List of", EcsVolume.class, "Status");
        return volumeIdStatusMap;
    }

    /**
     * This method is used primarily to get information on the status of each
     * volume that is known to cinder.
     *
     * @return An instance of java.util.Map<String, String> where the key of the
     *         map keeps the volume id of per known volume while the value keeps
     *         the corresponding status of the per known volume.
     */
    public Map<String, String> getStatusListOfVolumeSnapshots() {
        mLogger.info(EcsAction.COLLECTING, "List of", EcsVolumeSnapshot.class, "Status");
        Map<String, String> volumeSnapshotIdStatusMap = new HashMap<String, String>();
        List<? extends VolumeSnapshot> volumeSnapshotList = volumeSnapshotService().list();
        for (VolumeSnapshot volSsItem : volumeSnapshotList) {
            volumeSnapshotIdStatusMap.put(volSsItem.getId(), volSsItem.getStatus().toString());
        }
        mLogger.info(Verdict.COLLECTED, "List of", EcsVolumeSnapshot.class, "Status");
        return volumeSnapshotIdStatusMap;
    }

    /**
     * Returns an EcsVolume object representing the voulme on the system with
     * the given ID.
     *
     * @param volumeId
     *            - String
     * @return EcsVolume
     */
    public EcsVolume getVolume(String volumeId) {
        Volume volume = volumeService().get(volumeId);
        if (null == volume) {
            throw new EcsOpenStackException("No volume with the given volumeId (" + volumeId + ") can be found.");
        }
        return EcsVolumeBuilder.build(volume);
    }

    /**
     * Returns the id of the volume from which the volume snapshot is created.
     *
     * @param volumeSnapshotId - The id of the volume snapshot
     *
     * @return String - the id of the volume from which the volume snapshot is created
     */
    public String getVolumeIdFromSnapshot(String volumeSnapshotId) {
        return volumeSnapshotService().get(volumeSnapshotId).getVolumeId();
    }

    /**
     * Retrieves the status of the volume specified by ID.
     *
     * @param volumeId
     *            - String - id of the volume
     * @return EcsVolume.Status
     *             - the status of the volume
     */
    public EcsVolume.Status getVolumeStatusById(String volumeId) {
        return getVolume(volumeId).getStatus();
    }

    /**
     * Checks if the given volume type is defined Returns 'true' if volume type
     * is found and 'false' if volume type is not found
     *
     * @param volumeTypeIdentifier
     *            - String - name OR id of the volume type to be checked
     * @return boolean if the volume type is defined or not
     */
    public boolean isVolumeTypeDefined(final String volumeTypeIdentifier) {
        List<? extends VolumeType> volumeTypes = volumeService().listVolumeTypes();
        mLogger.info(EcsAction.FINDING, EcsVolumeType.class, volumeTypeIdentifier);
        for (VolumeType volumeType : volumeTypes) {
            if (volumeType.getName().equals(volumeTypeIdentifier) || volumeType.getId().equals(volumeTypeIdentifier)) {
                mLogger.info(Verdict.FOUND, EcsVolumeType.class, volumeTypeIdentifier);
                return true;
            }
        }
        mLogger.warn("Volume type " + volumeTypeIdentifier + "  not found");
        return false;
    }

    public List<String> listVolumes() {
        List<String> volumes = new ArrayList<String>();
        for (Volume volume : volumeService().list()) {
            volumes.add(volume.getId());
        }
        return volumes;
    }

    public List<String> listVolumeSnapshots() {
        List<String> snapshots = new ArrayList<String>();
        for (VolumeSnapshot snapshot : volumeSnapshotService().list()) {
            snapshots.add(snapshot.getId());
        }
        return snapshots;
    }

    /**
     * Update the cinder quota volume for a given tenant
     *
     * @param tenantId
     *            - String - id of the tenant to update the volumes for
     * @param volumes
     *            - int - new quota value for volumes
     * @param gigabytes
     *            - int - new quota value for gigabytes
     * @param snapshots
     *            - int - new quota value for snapshots
     */
    public void updateCinderQuotaVolumesForTenant(String tenantId, int volumes, int gigabytes, int snapshots) {
        BlockQuotaSet quota = Builders.blockQuotaSet().volumes(volumes).gigabytes(gigabytes).snapshots(snapshots)
                .build();
        quotaSetService().updateForTenant(tenantId, quota);
    }

    /**
     * Verifies that the cinder services are enabled
     */
    public void verifyCinderServicesEnabled() {
        if (!checkAllServicesStatusEnabled()) {
            LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.CINDER_SERVICE_STATE_CHANGE,
                    "could not verify that cinder service is up", Boolean.TRUE, () -> {
                        return checkAllServicesStatusEnabled();
                    });
            loopHelper.setIterationDelay(10);
            loopHelper.run();
        }
    }

    /**
     * Keep checking if Volume instance is in specified status within timeout
     * for volume change. Throws exception if no volume is found or if time
     * limit reached.
     *
     * @param volumeId
     *            - String - the volumeId
     * @param desiredStatus
     *            - {@link Volume.Status}
     */
    public void waitForVolumeStatus(String volumeId, Volume.Status desiredStatus) {
        Timeout timeout;
        if (volumeService().get(volumeId).getSnapshotId() != null) {
            timeout = Timeout.VOLUME_FROM_SNAPSHOT_READY;
        } else {
            timeout = Timeout.VOLUME_READY;
        }
        mLogger.info(EcsAction.STATUS_CHANGING, EcsVolume.class, volumeId + ", Target status: " + desiredStatus
                + ", Timeout=" + timeout);
        final String errorMessage = "Volume: " + volumeId + " did not reach status: " + desiredStatus;
        LoopHelper<Volume.Status> loopHelper = new LoopHelper<Volume.Status>(timeout, errorMessage, desiredStatus,
                () -> {
                    Volume.Status lastVolumeState = volumeService().get(volumeId).getStatus();
                    mLogger.debug("Volume: " + volumeId + "Current volume state: " + lastVolumeState.toString());
                    return lastVolumeState;
                });
        loopHelper.setErrorState(Volume.Status.ERROR);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, volumeService().get(volumeId).getStatus(), EcsVolume.class, volumeId);
    }
}

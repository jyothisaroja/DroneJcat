
package com.jcat.cloud.fw.components.model.storage.block;

import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.builder.VolumeSnapshotBuilder;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zpralak 2016- initial version
 *
 */
public class EcsVolumeSnapshot extends EcsComponent {
    public static class EcsVolumeSnapshotBuilder {

        private final VolumeSnapshotBuilder mVolumeSnapshotBuilder;
        private DeletionLevel mDeletionLevel = DeletionLevel.TEST_CASE;
        private boolean mForce = false;

        private EcsVolumeSnapshotBuilder(final String volumeId, final String volumeSnapshotName) {
            mVolumeSnapshotBuilder = Builders.volumeSnapshot().volume(volumeId).name(volumeSnapshotName);
        }

        /**
         * Sets the force. Default is false.
         *
         * @param isForce
         * @return this volume snapshot builder instance
         */
        public EcsVolumeSnapshotBuilder force(boolean isForce) {
            mForce = isForce;
            return this;
        }

        public EcsVolumeSnapshot build() {
            if (mForce) {
                mVolumeSnapshotBuilder.force(mForce);
            }

            return new EcsVolumeSnapshot(mVolumeSnapshotBuilder.build(), mDeletionLevel);
        }

        public EcsVolumeSnapshotBuilder setDeletionLevel(DeletionLevel deletionLevel) {
            mDeletionLevel = deletionLevel;
            return this;
        }
    }

    private final VolumeSnapshot mVolumeSnapshot;

    private final DeletionLevel mDeletionLevel;

    /**
     * @param volumeSnapshot
     * @param deletionLevel
     */
    public EcsVolumeSnapshot(VolumeSnapshot volumeSnapshot, DeletionLevel deletionLevel) {
        mVolumeSnapshot = volumeSnapshot;
        mDeletionLevel = deletionLevel;
    }

    public static EcsVolumeSnapshotBuilder builder(final String volumeId) {
        return new EcsVolumeSnapshotBuilder(volumeId, ControllerUtil.createName());
    }

    public VolumeSnapshot get() {
        return mVolumeSnapshot;
    }

    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    public String getName() {
        return mVolumeSnapshot.getName();
    }
}

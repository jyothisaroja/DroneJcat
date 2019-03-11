package com.jcat.cloud.fw.components.model.storage.block;

import static com.google.common.base.Preconditions.checkNotNull;
import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.CaseFormat;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015- initial version
 * @author eedann - 2015-07-02 - added builder methods to make the volume bootable
 * @author eelimei - 2016-01-18 - Added getImageRef and a method to build an EcsVolume object with an openstack volume object from the system that maybe was not created by JCAT.
 * @author zdagjyo - 2017-02-02 - added enum Status and method getStatus
 */
public class EcsVolume extends EcsComponent {

    public enum Status {
        AVAILABLE, ATTACHING, BACKING_UP, CREATING, DELETING, DOWNLOADING, UPLOADING, ERROR, ERROR_DELETING, ERROR_RESTORING, IN_USE, RESTORING_BACKUP, DETACHING, UNRECOGNIZED;

        @JsonValue
        public String value() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
        }

        @Override
        public String toString() {
            return value();
        }

        @JsonCreator
        public static Status fromValue(String status) {
            try {
                return valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, checkNotNull(status, "status")));
            } catch (IllegalArgumentException e) {
                return UNRECOGNIZED;
            }
        }
    }

    public static class EcsVolumeBuilder {

        private final VolumeBuilder mVolumeBuilder;

        private DeletionLevel mDeletionLevel = DeletionLevel.TEST_CASE;
        private EcsVolumeType mVolumeType = null;
        private boolean mIsBootable = false;
        private String mBootableImageRef = null;
        private String mSnapshotId = null;

        private EcsVolumeBuilder(final String volumeName, final int volumeSize) {
            mVolumeBuilder = Builders.volume().name(volumeName).size(volumeSize);
        }

        public static EcsVolume build(Volume volume) {
            return new EcsVolume(volume, DeletionLevel.PERMANENT);
        }

        /**
         * Sets if the volume is bootable or not. Default is false.
         *
         * @param isBootable
         * @return this volume builder instance
         */
        public EcsVolumeBuilder bootable(boolean isBootable) {
            mIsBootable = isBootable;
            return this;
        }

        public EcsVolume build() {
            if (mVolumeType != null) {
                mVolumeBuilder.volumeType(mVolumeType.getName());
            }
            if (mSnapshotId != null) {
                mVolumeBuilder.snapshot(mSnapshotId);
            }
            if (mBootableImageRef != null) {
                if (!mIsBootable) {
                    throw new EcsOpenStackException(
                            "If a bootable image ref is added to the volume builder, the bootable boolean must be set to true.");
                }
                mVolumeBuilder.imageRef(mBootableImageRef);
            }
            mVolumeBuilder.bootable(mIsBootable);
            return new EcsVolume(mVolumeBuilder.build(), mDeletionLevel);
        }

        /**
         * Sets the id of the image from which the volume is to be booted from.
         * The volume is required to be bootable.
         *
         * @param imageRef
         * @return this volume builder instance
         */
        public EcsVolumeBuilder imageRef(String imageRef) {
            mBootableImageRef = imageRef;
            return this;
        }

        /**
         * Sets the id of the snapshot from which the volume is to be booted from.
         * The volume is required to be bootable.
         *
         * @param snapshotId
         * @return this volume builder instance
         */
        public EcsVolumeBuilder snapshotId(String snapshotId) {
            mSnapshotId = snapshotId;
            return this;

        }

        public EcsVolumeBuilder setDeletionLevel(DeletionLevel deletionLevel) {
            mDeletionLevel = deletionLevel;
            return this;
        }

        /**
         * Sets volume type.
         *
         * @param volumeType
         * @return this volume builder instance
         */
        public EcsVolumeBuilder volumeType(EcsVolumeType volumeType) {
            mVolumeType = volumeType;
            return this;
        }
    }

    private final Volume mVolume;

    private final DeletionLevel mDeletionLevel;

    /**
     * Creates a EcsVolume object
     *
     * @param volume
     * @param deletionLevel
     */
    private EcsVolume(Volume volume, DeletionLevel deletionLevel) {
        mVolume = volume;
        mDeletionLevel = deletionLevel;
    }

    public static EcsVolumeBuilder builder(final int volumeSize) {
        return new EcsVolumeBuilder(ControllerUtil.createName(), volumeSize);
    }

    public static EcsVolumeBuilder builder(final String name, final int volumeSize) {
        return new EcsVolumeBuilder(name, volumeSize);
    }

    public Volume get() {
        return mVolume;
    }

    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    public String getImageRef() {
        return mVolume.getImageRef();
    }

    public String getName() {
        return mVolume.getName();
    }

    public String getSnapshot() {
        return mVolume.getSnapshotId();
    }

    public Status getStatus() {
        return Status.fromValue(mVolume.getStatus().toString());
    }

    @Override
    public String toString() {
        return String.format("EcsVolume{DeletionLevel=%s, os4jVolume{%s}}", mDeletionLevel, mVolume);
    }
}

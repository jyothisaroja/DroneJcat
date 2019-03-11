/**
 *
 */
package com.jcat.cloud.fw.components.model.storage.block;

import org.openstack4j.api.Builders;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.builder.VolumeTypeBuilder;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015- initial version
 *
 */
public class EcsVolumeType extends EcsComponent {
    public static class EcsVolumeTypeBuilder {

        private final VolumeTypeBuilder mVolumeTypeBuilder;
        private DeletionLevel mDeletionLevel = DeletionLevel.TEST_CASE;

        private EcsVolumeTypeBuilder(final String volumeTypeName) {
            mVolumeTypeBuilder = Builders.volumeType().name(volumeTypeName);
        }

        public EcsVolumeType build() {
            return new EcsVolumeType(mVolumeTypeBuilder.build(), mDeletionLevel);
        }

        public EcsVolumeTypeBuilder setDeletionLevel(DeletionLevel deletionLevel) {
            mDeletionLevel = deletionLevel;
            return this;
        }
    }

    private final VolumeType mVolumeType;

    private final DeletionLevel mDeletionLevel;

    /**
     * @param volumeType
     * @param deletionLevel
     */
    private EcsVolumeType(VolumeType volumeType, DeletionLevel deletionLevel) {
        mVolumeType = volumeType;
        mDeletionLevel = deletionLevel;
    }

    public static EcsVolumeTypeBuilder builder(final String volumeTypeName) {
        return new EcsVolumeTypeBuilder(volumeTypeName);
    }

    public VolumeType get() {
        return mVolumeType;
    }

    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    public String getName() {
        return mVolumeType.getName();
    }
}

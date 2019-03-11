package com.jcat.cloud.fw.components.system.cee.openstack.nova;

import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.openstack.compute.domain.NovaBlockDeviceMappingCreate;

/**
 * Class which represents block device mapping for a VM
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-10-13 initial version
 *
 */
public final class EcsBlockDevice {
    public static class Builder {
        private BlockDeviceMappingBuilder mNovaBlockDeviceMappingBuilder;
        private boolean deleteOnTerminationProvided = false;
        private boolean volumeSizeProvided = false;

        Builder(BlockDeviceMappingBuilder novaBlockDeviceMappingBuilder) {
            mNovaBlockDeviceMappingBuilder = novaBlockDeviceMappingBuilder;
        }

        Builder(BDMSourceType sourceType, String sourceId, BDMDestType destType) {
            mNovaBlockDeviceMappingBuilder = NovaBlockDeviceMappingCreate.builder().sourceType(sourceType)
                    .uuid(sourceId).destinationType(destType);
        }

        public Builder bootIndex(int index) {
            mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.bootIndex(index);
            return this;
        }

        public EcsBlockDevice build() {
            if (!deleteOnTerminationProvided) {
                // if user does not provide deleteOnTermination, by default it is set to true
                mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.deleteOnTermination(true);
            }

            if (!volumeSizeProvided) {
                // if user does not provide volume size, use default size
                mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.volumeSize(DEFAULT_VOLUME_SIZE);
            }
            return new EcsBlockDevice(mNovaBlockDeviceMappingBuilder.build());
        }

        public Builder deleteOnTermination(boolean deleteOnTermination) {
            deleteOnTerminationProvided = true;
            mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.deleteOnTermination(deleteOnTermination);
            return this;
        }

        public Builder deviceName(String deviceName) {
            mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.deviceName(deviceName);
            return this;
        }

        public Builder uuid(String id) {
            mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.uuid(id);
            return this;
        }

        public Builder volumeSize(int volumeSize) {
            volumeSizeProvided = true;
            mNovaBlockDeviceMappingBuilder = mNovaBlockDeviceMappingBuilder.volumeSize(volumeSize);
            return this;
        }
    }

    /**
     * default volume size (in GB) for boot VM from volume
     */
    private static final int DEFAULT_VOLUME_SIZE = 40;

    private BlockDeviceMappingCreate mBlockDeviceMappingCreate;

    private EcsBlockDevice(BlockDeviceMappingCreate blockDeviceMappingCreate) {
        mBlockDeviceMappingCreate = blockDeviceMappingCreate;
    }

    public static Builder builder() {
        return new Builder(Builders.blockDeviceMapping());
    }

    public static Builder builder(BDMSourceType sourceType, String sourceId, BDMDestType destType) {
        return new Builder(sourceType, sourceId, destType);
    }

    public BlockDeviceMappingCreate get() {
        return mBlockDeviceMappingCreate;
    }

    public Builder toBuilder() {
        return new Builder(mBlockDeviceMappingCreate.toBuilder());
    }
}

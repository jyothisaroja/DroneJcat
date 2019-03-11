package com.jcat.cloud.fw.components.model.image;

import org.openstack4j.api.Builders;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.Image.ImageVisibility;
import org.openstack4j.model.image.v2.builder.ImageBuilder;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;

/**
 * Class which represents a Image
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce - 2015-01-29 - initial version (includes functionality of previous VmImage)
 * @author ethssce - 2015-02-02 - added EcsImageUpdater
 * @author eedelk - 2015-02-03 - added DeletionLevel
 * @author ethssce - 2015-02-11 - renamed setDeletionLevel to deletionLevel
 * @author ehosmol - 2015-02-19 - Restructure EcsImage
 * @author ehosmol - 2015-05-08 - Adopted to {@link CeeVmImage}, tag created images by source {@link CeeVmImage} name
 * @author eelimei - 2016-01-18 - Added VmImage enum BAT_IMAGE
 * @author zdagjyo - 2017-04-01 - Added VmImage enum OPEN_SUSE_ROOT_IMAGE
 * @author zdagjyo - 2018-04-11 - Added VmImage enum BAT_UBUNTU_16
 */
public class EcsImage extends EcsComponent {

    /**
     * Enum used to create an openstack image. Images names should start with JCAT prefix.
     * <p>
     * <b>Copyright:</b> Copyright (c) 2015
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author ehosmol 2015-02-27
     *
     */
    public enum CeeVmImage implements VmImage {

        ATLAS_IMAGE(Name.ATLAS_IMAGE_NAME, "atlasadm", "qwqwqw", SessionPrompts.VM_PROMPT_ATLAS, ContainerFormat.BARE,
                DiskFormat.QCOW2, ""),

        BAT_JEOS(Name.BAT_JEOS_IMAGE_NAME, SharedConstants.ROOT, SharedConstants.ROOT, SessionPrompts.VM_PROMPT_BAT,
                ContainerFormat.BARE, DiskFormat.ISO,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/bat-opensuse-12.1-jeos-x86_64-0.0.2.0.img.gz"),

        BAT_IMAGE(Name.BAT_IMAGE_NAME, "batman", "passw0rd", SessionPrompts.COMMON_PROMPT, ContainerFormat.BARE,
                DiskFormat.QCOW2, ""),

        BAT_UBUNTU(Name.BAT_UBUNTU_IMAGE_NAME, "root", "root", SessionPrompts.VM_PROMPT_BAT, ContainerFormat.OVF,
                DiskFormat.RAW,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/ubuntu-server-64bit-12.04-watchdog-enabled-CEE8.img"),

        BAT_UBUNTU_16("JCAT_bat-ubuntu-16.04-server-64bit_v1.5", "root", "root", SessionPrompts.VM_PROMPT_BAT,
                ContainerFormat.OVF, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/bat-ubuntu-16.04-server-64bit_v1.5.img"),

        COMMON_CIRROS(Name.COMMON_CIRROS_IMAGE_NAME, "cirros", "cubswin:)", SessionPrompts.VM_PROMPT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/cirros/0.3.1/cirros-0.3.1-x86_64_mvnic.img"),

        DPDK(Name.DPDK_IMAGE_NAME, "cloud", "cloud", SessionPrompts.COMMON_PROMPT, ContainerFormat.BARE,
                DiskFormat.QCOW2, "proj-ecs-dev-local/se/ericsson/ecs/iv/images/"
                        + "characteristics_performance/dpdk/SPOCK_DPDK_VM_20140211.img"),

        MSP_ECS(Name.MSP_ECS_IMAGE_NAME, SharedConstants.ROOT, "wapwap12", SessionPrompts.COMMON_PROMPT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/MSP_ECS/PA1/MSP_ECS-PA1.img"),

        MSP_LOAD(Name.MSP_LOAD_IMAGE_NAME, SharedConstants.ROOT, SharedConstants.ROOT, SessionPrompts.COMMON_PROMPT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/MSP_Load/RAN/MSP_Load-RAN-PA7.img"),

        MSP_WWW(Name.MSP_WWW_IMAGE_NAME, SharedConstants.ROOT, SharedConstants.ROOT, SessionPrompts.COMMON_PROMPT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/MSP_WWW/Apache/MSP_WWW-Apache-PA1.img"),

        OPEN_SUSE_IMAGE(Name.OPEN_SUSE_IMAGE_NAME, "batman", "passw0rd", SessionPrompts.VM_PROMPT_BAT,
                ContainerFormat.OVF, DiskFormat.QCOW2, "proj-ecs-dev-local/se/ericsson/ecs/iv/images/"
                        + "bat-opensuse/12.1/bat-opensuse-cee8-12.1-jeos-x86_64-0.0.2.0.img"),

        OPEN_SUSE_ROOT_IMAGE(Name.OPEN_SUSE_ROOT_IMAGE_NAME, "root", "passw0rd", SessionPrompts.VM_PROMPT_OPENSUSE_ROOT,
                ContainerFormat.OVF, DiskFormat.QCOW2, "proj-ecs-dev-local/se/ericsson/ecs/iv/images/"
                        + "bat-opensuse/12.1/bat-opensuse-cee8-12.1-jeos-x86_64-0.0.2.0.img"),

        TC_STR6_491_IMAGE(Name.TC_STR6_491_IMAGE_NAME, "root", "root", SessionPrompts.VM_PROMPT_BAT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/TC_stR6_491_imagefile.img"),

        TEST_IMAGE(Name.TEST_IMAGE_NAME, SharedConstants.ROOT, SharedConstants.ROOT, SessionPrompts.VM_PROMPT,
                ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/cirros/0.3.1/cirros-0.3.1-x86_64-blank.img"),

        OMTOOL_WILY(Name.OMTOOL_WILY_IMAGE_NAME, SharedConstants.ROOT, SharedConstants.ROOT,
                SessionPrompts.COMMON_PROMPT, ContainerFormat.BARE, DiskFormat.QCOW2,
                "proj-ecs-dev-local/se/ericsson/ecs/bat/OMtool/wily-server-cloudimg-i386-disk1.img"),

        UBUNTU_14_04(Name.UBUNTU_14_04_IMAGE_NAME, "ubuntu", "", SessionPrompts.COMMON_PROMPT, ContainerFormat.BARE,
                DiskFormat.QCOW2, "proj-ecs-dev-local/se/ericsson/ecs/iv/images/characteristics_performance/"
                        + "ubuntu_14_04/trusty-server-cloudimg-i386-disk1.img");

        public static class Name {
            public static final String ATLAS_IMAGE_NAME = "atlas_vm-root-fs";
            public static final String BAT_JEOS_IMAGE_NAME = "JCAT_bat-JEOS";
            public static final String BAT_IMAGE_NAME = "BAT-image";
            public static final String BAT_UBUNTU_IMAGE_NAME = "JCAT_bat-ubuntu-12.04-server-64bit_pa2";
            public static final String COMMON_CIRROS_IMAGE_NAME = "JCAT_Common_CirrOS_i386";
            public static final String DPDK_IMAGE_NAME = "JCAT_DPDKLatencyTool";
            public static final String MSP_ECS_IMAGE_NAME = "JCAT_MSP_ECS-PA1";
            public static final String MSP_LOAD_IMAGE_NAME = "JCAT_MSP_Load-RAN-PA7";
            public static final String MSP_WWW_IMAGE_NAME = "JCAT_MSP_WWW-Apache-PA1";
            public static final String OPEN_SUSE_IMAGE_NAME = "JCAT_OPEN_SUSE";
            public static final String OPEN_SUSE_ROOT_IMAGE_NAME = "JCAT_OPEN_SUSE_ROOT";
            public static final String TC_STR6_491_IMAGE_NAME = "TC_stR6_491_image";
            public static final String TEST_IMAGE_NAME = "JCAT_CirrOS-0.3.1-x86_64_blank";
            public static final String OMTOOL_WILY_IMAGE_NAME = "Wily";
            public static final String UBUNTU_14_04_IMAGE_NAME = "JCAT_Ubuntu-14.04-trusty";

        }

        /**
         * Container Format
         */
        private ContainerFormat mContainerFormat;

        /**
         * Disk Format
         */
        private DiskFormat mDiskFormat;

        /**
         * Image name
         */
        private String mName;

        /**
         * User password
         */
        private String mPassword;

        /**
         * Regexp prompt of the image
         */
        private String mRegexPrompt;

        /**
         * The image source path in Artifactory
         */
        private String mSourcePathInArtifactory;

        /**
         * User name
         */
        private String mUserName;

        CeeVmImage(String name, String userName, String password, String regexPrompt, ContainerFormat containerFormat,
                DiskFormat diskFormat, String sourceInArtifactory) {

            mName = name;
            mUserName = userName;
            mPassword = password;
            mRegexPrompt = regexPrompt;
            mContainerFormat = containerFormat;
            mDiskFormat = diskFormat;
            mSourcePathInArtifactory = sourceInArtifactory;
        }

        /**
         * Method to get VmImage containing Image metadata
         *
         * @param imageName - Name of the used image
         * @return VmImage - Enum containing the metadata
         */
        public static VmImage getImage(String imageName) {
            return VmImage.getImage(imageName, CeeVmImage.values());
        }

        @Override
        public ContainerFormat getContainerFormat() {
            return ContainerFormat.valueOf(mContainerFormat.value());
        }

        @Override
        public DiskFormat getDiskFormat() {
            return DiskFormat.valueOf(mDiskFormat.value());
        }

        @Override
        public final String getName() {
            return mName;
        }

        @Override
        public String getPassword() {
            return mPassword;
        }

        @Override
        public String getRegexPrompt() {
            return mRegexPrompt;
        }

        @Override
        public String getSourcePathInArtifactory() {
            return mSourcePathInArtifactory;
        }

        @Override
        public String getUserName() {
            return mUserName;
        }
    }

    public enum ContainerFormat {
        /**
         * This indicates what is stored in Glance is an Amazon kernel image
         */
        AKI,

        /**
         * This indicates what is stored in Glance is an Amazon machine image
         */
        AMI,

        /**
         * This indicates what is stored in Glance is an Amazon ramdisk image
         */
        ARI,

        /**
         * This indicates there is no container or metadata envelope for the image
         */
        BARE,

        /**
         * This is the OVA container format
         */
        OVA,

        /**
         * This is the OVF container format
         */
        OVF,

        /**
         * Image is a container of type DOCKER
         */
        DOCKER,

        /**
         * Type unknown
         */
        UNRECOGNIZED;

        public String value() {
            return name().toUpperCase();
        }

    }

    public enum DiskFormat {
        /**
         * This indicates what is stored in Glance is an Amazon kernel image
         */
        AKI,
        /**
         * This indicates what is stored in Glance is an Amazon machine image
         */
        AMI,
        /**
         * This indicates what is stored in Glance is an Amazon ramdisk image
         */
        ARI,
        /**
         * An archive format for the data contents of an optical disc (e.g. CDROM).
         */
        ISO,
        /**
         * A disk format supported by the QEMU emulator that can expand dynamically and supports Copy on
         * Write
         */
        QCOW2,
        /**
         * This is an unstructured disk image format
         */
        RAW,

        /**
         * Type unknown
         */
        UNRECOGNIZED,

        /**
         * A disk format supported by VirtualBox virtual machine monitor and the QEMU emulator
         */
        VDI,

        /**
         * This is the VHD disk format, a common disk format used by virtual machine monitors from
         * VMWare, Xen, Microsoft, VirtualBox, and others
         */
        VHD,

        /**
         * Another common disk format supported by many common virtual machine monitors
         */
        VHDX,

        /**
         * Another common disk format supported by many common virtual machine monitors
         */
        VMDK;

        public String value() {
            return name().toUpperCase();
        }
    }

    /**
     * Builder class for EcsImage
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2015
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author ehosmol 2015- initial version
     *
     */
    public static class EcsImageBuilder {
        private DeletionLevel bDeletionLevel = DeletionLevel.TEST_CASE;
        private ImageBuilder bImageBuilder;
        private final VmImage mVmImage;

        /**
         * Constructor used by framework
         *
         * @param {@link Image}
         * @param {@link VmImage}
         */
        protected EcsImageBuilder(Image image, VmImage vmImage) {
            bImageBuilder = image.toBuilder();
            mVmImage = vmImage;
        }

        /**
         * This constructor is just used for update purposes
         *
         * @param id - id of the image in open stack
         */
        protected EcsImageBuilder(String id) {
            bImageBuilder = Builders.imageV2().id(id);
            mVmImage = null;
            bDeletionLevel = null;
        }

        /**
         * Builder to create EcsImage. Use this builder if you want one of the many default images specified as model
         * for the builder.
         *
         * @param vmImage - VmImage - uses one of the many default images specified as model for the builder
         */
        protected EcsImageBuilder(VmImage vmImage) {
            // use default values for isPublic
            bImageBuilder = Builders.imageV2().name(vmImage.getName())
                    .containerFormat(convertToOS4JContainerFormat(vmImage.getContainerFormat()))
                    .diskFormat(convertToOS4JDiskFormat(vmImage.getDiskFormat()));
            mVmImage = vmImage;
        }

        public EcsImage build() {
            if (mVmImage != null) {
                // tag each created image with source VmImage name
                bImageBuilder.additionalProperty(ECS_VM_IMAGE_NAME, mVmImage.getName());
            }
            EcsImage ecsImage = new EcsImage(bImageBuilder.build(), mVmImage, bDeletionLevel);
            return ecsImage;
        }

        public EcsImageBuilder containerFormat(ContainerFormat containerFormat) {
            bImageBuilder = bImageBuilder.containerFormat(convertToOS4JContainerFormat(containerFormat));
            return this;
        }

        /**
         * Sets deletion level. Default level is currently TEST_CASE
         */
        public EcsImageBuilder deletionLevel(DeletionLevel deletionLevel) {
            bDeletionLevel = deletionLevel;
            return this;
        }

        public EcsImageBuilder diskFormat(DiskFormat diskFormat) {
            bImageBuilder = bImageBuilder.diskFormat(convertToOS4JDiskFormat(diskFormat));
            return this;
        }

        public EcsImageBuilder instanceUuid(String instanceUuid) {
            bImageBuilder = bImageBuilder.instanceUuid(instanceUuid);
            return this;
        }

        public EcsImageBuilder minDisk(Long minDisk) {
            bImageBuilder = bImageBuilder.minDisk(minDisk);
            return this;
        }

        public EcsImageBuilder minRam(Long minRam) {
            bImageBuilder = bImageBuilder.minRam(minRam);
            return this;
        }

        public EcsImageBuilder name(String name) {
            bImageBuilder = bImageBuilder.name(name);
            return this;
        }

        public EcsImageBuilder property(String key, String value) {
            bImageBuilder = bImageBuilder.additionalProperty(key, value);
            return this;
        }

        public EcsImageBuilder visibility(ImageVisibility visibility) {
            bImageBuilder = bImageBuilder.visibility(visibility);
            return this;
        }
    }

    public static class SessionPrompts {
        public static final String COMMON_PROMPT = ".*[\\$#].*$";
        public static final String VM_PROMPT = "^\\$\\s$|^\\#\\s$";
        public static final String VM_PROMPT_ATLAS = ".*@atlas:.*[$\\#].*";
        public static final String VM_PROMPT_BAT = ".*:~[\\$\\>#].?$" + "|" + EcsSession.REGEX_BASIC_PROMPTS;
        public static final String VM_PROMPT_OPENSUSE_ROOT = ".*:~ [\\$\\>#].?$";
    }

    public static class SharedConstants {
        public static final String ROOT = "root";
    }

    public static final String ECS_VM_IMAGE_NAME = "ecs_vm_image_name";

    private final DeletionLevel mDeletionLevel;

    /**
     * Os4j Image
     */
    private Image mImage;

    /**
     * {@link VmImage}
     */
    private final VmImage mVmImage;

    public EcsImage(Image buildImage, VmImage vmImage, DeletionLevel bDeletionLevel) {
        mImage = buildImage;
        mVmImage = vmImage;
        mDeletionLevel = bDeletionLevel;
    }

    private static ContainerFormat convertFromOS4JContainerFormat(
            org.openstack4j.model.image.v2.ContainerFormat containerFormat) {
        switch (containerFormat) {
        case BARE:
            return ContainerFormat.BARE;
        case OVA:
            return ContainerFormat.OVA;
        case OVF:
            return ContainerFormat.OVF;
        case AKI:
            return ContainerFormat.AKI;
        case ARI:
            return ContainerFormat.ARI;
        case AMI:
            return ContainerFormat.AMI;
        case DOCKER:
            return ContainerFormat.DOCKER;
        case UNRECOGNIZED:
            return ContainerFormat.UNRECOGNIZED;
        }
        return null;
    }

    private static DiskFormat convertFromOS4JDiskFormat(org.openstack4j.model.image.v2.DiskFormat diskFormat) {
        switch (diskFormat) {
        case RAW:
            return DiskFormat.RAW;
        case VHD:
            return DiskFormat.VHD;
        case VHDX:
            return DiskFormat.VHDX;
        case VMDK:
            return DiskFormat.VMDK;
        case VDI:
            return DiskFormat.VDI;
        case ISO:
            return DiskFormat.ISO;
        case QCOW2:
            return DiskFormat.QCOW2;
        case AKI:
            return DiskFormat.AKI;
        case ARI:
            return DiskFormat.ARI;
        case AMI:
            return DiskFormat.AMI;
        case UNRECOGNIZED:
            return DiskFormat.UNRECOGNIZED;
        }
        return null;
    }

    private static org.openstack4j.model.image.v2.ContainerFormat convertToOS4JContainerFormat(
            ContainerFormat containerFormat) {
        switch (containerFormat) {
        case BARE:
            return org.openstack4j.model.image.v2.ContainerFormat.BARE;
        case DOCKER:
            return org.openstack4j.model.image.v2.ContainerFormat.DOCKER;
        case OVA:
            return org.openstack4j.model.image.v2.ContainerFormat.OVA;
        case OVF:
            return org.openstack4j.model.image.v2.ContainerFormat.OVF;
        case AKI:
            return org.openstack4j.model.image.v2.ContainerFormat.AKI;
        case ARI:
            return org.openstack4j.model.image.v2.ContainerFormat.ARI;
        case AMI:
            return org.openstack4j.model.image.v2.ContainerFormat.AMI;
        case UNRECOGNIZED:
            return org.openstack4j.model.image.v2.ContainerFormat.UNRECOGNIZED;
        }
        return null;
    }

    private static org.openstack4j.model.image.v2.DiskFormat convertToOS4JDiskFormat(DiskFormat diskFormat) {
        switch (diskFormat) {
        case RAW:
            return org.openstack4j.model.image.v2.DiskFormat.RAW;
        case VHD:
            return org.openstack4j.model.image.v2.DiskFormat.VHD;
        case VHDX:
            return org.openstack4j.model.image.v2.DiskFormat.VHDX;
        case VMDK:
            return org.openstack4j.model.image.v2.DiskFormat.VMDK;
        case VDI:
            return org.openstack4j.model.image.v2.DiskFormat.VDI;
        case ISO:
            return org.openstack4j.model.image.v2.DiskFormat.ISO;
        case QCOW2:
            return org.openstack4j.model.image.v2.DiskFormat.QCOW2;
        case AKI:
            return org.openstack4j.model.image.v2.DiskFormat.AKI;
        case ARI:
            return org.openstack4j.model.image.v2.DiskFormat.ARI;
        case AMI:
            return org.openstack4j.model.image.v2.DiskFormat.AMI;
        case UNRECOGNIZED:
            return org.openstack4j.model.image.v2.DiskFormat.UNRECOGNIZED;
        }
        return null;
    }

    /**
     * Builder to create EcsImage. use when you want to create EcsImage from existing OS4j Image. Just used by
     * framework.
     *
     * @param imageBuilder - Os4j image
     * @return {@link EcsImageBuilder}
     */
    public static EcsImageBuilder builder(Image image, VmImage vmImage) {
        return new EcsImageBuilder(image, vmImage);
    }

    /**
     * Builder to create EcsImage. Use this builder if you want to use one of the default images specified as model for
     * the builder.
     * Deletion level will be set to TEST_CASE by default
     *
     * @param vmImage - VmImage - uses one of the many default images specified as model for the builder
     */
    public static EcsImageBuilder builder(VmImage vmImage) {
        return new EcsImageBuilder(vmImage);
    }

    /**
     * Method to create an EcsImageBuilder for the update purpose. id is mandatory and later builder
     * will be used just for fields which are going to be updated
     *
     * @param id
     * @return
     */
    public static EcsImageBuilder imageWithId(String id) {
        return new EcsImageBuilder(id);
    }

    /**
     *
     * @return Container Format
     */
    public ContainerFormat getContainerFormat() {
        return convertFromOS4JContainerFormat(mImage.getContainerFormat());
    }

    /**
     *
     * @return Deletion Level
     */
    public DeletionLevel getDeletionLevel() {
        return mDeletionLevel;
    }

    /**
     *
     * @return DiskFormat
     */
    public DiskFormat getDiskFormat() {
        return convertFromOS4JDiskFormat(mImage.getDiskFormat());
    }

    /**
     * Get os4j image
     *
     * @return image
     */
    public Image getImage() {
        return mImage;
    }

    /**
     * @return User password
     */
    public String getPassword() {
        return mVmImage.getPassword();
    }

    /**
     * @return Regexp prompt of the image
     */
    public String getRegexPrompt() {
        return mVmImage.getRegexPrompt();
    }

    /**
     * @return the image source path in Artifactory
     */
    public String getSourcePathInArtifactory() {
        return mVmImage.getSourcePathInArtifactory();
    }

    /**
     * @return User name
     */
    public String getUserName() {
        return mVmImage.getUserName();
    }

    /**
     * Set OS4J {@link Image} inside {@link EcsImage}
     *
     * @param Image the image to set
     */
    public void setImage(Image image) {
        mImage = image;
    }

    /**
     * Convert an {@link EcsImage} to {@link EcsImageBuilder}
     *
     * @return {@link EcsImageBuilder}
     */
    public EcsImageBuilder toBuilder() {
        return new EcsImageBuilder(mImage, mVmImage);
    }

    @Override
    public String toString() {
        return String.format(
                "EcsImage{VmImage=%s, DeletionLevel=%s, ArtifactoryPath=%s, os4jImage{Name=%s, DiskFormat=%s, ContainerFormat=%s}}",
                mVmImage.getName(), mDeletionLevel, mVmImage.getSourcePathInArtifactory(), mImage.getName(),
                mImage.getDiskFormat(), mImage.getContainerFormat());
    }
}

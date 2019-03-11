package com.jcat.cloud.fw.components.system.cee.openstack.nova;

import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.builder.FlavorBuilder;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**<p>
 * Class which represents a Flavor
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 *
 * @author eqinann 2015-05-19 initial version
 *
 */
public class EcsFlavor extends EcsComponent {
    public static class EcsFlavorBuilder {
        private FlavorBuilder mFlavorBuilder;

        protected EcsFlavorBuilder(int ram, int vcpus, int disk) {
            this(ControllerUtil.createName(), ram, vcpus, disk);
        }

        protected EcsFlavorBuilder(PredefinedFlavor flavor) {
            mFlavorBuilder = Builders.flavor().id(flavor.id()).name(flavor.flavorName()).ram(flavor.ram())
                    .vcpus(flavor.vcpus()).disk(flavor.disk()).ephemeral(flavor.ephemeral()).swap(flavor.swap())
                    .rxtxFactor(flavor.rxtxFactor()).isPublic(flavor.isPublic());
        }

        protected EcsFlavorBuilder(String name, int ram, int vcpus, int disk) {
            mFlavorBuilder = Builders.flavor().name(name).ram(ram).vcpus(vcpus).disk(disk);
        }

        public EcsFlavor build() {
            return new EcsFlavor(mFlavorBuilder.build());
        }

        public EcsFlavorBuilder ephemeral(int ephemeral) {
            mFlavorBuilder.ephemeral(ephemeral);
            return this;
        }

        public EcsFlavorBuilder id(String id) {
            mFlavorBuilder.id(id);
            return this;
        }

        public EcsFlavorBuilder isPublic(boolean isPublic) {
            mFlavorBuilder.isPublic(isPublic);
            return this;
        }

        public EcsFlavorBuilder rxtxFactor(float rxtxFactor) {
            mFlavorBuilder.rxtxFactor(rxtxFactor);
            return this;
        }

        public EcsFlavorBuilder swap(int swap) {
            mFlavorBuilder.swap(swap);
            return this;
        }
    }

    /**
     * Predefined flavors
     * The numbers are extracted from Mos5.1
     *
     */
    public enum PredefinedFlavor {
        //
        // When saving changes, please UNDO (Ctrl+z) right after saving, if auto-format is configured
        //
        M1_TINY("1", "m1.tiny", 512, 1, 1, 0, 0, 1.0f, true), M1_SMALL("2", "m1.small", 2048, 1, 20, 0, 0, 1.0f, true), M1_MEDIUM(
                "3", "m1.medium", 4096, 2, 40, 0, 0, 1.0f, true), M1_LARGE("4", "m1.large", 8192, 4, 80, 0, 0, 1.0f,
                        true), M1_XLARGE("5", "m1.xlarge", 16384, 8, 160, 0, 0, 1.0f, true);

        private final String mDefaultId;
        private final int mDisk;
        private final int mEphemeral;
        private final String mFlavorName;
        private final boolean mIsPublic;
        private final int mRam;
        private final float mRxtxFactor;
        private final int mSwap;
        private final int mVcpus;

        PredefinedFlavor(String defaultId, String flavorName, int ram, int vcpus, int disk, int ephemeral, int swap,
                float rxtxFactor, boolean isPublic) {
            mDefaultId = defaultId;
            mFlavorName = flavorName;
            mRam = ram;
            mVcpus = vcpus;
            mDisk = disk;
            mEphemeral = ephemeral;
            mSwap = swap;
            mRxtxFactor = rxtxFactor;
            mIsPublic = isPublic;
        }

        /**
         * Find the PredefinedFlavor with a given flavor name
         *
         * @param flavorName Predefined flavor name
         * @return PredefinedFlavor object
         */
        public static PredefinedFlavor withName(String flavorName) {
            PredefinedFlavor[] flavors = PredefinedFlavor.values();
            for (PredefinedFlavor flavor : flavors) {
                if (flavor.flavorName().equals(flavorName)) {
                    return flavor;
                }
            }
            return null;
        }

        public int disk() {
            return mDisk;
        }

        public int ephemeral() {
            return mEphemeral;
        }

        public String flavorName() {
            return mFlavorName;
        }

        public String id() {
            return mDefaultId;
        }

        public boolean isPublic() {
            return mIsPublic;
        }

        public int ram() {
            return mRam;
        }

        public float rxtxFactor() {
            return mRxtxFactor;
        }

        public int swap() {
            return mSwap;
        }

        public int vcpus() {
            return mVcpus;
        }
    }

    private Flavor mOs4jFlavor;

    private EcsFlavor(Flavor flavor) {
        mOs4jFlavor = flavor;
    }

    /**
     * Build a customized EcsFlavor without flavor's name
     *
     * @param ram
     * @param vcpus
     * @param disk
     * @return {@link EcsFlavorBuilder}
     */
    public static EcsFlavorBuilder builder(int ram, int vcpus, int disk) {
        return new EcsFlavorBuilder(ram, vcpus, disk);
    }

    /**
     * Build a EcsFlavor using predefined flavor
     *
     * @param flavor {@link PredefinedFlavor}
     * @return {@link EcsFlavorBuilder}
     */
    public static EcsFlavorBuilder builder(PredefinedFlavor flavor) {
        return new EcsFlavorBuilder(flavor);
    }

    /**
     * Build a customized EcsFlavor
     *
     * @param name
     * @param ram
     * @param vcpus
     * @param disk
     * @return {@link EcsFlavorBuilder}
     */
    public static EcsFlavorBuilder builder(String name, int ram, int vcpus, int disk) {
        return new EcsFlavorBuilder(name, ram, vcpus, disk);
    }

    /**
     * Get OpenStack4J flavor instance that stored in EcsFlavor
     *
     * @return os4j Flavor
     */
    public Flavor get() {
        return mOs4jFlavor;
    }

    @Override
    public String toString() {
        return "EcsFlavor{id=" + (mOs4jFlavor.getId() == null ? "Unspecified" : mOs4jFlavor.getId()) + ", name="
                + mOs4jFlavor.getName() + ", vcpus=" + mOs4jFlavor.getVcpus() + ", ram=" + mOs4jFlavor.getRam()
                + ", disk=" + mOs4jFlavor.getDisk() + ", ephemeral=" + mOs4jFlavor.getEphemeral() + ", swap="
                + mOs4jFlavor.getSwap() + ", rxtx_factor=" + mOs4jFlavor.getRxtxFactor() + ", is_public="
                + mOs4jFlavor.isPublic() + "}";
    }

}

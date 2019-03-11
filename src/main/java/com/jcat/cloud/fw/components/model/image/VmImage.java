package com.jcat.cloud.fw.components.model.image;

import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.components.model.image.EcsImage.ContainerFormat;
import com.jcat.cloud.fw.components.model.image.EcsImage.DiskFormat;

/**
 * Interface to get properties for openstack images
 *
 * @author emagnbr
 *
 */
public interface VmImage {

    /**
     * Method to get VmImage containing Image metadata
     *
     * @param imageName - Name of the used image
     * @param images - Array of images to look in
     * @return VmImage - Enum containing the metadata
     */
    public static VmImage getImage(String imageName, VmImage[] images) {
        for (VmImage image : images) {
            if (imageName.equals(image.getName())) {
                return image;
            }
        }
        EcsLogger.getLogger(VmImage.class).error("Could not find an image defined with name: " + imageName);
        return null;
    }

    public abstract ContainerFormat getContainerFormat();

    public abstract DiskFormat getDiskFormat();

    public abstract String getName();

    public abstract String getPassword();

    public abstract String getRegexPrompt();

    public abstract String getSourcePathInArtifactory();

    public abstract String getUserName();

}

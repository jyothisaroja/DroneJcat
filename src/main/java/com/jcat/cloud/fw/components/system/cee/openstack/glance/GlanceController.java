package com.jcat.cloud.fw.components.system.cee.openstack.glance;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.ws.rs.core.Response;

import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.api.image.v2.ImageService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.v2.CachedImage;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.Image.ImageStatus;
import org.openstack4j.model.image.v2.builder.ImageBuilder;

import com.ecs.artifactory.ArtifactoryClient;
import com.ecs.artifactory.ArtifactoryClientConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.components.model.image.EcsImage.CeeVmImage;
import com.jcat.cloud.fw.components.model.image.EcsImage.ContainerFormat;
import com.jcat.cloud.fw.components.model.image.EcsImage.DiskFormat;
import com.jcat.cloud.fw.components.model.image.EcsImage.EcsImageBuilder;
import com.jcat.cloud.fw.components.model.image.VmImage;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil.DeletionLevel;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains methods associated with Image handling in the ECS.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2014-10-10 initial version
 * @author epergat 2014-11-07 Added cleanup method.
 * @author ethssce 2014-11-24 createImage uses createName for generating names
 * @author ethssce 2015-01-16 waitForImageToBecomeActive set to public
 * @author ethssce 2015-01-30 createImage uses EcsImage
 * @author ethssce 2015-02-02 added updateImage
 * @author eedelk 2015-02-03 createImage: added check for DeletionLevel,
 *         added: getActiveImagesByName, simplified and made public: doesImageExist,
 *         getImageIdByName
 * @author ethssce 2015-02-05 removed old createImage(VmImage), not used anymore
 * @author ethssce 2015-02-11 modified doesImageExist, taking the ID as parameter, fixed bug in LoopHelper
 * @author ehosmol 2015-02-19 reimplement getImage(), createImage() , createCustomImage(), doesImageExist(),
 *         addMember(), removeMember() and updateImage()
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author epergat, ehosmol 2015-05-04 Added support for getting metadata from image when finding the corresponding
 *         EcsImage.
 * @author epergat 2015-06-27 made cleanup more robust
 * @author eelimei 2016-01-18 Changed getImage such that the image does not have to have been created by JCAT if the name corresponds to a VMImage enum
 * @author zdagjyo 2017-03-06 Added method isImageSnapshot
 * @author zdagjyo 2017-03-08 Added methods createFilePayload, downloadImageWithId, imageV2Service and overloaded method createCustomImage
 * @author zdagjyo 2018-09-04 Added method listCachedImages
 */
public final class GlanceController extends EcsComponent {

    /**
     * Image Attribute values
     */
    private static final String ACTIVE = "active";
    private static final String DOWNLOAD_ERROR_MESSAGE = "Failed to download the following artifact from artifactory '%s'."
            + "Has the file been removed or had a name changed?";
    private static final String IMAGE_NOT_EXIST_ERROR = "Image does not exist : ";
    /**
     * Image Attributes
     */
    private static final String NAME = "name";
    private static final String STATUS = "status";
    private static final int IMAGE_UPLOAD_MAX_RETRY = 5;

    /**
     * Keep track of all created images by this instance
     * use CopyOnWriteArraySet due to concurrent access to the set(iterate through it and remove item at the same time)
     */
    private final CopyOnWriteArraySet<String> mCreatedImages;
    /**
     * Logger instance
     */
    private final EcsLogger mLogger = EcsLogger.getLogger(GlanceController.class);

    private final OpenStack4jEcs mOpenStack4jEcs;

    /**
     * The only one allowed to use this constructor is Google Guice.
     */
    @Inject
    private GlanceController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
        mCreatedImages = new CopyOnWriteArraySet<String>();
    }

    /**
     * Creates the payload based on an image file specified by path.
     *
     * @param path - absolute path of the image file
     * @return The image in file as a payload (ready to be sent to openstack).
     */
    private Payload<File> createFilePayload(String path) {
        File imageFile = new File(path);
        Payload<File> payload = Payloads.create(imageFile);
        return payload;
    }

    /**
     * Creates the payload based on the binary from Artifactory.
     *
     * @param imagePathInArtifactory
     * @return The image in artifactory as a payload (ready to be sent to openstack).
     */
    private Payload<InputStream> createPayload(String imagePathInArtifactory) {
        ArtifactoryClientConfiguration artifactoryConfig = new ArtifactoryClientConfiguration(
                CommonParametersValues.ARTIFACTORY_URL, CommonParametersValues.ARTIFACTORY_USERNAME,
                CommonParametersValues.ARTIFACTORY_PASSWORD);

        Response artifactResponse = new ArtifactoryClient(artifactoryConfig)
                .getArtifactResponse(CommonParametersValues.ARTIFACTORY_URL + imagePathInArtifactory);

        if (artifactResponse == null) {
            mLogger.error(String.format(DOWNLOAD_ERROR_MESSAGE, imagePathInArtifactory));
            throw new EcsOpenStackException(
                    "Could not get image payload from artifactory for image " + imagePathInArtifactory);
        }

        Payload<InputStream> payload = Payloads.create(artifactResponse.readEntity(InputStream.class));
        return payload;
    }

    private ImageService imageV2Service() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).imagesV2(),
                ImageService.class);
    }

    /**
     * Creates a member of an image
     *
     * @param imageId - String - id of image
     * @param memberId - String - id of member
     * @return true if member could be created, otherwise false
     */
    public boolean addMember(String imageId, String memberId) {
        return imageV2Service().createMember(imageId, memberId) != null;
    }

    /**
     * Cleanup all allocated resources made in the Glance controller.
     *
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", GlanceController.class, "");
        for (String imageId : mCreatedImages) {
            try {
                deleteImage(imageId);
            } catch (LoopTimeoutException timeoutException) {
                mLogger.error(String.format(
                        "Failed to confirm deletion of image (id:%s) within the given time limit, exception was",
                        imageId, timeoutException));
            } catch (Exception e) {
                mLogger.error(String.format("Got exception while trying to delete image(id:%s), exception was: %s",
                        imageId, e));
            }
        }
        mLogger.info(Verdict.DONE, "Clean up", GlanceController.class, "");
    }

    /**
     * Method which creates Glance Image from an {@link EcsImage}. First make sure an equivalent image does not exist.
     * usage example: createCustomImage( EcsImage.builder(VmImage).minDisk(80).build() );
     *
     * @param imageToUse - An EcsImage built by builder.
     * @return String - created Image Id
     */
    public String createCustomImage(EcsImage imageToUse) {
        return createCustomImage(imageToUse, null);
    }

    /**
     * Method which creates Glance Image from a local image file. First make sure an equivalent image does not exist.
     * usage example: createCustomImage( EcsImage.builder(VmImage).minDisk(80).build(), "~/Ubuntu-1-clone.qcow2");
     *
     * @param imageToUse - An EcsImage built by builder.
     * @param path - absolute path of the local image file
     * @return String - created Image Id
     */
    public String createCustomImage(EcsImage imageToUse, String path) {
        // TODO: current implementation works for cirros images, need to modify if it doesn't work for others
        List<? extends Image> images = imageV2Service().list();
        for (Image image : images) {
            if (image.getName().equals(imageToUse.getImage().getName())
                    && image.getContainerFormat().equals(imageToUse.getImage().getContainerFormat())
                    && image.getDiskFormat().equals(imageToUse.getImage().getDiskFormat()) && image.getSize() != null) {
                mLogger.info(Verdict.EXISTED, EcsImage.class, image.getId());
                if (imageToUse.getDeletionLevel() == DeletionLevel.TEST_CASE) {
                    mCreatedImages.add(image.getId());
                }
                return image.getId();
            }
        }
        Payload<?> payload = null;

        if (path != null) {
            String localImagePath = path;
            payload = createFilePayload(localImagePath);

            if (payload == null) {
                throw new EcsOpenStackException("Failed to get payload from local path");
            }
        } else {
            String imagePathInArtifactory = imageToUse.getSourcePathInArtifactory();
            for (int i = 0; i < IMAGE_UPLOAD_MAX_RETRY; i++) {
                try {
                    payload = createPayload(imagePathInArtifactory);
                } catch (Exception e) {
                    mLogger.warn("Failed to get image payload from artifactory, retry");
                    continue;
                }
                break;
            }

            if (payload == null) {
                throw new EcsOpenStackException(
                        String.format("Failed to get payload from artifactory after %s retries.", imageToUse,
                                IMAGE_UPLOAD_MAX_RETRY));
            }
        }

        mLogger.info(EcsAction.CREATING, imageToUse);
        Image imageCreated = null;
        for (int i = 0; i < IMAGE_UPLOAD_MAX_RETRY; i++) {
            try {
                imageCreated = imageV2Service().create(imageToUse.getImage());
                ActionResponse response = imageV2Service().upload(imageCreated.getId(), payload, imageCreated);
                if (!response.isSuccess()) {
                    throw new EcsOpenStackException("Failed to upload image");
                }
            } catch (Exception e) {
                // retry if ConnectionException happens due to network disturbance
                if (e instanceof ConnectionException) {
                    if (imageCreated != null) {
                        this.deleteImage(imageCreated.getId());
                    }
                    mLogger.warn("Failed to upload image, retry now");
                    continue;
                } else {
                    throw e;
                }
            }
            break;
        }
        if (imageCreated == null) {
            throw new EcsOpenStackException(
                    String.format("Failed to create image %s after %s retries.", imageToUse, IMAGE_UPLOAD_MAX_RETRY));
        }
        mLogger.info(Verdict.CREATED, EcsImage.class, imageCreated.getName() + ", id= " + imageCreated.getId());
        waitForImageToBecomeActive(imageCreated.getId());
        if (imageToUse.getDeletionLevel() == DeletionLevel.TEST_CASE) {
            mCreatedImages.add(imageCreated.getId());
        }
        return imageCreated.getId();
    }

    /**
     * Use this function if you just want to create an image using the most common
     * default values from #{@link CeeVmImage}. The deletion level will automatically be changed to PERMANENT as these most
     * common default images are likely to be re-used.
     *
     * @return - String - created imageID
     */
    public String createImage(VmImage vmImage) {
        EcsImage ecsImage = EcsImage.builder(vmImage).deletionLevel(DeletionLevel.PERMANENT).build();
        return createCustomImage(ecsImage);
    }

    /**
     * Delete a Glance Image. An exception will be thrown if the image was not deleted.
     *
     * @param imageId - String - id of the image to delete.
     */
    public void deleteImage(final String imageId) {
        mLogger.info(EcsAction.DELETING, EcsImage.class, imageId);

        // Delete the image
        imageV2Service().delete(imageId);

        // An image can take time to delete.
        new LoopHelper<Boolean>(Timeout.IMAGE_READY, "The image was not deleted within the given time frame",
                Boolean.TRUE, () -> {
                    Image image = imageV2Service().get(imageId);
                    if (image == null) {
                        return true;
                    } else if (image.getStatus() == ImageStatus.DELETED) {
                        return true;
                    }
                    return false;
                }).run();
        mLogger.info(Verdict.DELETED, EcsImage.class, imageId);
        mCreatedImages.remove(imageId);
    }

    /**
     * Check if the image exists in the ECS Cloud.
     *
     * @param imageId - String - id of image
     * @return true if image exists, otherwise false
     */
    public boolean doesImageExist(String imageId) {
        try {
            getImage(imageId);
        } catch (EcsOpenStackException e) {
            if (e.getMessage().contains(IMAGE_NOT_EXIST_ERROR)) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

    /**
     * Downloads image specified by id to the file specified.
     * The image gets downloaded to local file system. So the specified file should exist in local system.
     *
     * @param imageId - String - id of image
     * @param file - File - The local file to which downloaded image gets saved.
     * @return true if image is downloaded to specified file, otherwise false
     */
    public boolean downloadImageWithId(String imageId, File file) {
        mLogger.info(EcsAction.DOWNLOADING, EcsImage.class, "Id: " + imageId + " to file " + file);
        boolean downloaded = false;
        ActionResponse response = imageV2Service().download(imageId, file);
        if (response.getCode() == 200) {
            downloaded = true;
            if (file.length() < imageV2Service().get(imageId).getSize()) {
                LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.FILE_DOWNLOADED,
                        "failed to download file", Boolean.TRUE, () -> {
                            return file.length() == imageV2Service().get(imageId).getSize();
                        });
                loopHelper.setIterationDelay(10);
                loopHelper.run();
            }
            mLogger.info(Verdict.DOWNLOADED, EcsImage.class, "Id: " + imageId + " to file " + file);
        }
        return downloaded;
    }

    /**
     * Get a filtered list of images with status "active" and which have a given name
     *
     * @param imageName - String - name of image
     * @return List<? extends Image> -filtered list of images
     */
    public List<? extends Image> getActiveImagesByName(String imageName) {

        Map<String, String> imageFilter = ImmutableMap.of(NAME, imageName, STATUS, ACTIVE);
        List<? extends Image> imageList = imageV2Service().list(imageFilter);
        return imageList;
    }

    /**
     * Creates an EcsImage out of an specified image with id in openstack
     *
     * @param imageId - String - openstack image
     * @return {@link EcsImage}
     */
    public EcsImage getImage(String imageId) {
        Image image = imageV2Service().get(imageId);
        if (image == null) {
            throw new EcsOpenStackException(IMAGE_NOT_EXIST_ERROR + imageId);
        }
        String ecsVmImageName = image.getAdditionalPropertyValue(EcsImage.ECS_VM_IMAGE_NAME);
        if (image.getName().contains("BAT-image")) {
            ecsVmImageName = image.getName();
        }
        if (ecsVmImageName == null) {
            throw new EcsOpenStackException(
                    "The Image \"" + image.getName() + "\"(" + imageId + ") is not created by JCAT FW");
        }

        VmImage vmImage = CeeVmImage.getImage(ecsVmImageName);
        if (vmImage != null) {
            return EcsImage.builder(image, vmImage).build();
        }
        throw new EcsOpenStackException(
                "The Image \"" + image.getName() + "\"(" + imageId + ") is not created by JCAT FW");
    }

    /**
     * Get ID of the first image with provided name. Return null if no image with given name was found.
     *
     * @param imageName - String - name of the image
     * @return String - id of the image, null if not found
     */
    public String getImageIdByName(String imageName) {

        Map<String, String> imageFilter = ImmutableMap.of(NAME, imageName);
        List<? extends Image> imageList = imageV2Service().list(imageFilter);
        if (imageList.isEmpty()) {
            return null;
        }
        return imageList.get(0).getId();
    }

    /**
     * Return image status in open stack
     *
     * @param imagetId - String
     * @return String - the status of the image
     */
    public String getImageStatus(String imageId) {
        return imageV2Service().get(imageId).getStatus().toString().toUpperCase();
    }

    public String getProperty(String imageId, String key) {
        return imageV2Service().get(imageId).getAdditionalPropertyValue(key);
    }

    /**
     * Tests if the specified image is a snapshot image
     *
     * @param serverId - The id of the image
     *
     * @return boolean
     */
    public boolean isImageSnapshot(String imageId) {
        String instanceUuiId = imageV2Service().get(imageId).getInstanceUuid();
        if (instanceUuiId == null) {
            mLogger.warn("Instance uuid is not found for the image id: " + imageId + ", hence it is not a snapshot");
            return false;
        }
        mLogger.warn("Instance uuid is found for the image id: " + imageId + ", hence it is a snapshot");
        return true;
    }

    /**
     * Lists the cached images on node
     *
     * @return List<? extends CachedImage> - the list of cached images, null if cache is not enabled
     */
    public List<? extends CachedImage> listCachedImages() {
       return imageV2Service().listCachedImages();
    }

    /**
     * List existing images
     *
     * @return
     */
    public List<? extends EcsImage> listExistingImages() {
        List<? extends Image> imageList = imageV2Service().list();
        List<EcsImage> ecsImageList = new ArrayList<EcsImage>();
        for (Image image : imageList) {
            ecsImageList.add(new EcsImage(image, null, null));
        }
        return ecsImageList;
    }

    /**
     * Remove a member of an image
     *
     * @param imageId - String - id of image
     * @param memberId - String - id of member
     * @return true if the member could be removed from the image, otherwise false
     */
    public boolean removeMember(String imageId, String memberId) {
        ActionResponse response = imageV2Service().deleteMember(imageId, memberId);
        return response.isSuccess();
    }

    /**
     * Gets an locally updated EcsImage instance as input and push that to OpenStack. An updated image will be deleted
     * after testsuit finish.
     *
     * @param {{@link {@link EcsImage}
     * @return {@link EcsImage} updated {@link EcsImage}
     */
    public EcsImage updateImage(EcsImage ecsImage) {
        // TODO: update() openstack4j API not working properly
        // ecsImage.setImage(imageV2Service().update(ecsImage.getImage()));
        mCreatedImages.add(ecsImage.getImage().getId());
        return ecsImage;
    }

    /**
     * Updates an Image in openstack with specified ID. Input is an {@link EcsImageBuilder} containing Id
     * and just the fields which needs to be updated. Example EcsImage.imageWithId(id).name(name)
     * Remember that this method replaces the existing values, so if there is a need to only append a new
     * property, make sure that the new list of properties has all of the existing ones otherwise they will be
     * overwritten.
     *
     * @param ecsImageBuilder - containing image Id and fields need to be updated
     * @return {@link EcsImage} - Updated image
     */
    public EcsImage updateImage(EcsImageBuilder ecsImageBuilder) {
        EcsImage updateInfoImage = ecsImageBuilder.build();

        EcsImage actualImage = getImage(updateInfoImage.getImage().getId());
        ImageBuilder imageBuilder = actualImage.getImage().toBuilder();

        if (updateInfoImage.getImage().getMinDisk() >= 0) {
            imageBuilder.minDisk(updateInfoImage.getImage().getMinDisk());
        }
        if (updateInfoImage.getImage().getMinRam() >= 0) {
            imageBuilder.minRam(updateInfoImage.getImage().getMinRam());
        }
        if (updateInfoImage.getImage().getName() != null) {
            imageBuilder.name(updateInfoImage.getImage().getName());
        }
        if (!updateInfoImage.getContainerFormat().equals(ContainerFormat.UNRECOGNIZED)) {
            imageBuilder.containerFormat(updateInfoImage.getImage().getContainerFormat());
        }
        if (!updateInfoImage.getDiskFormat().equals(DiskFormat.UNRECOGNIZED)) {
            imageBuilder.diskFormat(updateInfoImage.getImage().getDiskFormat());
        }

        actualImage.setImage(imageBuilder.build());
        return updateImage(actualImage);
    }

    /**
     * Wait for the image to become active.
     *
     * @param image
     * @return Exception will be thrown if image does not reach active status
     */
    public void waitForImageToBecomeActive(final String imageId) {
        Timeout timeout = null;
        if (isImageSnapshot(imageId)) {
            timeout = Timeout.SERVER_SNAPSHOT_READY;
        } else {
            timeout = Timeout.IMAGE_READY;
        }
        mLogger.info(EcsAction.STATUS_CHANGING, EcsImage.class, imageId + ", Target status: " + ImageStatus.ACTIVE);
        final String errorMessage = String.format("Image with ID %s did not reach status: %s", imageId,
                ImageStatus.ACTIVE);
        LoopHelper<ImageStatus> loopHelper = new LoopHelper<ImageStatus>(timeout, errorMessage, ImageStatus.ACTIVE,
                () -> {
                    return imageV2Service().get(imageId).getStatus();
                });
        loopHelper.setErrorState(ImageStatus.UNRECOGNIZED);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, getImageStatus(imageId), EcsImage.class, imageId);
    }
}

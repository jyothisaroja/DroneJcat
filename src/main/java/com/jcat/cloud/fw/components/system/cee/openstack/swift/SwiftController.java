package com.jcat.cloud.fw.components.system.cee.openstack.swift;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.storage.ObjectStorageContainerService;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.model.storage.object.SwiftObject;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains methods related with openstack swift.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017-11-02 - initial version
 */
public final class SwiftController extends EcsComponent {

    /**
     * Logger instance
     */
    private final EcsLogger mLogger = EcsLogger.getLogger(SwiftController.class);

    private final OpenStack4jEcs mOpenStack4jEcs;

    @Inject
    protected EcsCicList mEcsCicList;

    /**
     * Hide Utility Class Constructor: Utility classes should not have a public
     * or default constructor.
     */
    @Inject
    private SwiftController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    private ObjectStorageContainerService containerService() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).objectStorage(),
                ObjectStorageContainerService.class).containers();
    }

    private ObjectStorageObjectService objectService() {
        return ControllerUtil.checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).objectStorage(),
                ObjectStorageObjectService.class).objects();
    }

    /**
     * Creates the payload based on a file specified by path.
     *
     * @param path - absolute path of the file
     * @return The content in file as a payload (ready to be sent to openstack).
     */
    public Payload<File> createFilePayload(String path) {
        File imageFile = new File(path);
        Payload<File> payload = Payloads.create(imageFile);
        return payload;
    }

    /**
     * Downloads specified file from specified container.
     *
     * @param containerName - the name of the container
     * @param fileName - the name of the file
     * @return DLPayload - the payload of the downloaded file
     */
    public DLPayload downloadFileFromContainer(String containerName, String fileName) {
        mLogger.info(EcsAction.DOWNLOADING, fileName, SwiftController.class, "from container " + containerName);
        if (!listAllContainers().contains(containerName)) {
            throw new EcsOpenStackException("The container " + containerName + " does not exist");
        }
        DLPayload payload = objectService().download(containerName, fileName);
        if (payload == null) {
            throw new EcsOpenStackException("Failed to download file " + fileName + " from container " + containerName);
        }
        mLogger.info(Verdict.DOWNLOADED, fileName, SwiftController.class, "from container " + containerName);
        return payload;
    }

    /**
     * Lists all containers in swift.
     *
     * @return List<String> - names of all containers in swift
     */
    public List<String> listAllContainers() {
        List<String> containers = new ArrayList<String>();
        for (SwiftContainer container : containerService().list()) {
            containers.add(container.getName());
        }
        return containers;
    }

    /**
     * Lists all files stored in the specified container.
     *
     * @param containerName - String - the name of the container
     * @return List<String> - names of all files stored in the container
     */
    public List<String> listFilesInContainer(String containerName) {
        List<String> files = new ArrayList<String>();
        for (SwiftObject file : objectService().list(containerName)) {
            files.add(file.getName());
        }
        return files;
    }

    /**
     * Uploads specified file to specified container.
     *
     * @param containerName - the name of the container
     * @param fileName - the name of the file
     * @param payload - the payload of the file to be uploaded
     * @return String - the checksum of the uploaded file
     */
    public String uploadFileToContainer(String containerName, String fileName, Payload<?> payload) {
        mLogger.info(EcsAction.UPLOADING, fileName, SwiftController.class, "to container " + containerName);
        ActionResponse response = containerService().create(containerName);
        if (response == null) {
            throw new EcsOpenStackException("Failed to create container " + containerName);
        }
        String fileChecksum = objectService().put(containerName, fileName, payload);
        if (fileChecksum == null) {
            throw new EcsOpenStackException("Failed to upload file " + fileName + " to container " + containerName);
        }
        mLogger.info(Verdict.UPLOADED, fileName, SwiftController.class, "to container " + containerName);
        return fileChecksum;
    }
}

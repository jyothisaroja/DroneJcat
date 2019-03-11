package com.jcat.cloud.fw.components.system.cee.openstack.keystone;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.identity.v3.IdentityService;
import org.openstack4j.api.identity.v3.ProjectService;
import org.openstack4j.api.identity.v3.RoleService;
import org.openstack4j.api.identity.v3.UserService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.RoleAssignment;
import org.openstack4j.model.identity.v3.Service;
import org.openstack4j.model.identity.v3.User;
import org.openstack4j.model.identity.v3.builder.ProjectBuilder;
import org.openstack4j.openstack.identity.v3.domain.KeystoneUser;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.LoopHelper.LoopTimeoutException;
import com.jcat.cloud.fw.components.model.EcsCloudService;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.identity.EcsProject;
import com.jcat.cloud.fw.components.model.identity.EcsProject.EcsProjectBuilder;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains procedures related with openstack keystone
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce - 2014-10-07 initial implementation
 * @author JcatMob - 2014-10-31 Add method for create/delete user
 * @author ethssce - 2014-11-24 createTenant uses createName for generating names
 * @author ethssce - 2015-05-04 created new createTenant(), updateTenant(), getTenant() which use EcsTenant class
 * @author epergat - 2015-06-27 made cleanup more robust
 * @author eqinann - 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo - 2017-04-27 Add method doesTenantExist
 * @author zdagjyo - 2017-12-01 Add method listEndpoints
 * @author zdagjyo - 2018-07-17 Remove tenant APIs and add project APIs createProject(),
 *         updateProject(), getProject() and doesProjectExist()
 * @author zmousar - 2018-07-19 Added listServices and getEndpointForService()
 * @author zdagjyo - 2018-08-08 Modified all APIs to use hardcoded curl requests instead of openstack4j APIs
 * @author zdagjyo - 2018-09-27 Add method getUser()
 * @author zdagjyo - 2018-10-15 Modified all APIs back to use openstack4j APIs
 */
public final class KeystoneController extends EcsComponent {

    private static final String PROJECT_NOT_EXIST_ERROR = "Project does not exist : ";
    /**
     * Keep track of all created projects in this instance
     * use CopyOnWriteArraySet due to concurrent access to the set(iterate through it and remove item at the same time)
     */
    private final Set<String> mCreatedProjectIds = new CopyOnWriteArraySet<String>();;

    /**
     * Keep track of all created users in this instance
     * use CopyOnWriteArraySet due to concurrent access to the set(iterate through it and remove item at the same time)
     */
    private final Set<String> mCreatedUserIds = new CopyOnWriteArraySet<String>();;

    private final EcsLogger mLogger = EcsLogger.getLogger(KeystoneController.class);
    private OpenStack4jEcs mOpenStack4jEcs;

    /**
     * Hide Utility Class Constructor:
     * Utility classes should not have a public or default constructor.
     */
    @Inject
    private KeystoneController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    private OSClientV3 getClient() {
        return mOpenStack4jEcs.getClient(Facing.PUBLIC);
    }

    private IdentityService identityService() {
        return ControllerUtil.checkRestServiceNotNull(getClient().identity(), IdentityService.class);
    }

    private ProjectService projectService() {
        return identityService().projects();
    }

    private RoleService roleService() {
        return identityService().roles();
    }

    /**
     * Gets a locally updated EcsProject instance as input and push that to OpenStack. An updated project will be deleted
     * after testsuit finish.
     *
     * @param {{@link {@link EcsProject}
     * @return {@link EcsProject} updated {@link EcsProject}
     */
    private EcsProject updateProject(EcsProject ecsProject) {
        ecsProject.set(projectService().update(ecsProject.get()));
        mCreatedProjectIds.add(ecsProject.get().getId());
        return ecsProject;
    }

    private UserService userService() {
        return identityService().users();
    }

    /**
     * Adds a specified role to a user for a project.
     *
     * @param projectId - ID of the project
     * @param userId - ID of the user
     * @param roleId - ID of the role that the user should have for the project
     * @return boolean - true, if the project could be added to the user with the specified role
     */
    public boolean addProjectRoleToUser(String projectId, String userId, String roleId) {
        boolean userProjectRoleAreConnected = false;
        // add project to user
        roleService().grantProjectUserRole(projectId, userId, roleId);
        // verify that project is added to user by checking if role exists for user-project pair
        List<? extends RoleAssignment> roleAssisgnments = roleService().listRoleAssignments(projectId);
        for (RoleAssignment roleAssisgnment : roleAssisgnments) {
            if (roleAssisgnment.getUserId().equals(userId) && roleAssisgnment.getRoleId().equals(roleId)) {
                mLogger.info(Verdict.ATTACHED, "role: " + roleId, "User: " + userId, "from Project " + projectId);
                userProjectRoleAreConnected = true;
                break;
            }
        }
        return userProjectRoleAreConnected;
    }

    /**
     * cleanup method to delete the instantiated Roles, Users and Projects.
     * This shall only be called by JcatTelcoDcTestCase class.
     *
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", KeystoneController.class, "");

        for (String projectId : mCreatedProjectIds) {

            if (!deleteProject(projectId)) {
                mLogger.error(String.format(
                        "Failed to confirm deletion of project (id:%s) within the given time limit, exception was",
                        projectId));
            }
        }

        for (String userId : mCreatedUserIds) {

            if (!deleteUser(userId)) {
                mLogger.error(String.format(
                        "Failed to confirm deletion of user (id:%s) within the given time limit, exception was",
                        userId));
            }
        }
        mLogger.info(Verdict.DONE, "Clean up", KeystoneController.class, "");
    }

    /**
     * Create new projects and returns its ID, checks if creation was successful. Exception will be thrown if project
     * could not be created. Name will be generated and includes test class name and time stamp.
     *
     * @return String - tenantId
     */
    public String createProject() {
        EcsProject projectToCreate = EcsProject.builder().build();
        return createProject(projectToCreate).getId();
    }

    /**
     * Create a new project and returns an EcsObject, checks if creation was successful. Exception will be thrown if
     * project could not be created.
     *
     * @param projectToCreate - EcsProject - Object that contains the builder to use
     * @return EcsProject - the created project as EcsObject
     */
    public EcsProject createProject(EcsProject projectToCreate) {
        mLogger.info(EcsAction.CREATING, projectToCreate);
        Project createdProject = projectService().create(projectToCreate.get());
        if (null == projectService().get(createdProject.getId())) {
            String message = "Could not create project: " + projectToCreate.getName();
            throw new EcsOpenStackException(message);
        }
        mCreatedProjectIds.add(createdProject.getId());
        mLogger.info(Verdict.CREATED, new EcsProject(createdProject));
        return new EcsProject(createdProject);
    }

    /**
     * Create a user
     *
     * @param userName - String - name of the user
     * @param userPassword - String - password for the user
     * @return String - id of the created user
     */
    public String createUser(String userName, String userPassword) {
        return createUser(userName, userPassword, null);
    }

    /**
     * Create and enable a user for a specific projectId
     *
     * @param userName - String - name of the user
     * @param userPassword - String - password for the user
     * @param projectId - String - id of the project that this user will belong to
     * @return String - id of the created user
     */

    public String createUser(String userName, String userPassword, String projectId) {
        mLogger.info(EcsAction.CREATING, "", "KeystoneUser",
                userName + " pwd: " + userPassword + " projectId: " + projectId);
        User userCreated = userService().create(KeystoneUser.builder().name(userName).password(userPassword)
                .defaultProjectId(projectId).enabled(true).build());
        if (userCreated == null) {
            throw new EcsOpenStackException("User " + userName + " was not created.");
        }
        String userId = userCreated.getId();
        if (userService().get(userId) == null) {
            throw new EcsOpenStackException("User " + userName + " was not created.");
        }
        mCreatedUserIds.add(userId);
        mLogger.info(Verdict.CREATED, "", "KeystoneUser", userName + " with ID: " + userId);
        return userId;
    }

    /**
     * Deletes an existing project and performs check if project has been deleted.
     *
     * @param projectId - String - projectID of project to delete
     * @return boolean - true if project has been deleted; false if the ID of a
     *         non-existing project has been provided
     * @throws LoopTimeoutException if the project still remains on the system after delete
     */

    public boolean deleteProject(String projectId) throws LoopTimeoutException {
        mLogger.info(EcsAction.DELETING, EcsProject.class, projectId);
        if (null == projectService().get(projectId)) {
            mLogger.error(String.format("project \"%s\" does not exist!", projectId));
            return false;
        }
        projectService().delete(projectId);
        new LoopHelper<Boolean>(Timeout.KEYSTONE_CHANGE, "Project " + projectId + " was still found after deletion",
                Boolean.TRUE, () -> {
                    Project projectToBeDeleted = projectService().get(projectId);
                    if (null != projectToBeDeleted) {
                        mLogger.debug("Project " + projectToBeDeleted.getId() + "' still exists.");
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.DELETED, "", EcsProject.class, projectId);
        mCreatedProjectIds.remove(projectId);
        return true;
    }

    /**
     * Deletes an existing user and performs check if user has been deleted.
     *
     * @param userId - String - ID of user to delete
     * @return boolean - true if user has been deleted; false if the ID of a
     *         non-existing user has been provided
     * @throws LoopTimeoutException if the user still remains on the system after delete
     */
    public boolean deleteUser(String userId) throws LoopTimeoutException {
        mLogger.info(EcsAction.DELETING, "", "Keystone User", userId);
        if (null == userService().get(userId)) {
            mLogger.error(String.format("user \"%s\" does not exist!", userId));
            return false;
        }
        userService().delete(userId);
        new LoopHelper<Boolean>(Timeout.KEYSTONE_CHANGE, "User " + userId + " was still found after deletion",
                Boolean.TRUE, () -> {
                    User userToBeDeleted = userService().get(userId);
                    if (null != userToBeDeleted) {
                        mLogger.debug("User '" + userToBeDeleted.getId() + "' still exists.");
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.DELETED, "", "Keystone User", userId);
        mCreatedUserIds.remove(userId);
        return true;
    }

    /**
     * Check if the project exists in the ECS Cloud.
     *
     * @param projectId - String - id of project
     * @return true if project exists, otherwise false
     */
    public boolean doesProjectExist(String projectId) {
        try {
            getProject(projectId);
        } catch (EcsOpenStackException e) {
            if (e.getMessage().contains(PROJECT_NOT_EXIST_ERROR)) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

    /**
     * get a list of Projects that have been instantiated in this instance
     *
     * @return List<String> - List of Project IDs
     */
    public List<String> getCreatedProjectIds() {
        return ControllerUtil.clone(mCreatedProjectIds);
    }

    /**
     * get a list of Users that have been instantiated in this instance
     *
     * @return List<String> - List of User IDs
     */
    public List<String> getCreatedUserIds() {
        return ControllerUtil.clone(mCreatedUserIds);
    }

    /**
     * Get the endpoint for the specified openstack service and perspective
     *
     * @param perspective - the perspective of oepnstack service
     * @param cloudService - EcsCloudService enum
     * @return URI - the endpoint URI
     */
    public URI getEndpointForService(Facing perspective, EcsCloudService cloudService) {
        List<? extends Endpoint> endpointList = listEndpoints();
        List<? extends Service> serviceList = listServices();
        String serviceId = null;
        for (Service service : serviceList) {
            if (service.getName().equalsIgnoreCase(cloudService.name())) {
                serviceId = service.getId();
                break;
            }
        }
        if (serviceId != null) {
            for (Endpoint endpoint : endpointList) {
                if (endpoint.getServiceId().equalsIgnoreCase((serviceId)) && endpoint.getIface().equals(perspective)) {
                    try {
                        return endpoint.getUrl().toURI();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new EcsOpenStackException(
                String.format("Could not find %s endpoint for service %s.", perspective, cloudService));
    }

    /**
     * use to get the project as an EcsProject instance
     *
     * @param projectId - id of the project
     * @return EcsProject - representation of the project as EcsProject object
     */
    public EcsProject getProject(String projectId) {
        Project project = projectService().get(projectId);
        if (null == project) {
            throw new EcsOpenStackException(PROJECT_NOT_EXIST_ERROR + projectId);
        }
        return new EcsProject(project);
    }

    /**
     * gets the the ID of the project with the given name
     *
     * @param projectName - name to look for
     * @return String - project ID, returns null if no project with that name exists
     */
    public String getProjectIdByName(String projectName) {
        String projectId = null;
        List<? extends Project> projectList = projectService().list();
        for (Project project : projectList) {
            if (project.getName().equals(projectName)) {
                projectId = project.getId();
                break;
            }
        }
        return projectId;
    }

    /**
     * gets the the ID of the role with the given name
     *
     * @param roleName - name to look for
     * @return String - role ID, returns null if no role with that name exists
     */

    public String getRoleIdByName(String roleName) {
        List<? extends Role> roleList = roleService().list();
        for (Role role : roleList) {
            if (role.getName().equals(roleName)) {
                return role.getId();
            }
        }
        return null;
    }

    /**
     * use to get the user as an EcsUser instance
     *
     * @param userId - id of the user
     * @return KeystoneUser - KeystoneUser object
     */
    public User getUser(String userId) {
        List<? extends User> userList = userService().list();
        for (User user : userList) {
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        mLogger.warn("No user exists with id:" + userId);
        return null;
    }

    /**
     * gets the the ID of the user with the given name
     *
     * @param userName - name to look for
     * @return String - user ID, returns null if no user with that name exists
     */
    public String getUserIdByName(String userName) {
        List<? extends User> userList = userService().list();
        for (User user : userList) {
            if (user.getName().equals(userName)) {
                return user.getId();
            }
        }
        return null;
    }

    /**
     * get a list of keystone endpoints.
     *
     * @return List<? extends Endpoint> - List of endpoints
     */
    public List<? extends Endpoint> listEndpoints() {
        return identityService().serviceEndpoints().listEndpoints();
    }

    /**
     * get a list of openstack services.
     *
     * @return List<? extends Service> - List of openstack services
     */
    public List<? extends Service> listServices() {
        return identityService().serviceEndpoints().list();
    }

    /**
     * Updates a Project in openstack with specified ID. Input is an {@link EcsProjectBuilder} containing Id
     * and just the fields which needs to be updated. Example EcsProject.projectWithId(id).name(name)
     *
     * @param EcsProjectBuilder - containing project Id and fields need to be updated
     * @return {@link EcsProject} - Updated project
     */
    public EcsProject updateProject(EcsProjectBuilder ecsProjectBuilder) {
        EcsProject updateInfoProject = ecsProjectBuilder.build();

        EcsProject actualProject = getProject(updateInfoProject.get().getId());
        ProjectBuilder projectBuilder = actualProject.get().toBuilder();

        if (updateInfoProject.getName() != null) {
            projectBuilder.name(updateInfoProject.getName());
        }
        if (updateInfoProject.getDescription() != null) {
            projectBuilder.description(updateInfoProject.getDescription());
        }
        projectBuilder.enabled(updateInfoProject.isEnabled());

        actualProject.set(projectBuilder.build());
        return updateProject(actualProject);
    }
}

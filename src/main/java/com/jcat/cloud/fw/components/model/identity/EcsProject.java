package com.jcat.cloud.fw.components.model.identity;

import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.identity.v3.builder.ProjectBuilder;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;

/**
 * Class which collects parameters to build a Project
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2018-07-19 initial version
 */
public final class EcsProject extends EcsComponent {

    /**
     *
     * Builder class for the Project
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2018
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     */
    public static class EcsProjectBuilder {
        // class member declaration
        private ProjectBuilder mProjectBuilder;

        protected EcsProjectBuilder() {
            String name = ControllerUtil.createName();
            // maximum-length for ProjectName is 64 characters
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            mProjectBuilder = Builders.identityV3().project().name(name);
        }

        protected EcsProjectBuilder(String name) {
            // maximum-length for ProjectName is 64 characters
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            mProjectBuilder = Builders.identityV3().project().name(name);
        }

        public EcsProjectBuilder(String id, boolean check) {
            // check exists to distinguish those EcsProjectBuilders with one string parameter
            mProjectBuilder = Builders.identityV3().project().id(id);
        }

        public EcsProject build() {
            return new EcsProject(mProjectBuilder.build());
        }

        public EcsProjectBuilder description(String description) {
            mProjectBuilder = mProjectBuilder.description(description);
            return this;
        }

        public EcsProjectBuilder enabled(boolean enabled) {
            mProjectBuilder = mProjectBuilder.enabled(enabled);
            return this;
        }

        public EcsProjectBuilder name(String name) {
            mProjectBuilder = mProjectBuilder.name(name);
            return this;
        }
    }

    // member variables for EcsProject
    private Project mProject;

    /**
     * SHALL NOT BE USED IN TEST CASE DIRECTLY!
     * will just be used in KeystoneController and EcsProject class to create EcsProject instance
     *
     * @param Project
     */
    public EcsProject(Project project) {
        mProject = project;
    }

    public static EcsProjectBuilder builder() {
        return new EcsProjectBuilder();
    }

    public static EcsProjectBuilder builder(String name) {
        return new EcsProjectBuilder(name);
    }

    public static EcsProjectBuilder updateBuilder(String id) {
        return new EcsProjectBuilder(id, true);
    }

    /**
     *
     * @return Project - Openstack4j instance for Project
     */
    public Project get() {
        return mProject;
    }

    /**
     *
     * @return String - description
     */
    public String getDescription() {
        return mProject.getDescription();
    }

    /**
     *
     * @return String - Id
     */
    public String getId() {
        return mProject.getId();
    }

    /**
     *
     * @return String - name
     */
    public String getName() {
        return mProject.getName();
    }

    /**
     *
     * @return boolean - enabled
     */
    public boolean isEnabled() {
        return mProject.isEnabled();
    }

    /**
     *
     * @param Project
     */
    public void set(Project project) {
        mProject = project;
    }

    @Override
    public String toString() {
        return String.format("Project: %s with ID: %s (description: %s , enabled: %s)", getName(), getId(),
                getDescription(), isEnabled());
    }
}

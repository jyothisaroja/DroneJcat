package com.jcat.cloud.fw.components.model.identity;

import java.util.Objects;

import org.openstack4j.model.common.Identifier;

/**
 * Class for taking user specified credentials for openstack operations
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2015-03-31 initial version
 * @author zdagjyo 2018-06-27 modified to support cee8
 */
public final class EcsCredentials {

    private final String mUserName;
    private final String mPassword;
    private final Identifier mDomainName;
    private final Identifier mProjectName;

    public EcsCredentials(String userName, String password) {
        mUserName = userName;
        mPassword = password;
        mDomainName = Identifier.byName("Default");
        mProjectName = Identifier.byName("admin");
    }

    public EcsCredentials(String userName, String password, Identifier domainName) {
        mUserName = userName;
        mPassword = password;
        mDomainName = domainName;
        mProjectName = Identifier.byName("admin");
    }

    public EcsCredentials(String userName, String password, Identifier domainName, Identifier projectName) {
        mUserName = userName;
        mPassword = password;
        mDomainName = domainName;
        mProjectName = projectName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EcsCredentials)) {
            return false;
        } else {
            EcsCredentials that = (EcsCredentials) other;
            return (this.getDomain().equals(that.getDomain()) && this.getProject().equals(that.getProject())
                    && this.getUser().equals(that.getUser()) && this.getPassword().equals(that.getPassword()));
        }
    }

    /**
     * @return the mDomainName
     */
    public Identifier getDomain() {
        return mDomainName;
    }

    /**
     * @return the mPassword
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * @return the mProjectName
     */
    public Identifier getProject() {
        return mProjectName;
    }

    /**
     * @return the mUserName
     */
    public String getUser() {
        return mUserName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUserName, mPassword, mDomainName, mProjectName);
    }
}

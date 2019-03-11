package com.jcat.cloud.fw.components.model.target;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;

/**
 * Entity to store user and pass combination. Used in {@link EcsSession}.
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
public class EcsUser extends EcsComponent {
    private final String mUserName;
    private final String mPassword;
    private final boolean mIsSystemUser;

    public EcsUser(String userName, String password, boolean isSystemUser) {
        mUserName = userName;
        mPassword = password;
        mIsSystemUser = isSystemUser;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getUsername() {
        return mUserName;
    }

    public boolean isSystemUser() {
        return mIsSystemUser;
    }
}

package com.jcat.cloud.fw.components.system.cee.watchmen;

/**
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015- initial version
 *
 */
public class SnmpEventKey {
    private int mMinor;
    private int mMajor;

    public SnmpEventKey(int minor, int major) {
        this.mMinor = minor;
        this.mMajor = major;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SnmpEventKey key = (SnmpEventKey) obj;
        if (this.mMinor != key.getMinor()) {
            return false;
        }
        if (this.mMajor != key.getMajor()) {
            return false;
        }
        return true;
    }

    public int getMajor() {
        return mMajor;
    }

    public int getMinor() {
        return mMinor;
    }

    @Override
    public int hashCode() {
        return mMinor + mMajor;
    }
}

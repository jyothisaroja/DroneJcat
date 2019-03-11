package com.jcat.cloud.fw.infrastructure.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Class containing information about all the extreme switch resources within an STP
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin 2014-05-16 initial version
 *
 */
public class ExtremeSwitchResourceGroup {
    /**
     * enum type which represents different ExtremeSwitchResources within an STP
     */
    public enum ExtremeSwitchName {
        EXTREMESWITCH_1("ExtremeSwitch_1"), EXTREMESWITCH_2("ExtremeSwitch_2"), EXTREMESWITCH_3("ExtremeSwitch_3"), EXTREMESWITCH_4(
                "ExtremeSwitch_4");

        // String value of extreme switch name
        private String mName;

        ExtremeSwitchName(String value) {
            this.mName = value;
        }

        // method for getting the String value of the extreme switch name
        public String getName() {
            return mName;
        }

    }

    private Map<ExtremeSwitchName, ExtremeSwitchResource> mExtremeSwitchResources;

    public ExtremeSwitchResourceGroup() {
        mExtremeSwitchResources = new HashMap<ExtremeSwitchName, ExtremeSwitchResource>();
    }

    /**
     * Add an extreme switch resource
     *
     * @param name
     * @param extremeSwitch
     */
    public void addExtremeSwitch(ExtremeSwitchName name, ExtremeSwitchResource extremeSwitch) {
        mExtremeSwitchResources.put(name, extremeSwitch);
    }

    /**
     * Get all the extreme switch resources of an STP
     *
     * @return Map - a Map containing all the extreme switch resources
     */
    public Map<ExtremeSwitchName, ExtremeSwitchResource> getAllExtremeSwitchResources() {
        return mExtremeSwitchResources;
    }

    /**
     * Get specific extreme switch resource
     *
     * @param name - ExtremeSwitchName - name of the extreme switch resource
     * @return ExtremeSwitchResource
     */
    public ExtremeSwitchResource getExtremeSwitchResource(ExtremeSwitchName name) {
        return mExtremeSwitchResources.get(name);
    }
}

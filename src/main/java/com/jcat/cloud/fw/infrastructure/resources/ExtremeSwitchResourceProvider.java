package com.jcat.cloud.fw.infrastructure.resources;

import javax.inject.Inject;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Provider;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup.ExtremeSwitchName;

/**
 * Provider for {@link ExtremeSwitchResource}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2013-05-13 initial version
 * @author ezhgyin 2013-10-09 Update the way of getting resource according to the new TASS adapter
 * @author ezhgyin 2014-05-16 Update get method to retrieve all the extreme switch resources of an STP
 *
 */
public class ExtremeSwitchResourceProvider implements Provider<ExtremeSwitchResourceGroup> {

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtremeSwitchResourceGroup get() {
        ExtremeSwitchResourceGroup extremeSwitchGroup = new ExtremeSwitchResourceGroup();
        for (ExtremeSwitchName extremeSwitchName : ExtremeSwitchName.values()) {
            // TODO: catch exception if resource not present
            ExtremeSwitchResource extremeSwitch;
            try {
                extremeSwitch = (ExtremeSwitchResource) mCfa.get(ExtremeSwitchResource.class,
                        extremeSwitchName.getName());
                extremeSwitchGroup.addExtremeSwitch(extremeSwitchName, extremeSwitch);
            } catch (IllegalArgumentException e) {
                // do nothing, since not all extreme switch resource will present at the same time
            }
        }
        return extremeSwitchGroup;
    };

}

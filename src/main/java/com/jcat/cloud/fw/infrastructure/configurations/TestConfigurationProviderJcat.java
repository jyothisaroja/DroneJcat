package com.jcat.cloud.fw.infrastructure.configurations;

import com.google.inject.Provider;


/**
 * TestConfiguration provider from JCAT {@link JcatTelcoDcListener}
 * <p/>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2014-01-07 Initial version
 */
public class TestConfigurationProviderJcat implements Provider<TestConfiguration> {

    private static TestConfiguration mTestConfiguration;

    @Override
    public TestConfiguration get() {
        if(mTestConfiguration == null) {
            mTestConfiguration = TestConfiguration.getTestConfiguration();
            if (mTestConfiguration == null) {
                throw new RuntimeException("Could not get TestNG configuration, this might be due to that you forgot to "
                        + "add listeners, please refer to README on how to specify listeners");
            }
        }

        return mTestConfiguration;
    }
}

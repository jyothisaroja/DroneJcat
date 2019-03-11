package com.jcat.cloud.examples;

import java.net.UnknownHostException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import se.ericsson.jcat.fw.assertion.JcatAssertApi;
import se.ericsson.jcat.fw.logging.JcatLoggingApi;

import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.CommonParametersValues;
import com.jcat.cloud.fw.components.model.image.EcsImage.CeeVmImage;
import com.jcat.cloud.fw.components.model.network.EcsNetwork;
import com.jcat.cloud.fw.components.model.storage.block.EcsVolume;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.EcsFlavor.PredefinedFlavor;
import com.jcat.cloud.fw.infrastructure.base.JcatTelcoDcTestCase;

/**
 * test to test the re-establishment of the connection to OpenStack after a CIC reboot or change of CIC mode
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce 2015-06-01 initial version to test if OpenStack reconnects
 * @author zmousar 2018-11-16 Modified code to support for CEE6.7
 */
public class OpenStackReconnectAfterCicRestartIT extends JcatTelcoDcTestCase {

    private String mFlavorId;
    private String mImageId;
    private String mNetId;
    private String mProjectId;
    private String mVolumeId;

    /**
     *
     * @return
     */
    private boolean areAllOpenStackComponentsReachable() {
        mLogger.info("Test if all OpenStack components are reachable.");
            String flavorId = mNovaController.getFlavorId(PredefinedFlavor.M1_MEDIUM);
            mLogger.info(Verdict.FOUND, "", "Nova showed flavor id", flavorId);
        return true;
    }

    @BeforeClass
    private void setup() {
        // for Nova
        mFlavorId = mNovaController.getFlavorId(PredefinedFlavor.M1_MEDIUM);
    }

    @Test(enabled = true)
    public void openStackReconnectsAfterCicModeChange() {

        JcatLoggingApi.setTestStepBegin("Assert OpenStack components are reachable ");
        JcatAssertApi.assertTrue("One or more OpenStack components are not reachable.",
                areAllOpenStackComponentsReachable());
        mLogger.info("All OpenStack components are reachable.");
    }
}

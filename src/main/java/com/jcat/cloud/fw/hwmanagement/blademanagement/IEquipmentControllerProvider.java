package com.jcat.cloud.fw.hwmanagement.blademanagement;

import javax.inject.Provider;

import org.apache.log4j.Logger;

import com.ericsson.commonlibrary.cf.spi.ConfigurationFacadeAdapter;
import com.google.inject.Inject;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.BspNetconfLib;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.DmxNetconfLib;
import com.jcat.cloud.fw.hwmanagement.blademanagement.hp.VcFlexController;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.resources.DmxResource;
import com.jcat.cloud.fw.infrastructure.resources.VcFlexResource;

/**
 * Provider to select proper configuration of the controller to be used. If hardware type is HP then VCFLEX(Hardware
 * managment) library will be loaded and if hardware type is EBS, then depending on version DMX or BSP8100(Hardware
 * managment) library will be loaded.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author emulign 2014-02-17 Initial version.
 * @author epergat 2014-05-19 minor refactoring.
 * @author ehosmol 2014-09-29 Update the logic in order to load prober library
 * @author eqinann 2015-2-9 Removed DmxResource and EcsBladeBuilder injections
 *
 */
public class IEquipmentControllerProvider implements Provider<IEquipmentController> {
    public static final String HARDWARE_EBS = "EBS";
    public static final String HARDWARE_HP = "HP";
    public static final String PROTOCOL_CLI = "CLI";
    public static final String PROTOCOL_NETCONF = "NETCONF";
    public static final String EBS_VERSION_3 = "DMX";
    public static final String EBS_VERSION_4 = "BSP";
    public static final int NUM_OF_SCXES_PER_SHELF = 2;
    public static final int E1_PORT_INDEX_IN_DMXNETCONFLIB = 31;
    public static final int E2_PORT_INDEX_IN_DMXNETCONFLIB = 32;

    /**
     * Provider containing resource configuration properties
     */
    @Inject
    private ConfigurationFacadeAdapter mCfa;

    private String mHardware;

    private final Logger mLogger = Logger.getLogger(IEquipmentControllerProvider.class);

    private String mProtocolType;
    private String mVersion;

    @Inject
    private TestConfiguration mTestConfiguration;

    /**
     * Method to parse the options regarding IEquipmentController
     */
    private void parseArray() {
        if (mTestConfiguration.getOptions() != null) {
            String option = mTestConfiguration.getOptions();
            switch (option) {
            case IEquipmentControllerProvider.HARDWARE_EBS:
                mHardware = IEquipmentControllerProvider.HARDWARE_EBS;
                break;
            case IEquipmentControllerProvider.HARDWARE_HP:
                mHardware = IEquipmentControllerProvider.HARDWARE_HP;
                break;
            case IEquipmentControllerProvider.PROTOCOL_CLI:
                mProtocolType = IEquipmentControllerProvider.PROTOCOL_CLI;
                break;
            case IEquipmentControllerProvider.PROTOCOL_NETCONF:
                mProtocolType = IEquipmentControllerProvider.PROTOCOL_NETCONF;
                break;
            case IEquipmentControllerProvider.EBS_VERSION_3:
                mVersion = IEquipmentControllerProvider.EBS_VERSION_3;
                break;
            case IEquipmentControllerProvider.EBS_VERSION_4:
                mVersion = IEquipmentControllerProvider.EBS_VERSION_4;
                mLogger.info(mVersion);
                break;
            default:
                throw new IllegalArgumentException("The option: " + option + " cannot be processed.");
            }
        }
        // Reset after deciding
        mTestConfiguration.setOptions(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IEquipmentController get() {
        parseArray();

        determineHardware();

        IEquipmentController equipment = instantiateHardware();

        return equipment;
    }

    /**
     * Checks the TestConfiguration data to see if any requests on
     * specific hardware has been requested.
     * If not it means the test case doesn't have any specific requests on the hardware be to used.
     * We try to figure out from the configuration what hardware configuration is available in TASS and later load the
     * relevant library.
     *
     * throws {@link java.lang.IllegalArgumentException} - If no hardware configuration found in tass
     */
    private void determineHardware() {

        if (mHardware != null) {
            return;
        }
        if (mCfa.contains(DmxResource.DMX)) {
            mHardware = IEquipmentControllerProvider.HARDWARE_EBS;
        } else if (mCfa.contains(VcFlexResource.VCFLEX)) {
            mHardware = IEquipmentControllerProvider.HARDWARE_HP;
        }
        if (mHardware == null) {
            throw new IllegalArgumentException("No hardware configuration found");
        }
    }

    /**
     * Checks the TestConfiguration data to see if any requests on
     * specific hardware has been requested.
     *
     */
    private IEquipmentController instantiateHardware() {
        IEquipmentController equipment = null;
        // Instantiate the right hardware
        if (mHardware.equals(HARDWARE_HP)) {
            if (mProtocolType == null || mProtocolType.equals(PROTOCOL_CLI)) {
                equipment = new VcFlexController(mCfa.get(VcFlexResource.class, VcFlexResource.VCFLEX));
            } else if (mProtocolType.equals(PROTOCOL_NETCONF)) {
                throw new UnsupportedOperationException("HP + NETCONF is not implemented");
            }
        } else if (mHardware.equals(HARDWARE_EBS)) {
            if (mProtocolType == null || mProtocolType.equals(PROTOCOL_NETCONF)) {
                if (mVersion == null || mVersion.equalsIgnoreCase(EBS_VERSION_4)) {
                    equipment = new BspNetconfLib(mCfa.get(DmxResource.class, DmxResource.DMX));
                } else if (mVersion.equalsIgnoreCase(EBS_VERSION_3)) {
                    equipment = new DmxNetconfLib(mCfa.get(DmxResource.class, DmxResource.DMX));
                } else {
                    throw new IllegalArgumentException("The version provided: " + mVersion + " is not valid.");
                }
            } else {
                throw new UnsupportedOperationException("EBS + CLI is not implemented");
            }
        } else {
            throw new IllegalArgumentException("The hardware type provided: " + mHardware + " is not valid.");
        }
        return equipment;
    }
}

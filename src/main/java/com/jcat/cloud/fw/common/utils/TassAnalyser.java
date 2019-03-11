package com.jcat.cloud.fw.common.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import se.ericsson.tass.interfaces.configdb.pdurbs.ws.ConfigDbPortType;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.ConfigDbService;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.ConfigdbFault;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.NoSuchResourceFault;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.types70.CustomEquipmentInstanceFieldType;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.types70.CustomEquipmentInstanceType;
import se.ericsson.tass.interfaces.configdb.pdurbs.ws.types70.TestConfigurationType;

import com.ericsson.tass.resources.CustomNode;
import com.ericsson.tass.resources.ICustomNode;
import com.ericsson.tass.utils.Utils;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 * Test case for identification of missing properties of nodes in Tass
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehosmol - 2013-11-12 - Initial Implementation
 *
 */
public class TassAnalyser {

    /**
     * This classed is used to fetch test configurations data from Tass resource manager. The class also provides the
     * functionality to compare test configurations against each other.
     *
     * <p>
     * <b>Copyright:</b> Copyright (c) 2013
     * </p>
     * <p>
     * <b>Company:</b> Ericsson
     * </p>
     *
     * @author ehosmol - 2013-11-12 - Initial Implementation
     *
     */
    private class TassAdapter {

        /**
         * Configdb endpoint
         */
        private static final String CONFIGDB_PATH = "/cxf/configdb";

        private static final String CONFIGDB_SERVICE = "ConfigDbServiceImplService",
                CONFIGDB_SERVICE_PORT = "ConfigDbServiceImplPort",
                CONFIGDB_SERVICE_URL = "http://internal.ws.generic.configdb.tass.ericsson.se/";

        private ConfigDbPortType mService;
        private ICustomNode mTemplateNode;

        /**
         * Constructor
         *
         * @param baseUrl - String - representing a URL to the TASS configuration database instance to use
         */
        public TassAdapter(String baseUrl, String nodeName) {

            String configDbPath = baseUrl + CONFIGDB_PATH;
            mLogger.debug("Setting configuration database path: " + configDbPath);

            QName serviceName = new QName(CONFIGDB_SERVICE_URL, CONFIGDB_SERVICE);
            QName portName = new QName(CONFIGDB_SERVICE_URL, CONFIGDB_SERVICE_PORT);
            mService = new ConfigDbService(Utils.loadUrl(configDbPath), serviceName).getPort(portName,
                    ConfigDbPortType.class);

            // We put it here in order to call it once!
            mLogger.debug("Getting component's information from node: " + nodeName);
            TestConfigurationType testConfigurationType = getTestConfigurationType(nodeName);

            // Load the test configuration data into an STC resource instance
            mLogger.debug("Instanciating a new node with retrieved information from TASS");
            mTemplateNode = new CustomNode(testConfigurationType);
        }

        /**
         * Gets the test configuration name
         *
         * @return
         */
        @SuppressWarnings("deprecation")
        private List<String> getTestConfigurationNames() {
            return mService.getTestConfigurationNames();
        }

        /**
         * Gets the test configuration defined in TASS.
         *
         * @param name - String - name of the test configuration/node
         * @return Test configuration
         */
        private TestConfigurationType getTestConfigurationType(String name) {
            try {
                mLogger.debug("Getting configuration from: " + name);
                return mService.getTestConfigurationV70(name);
            } catch (ConfigdbFault e) {
                throw new IllegalArgumentException("TASS config service unknown fault", e);
            } catch (NoSuchResourceFault e) {
                throw new IllegalArgumentException("No configuration found for " + name, e);
            }
        }

        /**
         * Returns list of all the test configurations available in TASS
         *
         * @return
         */
        public final Set<String> getAllIds() {
            List<String> response = getTestConfigurationNames();
            if (response == null) {
                throw new IllegalStateException("No IDs found in the data source!");
            }
            return new HashSet<String>(response);
        }

        /**
         * Compares two test configuration against each other
         *
         * @param nodeName - String - name of the test configuration which is going to be compared
         */
        public final void nodeCompare(String nodeName) {

            CustomNode targetNode = new CustomNode(getTestConfigurationType(nodeName));

            mLogger.info("Analysing Testconfiguraiton: " + targetNode.getId());
            // Loop through all the equipments in the template node and finds the same equipment in the target node
            for (CustomEquipmentInstanceType templateEquipment : mTemplateNode.getCustomFields()) {
                // get the type of the equipments for example OpenStack, Dmx and ...
                final String templateEquipmentType = templateEquipment.getEquipmentType();
                @SuppressWarnings("unchecked")
                // filter the equipments with the same type in the target node(test configuration)
                Collection<CustomEquipmentInstanceType> equipment = CollectionUtils.select(
                        targetNode.getCustomFields(), obj -> ((CustomEquipmentInstanceType) obj).getEquipmentType()
                                .equalsIgnoreCase(templateEquipmentType));

                if (equipment.isEmpty()) {
                    mLogger.error("Missing equipmentType: " + templateEquipmentType);
                } else {

                    // If the specified equipment is found in the target node then loop through template equipment
                    // fields and try to find the matched equivalent fields in the founded equipment in the target node.
                    for (CustomEquipmentInstanceFieldType templateField : templateEquipment.getFields()) {
                        final String templateFieldName = templateField.getFieldName();
                        @SuppressWarnings("unchecked")
                        // Filter fields with the same name as template fileds
                        Collection<CustomEquipmentInstanceFieldType> field = CollectionUtils.select(equipment
                                .iterator().next().getFields(), obj -> ((CustomEquipmentInstanceFieldType) obj)
                                .getFieldName().equalsIgnoreCase(templateFieldName)
                                && ((CustomEquipmentInstanceFieldType) obj).getFieldValue() != null);

                        if (field.isEmpty()) {
                            mLogger.error("Missing property or property value: " + templateFieldName
                                    + " in EquipmentType: " + templateEquipmentType);
                        }
                    }
                }
            }
        }

    }

    /**
     * Default node which all the other nodes will compared against
     */
    private static final String DEFAULTNODE = "NODE_EXAMPLE";

    private final Logger mLogger = Logger.getLogger(TassAnalyser.class);

    @Test
    public void tassAnalyse() {
        TestConfiguration configuration = TestConfiguration.getTestConfiguration();
        configuration.parseProperties(System.getProperties());
        configuration.configureLog4j2Configuration();

        String tassUrl = configuration.getBaseUrl();
        TassAdapter adapter = new TassAdapter(tassUrl, DEFAULTNODE);
        for (String node : adapter.getAllIds()) {
            adapter.nodeCompare(node);
        }
    }
}

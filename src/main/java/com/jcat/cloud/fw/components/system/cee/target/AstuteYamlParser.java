package com.jcat.cloud.fw.components.system.cee.target;

import java.io.File;
import java.util.Map;

/**
 *
 * This class parses the content of "astute.yaml" and extracts the following parameters:
 *
 * - idam user name tagged as "system admin"
 * - ...
 *
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eeddai - 2015-01-25 - Initial version
 * @author ezhgyin - 2015-03-23 - adapt to new way of reading yaml file
 * @author ehosmol - 2015-05-05 - add {@link #getUsers()}
 * @author ezhgyin - 2016-03-08 - move getIdamSystemAdminUser() and getUsers() to ConfigYamlPaser class
 */

public class AstuteYamlParser extends BaseYamlParser {

    private String mCeilometerPassword = "";
    private static final String TAG_CEILOMETER = "ceilometer";
    private static final String TAG_CEILOMETER_PASSWORD = "db_password";

    /**
     * Constructor
     *
     * @param yamlFile
     */
    public AstuteYamlParser(File yamlFile) {
        super(yamlFile);
    }

    @SuppressWarnings("unchecked")
    public String getCeilometerPassword() {
        if (mCeilometerPassword.isEmpty()) {
            Map<String, Object> configMap = getYamlConfig();
            Map<String, String> ceilometerData = (Map<String, String>) configMap.get(TAG_CEILOMETER);
            mCeilometerPassword = ceilometerData.get(TAG_CEILOMETER_PASSWORD);
        }
        return mCeilometerPassword;
    }
}

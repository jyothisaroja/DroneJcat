package com.jcat.cloud.fw.fwservices.monitoring.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * Utility singleton class that parses the monitoring properties
 * from the resource directory
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-06-29 Initial version
 * @author ehkiyki 2015-07-03 Made the class a singleton
 */
public class MonitoringProperties {

    private static final String FILE_NAME = "src/main/resources/monitoring.properties";

    // Zabbix API properties
    private static final String ZABBIX_URL = "zabbix.url";
    private static final String ZABBIX_CONENT_TYPE = "zabbix.contentType";
    private static final String ZABBIX_NODE = "zabbix.node";
    private static final String ZABBIX_USERNAME = "zabbix.username";
    private static final String ZABBIX_PASSWORD = "zabbix.password";

    private Properties mProp;
    private static MonitoringProperties mInstance;

    /**
     * Private constructor that read the properties file
     * and stores the properties in a class variable so that
     * it can be accessed when creating the various properties.
     */
    private MonitoringProperties() {
        mProp = getProperties();
    }

    /**
     * Returns the singleton instance of this class.
     * @return The singleton instance of this class.
     */
    public synchronized static MonitoringProperties getInstance() {
        if (mInstance == null) {
            mInstance = new MonitoringProperties();
        }
        return mInstance;
    }

    /**
     * Loads the properties file into memory.
     * @return The loaded properties object.
     */
    private Properties getProperties() {
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(FILE_NAME);
            properties.load(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    /**
     * @return The Zabbix API properties.
     */
    public ZabbixProperties newZabbixProperties() {
        return new ZabbixProperties(mProp.getProperty(ZABBIX_URL), mProp.getProperty(ZABBIX_CONENT_TYPE),
                mProp.getProperty(ZABBIX_NODE), mProp.getProperty(ZABBIX_USERNAME), mProp.getProperty(ZABBIX_PASSWORD));
    }
}

package com.jcat.cloud.fw.fwservices.monitoring.app;

import java.util.Date;
import java.util.List;

import com.google.inject.Inject;
import com.jcat.cloud.fw.fwservices.monitoring.ZabbixItem;
import com.jcat.cloud.fw.fwservices.monitoring.ZabbixWrapper;
import com.jcat.cloud.fw.fwservices.monitoring.common.ZabbixProperties;
import com.jcat.cloud.fw.fwservices.monitoring.db.MonitoringSample;
import com.jcat.cloud.fw.fwservices.monitoring.util.DatabaseHelper;

/**
 *
 * Collects samples from Zabbix on a
 * given interval basis. The collected samples are automatically
 * inserted into a database, specified and configured by Hibernate
 * (src/main/resources/hibernate.cfg.xml).
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-06-26 - Initial version
 *
 */
public class NodeStatisticCollector {

    @Inject
    private DatabaseHelper mDbHelper;
    private ZabbixWrapper mZabbixHandler;

    public NodeStatisticCollector() {
        mZabbixHandler = new ZabbixWrapper();
    }

    /**
     * Try to sleep for a given time.
     *
     * @param ms The time ms to sleep.
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Uses the Zabbix service to query for data and then
     * insert the data into the database.
     * @param nodeName The name or the ip of the node
     * @param hostIpAddress The ip address of the node/DC
     * @param item The items that should be sampled, if set to null, try to fetch data for all items
     * @param from Sample from this date and time
     * @param to Sample up to this date and time
     */
    public void saveSamples(String nodeName, List<String> hostNames, List<ZabbixItem> items, Date from, Date to) {
        List<MonitoringSample> samples;
        for (String hostName : hostNames) {
            if (items == null) {
                // Ask for all available items for this host
                samples = mZabbixHandler.getSamples(nodeName, hostName, null, from, to);
                mDbHelper.save(samples);
            } else {
                // Ask for the given items
                for (ZabbixItem item : items) {
                    samples = mZabbixHandler.getSamples(nodeName, hostName, item, from, to);
                    if (samples != null) { // The host has the item available
                        mDbHelper.save(samples);
                    }
                }
            }
        }
    }

    /**
     * Starts the sample collection and runs forever until an exception is
     * thrown. It collects the data by querying Zabbix and then stores this
     * data in a database and does this for the given items. The update
     * interval can be configured but should not be less than 60000 ms (1 s).
     * @param properties Contains the Zabbix API url, username and password
     * @param items The items that should be monitored
     * @param updateIntervalMs The update interval in ms
     */
    public void start(ZabbixProperties properties, List<ZabbixItem> items, long updateIntervalMs) {
        System.out.println("[ INFO] Started sampling...");
        // Authenticate with the Zabbix service to get a valid session
        mZabbixHandler.authenticate(properties);
        Date from = new Date();
        Date to;

        while (true) {
            // Do not poll too often, wait
            sleep(updateIntervalMs);
            to = new Date();
            System.out.println("[ INFO] Collecting data between " + from.toString() + " and " + to.toString());
            List<String> hostNames = mZabbixHandler.getHostNames();
            saveSamples(properties.getNode(), hostNames, items, from, to);
            // Add 1 second to avoid data overlap in the next query
            from = new Date(to.getTime() + 1000);
            System.out.println("[ INFO] Inserted the data into the database");
        }
    }

    /**
     * Starts the sample collection and runs forever until an exception is
     * thrown. It collects the data by querying Zabbix and then stores this
     * data in a database. The update interval can be configured but should
     * not be less than 60000 ms (1 s).
     * @param properties Contains the Zabbix API url, username and password
     * @param updateIntervalMs The update interval in ms
     */
    public void start(ZabbixProperties properties, long updateIntervalMs) {
        start(properties, null, updateIntervalMs);
    }
}

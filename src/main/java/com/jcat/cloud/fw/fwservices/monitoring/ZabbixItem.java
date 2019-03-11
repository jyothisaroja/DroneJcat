package com.jcat.cloud.fw.fwservices.monitoring;

/**
 *
 * Class mapping Zabbix item names to their type IDs.
 * The correct type IDs are required to get the correct
 * result from the request. The type IDs are as follow:
 *
 * 0 - float
 * 1 - string
 * 2 - log
 * 3 - integer
 * 4 - test
 *
 * Reference:
 * https://www.zabbix.com/documentation/2.0/manual/appendix/api/history/get
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-06-29 - Initial version
 *
 */
public class ZabbixItem {

    private String mName;
    private int mTypeId;

    /**
     * Default constructor.
     */
    public ZabbixItem() {
    }

    /**
     * Available memory item factory method.
     * @return The Zabbix item
     */
    public static ZabbixItem getAvailableMemory() {
        ZabbixItem item = new ZabbixItem();
        item.setName("Available memory");
        item.setTypeId(3);
        return item;
    }

    /**
     * CPU load item factory method.
     * @return The Zabbix item
     */
    public static ZabbixItem getCpuLoad() {
        ZabbixItem item = new ZabbixItem();
        item.setName("Processor load (1 min average per core)");
        item.setTypeId(0);
        return item;
    }

    /**
     * Free memory item factory method.
     * @return The Zabbix item
     */
    public static ZabbixItem getFreeMemory() {
        ZabbixItem item = new ZabbixItem();
        item.setName("Free memory");
        item.setTypeId(0);
        return item;
    }

    /**
     * Number of instances item factory method.
     * @return The Zabbix item
     */
    public static ZabbixItem getNumInstances() {
        ZabbixItem item = new ZabbixItem();
        item.setName("Number of instances");
        item.setTypeId(3);
        return item;
    }

    /**
     * Number of processes item factory method.
     * @return The Zabbix item
     */
    public static ZabbixItem getNumProcs() {
        ZabbixItem item = new ZabbixItem();
        item.setName("Number of processes");
        item.setTypeId(3);
        return item;
    }

    /**
     * @return The name of the item
     */
    public String getName() {
        return mName;
    }

    /**
     * @return typeId The Zabbix type id that defines the data type of the item
     */
    public int getTypeId() {
        return mTypeId;
    }

    /**
     * @param name The name of the item
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @param typeId The Zabbix type id that defines the data type of the item
     */
    public void setTypeId(int typeId) {
        mTypeId = typeId;
    }
}

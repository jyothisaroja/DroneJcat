package com.jcat.cloud.fw.fwservices.monitoring.db;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * This class is used by Hibernate to map database entries to POJOs.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-07-02 Initial version
 * @author ethssce 2015-08-17 adjusted to namespace structure
 */

@Entity
@Table(name = "sample")
public class MonitoringSample {

    // The entry id
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    // The name of the node
    @Column(name = "node")
    private String node;

    // The name of the host. E.g. "cic-0.1"
    @Column(name = "host")
    private String host;

    // The name of the sampled data. E.g. "Memory utilization"
    @Column(name = "data")
    private String data;

    // The sampled value
    @Column(name = "value")
    private float value;

    // The date and time of the sample
    @Column(name = "timestamp")
    private Timestamp timestamp;

    /**
     * Required default constructor.
     */
    public MonitoringSample() {
    }

    /**
     * @return The name of the sampled data
     */
    public String getData() {
        return data;
    }

    /**
     * @return The name of the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return The entry id
     */
    public int getId() {
        return id;
    }

    /**
     * @return The name of the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return The date and time of the sample
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * @return The sampled value
     */
    public float getValue() {
        return value;
    }

    /**
     * @param data The name of the sampled data
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @param host The name of the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param id The entry id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @param node The name of the node
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * @param timestamp The date and time of the sample
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @param value The sampled value
     */
    public void setValue(float value) {
        this.value = value;
    }
}

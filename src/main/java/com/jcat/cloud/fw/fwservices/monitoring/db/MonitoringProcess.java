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
 *
 */

@Entity
@Table(name = "process")
public class MonitoringProcess {

    // The entry id
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    // The process id
    @Column(name = "pid")
    private int pid;

    // The name of the node
    @Column(name = "node")
    private String node;

    // The name of the host. E.g. "cic-0.1"
    @Column(name = "host")
    private String host;

    // The name of the user
    @Column(name = "user")
    private String user;

    // The name of the sampled process. E.g. "top"
    @Column(name = "process")
    private String process;

    // The sampled cpu utilization
    @Column(name = "cpu")
    private float cpu;

    // The sampled memory utilization
    @Column(name = "mem")
    private float mem;

    // The date and time of the sample
    @Column(name = "timestamp")
    private Timestamp timestamp;

    /**
     * Required default constructor.
     */
    public MonitoringProcess() {
    }

    /**
     * @return The cpu utilization
     */
    public float getCpu() {
        return cpu;
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
     * @return The memory utilization
     */
    public float getMem() {
        return mem;
    }

    /**
     * @return The name of the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return The process id
     */
    public int getPid() {
        return pid;
    }

    /**
     * @return The name of the process
     */
    public String getProcess() {
        return process;
    }

    /**
     * @return The date and time of the sample
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * @return The name of the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param cpu The cpu utilization
     */
    public void setCpu(float cpu) {
        this.cpu = cpu;
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
     * @param mem The memory utilization
     */
    public void setMem(float mem) {
        this.mem = mem;
    }

    /**
     * @param node The name of the node
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * @param pid The process id
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * @param process The name of the process
     */
    public void setProcess(String process) {
        this.process = process;
    }

    /**
     * @param timestamp The date and time of the sample
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @param user The name of the user
     */
    public void setUser(String user) {
        this.user = user;
    }
}

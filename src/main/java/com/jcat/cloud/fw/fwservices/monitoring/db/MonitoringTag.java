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
 * @author ehkiyki 2015-07-06 Initial version
 *
 */

@Entity
@Table(name = "tag")
public class MonitoringTag {

    // The entry id
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    // The suite Id
    @Column(name = "suiteId")
    private int suiteId;

    // The test Id
    @Column(name = "testId")
    private int testId;

    // The name of the node
    @Column(name = "node")
    private String node;

    // The name of the host
    @Column(name = "host")
    private String host;

    // The name of the tag
    @Column(name = "tag")
    private String tag;

    // The configured timeout time in seconds
    @Column(name = "timeout")
    private int timeout;

    // The final result state
    @Column(name = "result")
    private String result;

    // The date and time of the tag start
    @Column(name = "started")
    private Timestamp started;

    // The date and time of the tag finish
    @Column(name = "finished")
    private Timestamp finished;

    /**
     * Required default constructor.
     */
    public MonitoringTag() {
    }

    /**
     * @return The finish time of the tag
     */
    public Timestamp getFinished() {
        return finished;
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
     * @return The result
     */
    public String getResult() {
        return result;
    }

    /**
     * @return The start time of the tag
     */
    public Timestamp getStarted() {
        return started;
    }

    /**
     * @return The suite id
     */
    public int getSuiteId() {
        return suiteId;
    }

    /**
     * @return The tag name
     */
    public String getTag() {
        return tag;
    }

    /**
     * @return The test id
     */
    public int getTestId() {
        return testId;
    }

    /**
     * @return The configured timeout in seconds
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param finished The finish time of the tag
     */
    public void setFinished(Timestamp finished) {
        this.finished = finished;
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
     * @param The result
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @param started The start time of the tag
     */
    public void setStarted(Timestamp started) {
        this.started = started;
    }

    /**
     * @param id The suite id
     */
    public void setSuiteId(int suiteId) {
        this.suiteId = suiteId;
    }

    /**
     * @param tag The tag name
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @param testId The test id
     */
    public void setTestId(int testId) {
        this.testId = testId;
    }

    /**
     * @param timeout The configured timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

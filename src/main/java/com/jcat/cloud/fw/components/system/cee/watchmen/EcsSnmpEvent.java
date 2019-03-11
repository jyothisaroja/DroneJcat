/**
 *
 */
package com.jcat.cloud.fw.components.system.cee.watchmen;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.bea.xml.stream.EventState;
import com.google.common.base.Objects;
import com.jcat.cloud.fw.common.exceptions.EcsWatchmenException;

/**
 * base class for all SNMP events
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015-05-20 initial version
 * @author eelimei 2016-01-18 Added a missing LogEntryMoiType
 * @author zdagjyo 2018-03-23 Added a missing LogEntryMoiType : License
 */
public class EcsSnmpEvent {

    public abstract static class Builder<S extends Builder<S, T>, T extends EcsSnmpEvent> {
        protected T event;

        public S activeServerity(Severity activeSeverity) {
            event.setActiveSeverity(activeSeverity);
            return getThis();
        }

        public T build() {
            return event;
        }

        public abstract S getThis();

        public S isStateful(boolean isStateful) {
            event.setIs_stateful(isStateful);
            return getThis();
        }
    }

    public enum EventType {
        OTHER(1), COMMUNICATIONS_ALARM(2), QUALITY_OF_SERVICE_ALARM(3), PROCESSING_ERROR_ALARM(4), EQUIPMENT_ALARM(5), ENVIRONMENTAL_ALARM(
                6), INTEGRITY_VIOLATION(7), OPERATIONAL_VIOLATION(8), PHYSICAL_VIOLATION(9), SECURITY_SERVICE_OR_MECHANISM_VIOLATION(
                        10), TIME_DOMAIN_VIOLATION(11);

        private int value;

        EventType(int value) {
            this.value = value;
        }

        /**
         * Get EventType from its int value
         *
         * @param typeValue
         * @return
         */
        public static EventType getEventType(int typeValue) {
            for (EventType type : EventType.values()) {
                if (type.getValue() == typeValue) {
                    return type;
                }
            }
            throw new EcsWatchmenException("Failed to convert value '" + typeValue + "' into a valid EventType.");
        }

        /**
         * Get the int value of a EventType
         *
         * @return
         */
        public int getValue() {
            return value;
        }
    }

    /**
     * The different field types that exist in the source field.
     */
    public enum LogEntryMoiType {
        REGION("Region"), UPSTREAM_NTPSERVER_CONNECTION("UpstreamNTPServerConnection"), CEE_FUNCTION("CeeFunction"), CTRL_DOMAIN(
                "CtrlDomain"), CIC("CIC"), DISTRIBUTED_STORAGE("DistributedStorage"), FUEL("Fuel"), EQUIPMENT("Equipment"), LICENSE("License"),
                 STORAGE_SYSTEM_NAME("ExternalStorage"), HOST("Host"), NETWORK("Network"), AGGR("Aggregator"),
                 TOP_OF_RACK_SWITCH("TopOfRackSwitch"), ETHERNET_PORT("EthernetPort"), FAN("Fan"), SERVER_BLADE("ServerBlade"),
                 POWER_SUPPLY("PowerSupply"), TENANT("Tenant"), VM_ID("VM"), SERVICE("Service"),CERTIFICATE("Certificate"),NODE("Node"),COREDUMP("CoreDump");

        private String mName;

        private LogEntryMoiType(String name) {
            mName = name;
        }

        /**
         * Get LogEntryMoiType by string value
         *
         * @param moi
         * @return
         */
        public static LogEntryMoiType getMoiType(String moi) {
            for (LogEntryMoiType currMoi : LogEntryMoiType.values()) {
                if (moi.equals(currMoi.mName)) {
                    return currMoi;
                }
            }
            throw new EcsWatchmenException("Failed to convert value '" + moi + "' into a valid LogEntryMoiType.");
        }
    }

    /**
     * Active Severity
     *
     */
    public enum Severity {
        CLEARED(1), CRITICAL(3), INDETERMINATE(2), MAJOR(4), MINOR(5), WARNING(6);
        private int level;

        Severity(int level) {
            this.level = level;
        }

        /**
         * Get severity from its int value
         *
         * @param level
         * @return
         */
        public static Severity getSeverity(int level) {
            for (Severity s : Severity.values()) {
                if (s.level == level) {
                    return s;
                }
            }
            throw new EcsWatchmenException("Failed to convert value '" + level + "' into a valid Severity.");
        }

        public int getLevel() {
            return level;
        }
    }

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssXXX");

    private Severity active_severity;

    private String additional_info;

    private String additional_text;

    private EventType event_type;

    private boolean is_stateful;

    private DateTime last_event_time;

    private int major_type;

    private int minor_type;

    // Maximum time to wait for this event to be found in the log.
    private int mMaximumTimeoutInSeconds;

    private long probable_cause;

    private int sequence_no;

    private Map<LogEntryMoiType, String> source = new HashMap<LogEntryMoiType, String>();

    private String specific_problem;

    // Region name, default value is the node name.
    protected String mRegionName = null;

    protected EventState mEventState = null;

    public EcsSnmpEvent() {

    }

    public EcsSnmpEvent(int minorId, int majorId, int maximumTimeoutInSeconds) {
        major_type = majorId;
        minor_type = minorId;
        mMaximumTimeoutInSeconds = maximumTimeoutInSeconds;
    }

    /**
     * Constructor used by deserializer
     *
     * @param active_severity
     * @param additional_info
     * @param additional_text
     * @param event_type
     * @param is_stateful
     * @param last_event_time
     * @param major_type
     * @param minor_type
     * @param probable_cause
     * @param sequence_no
     * @param source
     * @param specific_problem
     */
    public EcsSnmpEvent(Severity active_severity, String additional_info, String additional_text, int event_type,
            boolean is_stateful, DateTime last_event_time, int major_type, int minor_type, long probable_cause,
            int sequence_no, String source, String specific_problem) {
        this.active_severity = active_severity;
        this.additional_info = additional_info;
        this.additional_text = additional_text;
        this.event_type = EventType.getEventType(event_type);
        this.is_stateful = is_stateful;
        this.last_event_time = last_event_time;
        this.major_type = major_type;
        this.minor_type = minor_type;
        this.probable_cause = probable_cause;
        this.sequence_no = sequence_no;
        this.source = parseSourceInfo(source);
        this.specific_problem = specific_problem;
    }

    /**
     * parse source string into a key-value map
     *
     * @param source
     * @return
     */
    private Map<LogEntryMoiType, String> parseSourceInfo(String source) {
        Map<LogEntryMoiType, String> sourceInfo = new HashMap<LogEntryMoiType, String>();
        String[] elements = source.split(",");
        for (String str : elements) {
            String[] elementValues = str.split("=");
            sourceInfo.put(LogEntryMoiType.getMoiType(elementValues[0]), elementValues[1]);
        }
        return sourceInfo;
    }

    /**
     * Check if source value in current object matches values in given object (ignore values which does not exist in
     * current object)
     *
     * @param moiArgs
     * @return
     */
    private boolean sourceMatches(Map<LogEntryMoiType, String> moiArgs) {
        if (moiArgs == null) {
            if (source == null) {
                return true;
            } else {
                return false;
            }
        }
        for (LogEntryMoiType key : source.keySet()) {
            String eventValue = moiArgs.get(key);
            String currValue = source.get(key);
            if (!currValue.equalsIgnoreCase(eventValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * method which adds key-value pairs to source field (only used in builders of subclasses of EcsSnmpEvent)
     *
     * @param key
     * @param value
     */
    protected void addMoiArgs(LogEntryMoiType key, String value) {
        this.source.put(key, value);
    }

    /**
     * Check two events to see if they are exactly the same.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EcsSnmpEvent)) {
            return false;
        }

        final EcsSnmpEvent that = (EcsSnmpEvent) obj;

        Map<LogEntryMoiType, String> moiArgs = that.getSource();

        // TODO: Add more check conditions if necessary
        return (this.major_type == that.major_type) && (this.minor_type == that.minor_type)
                && (this.is_stateful == that.is_stateful) && (this.active_severity == that.active_severity)
                && (this.probable_cause == that.probable_cause) && (source != null && source.equals(moiArgs));
    }

    public Severity getActiveSeverity() {
        return active_severity;
    }

    public String getAdditionalInfo() {
        return additional_info;
    }

    public String getAdditionalText() {
        return additional_text;
    }

    public EventType getEventType() {
        return event_type;
    }

    public DateTime getLastEventTime() {
        return last_event_time;
    }

    public int getMajorType() {
        return major_type;
    }

    public int getMaximumTimeout() {
        return mMaximumTimeoutInSeconds;
    }

    public int getMinorType() {
        return minor_type;
    }

    public long getProbableCause() {
        return probable_cause;
    }

    public int getSequenceNo() {
        return sequence_no;
    }

    public Map<LogEntryMoiType, String> getSource() {
        return source;
    }

    public String getSpecific_problem() {
        return specific_problem;
    }

    public boolean isStateful() {
        return is_stateful;
    }

    /**
     * Compare with another object to see if their corresponding fields matches (ignore non-existing values in this
     * object)
     * This is used to match an event (without providing all the details) to events received in Watchmen (with all the
     * detailed information)
     *
     * @param obj
     * @return
     */
    public boolean matches(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EcsSnmpEvent)) {
            return false;
        }

        final EcsSnmpEvent that = (EcsSnmpEvent) obj;
        Map<LogEntryMoiType, String> moiArgs = that.getSource();

        return (this.major_type == that.major_type) && (this.minor_type == that.minor_type)
                && (this.is_stateful == that.is_stateful) && (this.active_severity == that.active_severity)
                && sourceMatches(moiArgs);
    }

    public void setActive_severity(int active_severity) {
        for (Severity s : Severity.values()) {
            if (s.level == active_severity) {
                this.active_severity = s;
                return;
            }
        }
        throw new EcsWatchmenException("Can not find corresponding Severity in fw for level:" + active_severity);
    }

    public void setActiveSeverity(Severity activeSeverity) {
        this.active_severity = activeSeverity;
    }

    public void setAdditional_info(String additional_info) {
        this.additional_info = additional_info;
    }

    public void setAdditional_text(String additional_text) {
        this.additional_text = additional_text;
    }

    public void setEvent_type(int event_type) {
        this.event_type = EventType.getEventType(event_type);
    }

    public void setIs_stateful(boolean is_stateful) {
        this.is_stateful = is_stateful;
    }

    public void setLast_event_time(String last_event_time) throws ParseException {
        this.last_event_time = new DateTime(formatter.parse(last_event_time));
    }

    public void setMajor_type(int major_type) {
        this.major_type = major_type;
    }

    public void setMinor_type(int minor_type) {
        this.minor_type = minor_type;
    }

    public void setProbable_cause(long probable_cause) {
        this.probable_cause = probable_cause;
    }

    public void setSequence_no(int sequence_no) {
        this.sequence_no = sequence_no;
    }

    public void setSource(String source) {
        this.source = parseSourceInfo(source);
    }

    public void setSpecific_problem(String specific_problem) {
        this.specific_problem = specific_problem;
    }

    @Override
    public String toString() {
        //TODO: add more fields if necessary
        return Objects.toStringHelper(this).add("major_type", major_type).add("minor_type", minor_type)
                .add("is_stateful", is_stateful).add("active_severity", active_severity).add("source", source)
                .add("probable_cause", probable_cause).add("specific_problem", specific_problem)
                .add("additional_info", additional_info).add("additional_text", additional_text)
                .add("Last Event Time", last_event_time).toString();
    }
}

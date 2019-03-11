package com.jcat.cloud.fw.components.system.cee.watchmen;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.jcat.cloud.fw.common.exceptions.EcsWatchmenException;
import com.jcat.cloud.fw.components.system.cee.watchmen.EcsSnmpEvent.LogEntryMoiType;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.CentralizedStorageAlert;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.CicFailed;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.CicRestarted;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.CompleteCICServiceRestarted;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.ComputeHostFailed;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.ComputeHostRestarted;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.EthernetPortAggregatorFault;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.EthernetPortFault;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.EthernetSwitchPortFault;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.FanFailure;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.FuelFailed;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.FuelRestarted;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.HighCpuLoad;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.HighLocalDiskUtilization;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.HighMemoryUtilization;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.NTPStratumLevelFailure;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.NTPUpstreamServerFailure;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.PowerSupplyFailure;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.VmEvacuationFailed;
import com.jcat.cloud.fw.components.system.cee.watchmen.events.VmUnavailable;

/**
 * Factory for creating SNMP events based on watchmen log entry.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-05-19 initial version
 * @author eelimei 2016-01-18 Added create method for new alarm type
 *
 */
public class EcsSnmpFactory {

    public enum LogActiveSeverity {
        CLEARED(1), INDETERMINATE(2), CRITICAL(3), MAJOR(4), MINOR(5), WARNING(6);

        private int mSeverity;

        private LogActiveSeverity(int severity) {
            mSeverity = severity;
        }

        public static LogActiveSeverity getSeverity(int value) {
            for (LogActiveSeverity status : LogActiveSeverity.values()) {
                if (status.getSeverity() == value) {
                    return status;
                }
            }
            throw new EcsWatchmenException(
                    "The value in the log entry could not be converted to a valid 'active severity' value.");
        }

        private int getSeverity() {
            return mSeverity;
        }
    }

    /**
     * Contains all the different entries that exist for an watchmen log entry.
     *
     * @author epergat 2015- initial version
     *
     */
    public enum LogEntryType {
        SOURCE, MINOR_ID, MAJOR_ID, ACTIVE_SEVERITY, EVENT_TYPE, PROBABLE_CAUSE, SPECIFIC_PROBLEM, ADDITIONAL_INFO, ADDITIONAL_TEXT, IS_STATEFUL, SEQUENCE_NO, LAST_EVENT_TIME, SYNC_URL;
    }

    private static Logger mLogger = Logger.getLogger(EcsSnmpFactory.class);

    private static EcsSnmpEvent createCentralizedStorageAlert(Map<LogEntryMoiType, String> moiMap) {
        String storageSystemName = moiMap.get(LogEntryMoiType.STORAGE_SYSTEM_NAME);
        return CentralizedStorageAlert.builder().storageSystemName(storageSystemName).build();
    }

    private static EcsSnmpEvent createCicFailedEvent(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String cic = moiMap.get(LogEntryMoiType.CIC);
        return CicFailed.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain).cic(cic).build();
    }

    private static EcsSnmpEvent createCicRestarted(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String cic = moiMap.get(LogEntryMoiType.CIC);
        return CicRestarted.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain).cic(cic).build();
    }

    private static EcsSnmpEvent createCompleteCICServiceRestarted(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);

        return CompleteCICServiceRestarted.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain)
                .build();
    }

    private static EcsSnmpEvent createComputeHostFailed(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String host = moiMap.get(LogEntryMoiType.HOST);

        return ComputeHostFailed.builder().region(region).ceeFunction(ceeFunction).host(host).build();
    }

    private static EcsSnmpEvent createComputeHostRestarted(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String host = moiMap.get(LogEntryMoiType.HOST);

        return ComputeHostRestarted.builder().region(region).ceeFunction(ceeFunction).host(host).build();
    }

    private static EcsSnmpEvent createEthernetPortAggregatorFault(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String host = moiMap.get(LogEntryMoiType.HOST);
        String network = moiMap.get(moiMap.get(LogEntryMoiType.NETWORK));
        String aggr = moiMap.get(LogEntryMoiType.AGGR);
        ;

        return EthernetPortAggregatorFault.builder().region(region).ceeFunction(ceeFunction).host(host)
                .network(network).aggr(aggr).build();
    }

    private static EcsSnmpEvent createEthernetPortFault(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String host = moiMap.get(LogEntryMoiType.HOST);
        String network = moiMap.get(moiMap.get(LogEntryMoiType.NETWORK));
        String aggr = moiMap.get(LogEntryMoiType.AGGR);
        ;
        int ethernetPort = Integer.parseInt(moiMap.get(LogEntryMoiType.ETHERNET_PORT));
        return EthernetPortFault.builder().region(region).ceeFunction(ceeFunction).host(host).network(network)
                .aggr(aggr).ethernetPort(ethernetPort).build();
    }

    private static EcsSnmpEvent createEthernetSwitchPortFault(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String topOfRackSwitch = moiMap.get(LogEntryMoiType.TOP_OF_RACK_SWITCH);
        int ethernetPort = Integer.parseInt(moiMap.get(LogEntryMoiType.ETHERNET_PORT));
        return EthernetSwitchPortFault.builder().region(region).equipment(equipment).ethernetPort(ethernetPort)
                .topOfRackSwith(topOfRackSwitch).build();
    }

    private static EcsSnmpEvent createFanFailure(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String topOfRackSwitch = moiMap.get("TopOfRackSwitch");
        String fan = moiMap.get("Fan");
        return FanFailure.builder().region(region).equipment(equipment).fan(fan).topOfRackSwith(topOfRackSwitch)
                .build();
    }

    private static EcsSnmpEvent createFuelFailed(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String fuelId = moiMap.get(LogEntryMoiType.FUEL);
        return FuelFailed.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain).fuelId(fuelId)
                .build();
    }

    private static EcsSnmpEvent createFuelRestartedEvent(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String fuelId = moiMap.get(LogEntryMoiType.FUEL);
        return FuelRestarted.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain).fuelId(fuelId)
                .build();
    }

    private static EcsSnmpEvent createHighCpuLoad(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String serverBlade = moiMap.get(LogEntryMoiType.SERVER_BLADE);
        return HighCpuLoad.builder().region(region).equipment(equipment).serverBlade(serverBlade).build();
    }

    private static EcsSnmpEvent createHighLocalDiskUtilization(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String serverBlade = moiMap.get(LogEntryMoiType.SERVER_BLADE);
        return HighLocalDiskUtilization.builder().region(region).equipment(equipment).serverBlade(serverBlade).build();
    }

    private static EcsSnmpEvent createHighMemoryUtilization(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String serverBlade = moiMap.get(LogEntryMoiType.SERVER_BLADE);
        return HighMemoryUtilization.builder().region(region).equipment(equipment).serverBlade(serverBlade).build();
    }

    private static EcsSnmpEvent createNTPStratumLevelFailure(HashMap<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String upstreamNTPServerConnection = moiMap.get(LogEntryMoiType.UPSTREAM_NTPSERVER_CONNECTION);
        String cic = moiMap.get(LogEntryMoiType.CIC);
        return NTPStratumLevelFailure.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain)
                .upstreamNTPServerConnnection(upstreamNTPServerConnection).cic(cic).build();
    }

    private static EcsSnmpEvent createNTPUpstreamServerFailure(HashMap<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String ctrlDomain = moiMap.get(LogEntryMoiType.CTRL_DOMAIN);
        String upstreamNTPServerConnection = moiMap.get(LogEntryMoiType.UPSTREAM_NTPSERVER_CONNECTION);
        String cic = moiMap.get(LogEntryMoiType.CIC);
        return NTPUpstreamServerFailure.builder().region(region).ceeFunction(ceeFunction).ctrlDomain(ctrlDomain)
                .upstreamNTPServerConnnection(upstreamNTPServerConnection).cic(cic).build();
    }

    private static EcsSnmpEvent createPowerSupplyFailure(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String equipment = moiMap.get(LogEntryMoiType.EQUIPMENT);
        String topOfRackSwitch = moiMap.get("TopOfRackSwitch");
        String powerSupply = moiMap.get("PowerSupply");
        return PowerSupplyFailure.builder().region(region).equipment(equipment).powerSupply(powerSupply)
                .topOfRackSwith(topOfRackSwitch).build();
    }

    private static EcsSnmpEvent createVmEvacuationFailed(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String tenant = moiMap.get(LogEntryMoiType.TENANT);
        String vmId = moiMap.get(LogEntryMoiType.VM_ID);

        return VmEvacuationFailed.builder().region(region).ceeFunction(ceeFunction).tenant(tenant).vmId(vmId).build();
    }

    private static EcsSnmpEvent createVmUnavailable(Map<LogEntryMoiType, String> moiMap) {
        String region = moiMap.get(LogEntryMoiType.REGION);
        String ceeFunction = moiMap.get(LogEntryMoiType.CEE_FUNCTION);
        String tenant = moiMap.get(LogEntryMoiType.TENANT);
        String vmId = moiMap.get(LogEntryMoiType.VM_ID);

        return VmUnavailable.builder().region(region).ceeFunction(ceeFunction).tenant(tenant).vmId(vmId).build();
    }

    private static HashMap<LogEntryMoiType, String> extractMoiValues(String sourceMoi) {
        // The MoI (Managed object Instance) has the format [<key>=<value>,]*
        StringTokenizer st = new StringTokenizer(sourceMoi, ",");
        HashMap<LogEntryMoiType, String> moiMap = new HashMap<LogEntryMoiType, String>();
        while (st.hasMoreTokens()) {
            // format of currToken is <key>=<value>
            String currToken = st.nextToken();
            String[] args = currToken.split("=");
            moiMap.put(LogEntryMoiType.getMoiType(args[0].trim()), args[1]);
        }
        return moiMap;
    }

    private static String getValue(String st, int index) {
        return st.split(":")[index].trim();
    }

    private static HashMap<LogEntryType, String> parseLogEntry(String logEntry) {
        HashMap<LogEntryType, String> entries = new HashMap<LogEntryType, String>();

        /*
         * Example of log entry:
         * <INT><DATE> cic-0-1 watchmen-history - INFO - source: <TEXT>; major_type: <INT>; minor_type: <INT>;
         * active_severity: <INT>; event_type: <INT>; probable_cause: <INT>; specific_problem: <TEXT>; additional_info:
         * <TEXT>; additional_text: <TEXT>; is_stateful: <True | False>; sequence_no: <INT>; last_event_time: <DATE>
         * sync_url: <URL>
         */

        /*
         * As can be seen above, the watchmen log entry is just a couple of key-value pairs seperated by a semi-colon
         * (;)
         * So first we split the values based on the semicolon and we get a list of <key:Value> and the method below
         * called getValue() just does that, split the string in two sections based on the colon (:).
         */
        try {
            StringTokenizer st = new StringTokenizer(logEntry, ";");
            entries.put(LogEntryType.SOURCE, getValue(st.nextToken(), 3));
            entries.put(LogEntryType.MAJOR_ID, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.MINOR_ID, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.ACTIVE_SEVERITY, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.EVENT_TYPE, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.PROBABLE_CAUSE, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.SPECIFIC_PROBLEM, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.ADDITIONAL_INFO, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.ADDITIONAL_TEXT, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.IS_STATEFUL, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.SEQUENCE_NO, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.LAST_EVENT_TIME, getValue(st.nextToken(), 1));
            entries.put(LogEntryType.SYNC_URL, getValue(st.nextToken(), 1));
        } catch (Exception ex) {
            throw new EcsWatchmenException("Invalid watchmen log entry (" + logEntry
                    + "was given to the EcsSnmpFactory.");
        }
        return entries;
    }

    /**
     * Creates a SNMP event based on a log entry from watchmen history file.
     *
     * @param logEntry
     * @return
     */
    public static EcsSnmpEvent create(String logEntry) {

        HashMap<LogEntryType, String> entries = parseLogEntry(logEntry);

        // Time to get the unique identifier for the log event.
        int majorId = Integer.parseInt(entries.get(LogEntryType.MAJOR_ID));
        int minorId = Integer.parseInt(entries.get(LogEntryType.MINOR_ID));

        // Extract the Managed object Instance information.
        HashMap<LogEntryMoiType, String> moiMap = extractMoiValues(entries.get(LogEntryType.SOURCE));

        // Time to create the event based on the data we have extracted.
        if (majorId == CentralizedStorageAlert.Major) {

            if (minorId == CentralizedStorageAlert.Minor) {
                return createCentralizedStorageAlert(moiMap);
            }
            if (minorId == CicFailed.Minor) {
                return createCicFailedEvent(moiMap);
            }
            if (minorId == FuelRestarted.Minor) {
                return createFuelRestartedEvent(moiMap);
            }
            if (minorId == HighLocalDiskUtilization.Minor) {
                return createHighLocalDiskUtilization(moiMap);
            }
            if (minorId == EthernetSwitchPortFault.Minor) {
                return createEthernetSwitchPortFault(moiMap);
            }
            if (minorId == EthernetPortFault.Minor) {
                return createEthernetPortFault(moiMap);
            }
            if (minorId == EthernetPortAggregatorFault.Minor) {
                return createEthernetPortAggregatorFault(moiMap);
            }
            if (minorId == VmUnavailable.Minor) {
                return createVmUnavailable(moiMap);
            }
            if (minorId == CicRestarted.Minor) {
                return createCicRestarted(moiMap);
            }
            if (minorId == PowerSupplyFailure.Minor) {
                return createPowerSupplyFailure(moiMap);
            }
            if (minorId == VmEvacuationFailed.Minor) {
                return createVmEvacuationFailed(moiMap);
            }
            if (minorId == FuelFailed.Minor) {
                return createFuelFailed(moiMap);
            }
            if (minorId == FanFailure.Minor) {
                return createFanFailure(moiMap);
            }
            if (minorId == HighMemoryUtilization.Minor) {
                return createHighMemoryUtilization(moiMap);
            }
            if (minorId == HighCpuLoad.Minor) {
                return createHighCpuLoad(moiMap);
            }
            if (minorId == CompleteCICServiceRestarted.Minor) {
                return createCompleteCICServiceRestarted(moiMap);
            }
            if (minorId == ComputeHostRestarted.Minor) {
                return createComputeHostRestarted(moiMap);
            }
            if (minorId == ComputeHostFailed.Minor) {
                return createComputeHostFailed(moiMap);
            }
            if (minorId == NTPUpstreamServerFailure.Minor) {
                return createNTPUpstreamServerFailure(moiMap);
            }
            if (minorId == NTPStratumLevelFailure.Minor) {
                return createNTPStratumLevelFailure(moiMap);
            }
        }
        throw new EcsWatchmenException("The event type with major id: " + majorId + " and minor id: " + minorId
                + " is not implemented in the Watchmen log parser.");
    }
}

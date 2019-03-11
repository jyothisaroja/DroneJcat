package com.jcat.cloud.fw.fwservices.monitoring.app;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.python.google.common.collect.Iterables;

import com.google.inject.Inject;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.model.target.EcsTarget;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.fwservices.monitoring.db.MonitoringProcess;
import com.jcat.cloud.fw.fwservices.monitoring.util.DatabaseHelper;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 *
 * Collects process information from the nodes for each host that is ready
 * on an interval basis. The information is stored in a database that is
 * specified and configured in Hibernate (src/main/resources/hibernate.cfg.xml).
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
public class ProcessCollector {
    @Inject
    private DatabaseHelper mDbHandler;

    @Inject
    private EcsComputeBladeList mComputeBladeList;

    @Inject
    private EcsCicList mCicList;

    private static final String PS_CMD = "ps -Ao pid,%cpu,%mem,user,comm\n";

    private static final String TIME_CMD = "date +%s\n";

    private String mNode;

    /**
     * Logger instance to be used in a test case
     */
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor.
     */
    @Inject
    public ProcessCollector(TestConfiguration testConfig) {
        mNode = testConfig.getNode();
    }

    private List<MonitoringProcess> getProcessInfo(List<? extends EcsTarget> hosts) {
        List<MonitoringProcess> processes = new ArrayList<MonitoringProcess>();
        for (EcsTarget host : hosts) {
            long unixTime = getUnixTime(host);
            String response = host.sendCommand(PS_CMD);
            // Parse the string to a list of process objects and add those to the list
            processes.addAll(parseProcesserInfo(response, host.getHostname(), unixTime));
        }
        return processes;
    }

    /**
     * Gets the time from the host of by starting a SSH session
     * and performing the date command.
     * @param host The name of the host
     * @return The Unix time
     */
    private long getUnixTime(EcsTarget host) {
        String response = host.sendCommand(TIME_CMD);
        return Long.parseLong(response.split("\n")[1].trim());
    }

    /**
     * Parses the given String which is an shell output from the ps command
     * and returns a list of processes that were included.
     * @param str The shell output
     * @param unixTime The Unix time (seconds) when the samples were taken
     * @return A list of process data
     */
    private List<MonitoringProcess> parseProcesserInfo(String str, String host, long unixTime) {
        List<MonitoringProcess> processes = new ArrayList<MonitoringProcess>();
        String[] processData = str.split("\n");
        // Skip the first two rows and the last one since they contain text
        // from the shell that is not a process data row
        for (int i = 2; i < processData.length - 1; i++) {
            String[] sampleStr = processData[i].trim().split("\\s+");
            MonitoringProcess process = new MonitoringProcess();
            process.setPid(Integer.parseInt(sampleStr[0]));
            process.setNode(mNode);
            process.setHost(host);
            process.setCpu(Float.parseFloat(sampleStr[1]));
            process.setMem(Float.parseFloat(sampleStr[2]));
            process.setUser(sampleStr[3]);
            process.setProcess(sampleStr[4]);
            process.setTimestamp(new Timestamp(unixTime * 1000)); // Convert s to ms
            processes.add(process);
        }
        return processes;
    }

    /**
     * Gets a list of processes currently running on the given node for all hosts
     * by parsing the output from the Unix ps command.
     * @return A list of process data
     */
    public Iterable<MonitoringProcess> collect() {
        List<EcsComputeBlade> computeBlades = mComputeBladeList.getAllComputeBlades();
        List<EcsCic> cics = mCicList.getAllCics();

        List<MonitoringProcess> processInfoForComputeBlades = getProcessInfo(computeBlades);
        List<MonitoringProcess> processInfoForCics = getProcessInfo(cics);
        return Iterables.concat(processInfoForComputeBlades, processInfoForCics);
    }

    /**
     * Start the sampling process including the nice processes (processes
     * that use no cpu or memory).
     * @param samplingIntervalMs The process sampling interval
     */
    public void start(long samplingIntervalMs) {
        start(samplingIntervalMs, false);
    }

    /**
     * Starts the sampling process.
     * @param samplingIntervalMs The process sampling interval
     * @param skipNiceProcesses If set to true, processes that use no cpu and no memory are skipped
     * (use when you need to save space in the database)
     */
    public void start(long samplingIntervalMs, boolean skipNiceProcesses) {
        System.out.println("[ INFO] Started to sample process data...");
        while (true) {
            try {
                Iterable<MonitoringProcess> processSamples = collect();
                for (MonitoringProcess sample : processSamples) {
                    if (!skipNiceProcesses) {
                        mDbHandler.save(sample);
                    } else if (sample.getCpu() > 0 || sample.getMem() > 0) {
                        mDbHandler.save(sample);
                    }
                }
            } catch (Exception e) {
                mLogger.error("[ERROR] Could not collect process data: " + e.getMessage());
            }

            // pause before next sampling
            try {
                TimeUnit.MILLISECONDS.sleep(samplingIntervalMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

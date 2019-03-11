package com.jcat.cloud.fw.components.model.compute;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsTrafficException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.image.EcsImage;
import com.jcat.cloud.fw.fwservices.traffic.model.FioResult;
import com.jcat.cloud.fw.fwservices.traffic.model.TestAppResult;
import com.jcat.cloud.fw.fwservices.traffic.plugins.BatTrafficPlugin;

/**
 * Class which represents BAT VM, handles operations performed on it.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zmousar 2018-08-20 initial version
 */
public class EcsBatVm extends EcsVm {

    private static String DETACHED_EXECUTION = " 2>&1 &";
    private static String LOG_FILE = "bat_log.txt";
    private static String PATH = "/home/batman";
    private static String REDIRECT_TO_LOG = " > " + PATH   + "/" + LOG_FILE;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsBatVm.class);
    private int mStatsStartIndex = 0;
    private int mStatsEndIndex = 0;

    @Inject
    public EcsBatVm(@Assisted("serverId") String serverId, @Assisted("ecsImage") EcsImage ecsImage,
            @Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("networkId") String networkId,
            @Assisted("vmIp") String vmIp) {
        super(serverId, ecsImage, hypervisorHostname, hypervisorInstanceId, networkId, vmIp);
    }

    /**
     * Parse the value of FIO traffic
     * e.g. READ: io=18212.6KB, aggrb=499KB/s, minb=499.5KB/s, maxb=499KB/s, mint=36426msec, maxt=36426msec
     *
     * @param readWriteTrafficStatus line of traffic stats
     * @return Map of FIO traffic elements
     */
    private Map<String, Float> parseFioTraffic(String readWriteTrafficStatus) {
        final String separator = "\\s";
        final String regex = "(([\\w-]+)=(\\d+.+))";
        Map<String, Float> map = new HashMap<String, Float>();
        // Split the readWriteTrafficStatus into parts
        String[] parts = readWriteTrafficStatus.split(separator);
        // Go through each part
        for (String item : parts) {
            // Ignore parts such as "total:"
            if (item.matches(regex)) {
                // Split each part with = mark
                String[] sub = item.split("=");

                // Split and convert result to int
                String[] value = sub[1].split("[a-zA-Z]");
                float result = new Float(value[0]);
                map.put(sub[0], result);
            }
        }
        return map;
    }

    /**
     * Prints script execution result in debug mode
     * @param command - command to execute on bat Controller VM
     * @param scriptName - name of script
     *
     * @return the complete execution result in a string
     * @throws IOException
     */
    private String printScriptExecutionResultInDebugMode(String command, String scriptName) throws IOException {
        String fileContent;
        try {
            new LoopHelper<Boolean>(Timeout.BAT_SCRIPTS_RUN, 3, "Bat script doesn't seem to finish in time", true,
                    () -> {
                        sync();
                        String content = tailFile(PATH, LOG_FILE);
                        if (content.contains("a BAT script is already running, wait for it to finish")) {
                            mLogger.warn("Previous BAT command was still running! This was not supposed to happen. Check what happened before.");
                            mSshSession.send(command);
                            return false;
                        }
                        if (content.toUpperCase().contains(scriptName.toUpperCase() + ".ERROR")) {
                            throw new EcsTrafficException(BatTrafficPlugin.class,
                                     "Error was found during executing script: " + command
                                            + "\n Last lines from console log:\n" + content);
                        }
                        if (content.contains("script finished")) {
                            return true;
                        }
                        return false;
                    }).setIterationDelay(10).run();
        } finally {
            fileContent = readFile(PATH, LOG_FILE);
            deleteFile(PATH, LOG_FILE);
        }
        return fileContent;
    }

    /**
     * Execute a bat script non-blocking in batVms
     *
     * @param cmd the script's name and parameter
     * @return the result of the execution
     * @throws IOException
     */
    private String sendBatCommandAsync(String cmd) {
        String scriptName = cmd.substring(0, cmd.indexOf(".sh"));
        cmd = "nohup " + PATH + "/scripts/" + cmd + REDIRECT_TO_LOG + DETACHED_EXECUTION;
        mSshSession.send(cmd);
        try {
            return printScriptExecutionResultInDebugMode(cmd, scriptName);
        } catch (IOException e) {
            mLogger.warn("Something went wrong while running script " + e.getMessage());
            return "";
        }
    }

    /**
     * Returns the IP of Bat VM.
     *
     * @return String
     */
    public String getIpAddress() {
        return mVmIp;
    }

    /**
     * Fetch TestApp traffic status
     * @param resetTraffic - used for resetting TestApp Traffic
     *
     * @return
     */
    public List<TestAppResult> getTestAppTraffic(boolean resetTraffic) {
        mLogger.info(EcsAction.FINDING, TestAppResult.class);
        String testAppTrafficResult;
        if(resetTraffic) {
            testAppTrafficResult = sendBatCommandAsync("testapp_stats.sh -r");
        }
        else {
            testAppTrafficResult = sendBatCommandAsync("testapp_stats.sh");
        }
        List<TestAppResult> results = new ArrayList<TestAppResult>();
        String[] lines = testAppTrafficResult.split(System.getProperty("line.separator"));
        mStatsStartIndex = Arrays.asList(lines).indexOf("total:");
        mStatsEndIndex = Arrays.asList(lines).indexOf("script finished");
        for (String line : lines) {
            if (line.indexOf("total") == -1) {
                mLogger.debug("This line of reply from TestApp is skipped: \"" + line + "\"");
                continue;
            }
            String name = line.substring(0, line.indexOf(" "));
            int sent = Integer.parseInt(line.substring(line.indexOf("send=") + "send=".length(),
                    line.indexOf(" ", line.indexOf("send=") + "send=".length())));
            int sendFailed = Integer.parseInt(line.substring(line.indexOf("send-failed=") + "send-failed=".length(),
                    line.indexOf(" ", line.indexOf("send-failed=") + "send-failed=".length())));
            int received = Integer.parseInt(line.substring(line.indexOf("recv=") + "recv=".length(),
                    line.indexOf(" ", line.indexOf("recv=") + "recv=".length())));
            int failed = Integer.parseInt(line.substring(line.indexOf("fail=") + "fail=".length(),
                    line.indexOf(" ", line.indexOf("fail=") + "fail=".length())));
            int timeout = Integer.parseInt(line.substring(line.indexOf("timeout=") + "timeout=".length(),
                    line.indexOf(" ", line.indexOf("timeout=") + "timeout=".length())));
            int unknown = Integer.parseInt(line.substring(line.indexOf("unknown=") + "unknown=".length(),
                    line.length() - 1));
            results.add(new TestAppResult(name, sent, received, sendFailed, failed, timeout, unknown));
        }
        return results;
    }

    /**
     * Fetch FIO traffic status
     *
     * @return
     */
    public List<FioResult> getFioTraffic() {
        mLogger.info(EcsAction.FINDING, FioResult.class);
        String fioResponse = sendBatCommandAsync("fio_stats.sh");
        Calendar systemTime = Calendar.getInstance();
        systemTime.setTime(getDate());
        List<FioResult> results = new ArrayList<FioResult>();
        List<String> lines = Arrays.asList(fioResponse.split(System.getProperty("line.separator")));
        for (Iterator<String> iter = lines.iterator(); iter.hasNext();) {
            String line = iter.next();
            if (line.contains("Fio statistics")) {
                String name = line.substring(line.indexOf("on VM ") + "on VM ".length(),
                        line.indexOf(" ", line.indexOf("on VM ") + "on VM ".length()));
                String date = line.substring(line.indexOf("from ") + "from ".length(), line.length() - 1);
                Calendar fioTime = Calendar.getInstance();
                try {
                    fioTime.setTime(new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH).parse(date));
                    fioTime.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));

                } catch (ParseException e) {
                    throw new EcsTrafficException(BatTrafficPlugin.class,
                            "Parsing date from FIO status failed. 0xc00000e9 " + date);
                }
                if (Math.abs(fioTime.getTimeInMillis() - systemTime.getTimeInMillis()) > 600000) {
                    throw new EcsTrafficException(BatTrafficPlugin.class,
                            "FIO status shows too much time deviation from system time (>10min). Systime: "
                                    + systemTime.getTime() + " FioTime: " + fioTime.getTime());
                }
                /**
                 * Need to get "READ" and "WRITE" stats from Fio traffic
                 * ex: batman@bat-t0-controller:~/scripts$ ./fio_stats.sh
                 *    <13>Jan  3 07:34:15 [2019-01-03, 07:34:15.770] fio_stats.info(B: Fio statistics on VM BAT-T0-B1-0-3 in /home/batman/fio/ephemeral from Jan 3 07:34
                 *      IO depths    : 1=100.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=0.0%
                 *      errors    : total=0, first_error=0/<Success>
                 *    Run status group 0 (all jobs):
                 *       READ: io=20488MB, aggrb=341KB/s, minb=341KB/s, maxb=341KB/s, mint=61413835msec, maxt=61413835msec
                 *       WRITE: io=20484MB, aggrb=341KB/s, minb=341KB/s, maxb=341KB/s, mint=61413835msec, maxt=61413835msec
                 */
                iter.next();
                iter.next();
                iter.next();
                String read = iter.next();
                Map<String, Float> readMap = parseFioTraffic(read);
                String write = iter.next();
                Map<String, Float> writeMap = parseFioTraffic(write);
                try {
                    results.add(new FioResult(name, fioTime, readMap, writeMap));
                } catch (NullPointerException e) {
                    mLogger.error("Something is missing: name:" + name + " fioTime:" + fioTime + " readMap:" + readMap
                            + " writeMap:" + writeMap);
                    return null;
                }
            } else {
                mLogger.debug("This line of reply from FIO is skipped: \"" + line + "\"");
            }
        }
        return results;
    }

    /**
     * Get the total number of lines for only Bat Vms traffic stats in Bat_Ciontroller_Vm
     * ex: batman@bat-t0-controller:~/scripts$ ./testapp_stats.sh
     *      <13>Dec 31 08:02:52 [2018-12-31, 08:02:52.795] testapp_stats.info(B: checking TestApp statistics for: A1 B1
     *      <13>Dec 31 08:02:52 [2018-12-31, 08:02:52.798] testapp_stats.info(B: checking TestApp statistics
     *      <13>Dec 31 08:02:53 [2018-12-31, 08:02:53.079] testapp_stats.info(B: fetching statistics for BAT-T0-A1-0-2 from 172.20.0.5
     *      <13>BAT-T0-A1-0-2   00h:26m:20s  total:  send=2696440  send-failed=0        recv=2696440  fail=0  timeout=0       unknown=0
     *      <13>BAT-T0-A1-0-3   00h:26m:14s  total:  send=808665   send-failed=1889135  recv=0        fail=0  timeout=808653  unknown=0
     *      <13>Dec 31 08:02:54 [2018-12-31, 08:02:54.683] testapp_stats.info(B: script finished]
     * here it returns 5 - 3 = 2 ( total number of lines for only Bat Vms stats)
     * @return
     */
    public int getNumberOfLinesInStats() {
        if (mStatsStartIndex == -1) {
            return -1;
        }
        return mStatsEndIndex - mStatsStartIndex;
    }

    /**
     * Starts Fio traffic using bat_fio_start.sh
     *
     */
    public void startFioTraffic() {
        mLogger.info(EcsAction.STARTING, "Fio Traffic", getHostname(), "");
        sendBatCommandAsync("fio_start.sh -b");
        mLogger.info(Verdict.STARTED, "Fio Traffic", getHostname(), "");
    }

    /**
     * Starts TestApp traffic using testapp_start.sh
     *
     */
    public void startTestAppTraffic() {
        mLogger.info(EcsAction.STARTING, "TestApp Traffic", getHostname(), "");
        sendBatCommandAsync("testapp_start.sh");
        mLogger.info(Verdict.STARTED, "TestApp Traffic", getHostname(), "");
    }

    /**
     * Stops Fio traffic using bat_fio_stop.sh
     *
     */
    public void stopFioTraffic() {
        mLogger.info(EcsAction.STOPPING, "Fio Traffic", getHostname(), "");
        sendBatCommandAsync("fio_stop.sh");
        mLogger.info(Verdict.STOPPED, "Fio Traffic", getHostname(), "");
    }

    /**
     * Stops TestApp traffic using testapp_stop.sh
     *
     */
    public void stopTestAppTraffic() {
        mLogger.info(EcsAction.STOPPING, "TestApp Traffic", getHostname(), "");
        sendBatCommandAsync("testapp_stop.sh");
        mLogger.info(Verdict.STOPPED, "TestApp Traffic", getHostname(), "");
    }
}

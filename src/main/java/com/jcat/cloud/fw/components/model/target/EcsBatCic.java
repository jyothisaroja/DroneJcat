package com.jcat.cloud.fw.components.model.target;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openstack4j.model.compute.Server;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.exceptions.EcsTrafficException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.compute.EcsBatVm;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.fwservices.traffic.model.FioResult;
import com.jcat.cloud.fw.fwservices.traffic.model.TestAppResult;
import com.jcat.cloud.fw.fwservices.traffic.plugins.BatTrafficPlugin;

/**
 * Cic subclass for BAT enabled CIC
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-08-01 initial version
 * @author zpralak 2017-11-24 Modified Methods sendBatCommandAsync, printScriptExecutionResultInDebugMode, prepareBat, startBatVms, startFioTraffic
 * @author zpralak 2017-12-06 Added getComputesWithBatVmsFromHwInv
 * @author zmousar 2018-12-21 Modified Code to support for CEE9
 *
 */
public class EcsBatCic extends EcsCic {


    private enum BAT_VM_NAMES {
        BAT_CTRL_VM1("BAT-T0-controller"), BAT_CTRL_VM2("BAT-T1-controller");

        private final String mName;

        BAT_VM_NAMES(String name) {
            mName = name;
        }

        public String toString() {
            return mName;
        }
    }

    private static String BAT_PATH = "/var/lib/glance/BATscripts";
    private static String LOG_FILE = "bat_log.txt";
    private static String OPENSTACK_PASSWORD = "admin";
    private static String DETACHED_EXECUTION = " 2>&1 &";
    private static String REDIRECT_TO_LOG = " > " + BAT_PATH + "/" + LOG_FILE;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsBatCic.class);
    private static String BAT_SCRIPTS_PATH = "/home/batman/scripts";
    private static String POWER_STATE_RUNNING = "1";
    public  int mNumberOfLinesInStats;
    private boolean mIsBatPrepared = false;

    private boolean mAreBatScriptsPresent = false;

    List<String> mBatControllerVmIds = new ArrayList<String>();
    List<EcsBatVm> mEcsBatVmList = new ArrayList<EcsBatVm>();

    EcsBatVm mEcsBatVm;

    @Inject
    NovaController mNovaController;

    @Inject
    public EcsBatCic(@Assisted("username") String userName, @Assisted("password") String password,
            @Assisted("ipAddress") String ipAddress, @Assisted("port") int port) {
        super(userName, password, ipAddress, port);
    }

    /**
     * sets password in session
     */
    private void exportPassword() {
        mSshSession.send("export OS_PASSWORD=" + OPENSTACK_PASSWORD);
    }

    private boolean isInWhiteList(String command) {
        String[] whiteList = { "bat_fio_stats", "bat_fio_stop" };
        for (String whiteItem : whiteList) {
            if (command.contains(whiteItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints script execution result in debug mode
     *
     * @return the complete execution result in a string
     * @throws IOException
     */
    private String printScriptExecutionResultInDebugMode(String command) throws IOException {
        String fileContent;
        try {
            new LoopHelper<Boolean>(
                    Timeout.BAT_SCRIPT_SINGLE,
                    mEcsComputeBladeList.getComputeBladeDestinationIds().size() - mEcsCicList.size(),
                    "Bat script doesn't seem to finish in time",
                    true,
                    () -> {
                        sync();
                        String content = tailFile(BAT_PATH, LOG_FILE);
                        if (content.contains("a BAT script is already running, wait for it to finish")) {
                            mLogger.warn("Previous BAT command was still running! This was not supposed to happen. Check what happened before.");
                            mSshSession.send(command);
                            return false;
                        }
                        if (content.toUpperCase().contains("ERROR") && !isInWhiteList(command)) {
                            if (command.equals("bat_vm_pairing.sh")
                                    && content.contains("error: no peer VM found for VM")) {
                                mLogger.warn(
                                        "note that it is normal for a VM to not have a peer if there is an odd number of VMs of that type for that tenant");
                            } else {
                                throw new EcsTrafficException(BatTrafficPlugin.class,
                                        "Error was found during executing script: " + command
                                                + "\n Last lines from console log:\n" + content);
                            }
                        }
                        if (content.contains("script finished")) {
                            return true;
                        }
                        return false;
                    }).setIterationDelay(10).run();
        } finally {
            fileContent = readFile(BAT_PATH, LOG_FILE);
            deleteFile(BAT_PATH, LOG_FILE);
        }
        return fileContent;
    }

    /**
     * Execute a bat script non-blocking
     *
     * @param cmd the script's name and parameter
     * @return the result of the execution
     * @throws IOException
     */
    private String sendBatCommandAsync(String cmd) {
        exportPassword();
        if (cmd.equals("bat_hw_inventory.sh")) {
            cmd = "nohup " + BAT_PATH + "/" + cmd + " -X " + REDIRECT_TO_LOG + DETACHED_EXECUTION;
        } else {
            cmd = "nohup " + BAT_PATH + "/" + cmd + REDIRECT_TO_LOG + DETACHED_EXECUTION;
        }
        // pls do not remove the following line since it's a fix that under certain condition the command would be
        // blocking (i cannot remember what condition it was)
        // cmd = "{ " + BAT_PATH + cmd + REDIRECT_TO_LOG + DETACHED_EXECUTION + " } && {}";
        mSshSession.send(cmd);
        mLogger.debug(cmd);
        try {
            return printScriptExecutionResultInDebugMode(cmd);
        } catch (IOException e) {
            mLogger.warn("Something went wrong while running bat script " + e.getMessage());
            return "";
        }

    }

    private void setBatControllerVmIds() {
        if(mBatControllerVmIds.isEmpty()) {
            mBatControllerVmIds.add(mNovaController.getVmIdByName(BAT_VM_NAMES.BAT_CTRL_VM1.toString()));
            mBatControllerVmIds.add(mNovaController.getVmIdByName(BAT_VM_NAMES.BAT_CTRL_VM2.toString()));
        }
    }

    public List<EcsBatVm> getBatControllerVms() {
        return mEcsBatVmList;
    }

    /**
     * Mandatory scripts are present in Bat Controller Vms after bat_controller_start.sh
     * Verify those scripts are injected, used for start, fetch, stop TestAppTraffic.
     */
    public void verifyBatScriptsAvailable() {
        if(!mAreBatScriptsPresent) {
            // Required scripts need to be exist in bat-controller-vm
            List<String> requiredFiles = Arrays.asList("testapp_start.sh", "testapp_stats.sh", "testapp_stop.sh", "fio_start.sh", "fio_stats.sh"
                 , "fio_stop.sh");
            setBatControllerVmIds();
            for(String batCtrlVmId: mBatControllerVmIds) {
                EcsBatVm ecsBatVm = (EcsBatVm) mNovaController.getEcsVm(batCtrlVmId);
                mLogger.info(EcsAction.FINDING, "injected files", "on ", ecsBatVm.getHostname());
                List<String> existedFiles = ecsBatVm.listFiles(BAT_SCRIPTS_PATH);
                if(!(existedFiles.containsAll((Collection<?>) requiredFiles))) {
                    throw new EcsOpenStackException("Required files are not injected in Bat controller VM: " + ecsBatVm.getHostName()
                            + ", so unable to work with TestApp Traffic");
                }
                ecsBatVm.changeDirectory(BAT_SCRIPTS_PATH);
                mLogger.info(Verdict.FOUND, "injected files", "on ", ecsBatVm.getHostname());
                mEcsBatVmList.add(ecsBatVm);
            }
            mAreBatScriptsPresent = true;
        }
    }

    /*public void attachVolume() {
        mLogger.info(EcsAction.ATTACHING, "Volume", "B VM", "on " + getHostname());
        sendBatCommandAsync("bat_vol_attach.sh");
        mLogger.info(Verdict.ATTACHED, "Volume", "B VM", "on " + getHostname());
    }*/

    /**
     * Gets the number of compute blades that are chosen to deploy BAT Vms by using hardware inventory file
     *
     * EX: root@cic-1:~# cat /var/lib/glance/BATscripts/runtime_inv/hw_inv.txt|wc -l
     *     5
     *
     * @return - returns number of compute blades with BAT Vms using hardware inventory
     */
    public int getComputesWithBatVmsFromHwInv() {
        int numberOfComputesWithBatVms = 0;
        if (isRuntimeInventoryPrepared()) {
            // Command to calculate number of compute blades with BAT Vms using hardware inventory file
            String cmd = "cat " + BAT_PATH + "/runtime_inv/hw_inv.txt|wc -l";
            String result = mSshSession.send(cmd);
            numberOfComputesWithBatVms = Integer.parseInt(result);
            return numberOfComputesWithBatVms;
        }
        return numberOfComputesWithBatVms;
    }

    /**
     * Fetch FIO traffic status
     * @param batControllerVmIndex - Index of Bat Controller VM
     *
     * @return
     */
    public List<FioResult> getFioTraffic(int batControllerVmIndex) {
        verifyBatScriptsAvailable();
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        List<FioResult> results = mEcsBatVm.getFioTraffic();
        return results;
    }

    /**
     * Fetch TestApp traffic status
     * @param batControllerVmIndex - Index of Bat Controller VM
     * @param resetTraffic - used for resetting TestApp Traffic
     *
     * @return
     */
    public List<TestAppResult> getTestAppTraffic(int batControllerVmIndex, boolean resetTraffic) {
        verifyBatScriptsAvailable();
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        List<TestAppResult> results = mEcsBatVm.getTestAppTraffic(resetTraffic);
        mNumberOfLinesInStats = mEcsBatVm.getNumberOfLinesInStats();
        return results;
    }

    /**
     * Get the total number of lines for only Bat Vms traffic stats in Bat_Controller_VM
     */
    public int getNumberOfLinesInStats() {
        return mNumberOfLinesInStats;
    }

    /**
     * Checking if BAT setup has been prepared before
     *
     * @return
     */
    public boolean isBatPrepared() {
        if (mIsBatPrepared) {
            mLogger.info(Verdict.VALIDATED, "Prepared", "BAT", "");
        }
        return mIsBatPrepared;
    }

    /**
     * Checking if runtime inventory has been prepared before
     * This method needs to be implemented
     *
     * @return
     */
    public boolean isRuntimeInventoryPrepared() {
        if (doesDirectoryExist(BAT_PATH + "/runtime_inv")) {
            mLogger.info(Verdict.VALIDATED, "Prepared", "RT-Inventory", "");
            return true;
        }
        return false;
    }

    /**
     * Launch bat controller Vms to start and test the TestAppTraffic
     */
    public void launchBatControllerVms() {
        mLogger.info(EcsAction.CREATING, EcsBatVm.class ,"Bat Controller Vms to start and test the TestAppTraffic");
        sendBatCommandAsync("bat_controller_start.sh");
        mLogger.info(Verdict.CREATED, EcsBatVm.class ,"Bat Controller Vms to start and test the TestAppTraffic");
        setBatControllerVmIds();
        for (String vmId : mBatControllerVmIds) {
            mNovaController.waitForServerStatus(vmId, Server.Status.ACTIVE);
            if (!(mNovaController.getVmStatus(vmId) == Server.Status.ACTIVE && mNovaController.getPowerState(vmId)
                    .equals(POWER_STATE_RUNNING))) {
                throw new EcsOpenStackException("BAT Controller VM is not active & running: "
                  + mNovaController.getVmName(vmId));
            }
        }
        verifyBatScriptsAvailable();
    }

    public void pairBatVms() {
        mLogger.info(EcsAction.CREATING, "VM Pairs", getHostname(), "");
        sendBatCommandAsync("bat_vm_pairing.sh");
        mLogger.info(Verdict.CREATED, "VM Pairs", getHostname(), "");
    }

    /**
     * Executes bat_prepare and bat_hw_inventory script to prepare bat for starting
     */
    public void prepareBat() {
        mLogger.info(EcsAction.STARTING, "BAT prepare", getHostname(), "");
        String cmdOutput = sendBatCommandAsync("bat_prepare.sh -f bat-config.txt");
        if (!cmdOutput.contains("script finished")) {
            throw new EcsTrafficException(BatTrafficPlugin.class, "BAT prepare was not successful");
        }
        setBatPrepared(true);
        mLogger.info(Verdict.FINISHED, "BAT prepare", getHostname(), "");
        mLogger.info(EcsAction.STARTING, "BAT rt-inv", getHostname(), "");
        sendBatCommandAsync("bat_hw_inventory.sh");
        if (!doesDirectoryExist(BAT_PATH + "/runtime_inv")) {
            throw new EcsTrafficException(BatTrafficPlugin.class, "BAT prepare was not successful");
        }
        mLogger.info(Verdict.FINISHED, "BAT runtime inv", getHostname(), "");
    }

    public void setBatPrepared(boolean batPreapred) {
        mIsBatPrepared = batPreapred;
    }

    /**
     * Boot BAT VMs using bat_vm_start.sh
     *
     */
    public void startBatVms() {
        mLogger.info(EcsAction.STARTING, "BAT VMs", getHostname(), "");
        sendBatCommandAsync("bat_vm_start.sh");
        mLogger.info(Verdict.STARTED, "BAT VMs", getHostname(), "");
    }

    /**
     * Starts Fio traffic using bat_fio_start.sh
     * @param batControllerVmIndex - Index of Bat Controller VM
     *
     */
    public void startFioTraffic(int batControllerVmIndex) {
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        mEcsBatVm.startFioTraffic();
    }

    /**
     * Starts TestApp traffic using bat_testapp_start.sh
     * @param batControllerVmIndex - Index of Bat Controller VM
     *
     */
    public void startTestAppTraffic(int batControllerVmIndex) {
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        mEcsBatVm.startTestAppTraffic();
    }

    /**
     * Stops Fio traffic using bat_fio_stop.sh
     * @param batControllerVmIndex - Index of Bat Controller VM
     *
     */
    public void stopFioTraffic(int batControllerVmIndex) {
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        mEcsBatVm.stopFioTraffic();
    }

    /**
     * Stops TestApp traffic using bat_testapp_stop.sh
     * @param batControllerVmIndex - Index of Bat Controller VM
     *
     */
    public void stopTestAppTraffic(int batControllerVmIndex) {
        mEcsBatVm = mEcsBatVmList.get(batControllerVmIndex);
        mEcsBatVm.stopTestAppTraffic();
    }

    /**
     * Verifies BAT VM connectivity using bat_vm_ssh.sh
     *
     */
    public boolean verifyBatVmConnectivity() {
        mLogger.info(EcsAction.VALIDATING, "connectivity", getHostname(), "BAT VMs");
        sendBatCommandAsync("bat_vm_ssh.sh");
        return true;
    }
}

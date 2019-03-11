package com.jcat.cloud.fw.components.system.cee.ecssession;

import java.util.regex.Pattern;

import com.ericsson.commonlibrary.remotecli.ExtendedCli;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsSessionException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.compute.EcsVm;
import com.jcat.cloud.fw.components.model.target.EcsUser;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;

/**
 * Describes a session to a VM.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2018-07-06 - Initial version
 * @author zmousar - 2018-08-21 - Added serialName to do virsh Console
 * @author zdagjyo - 2018-08-30 - Modified code to support login to vm created with opensuse image
 */
public class VmSessionVirsh extends EcsSession {

    private static final String SERIAL_NAME_CMD = "virsh dumpxml %s | grep pty -A 3 | grep \"alias name\" | awk  -F\\' '{print $2}'";
    private boolean mIsSessionEstablished;
    private boolean mWaitForAsyncCmdExecution;
    private String mVmPrompt;
    private String mHypervisorHostname;
    private String mHypervisorInstanceId;
    @Inject
    private EcsComputeBladeList mEcsComputeBladeList;
    @Inject
    private ComputeBladeSessionFactory mComputeBladeSessionFactory;
    @Inject
    private FuelResource mFuelResource;
    private final EcsLogger mLogger = EcsLogger.getLogger(this.getClass());

    @Inject
    public VmSessionVirsh(@Assisted("hypervisorHostname") String hypervisorHostname,
            @Assisted("hypervisorInstanceId") String hypervisorInstanceId, @Assisted("vmUserName") String vmUserName,
            @Assisted("vmPassword") String vmPassword, @Assisted("prompt") String prompt) {
        super(EcsConnectionException.EcsConnectionTarget.VM);
        mHypervisorHostname = hypervisorHostname;
        System.out.println("hypervisor hostname is " + mHypervisorHostname);
        mHypervisorInstanceId = hypervisorInstanceId;
        mCurrentUser = new EcsUser(vmUserName, vmPassword, false);
        mVmPrompt = prompt;
    }

    /**
     * Method used to create a new the {@link ComputeBladeSession} which will connect
     * to compute
     *
     * @return - {@link ComputeBladeSession}
     */
    private ComputeBladeSession connectToCompute() {
        System.out.println("fuel rsrc is hostname is " + mFuelResource);
        String computeIp = mEcsComputeBladeList.getComputeIpByName(mHypervisorHostname);
        ComputeBladeSession newSession = mComputeBladeSessionFactory.create(computeIp, mFuelResource.getIpPublic(),
                mFuelResource.getFuelPublicSshPort());
        newSession.connect();
        return newSession;
    }

    /**
     * Method that gets the {@link ExtendedCli} session towards the vm host
     * machine and connects to the VM while returning {@link ExtendedCli}
     * session towards VM
     *
     * @param hostCli
     *        - {@link ExtendedCli} session to the vm host
     * @param vmInstanceName
     *        - name (or ID/UUID) of the VM used to connect to it (all 3
     *        ways work)
     * @param currentUser
     * @return
     */
    private ExtendedCli connectToVm(ExtendedCli hostCli, String vmInstanceName, EcsUser currentUser) {
        mLogger.debug("Connecting using virsh console to VM with ID: " + vmInstanceName);
        String serialName = hostCli.send(String.format(SERIAL_NAME_CMD, vmInstanceName));
        hostCli.setExpectedRegexPrompt(mVmPrompt);
        // send virsh command
        String consoleCmd = "virsh console " + vmInstanceName + " --force";
        if (serialName.contains("serial")) {
            consoleCmd = "virsh console " + vmInstanceName + " --devname " + serialName + " --force";
        }
        sendAsyncCommand(hostCli, consoleCmd, "Connected", "Connected prompt");
        sendAsyncCommand(hostCli, "\n", "login:", "VM user prompt");
        // directly return the prompt if the session is already established
        if (mIsSessionEstablished) {
            return hostCli;
        }
        // send vm username
        sendAsyncCommand(hostCli, mCurrentUser.getUsername(), "Password:", "VM password prompt");
        // send vm password
        sendAsyncCommand(hostCli, mCurrentUser.getPassword(), mVmPrompt, "VM login prompt");
        // vm prompt found, ssh connection to vm established, send hostname
        mHostname = hostCli.send("hostname");
        mIsSessionEstablished = true;
        return hostCli;
    }

    /**
     * Checks if the specified regular expression pattern is found in the specified string.
     *
     * @param string
     * @param regexp
     * @return boolean - true if the regex is found in string, else false
     */
    private boolean find(final String string, final String regexp) {
        return Pattern.compile(regexp, Pattern.DOTALL | Pattern.MULTILINE).matcher(string).find();
    }

    /**
     * Executes the specified command on specified cli, throws exception if the command execution fails.
     *
     * @param hostCli - the cli on which the command is to be executed
     * @param command - the command to execute
     * @param output - a string that is expected to be in the output obtained from command execution
     * @param errorMessage - error message to be displayed in case the command fails
     */
    private void sendAsyncCommand(ExtendedCli hostCli, String command, String output, String errorMessage) {
        new LoopHelper<Boolean>(Timeout.VIRSH_CMD_EXECUTION,
                "Could not find " + errorMessage + " in the command output", Boolean.TRUE, () -> {
                    hostCli.sendAsync(command);
                    mWaitForAsyncCmdExecution = true;
                    waitForAsyncCommandExecution();
                    StringBuilder cmdResult = hostCli.readCurrent();
                    String actualResult = cmdResult.toString();
                    mLogger.debug("command sent: " + command + ", received output: " + actualResult);
                    if (command.equals("\n") && find(actualResult, mVmPrompt)) {
                        mIsSessionEstablished = true;
                        return true;
                    } else if (command.equals("\n")) {
                        mIsSessionEstablished = false;
                    }
                    if (actualResult.contains("Login timed out")) {
                        sendAsyncCommand(hostCli, mCurrentUser.getPassword(), mVmPrompt, "VM login prompt");
                        return true;
                    }
                    if (command.contains("virsh")
                            && actualResult.contains("Active console session exists for this domain")) {
                        mLogger.error("Active console session exists for this domain");
                        throw new EcsSessionException("Active console session exists for this VM");
                    }
                    return find(actualResult, output);
                }).setIterationDelay(10).run();
    }

    /**
    * Waits for 3 seconds (this method is invoked before reading
    * the output from asynchronous command execution as it takes
    * some time to read the output from host cli).
    */
    private void waitForAsyncCommandExecution() {
        new LoopHelper<Boolean>(Timeout.ASYNC_CMD_EXECUTION, "Failed to wait for 3 seconds", Boolean.TRUE, () -> {
            if (mWaitForAsyncCmdExecution) {
                mWaitForAsyncCmdExecution = false;
                return false;
            }
            return true;
        }).setIterationDelay(3).run();
    }

    @Override
    protected void connect() throws EcsConnectionException {
        mLogger.info("Attempting to establish Ssh session to VM via virsh, first connect to vm host: cmp/kvm");
        // connect to vm through virsh
        ExtendedCli connectToVm = null;
        EcsSession session = connectToCompute();
        // connect to host on which vm is booted
        String vmHostname = session.send("hostname");
        mLogger.info(Verdict.CONNECTED, "", "vm host", vmHostname + ", first step connected");
        mLogger.info("now ssh to VM via virsh console");
        mLogger.info(EcsAction.CONNECTING, EcsVm.class, mHypervisorInstanceId + ", via virsh");
        mLogger.info(EcsAction.WAITING, EcsVm.class, " to establish ssh connection to VM, timeout:"
                + Timeout.VIRSH_CMD_EXECUTION.getTimeoutInSeconds() + "seconds");
        try {
            connectToVm = connectToVm(session.getExtendedCli(), mHypervisorInstanceId, mCurrentUser);
        } catch (EcsSessionException e) {
            mLogger.warn("Failed to connect to VM " + mHypervisorInstanceId + ". Will try again...");
            mLogger.info(EcsAction.CONNECTING, "", "vm host",
                    " first step: connect to " + mHypervisorHostname + " before connecting to VM");
            connectToVm = connectToVm(session.getExtendedCli(), mHypervisorInstanceId, mCurrentUser);
        }
        mCli = connectToVm;
        mLogger.info(Verdict.WAITED, EcsVm.class, " established ssh connection to VM");
        mLogger.info(Verdict.CONNECTED, EcsVm.class,
                "id:" + mHypervisorInstanceId + ", hostname:" + mHostname + " via virsh");
    }

    /**
     * @return String - prompt for VM (regular, BAT and root)
     */
    public String getVmPrompt() {
        return mVmPrompt;
    }
}

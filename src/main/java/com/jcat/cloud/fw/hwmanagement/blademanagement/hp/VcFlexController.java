/**
 *
 */
package com.jcat.cloud.fw.hwmanagement.blademanagement.hp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.ericsson.commonlibrary.remotecli.Cli;
import com.ericsson.commonlibrary.remotecli.CliBuilder;
import com.ericsson.commonlibrary.remotecli.CliFactory;
import com.ericsson.commonlibrary.remotecli.exceptions.RemoteCliException;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.hwmanagement.blademanagement.EquipmentControllerException;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.PortAdminState;
import com.jcat.cloud.fw.infrastructure.resources.VcFlexResource;

import se.ericsson.jcat.fw.assertion.JcatAssertApi;

/**
 * Implements {@link IEquipmentController} with VcFlex Cli api calls for working towards the Hp hardware.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2014-05-19 Initial version.
 * @author ethssce 2014-06-13 changed return type of getBladeAdminState() and getArpResponder(...), change parameter of
 *         setPortState()
 * @author ehosmol 2014-10-12 clean up, removed unnecessary functions
 * @author zdagjyo - 2017-01-23 - Modify methods powerOff and powerOn to include EcsComputeBlade
 * @author zpralak - 2017-10-18 - Modify method powerReset method to include EcsComputeBlade And modified sendCommand method
 *
 */
public final class VcFlexController implements IEquipmentController {

    private static final String FAILED_TO_SEND = "Failed to send command:'%s'.";
    private static final int NUM_OF_VCFLEX_PER_SHELF = 2;
    private static final String PORT_CMD = "show enet-connection Profile_enc%s_Bay-%d:%d";
    private static final String PORT_CMD_WITH_SPEED_TYPE = "show enet-connection Profile_enc%s_Bay-%d:%d SpeedType=%s";
    private static final String POWER_OFF_CMD = "POWEROFF SERVER %d FORCE";
    private static final String POWER_ON_CMD = "POWERON SERVER %d";
    private static final String RESET_CMD = "RESET SERVER %d";
    private static final String SHOW_VLAN = "show vlan";
    private static final String CONTINUE_PROMPT = "you want to continue|CONTINUE";
    private static final String LOGIN_PROMPT = "(.*)>";
    // @TODO: Define this in the QualityOfService class or similar.
    private static final int TIMEOUT = 30000;

    private final EcsLogger mLogger = EcsLogger.getLogger(VcFlexController.class);
    private Cli mCli;
    // The selected VcFlex resource that this controller connects to
    private final VcFlexResource mVcFlexConfiguration;

    @Inject
    public VcFlexController(VcFlexResource vcFlexResources) {
        mVcFlexConfiguration = vcFlexResources;
    }

    // TODO: test
    private int calculatePort(int bridgeInstance) {
        // Intended behaviour or missing parentesis?
        return (bridgeInstance - 1) % NUM_OF_VCFLEX_PER_SHELF + 1;
    }

    // TODO: test
    private int calculateShelf(int bridgeInstance) {
        return (bridgeInstance - 1) / NUM_OF_VCFLEX_PER_SHELF;
    }

    /**
     * This is a helper methods that loops the matched prompt of the cli instance and resolves any continue?
     * prompt.
     *
     * @return response String or null
     */
    private String checkForPossiblePasswordOrContinuePrompt() {
        // This can be asked many times
        String response = null;
        while (mCli.getMatchedPrompt().matches(CONTINUE_PROMPT)) {
            if (mCli.getMatchedPrompt().matches(CONTINUE_PROMPT)) {
                mLogger.info(EcsAction.SENDING, "yes", "host",
                        "The session was prompted for a continue? question and answered 'yes'");
                if (mCli.getMatchedPrompt().contains("y/n")) {
                    response = mCli.send("y");
                } else {
                    response = mCli.send("yes");
                }
            }
        }
        return response;
    }

    /**
     * The function returns the slot of a server in the enclosure given the 4th octet
     * of the ip
     *
     * @param ip4thOctet
     * @return 1 if input is 1, otherwise according to the formula (input / 2 - 1)
     */
    private int ip4thOctetToCotsSlot(int ip4thOctet) {
        if (ip4thOctet == 1) {
            return ip4thOctet;
        }

        return ip4thOctet / 2 - 1;
    }

    private boolean sendCommand(String command) {
        String host = mVcFlexConfiguration.getIp();
        String output = null;
        boolean result;
        try {
            mLogger.info(EcsAction.SENDING, "", host, command);
            mCli.setExpectedRegexPrompt(CONTINUE_PROMPT + "|" + LOGIN_PROMPT);
            output = mCli.send(command);
            String tempResponse = checkForPossiblePasswordOrContinuePrompt();
            if (tempResponse != null) {
                output = tempResponse;
            }
            mLogger.info(Verdict.RECEIVED, "", host, output);
            result = true;
        } catch (RemoteCliException ex) {
            mLogger.error(String.format(FAILED_TO_SEND, command));
            result = false;
        }
        return result;
    }

    /**
     * Closes the established session to OA Shelf.
     * This API needs to be invoked after any other API in this class is invoked.
     */
    @Override
    public void closeConnection() {
        JcatAssertApi.assertNotNull("The function openConnection() must be executed before closeConnection()", mCli);
        mCli.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationData getConfiguration() {
        return mVcFlexConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPortEnabled(int bridgeInstance, int portInstance) {
        int shelf = calculateShelf(bridgeInstance);
        int port = calculatePort(bridgeInstance);

        boolean result = false;

        String cmd = String.format(PORT_CMD, shelf, ip4thOctetToCotsSlot(portInstance), port);

        try {
            String cmdOutput = mCli.send(cmd);
            Pattern pattern = Pattern.compile("Min Allocated Speed.*10Gb");
            Matcher matcher = pattern.matcher(cmdOutput);
            // Fatal error if we can't make this or up to the user of this API?
            result = matcher.find();
        } catch (RemoteCliException ex) {
            mLogger.error(String.format(FAILED_TO_SEND, cmd));
        }
        return result;
    }

    /**
     * Establishes a session to OA shelf.
     * This API needs to be invoked before any other API in this class is invoked.
     *
     * @throws EquipmentControllerException
     */
    @Override
    public void openConnection() throws EquipmentControllerException {
        String host = mVcFlexConfiguration.getIp();
        String username = mVcFlexConfiguration.getUserName();
        String password = mVcFlexConfiguration.getUserPassword();
        CliBuilder specialPromptBuilder = CliFactory.newSshBuilder(host, username, password);

        specialPromptBuilder.setPort(mVcFlexConfiguration.getPortCli());

        specialPromptBuilder.setExpectedRegexPrompt("(.*)>");
        mCli = specialPromptBuilder.build();
        mCli.connect();

        mCli.setSendTimeoutMillis(TIMEOUT);
    }

    /**
     * Powers off the specified compute blade.
     * Connection will have to be maintained by test case
     * (using openConnection()) but not a part of this FW API.
     *
     * @param blade - the compute blade to power off
     * @return boolean
     * @throws EquipmentControllerException
     */
    @Override
    public boolean powerOff(EcsComputeBlade blade) throws EquipmentControllerException {
        mLogger.info(EcsAction.POWERING_OFF, EcsComputeBlade.class, blade.getHostname());
        boolean poweredOff = false;
        int bayNumber = blade.getBayNumber();
        poweredOff = sendCommand(String.format(POWER_OFF_CMD, bayNumber));
        if (poweredOff) {
            mLogger.info(Verdict.POWERED_OFF, EcsComputeBlade.class, blade.getHostname());
        } else {
            throw new EcsOpenStackException("Failed to power off the compute blade " + blade.getHostname());
        }
        return poweredOff;
    }

    /**
     * Powers on the specified compute blade.
     * Connection will have to be maintained by test case
     * (using openConnection()) but not a part of this FW API.
     *
     * @param blade - the compute blade to power on
     * @return boolean
     * @throws EquipmentControllerException
     */
    @Override
    public boolean powerOn(EcsComputeBlade blade) throws EquipmentControllerException {
        mLogger.info(EcsAction.POWERING_ON, EcsComputeBlade.class, blade.getHostname());
        boolean poweredOn = false;
        int bayNumber = blade.getBayNumber();
        poweredOn = sendCommand(String.format(POWER_ON_CMD, bayNumber));
        if (poweredOn) {
            mLogger.info(Verdict.POWERED_ON, EcsComputeBlade.class, blade.getHostname());
        } else {
            throw new EcsOpenStackException("Failed to power on the compute blade " + blade.getHostname());
        }
        return poweredOn;
    }

    /**
     * power reset the specified compute blade.
     *
     * @param blade - the compute blade to power reset
     * @return boolean
     * @throws EquipmentControllerException
     */
    @Override
    public boolean powerReset(EcsComputeBlade blade) throws EquipmentControllerException {
        mLogger.info(EcsAction.RESETING, EcsComputeBlade.class, blade.getHostname());
        int bayNumber = blade.getBayNumber();
        openConnection();
        boolean powerReset = sendCommand(String.format(RESET_CMD, bayNumber));
        closeConnection();
        if (!powerReset) {
            throw new EcsOpenStackException("Failed to reset the compute blade " + blade.getHostname());
        }
        mLogger.info(Verdict.RESET, EcsComputeBlade.class, blade.getHostname());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPortState(int bridgeInstance, int portInstance, PortAdminState portState) {
        int shelf = calculateShelf(bridgeInstance);
        int port = calculatePort(bridgeInstance);

        String cmd = String.format(PORT_CMD_WITH_SPEED_TYPE, shelf, ip4thOctetToCotsSlot(portInstance), port,
                portState);

        try {
            mCli.send(cmd);
        } catch (RemoteCliException ex) {
            mLogger.error(String.format(FAILED_TO_SEND, cmd));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showDefaultVlan(int bridgeInstance, int portInstance) {

        int defaultVlan = -1;
        String result = "";

        try {
            result = mCli.send(SHOW_VLAN);
            String pattern = "(.*)Default VLAN ID \\(untagged\\) = (.*)\\.(.*)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(result);
            if (m.find()) {
                defaultVlan = Integer.parseInt(m.group(2));
            }
        } catch (RemoteCliException ex) {
            mLogger.error(String.format(FAILED_TO_SEND, SHOW_VLAN));
        } catch (NumberFormatException ex) {
            mLogger.error("Command has changed syntax so regexp fails.");
        }
        return defaultVlan;
    }
}

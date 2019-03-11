package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ericsson.commonlibrary.remotecli.Cli;
import com.ericsson.commonlibrary.remotecli.CliBuilder;
import com.ericsson.commonlibrary.remotecli.CliFactory;
import com.google.inject.Inject;
import com.jcat.cloud.fw.infrastructure.resources.SerialConsolesResource;

// TODO: this class should be generalized, at least some of the methods
/**
 * Implementation of {@link IConsoleConnector} using telnet console
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eladand 2013-11-04 Initial version.
 *
 */
public class ConsoleConnectorLib implements IConsoleConnector {

    private final SerialConsolesResource mConsoleConfiguration;

    private static final String DC_USERNAME = "dcuser";
    private static final String DC_PASSWORD = "dcuser";
    private static final String DC_PROMPT_FOR_PASSWORD = "Digi CM";
    private static final String DC_REGEX_PROMPT = "for port menu";

    private static final String DC_REGEX_PROMPT_AFTER_ENTER1 = "Wind River Linux [.0-9]+ ";
    private static final String DC_REGEX_PROMPT_AFTER_ENTER2 = " console|@";
    private static final String PATTERN_IF_LOGIN_NEEDED = "console";
    private static final String PROMPT_FOR_PASSWORD = "Password: ";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    private static final String DISCONNECT_REGEX_PROMPT = ".*console.*";
    private static final String EXIT = "exit";

    private final Logger mLogger = Logger.getLogger(ConsoleConnectorLib.class);

    private Cli mSpecialPromptTelnet;

    private String mIp;
    private int mPort;

    /**
     * Main constructor
     *
     * @param dmxConfiguration
     */
    @Inject
    public ConsoleConnectorLib(SerialConsolesResource consoleConfiguration) {
        mConsoleConfiguration = consoleConfiguration;
    }

    @Override
    public String send(String command) {
        return mSpecialPromptTelnet.send(command);
    }

    /**
     * extract mIp and mPort from shelf and slot (target)
     *
     * @param shelf, slot
     */
    private void getConsoleParams(int shelf, int slot) {
        List<SerialConsolesResource.SerialConsole> consoleResources = mConsoleConfiguration.get();
        String target = shelf + "-" + slot;
        boolean found = false;
        for (SerialConsolesResource.SerialConsole i : consoleResources) {
            if (i.getTarget().equals(target)) {
                found = true;
                mIp = i.getIp();
                mPort = i.getPort();
                break;
            }
        }
        if (!found) {
            mLogger.error("Serial console resource is not found for shelf " + shelf + ", " + "slot " + slot
                    + ", maybe the resource has some wrong format or is missing");
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    protected Cli createTelnetCli(String ip, String username, String password, int port) {
        CliBuilder specialPromptBuilder = CliFactory.newTelnetBuilder(ip, username, password).setPort(mPort);
        return specialPromptBuilder.build();
    }

    /**
     * login into the serial console server
     *
     * @param shelf, slot
     */
    private Cli dcConnect(int shelf, int slot) {
        getConsoleParams(shelf, slot);
        mLogger.info("Will open a telnet connection on: " + mIp + ":" + mPort + " using username: " + DC_USERNAME);
        // TODO remote-cli-1.6.0.jar can not handle this first version:
        boolean tryNew = false;
        if (tryNew) {
            Cli specialPromptTelnet = createTelnetCli(mIp, DC_USERNAME, DC_PASSWORD, mPort);
            specialPromptTelnet.setExpectedRegexPrompt(DC_REGEX_PROMPT);
            specialPromptTelnet.connect();
            return specialPromptTelnet;
        } else {
            Cli specialPromptTelnet = createTelnetCli(mIp, DC_USERNAME, DC_PASSWORD, mPort);
            specialPromptTelnet.connect();
            String hostResponse = specialPromptTelnet.getMatchedPrompt();
            mLogger.info("dc login prompt:" + hostResponse);
            specialPromptTelnet.setExpectedRegexPrompt(DC_PROMPT_FOR_PASSWORD);
            specialPromptTelnet.send(DC_USERNAME);
            hostResponse = specialPromptTelnet.getMatchedPrompt();
            mLogger.info("dc password prompt:" + hostResponse);
            specialPromptTelnet.setExpectedRegexPrompt(DC_REGEX_PROMPT);
            specialPromptTelnet.send(DC_PASSWORD);
            return specialPromptTelnet;
        }
    }

    @Override
    public void connect(int shelf, int slot) {

        mSpecialPromptTelnet = dcConnect(shelf, slot);
        String systemName = "(p1-sr" + shelf + "-sl" + slot + "|localhost)";
        mSpecialPromptTelnet.setExpectedRegexPrompt(DC_REGEX_PROMPT_AFTER_ENTER1 + systemName
                + DC_REGEX_PROMPT_AFTER_ENTER2 + systemName);
        // just sending an enter to learn if we are already reached the shell or login is needed:
        mSpecialPromptTelnet.send("");
        String hostResponse = mSpecialPromptTelnet.getMatchedPrompt();
        Pattern pattern = Pattern.compile(PATTERN_IF_LOGIN_NEEDED);
        Matcher matcher = pattern.matcher(hostResponse);
        if (matcher.find()) {
            mSpecialPromptTelnet.setExpectedRegexPrompt(PROMPT_FOR_PASSWORD);
            mSpecialPromptTelnet.send(USERNAME);
            mSpecialPromptTelnet.setExpectedRegexPrompt(USERNAME + "@" + systemName);
            mSpecialPromptTelnet.send(PASSWORD);
        } else {
            mSpecialPromptTelnet.setExpectedRegexPrompt(USERNAME + "@" + systemName);
        }

    }

    @Override
    public void disconnect() {
        mSpecialPromptTelnet.setExpectedRegexPrompt(DISCONNECT_REGEX_PROMPT);
        mSpecialPromptTelnet.send(EXIT);
        mSpecialPromptTelnet.disconnect();
    }
}

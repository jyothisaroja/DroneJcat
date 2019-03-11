package com.jcat.cloud.fw.components.system.cee.services.checker;

import org.apache.log4j.Logger;

import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.services.checker.EcsChecker;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;

/**
 * This class provides extreme periodic checker related
 * utilities.
 *
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author efikayd - 2015-03-31 - Initial version
 * @author efikayd - 2015-03-31 - Implemented isEnabled method
 * @author efikayd - 2015-04-01 - Implemented setEnable, setDisable and parseExitCode methods.
 * @author efikayd - 2015-03-31 - Implemented isEnabled method
 * @author efikayd - 2015-04-14 - Implemented restart method
 * @author ehosmol 2015-05-18 adopt to {@link EcsCic}
 */
public class EcsExtremePeriodicChecker extends EcsComponent implements EcsChecker {

    public static final int NEUTRON_CONF_FILE_NOT_EXIST = -1;
    public static final int UNABLE_TO_RESTART_NEUTRON_SERVER = -2;

    private final EcsSession mSshSession;
    private final EcsCic mEcsCic;
    private final Logger mLogger = Logger.getLogger(EcsExtremePeriodicChecker.class);

    private static final String CMD_TEST_IF_ENABLED = "awk '/^[[:space:]]*start_periodic_checker[^[:alnum:]]+[Ff]alse/ {print \"DISABLED\"}' /etc/neutron/neutron.conf";
    private static final String CMD_DISABLE_REPLACEMENT = "sed -i '/start_periodic_checker/d;/\\[extreme\\]/a start_periodic_checker = False' /etc/neutron/neutron.conf";
    private static final String CMD_ENABLE_REPLACEMENT = "sed -i '/start_periodic_checker/d;/\\[extreme\\]/a start_periodic_checker = True' /etc/neutron/neutron.conf";
    private static final String CMD_NEUTRON_SERVER_RESTART = "crm resource restart neutron-server";
    private static final String CMD_TEST_IF_NEUTRON_CONF_FILE_EXISTS = "[ -e /etc/neutron/neutron.conf ] || echo \"FILE_NOT_EXIST\"";
    private static final String DISABLED = "DISABLED";
    private static final String FILE_NOT_EXIST = "FILE_NOT_EXIST";
    private static final String PREFIX_FOR_EXIT_CODE = "EXIT CODE";
    private static final int INDEX_FOR_EXIT_CODE = 1;
    private static final String COLON_SEPERATOR = ":";
    private static final String SUCCESS_EXIT_CODE = "0";

    public EcsExtremePeriodicChecker(EcsSession sshSession, EcsCic ecsCic) {
        mSshSession = sshSession;
        mEcsCic = ecsCic;
    }

    private String parseExitCode(String executionOutput) {
        String[] lines = executionOutput.split("\n");
        String exitCode = "";

        for (String line : lines) {
            if (line.trim().startsWith(PREFIX_FOR_EXIT_CODE)) {
                String[] lineItems = line.split(COLON_SEPERATOR);
                exitCode = lineItems[INDEX_FOR_EXIT_CODE];
                break;
            }
        }

        return exitCode.trim();
    }

    @Override
    public boolean isEnabled() {
        String output = mSshSession.send(CMD_TEST_IF_ENABLED);
        mLogger.info("ExtremeChecker status: " + output);
        return !output.trim().equals(DISABLED);
    }

    @Override
    public int restart() {
        String cmdString = "awk 'BEGIN {print \"EXIT CODE:\"  system(\"" + CMD_NEUTRON_SERVER_RESTART + "\")}'";
        String output = mSshSession.send(cmdString);
        String exitCode = parseExitCode(output);
        if (exitCode.equals(SUCCESS_EXIT_CODE)) {
            mLogger.info("Neutron server has been restarted! Output: " + output);
        } else {
            mLogger.info("Neutron server could NOT be re-started! Output: " + output);
            return UNABLE_TO_RESTART_NEUTRON_SERVER;
        }

        return EXIT_SUCCESS;
    }

    @Override
    public int setDisable() {
        String output = mSshSession.send(CMD_TEST_IF_NEUTRON_CONF_FILE_EXISTS);
        mLogger.info("neutron.conf file existence test command output: " + output);
        if (output.trim().equals(FILE_NOT_EXIST)) {
            mLogger.info("/etc/neutron/neutron.conf file does not exit!");
            return NEUTRON_CONF_FILE_NOT_EXIST;
        }

        mLogger.info("Executing the command: " + CMD_DISABLE_REPLACEMENT);
        output = mSshSession.send(CMD_DISABLE_REPLACEMENT);
        mLogger.info("Output: " + output);

        return EXIT_SUCCESS;
    }

    @Override
    public int setEnable() {
        String output = mSshSession.send(CMD_TEST_IF_NEUTRON_CONF_FILE_EXISTS);
        if (output.trim().equals(FILE_NOT_EXIST)) {
            mLogger.info("/etc/neutron/neutron.conf file does not exit!");
            return NEUTRON_CONF_FILE_NOT_EXIST;
        }

        mLogger.info("Executing the command: " + CMD_ENABLE_REPLACEMENT);
        output = mSshSession.send(CMD_ENABLE_REPLACEMENT);
        mLogger.info("Output: " + output);

        return EXIT_SUCCESS;
    }

}

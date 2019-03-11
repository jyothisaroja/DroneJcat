package com.jcat.cloud.fw.components.model.target.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.commonlibrary.remotecli.Cli;
import com.ericsson.commonlibrary.remotecli.CliBuilder;
import com.ericsson.commonlibrary.remotecli.CliFactory;
import com.ericsson.commonlibrary.remotecli.ExtendedCli;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionTarget;
import com.jcat.cloud.fw.common.exceptions.EcsSessionException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsUser;

/**
 * Describes an Ssh session.
 *
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat - 2014-12-01 - Initial version
 * @author epergat - 2015-03-14 - Extended with connection monitoring.
 * @author efikayd - 2015-03-19 - Added the static member
 *         UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS
 * @author ezhgyin 2015-03-24 adapt to new LoopHelper logic
 * @author eelimei 2015-05-04 Added more specific solution for prompt mathcing and automatic handling of pwd/continue
 *         prompts.
 *         Also added an automatic verification of the current prompt after send to warn if the prompt was accidently
 *         matched before the end of the command.
 * @author eelimei 2015-05-04 isConnected now checkes the mHostName, instead of the current prompt. Bugfix
 * @author ehosmol 2015-05-06 Adopt {@link #checkForPossiblePasswordOrContinuePrompt(Cli)} to handle password prompt
 *         automatically
 * @author eedann 2015-05-29 added VM_PROMPT_ROOT
 * @author ehosmol 2015-06-10 Modify run method
 * @author eqinann 2015-09-09 Added support for ExtendedCli
 * @author zdagjyo 2017-09-15 Added enum ErrorMessage and methods getDefaultPrompt, getUniversalSendTimeout, removed methods getFuelPrompt
 *         and getLxcPrompt.
 * @author zmousar 2018-04-02 Modified method send
 */

public abstract class EcsSession {

    public enum ErrorMessage {
        SESSION_ERROR("Was not able to send the command, session is not connected!");
        private final String mMessage;

        ErrorMessage(String message) {
            mMessage = message;
        }

        public String errorMessage() {
            return mMessage;
        }
    }

    private static final int SSH_TERMINAL_HIGHT_CHARACTERS = 24;

    private static final int SSH_TERMINAL_HIGHT_PIXELS = 480;

    private static final int SSH_TERMINAL_WIDTH_CHARACTERS = 1300;

    private static final int SSH_TERMINAL_WIDTH_PIXELS = 640;

    protected static final String LXC_PROMPT = ".*@dc.*:.*[$].*";

    protected static final String CIC_COMPUTE_PROMPT = ".*@[cic|compute|scaleio].*:.*[#\\$].*";

    protected static final String FUEL_PROMPT = "\\[.*@fuel.*\\]#.*";

    private static final String CONTINUE_PROMPT = "you want to continue|CONTINUE|Press 'y'";

    private static final String LOCAL_PROMPT = "\\[.*\\].->.$";

    protected static final String PASSWORD_PROMPT = ".*[Pp]assword[:|.*:]|.*password for.*: $";

    // Send session timeout value in milliseconds to be used by subclasses
    protected static final int UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS = 60000;

    public static final String REGEX_BASIC_PROMPTS = PASSWORD_PROMPT + "|" + CONTINUE_PROMPT + "|" + LOCAL_PROMPT;

    private static final String PERSONAL_USER_PROMPT = "\n\\$.$";

    private String mNextPossiblePassword = null;

    // The Cli instance used.
    protected Cli mCli = null;

    // ECS Target of this session
    protected EcsConnectionTarget mConnectionTarget;

    protected EcsUser mCurrentUser;

    // current hostname
    protected String mHostname = null;

    // Default logger
    protected EcsLogger mLogger = EcsLogger.getLogger(this.getClass());

    private String mReturnedPrompt = "";

    public EcsSession(EcsConnectionTarget connectionTarget) {
        mConnectionTarget = connectionTarget;
    }

    /**
     * This is a helper methods that loops the matched prompt of the cli instance and resolves any password or continue?
     * prompt.
     * If a password prompt is found, the field nextPossiblePassword will be used primarily, otherwise looks for the
     * password stored
     * in the {@link EcsSession} as {@link EcsUser}.
     *
     * @param cli
     * @return response String or null
     */
    protected String checkForPossiblePasswordOrContinuePrompt() {
        // This can be asked many times
        String response = null;
        while (mCli.getMatchedPrompt().matches(PASSWORD_PROMPT) || mCli.getMatchedPrompt().matches(CONTINUE_PROMPT)) {
            if (mCli.getMatchedPrompt().matches(PASSWORD_PROMPT)) {
                mLogger.info("The session was prompted for a password");
                if (mNextPossiblePassword == null) {
                    if (mCurrentUser.getPassword() == null) {
                        throw new EcsSessionException(
                                "The session is asking for either current user ("
                                        + mCurrentUser.getUsername()
                                        + ")'s password or a specific password but none can be found! Please setNextpossiblePassword() or use a legitamte user.");
                    }
                    mLogger.info(EcsAction.SENDING, mCurrentUser.getPassword(), mHostname, "password");
                    response = mCli.send(mCurrentUser.getPassword());
                } else {
                    mLogger.info(EcsAction.SENDING, mNextPossiblePassword, mHostname, "password");
                    response = mCli.send(mNextPossiblePassword);
                }
            } else {
                mLogger.info(EcsAction.SENDING, "yes", mHostname,
                        "The session was prompted for a continue? question and answered 'yes'");
                String matchedPrompt = mCli.getMatchedPrompt();
                if (matchedPrompt.contains("y/n") || matchedPrompt.contains("Press 'y'")) {
                    response = mCli.send("y");
                } else {
                    response = mCli.send("yes");
                }
            }
        }
        return response;
    }

    protected abstract void connect() throws EcsConnectionException;

    /**
     * Build a new cli connection with non-password authentication
     * Sets expected regex prompt to jcat's all expected prompts.
     * Sets send timeout to jcat's universal send timeout for all sessions.
     *
     * @param ipAddress
     * @param userName
     * @param port
     * @return
     */
    protected void connect(String ipAddress, String userName, int port, final String expectedRegexPrompt) {
        connect(ipAddress, userName, null, port, expectedRegexPrompt);
    }

    /**
     * Build a new cli connection with default SSH port and non-password authentication
     * Sets expected regex prompt to jcat's all expected prompts.
     * Sets send timeout to jcat's universal send timeout for all sessions.
     *
     * @param ipAddress
     * @param userName
     * @param port
     * @return
     */
    protected void connect(String ipAddress, String userName, final String expectedRegexPrompt) {
        connect(ipAddress, userName, null, 22, expectedRegexPrompt);
    }

    /**
     * Build a new cli connection with the input parameters.
     * Sets expected regex prompt to jcat's all expected prompts.
     * Sets send timeout to jcat's universal send timeout for all sessions.
     * As no port is specified, port 22 would be used by default while connecting to target.
     *
     * @param ipAddress
     * @param userName
     * @param password
     * @return
     */
    protected void connect(String ipAddress, String userName, String password, final String expectedRegexPrompt) {
        connect(ipAddress, userName, password, 22, expectedRegexPrompt);
    }

    /**
     * Build a new cli connection with the input parameters.
     * Sets expected regex prompt to jcat's all expected prompts.
     * Sets send timeout to jcat's universal send timeout for all sessions.
     *
     * @param ipAddress
     * @param userName
     * @param password
     * @param port
     * @return
     */
    protected void connect(String ipAddress, String userName, String password, int port,
            final String expectedRegexPrompt) {
        CliBuilder builder = CliFactory.newSshBuilder();
        builder.setHost(ipAddress)
                .setSshPtySize(SSH_TERMINAL_WIDTH_CHARACTERS, SSH_TERMINAL_HIGHT_CHARACTERS, SSH_TERMINAL_WIDTH_PIXELS,
                        SSH_TERMINAL_HIGHT_PIXELS).setUsername(userName).setPort(port);
        if (null != password) {
            builder.setPassword(password);
        }
        mCli = builder.build();
        mCli.setExpectedRegexPrompt(expectedRegexPrompt);
        mCli.setSendTimeoutMillis(UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS);
        mCli.connect();
        mHostname = mCli.send("hostname");
        mReturnedPrompt = mCli.send("");
    }

    /**
     * Disconnects this session.
     */
    public void disconnect() {
        if (mCli != null) {
            mCli.disconnect();
        }
    }

    public String getCurrentPrompt() {
        return mCli.getExpectedRegexPrompt();
    }

    /**
     * Returns current session user
     *
     * @return the mCurrentUser
     */
    public EcsUser getCurrentUser() {
        return mCurrentUser;
    }

    public ExtendedCli getExtendedCli() {
        return (ExtendedCli) mCli;
    }

    public String getHostname() {
        if (mHostname == null) {
            mHostname = send("hostname");
        }
        return mHostname;
    }

    public String getPersonalUserPrompt() {
        return PERSONAL_USER_PROMPT;
    }

    public String getReturnedPrompt() {
        return mReturnedPrompt;
    }

    /**
     * Returns the value of constant UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS.
     *
     * @return int
     */
    public int getUniversalSendTimeout() {
        return UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS;
    }

    /**
     * ONLY for backwards compatability, will be removed in near future.
     *
     * @return
     */
    @Deprecated
    public Cli getUnsupervisedConnection() {
        connect();
        return mCli;
    }

    /**
     * Checks if the connection is connected by sending hostname command. No reconnection will take place.
     *
     * @return
     */
    public boolean isConnected() {
        if (mCli == null) {
            mLogger.debug("Session to : " + mConnectionTarget.toString() + " needs to be initialized");
            return false;
        }
        try {
            String response = mCli.send("hostname");
            if (!response.contains(mHostname)) {
                mLogger.debug("Could not verify that the connection to : " + mConnectionTarget.toString()
                        + "was up because the hostname: " + response + " did not match the hostname of the session: "
                        + mHostname);
                return false;
            } else {
                return true;
            }
        } catch (RuntimeException ex) {
            mLogger.debug("Could not verify that the session to : " + mConnectionTarget.toString()
                    + " was up because an exception was caught while sending command hostname");
            return false;
        }
    }

    /**
     * The method will check the connection and IF NOT connected, try to re-connect.
     *
     * @return boolean if the connection is up before returning.
     */
    public boolean reconnect() {
        if (!isConnected()) {
            try {
                connect();
            } catch (RuntimeException ex) {
                mLogger.warn("Unable to connect to *" + mHostname);
                return false;
            }
            return true;
        } else {
            return true;
        }

    }

    /**
     * Sends a command to the destination that is associated with this
     * SSH session. If the connection is detected not connected, the method will try to reconnect before sending the
     * command.
     *
     * @param cmd - command to be executed.
     * @return
     */
    public String send(String cmd) {
        String response = null;
        if (!reconnect()) {
            throw new EcsSessionException(ErrorMessage.SESSION_ERROR.errorMessage());
        }
        try {
            mLogger.info(EcsAction.SENDING, "", mHostname, cmd);
            response = mCli.send(cmd);
            String tempResponse = checkForPossiblePasswordOrContinuePrompt();
            if (tempResponse != null) {
                response = tempResponse;
            }
        } catch (RuntimeException ex) {
            mLogger.error("Exception was caught while sending command " + cmd);
            throw ex;
        }
        // A little hack to store the returned prompt :)
        if (cmd.equals("hostname")) {
            mReturnedPrompt = mCli.send("");
        }
        // handle to remove the response if it contains only the prompt line
        String matchedPrompt = mCli.getMatchedPrompt();
        String escCharacters = "[](){}*+?^$|";
        for (int i = 0; i < escCharacters.length(); i++) {
            if (matchedPrompt.contains("" + escCharacters.charAt(i))) {
                matchedPrompt = matchedPrompt.replace("" + escCharacters.charAt(i), "\\" + escCharacters.charAt(i));
            }
        }
        Matcher matcher = Pattern.compile("^" + matchedPrompt + "$").matcher(response);
        if (matcher.find()) {
            response = "";
        }
        mLogger.info(Verdict.RECEIVED, "", mHostname, response);
        return response;
    }

    /**
     * Set the current session user
     *
     * @param {@link EcsUser} current User to set
     */
    public void setCurrentUser(EcsUser currentUser) {
        mCurrentUser = currentUser;
    }

    /**
     * Manually set the hostname of the session
     * Hostname will automatically be set while connecting
     *
     * @param hostname
     */
    public void setHostname(String hostname) {
        mHostname = hostname;
    }

    /**
     * Manually set the timeout value to execute a command
     *
     * @param timeout - timeout value in milliseconds
     */
    public void setSendTimeoutMillis(int timeout) {
        if (mCli == null) {
            connect();
        }
        mCli.setSendTimeoutMillis(timeout);
    }

    /**
     *
     * If this session is prompted for a password while sending a command. This password will be used.
     * Uses of this method more than once will overwrite the previous password.
     *
     * @param newPasswordForThisSession
     */
    public void setNextPossiblePassword(String newPasswordForThisSession) {
        mNextPossiblePassword = newPasswordForThisSession;
    }
}

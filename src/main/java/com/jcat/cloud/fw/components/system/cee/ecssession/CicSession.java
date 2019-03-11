package com.jcat.cloud.fw.components.system.cee.ecssession;

import java.io.*;

import com.ericsson.commonlibrary.remotecli.exceptions.AuthenticationException;
import com.ericsson.commonlibrary.remotecli.exceptions.ConnectionToServerException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionTarget;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException.EcsConnectionType;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsUser;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import org.testng.Assert;

/**
 * Describes an session to a CIC.
 *
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat - 2014-12-01 - Initial version
 * @author epergat 2015-03-18 refactored to extend EcsSession
 * @author efikayd - 2015-03-19 - Added the usage of
 *         UNIVERSAL_SEND_SESSION_TIMEOUT_MILLIS.
 * @author eelimei 2015-05-04 Restructure and adoption to automatic functionality in EcsSession for prompts.
 * @author eqinann 2015-05-06 Added automatic upload key function
 * @author ehosmol 2015-05-06 Adopt to {@link EcsUser}
 */
public class CicSession extends EcsSession {

    private final EcsUser mInitialUserName;
    private final String mIpAddress;
    private final int mPort;

    /**
     *
     * @param ipAddress - public IP address of the CIC.
     */
    @Inject
    public CicSession(@Assisted("userName") String userName, @Assisted("password") String password,
            @Assisted("ipAddress") String ipAddress, @Assisted("port") int port) {
        super(EcsConnectionTarget.CIC);
        mCurrentUser = new EcsUser(userName, password, true);
        mInitialUserName = new EcsUser(userName, password, true);
        mIpAddress = ipAddress;
        mPort = port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void connect() {
        String prompt = REGEX_BASIC_PROMPTS + "|" + CIC_COMPUTE_PROMPT + "|" + getPersonalUserPrompt();
        try {
            mLogger.info(EcsAction.CONNECTING, "", mHostname, EcsCic.class.getSimpleName());
            connect(mIpAddress, mInitialUserName.getUsername(), mInitialUserName.getPassword(), mPort, prompt);
            checkForPossiblePasswordOrContinuePrompt();
        } catch (AuthenticationException | IllegalArgumentException | ConnectionToServerException ex) {
            mLogger.warn("[" + ex.getClass().getSimpleName() + "]: " + ex.getMessage());
            FileOutputStream fileOutputStream;
            InputStream in;
            File tempFile = new File("UploadSSHKey.py");
            try {
                String node = System.getProperties().getProperty("node").trim();
                mLogger.info(EcsAction.TRANSFERING, "SSH Key", mHostname, "");
                // Getting the current path
                Process p = new ProcessBuilder("pwd").start();
                String currentAbsolutePath = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
                String workspacePath = currentAbsolutePath.substring(0, currentAbsolutePath.length() - 24);
                in = getClass().getClassLoader().getResourceAsStream("UploadSSHKey.py");

                Assert.assertNotNull(in);

                fileOutputStream= new FileOutputStream(tempFile);
                byte [] buffer = new byte[4096];
                int bytesRead = in.read(buffer);
                while (bytesRead != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    bytesRead = in.read(buffer);
                }
                in.close();
                fileOutputStream.close();

                // Get repo's absolute path from current path and combine with script's path
                p = new ProcessBuilder("python", "./UploadSSHKey.py",
                        "-n" + node).start();
                BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = stdOut.readLine()) != null) {
                    mLogger.info(Verdict.RECEIVED, "", mHostname, line);
                }
                stdOut.close();
                BufferedReader errorOut = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = errorOut.readLine()) != null) {
                    mLogger.error(line);
                }
                errorOut.close();
                // Connect again
                connect(mIpAddress, mInitialUserName.getUsername(), mInitialUserName.getPassword(), mPort, prompt);
                checkForPossiblePasswordOrContinuePrompt();
            } catch (IOException e) {
                throw new EcsConnectionException(EcsConnectionType.SSH, EcsConnectionTarget.CIC, mIpAddress, 22,
                        mInitialUserName.getUsername(), e);
            }
            finally {
                if(tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception ex) {
            throw new EcsConnectionException(EcsConnectionType.SSH, mConnectionTarget, mIpAddress, mPort,
                    mInitialUserName.getUsername(), ex);
        }
        mCli.send("source openrc");
        mLogger.info(Verdict.CONNECTED, "", mHostname, EcsCic.class.getSimpleName());
    }
}
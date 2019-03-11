package com.jcat.cloud.fw.components.system.cee.services.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.services.database.EcsDatabaseClient;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;

/**
 * This class contains available functionality concerning Mysql
 * client that is within the CIC.
 *
 * @TODO: Change to use JDBC instead of command line?
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-01-08 initial version
 * @author epergat 2015-02-09 Added performSql method
 * @author epergat 2015-03-24 Added loopHelper to remove race condition when verifying backup.
 * Also removed automatic cleanup of databases since it removed databases that testers wanted to restore.
 * @author efikayd 2015-03-26 Added start/stop methods and utility methods for the MySQL Service
 * @author efikayd 2015-04-17 Updated start/stop methods for the MySQL Service
 * @author ehosmol 2015-05-18 adopt to {@link EcsCic}
 * @author zdagjyo 2017-02-14 Modify access modifier of method verifyStatus from private to public
 */
public class EcsMysql extends EcsComponent implements EcsDatabaseClient {

    private final EcsSession mSshSession;
    private final EcsCic mEcsCic;

    private final List<String> mCreatedBackups;

    private final Logger mLogger = Logger.getLogger(EcsMysql.class);

    private static final String CMD_EXPORT_OCF_RESOURCE_INSTANCE = "export OCF_RESOURCE_INSTANCE=clone_p_mysql; ";
    private static final String CMD_EXPORT_OCF_ROOT = "export OCF_ROOT=/usr/lib/ocf; ";
    private static final String CMD_EXPORT_OCF_RESKEY_SOCKET = "export OCF_RESKEY_socket=/var/run/mysqld/mysqld.sock; ";
    private static final String CMD_EXPORT_OCF_RESKEY_ADDITIONAL_PARAMETERS = "export OCF_RESKEY_additional_parameters=\"--wsrep-new-cluster\"; ";
    private static final String CMD_RENAME_GALERA = "sudo mv /var/lib/mysql/grastate.dat /var/lib/mysql/grastate.old; ";
    private static final String CMD_MYSQL_SERVER_START = "/usr/lib/ocf/resource.d/mirantis/mysql-wss start";
    private static final String CMD_MYSQL_SERVER_STOP = "/usr/lib/ocf/resource.d/mirantis/mysql-wss stop";
    private static final String MYSQL_STARTED = "MySQL started";
    private static final String MYSQL_STOPPED = "MySQL stopped";

    public EcsMysql(EcsSession sshSession, EcsCic ecsCic) {
        mSshSession = sshSession;
        mEcsCic = ecsCic;
        mCreatedBackups = new ArrayList<String>();
    }

    @Override
    public EcsDatabaseBackup backup(String nameOfDatabase) {
        final String backupFile = nameOfDatabase + "_backup_" + UUID.randomUUID() + ".sql";
        String cmd = "mysqldump " + nameOfDatabase + " > " + backupFile;
        mSshSession.send(cmd);

        new LoopHelper<Boolean>(Timeout.CREATE_MYSQL_BACKUP, "Could not create a backup of database + '"
                + nameOfDatabase + "'", Boolean.TRUE, new LoopHelper.ICheck<Boolean>() {

            @Override
            public Boolean getCurrentState() {
                String output = mSshSession.send("ls -al");
                if (output.contains(backupFile)) {
                    return true;
                }
                return false;
            }

        }).run();

        mCreatedBackups.add(backupFile);
        return new EcsDatabaseBackup(nameOfDatabase, backupFile);
    }

    @Override
    public void create(String nameOfDatabase) {
        // make sure mysql is running
        startMySQLService();
        String output = mSshSession.send("mysql -uroot -e 'create database " + nameOfDatabase + "'");
        if (output.contains("database exist")) {
            throw new RuntimeException("Can't create the database '" + nameOfDatabase
                    + "'in MySql because it already exists!");
        }
        verifyStatus(nameOfDatabase, Status.EXIST);
    }

    @Override
    public Boolean deinitialize() {
        for (String backup : mCreatedBackups) {
            mSshSession.send("rm -f " + backup);
        }
        return true;
    }

    @Override
    public void delete(String nameOfDatabase) {
        String output = mSshSession.send("mysql -uroot -e 'drop database " + nameOfDatabase + "'");

        if (output.contains("database doesn't exist")) {
            throw new RuntimeException("The database '" + nameOfDatabase
                    + "' couldn't be deleted since it doesn't exist!");
        }

        verifyStatus(nameOfDatabase, Status.NOT_EXIST);
    }

    @Override
    public boolean doesDatabaseExist(String nameOfDatabase) {
        String output = mSshSession.send("mysql -uroot -e 'show databases'");
        return output.contains(nameOfDatabase);
    }

    @Override
    public String performSql(String nameOfDatabase, String sql) {
        String output = mSshSession.send("mysql -uroot -D " + nameOfDatabase + " -e '" + sql + ";'");

        if (output.contains("Unknown database")) {
            throw new RuntimeException("Can't execute SQL query against database '" + nameOfDatabase
                    + "', the database doesn't exist!");
        }

        return output;
    }

    @Override
    public void restore(EcsDatabaseBackup backup) {
        String pathToRestore = backup.getPath();
        String databaseName = backup.getDatabaseName();

        if (!doesDatabaseExist(databaseName)) {
            create(databaseName);
        }
        String output = mSshSession.send("mysql " + databaseName + " < " + pathToRestore);
        if (output.contains("Could not find")) {
            throw new RuntimeException("The database '" + databaseName + "' could not be restored from backup '"
                    + pathToRestore + "'.");
        }
    }

    /**
     * Start the MySQL Service
     * @return TRUE if service starts successfully. False, otherwise
     */
    public boolean startMySQLService() {
        mLogger.info("Try to start MYSQL service");
        boolean hasServiceStarted = false;
        String cmdMySqlServiceStart = CMD_EXPORT_OCF_RESOURCE_INSTANCE + CMD_EXPORT_OCF_ROOT
                + CMD_EXPORT_OCF_RESKEY_SOCKET + CMD_EXPORT_OCF_RESKEY_ADDITIONAL_PARAMETERS + CMD_RENAME_GALERA
                + CMD_MYSQL_SERVER_START;

        String output = mSshSession.send(cmdMySqlServiceStart);
        mLogger.info("Running the command: " + cmdMySqlServiceStart);
        mLogger.info("Execution output: " + output);

        hasServiceStarted = output.contains(MYSQL_STARTED);
        if (hasServiceStarted) {
            mLogger.info("MYSQL service has been started successfully!");
        }
        return hasServiceStarted;
    }

    /**
     * Stop the MySQL Service
     * @return TRUE if service stops successfully. False, otherwise.
     */
    public boolean stopMySQLService() {
        mLogger.info("Try to stop MYSQL service");
        boolean hasServiceStopped = false;
        String cmdMySqlServiceStop = CMD_EXPORT_OCF_RESOURCE_INSTANCE + CMD_EXPORT_OCF_ROOT
                + CMD_EXPORT_OCF_RESKEY_SOCKET + CMD_EXPORT_OCF_RESKEY_ADDITIONAL_PARAMETERS + CMD_RENAME_GALERA
                + CMD_MYSQL_SERVER_STOP;
        String output = mSshSession.send(cmdMySqlServiceStop);
        mLogger.info("Running the command: " + cmdMySqlServiceStop);
        mLogger.info("Execution output: " + output);

        hasServiceStopped = output.contains(MYSQL_STOPPED);
        if (hasServiceStopped) {
            mLogger.info("MYSQL service has been stoppped successfully!");
        }
        return hasServiceStopped;
    }

    /**
     * Used for verifying status
     * @param nameOfDatabase
     * @param expectedStatus
     */
    public void verifyStatus(String nameOfDatabase, Status expectedStatus) {
        boolean exists = doesDatabaseExist(nameOfDatabase);
        Status currStatus = null;
        if (exists) {
            currStatus = Status.EXIST;
        } else {
            currStatus = Status.NOT_EXIST;
        }
        if (!currStatus.equals(expectedStatus)) {
            throw new RuntimeException(String.format(
                    "The database %s has status %s, which is different from the expected status %s", nameOfDatabase,
                    currStatus, expectedStatus));
        }
    }
}

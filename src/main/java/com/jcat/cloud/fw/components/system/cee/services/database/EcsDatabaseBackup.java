package com.jcat.cloud.fw.components.system.cee.services.database;

/**
 * Represents a database backup.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-01-08 initial version
 */
public class EcsDatabaseBackup {

    // Absolute path to the backup
    private String mPathToBackup;

    // Name of the database
    private String mDatabasename;

    /**
     * constructor
     * @param databasename name of the database to be backup'ed.
     * @param pathToBackup Absolute path to the database backup.
     */
    public EcsDatabaseBackup(String databasename, String pathToBackup) {
        if (pathToBackup.contains("..")) {
            throw new RuntimeException("A path to a database backup can not be relative, must be absolute!");
        }
        mPathToBackup = pathToBackup;
        mDatabasename = databasename;
    }

    /**
     * Returns the absolute path to the database backup.
     * @return absolute path to the database backup
     */
    public String getPath() {
        return mPathToBackup;
    }

    /**
     * Returns the name of the database that was backup:ed.
     * @return name of the database that was backup:ed.
     */
    public String getDatabaseName() {
        return mDatabasename;
    }
}

package com.jcat.cloud.fw.components.model.services.database;

import com.jcat.cloud.fw.components.system.cee.services.database.EcsDatabaseBackup;

/**
 * This class is a interface for database related clients.
 * All databases in ECS JCAT must fulfill the set of methods
 * below
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2015-01-08 initial version
 * @author epergat 2015-02-09 Implemented performSql method
 */
public interface EcsDatabaseClient {

    /**
     *
     * @author epergat
     *
     */
    public enum Status {
        EXIST, NOT_EXIST;
    }

    /**
     * Creates the given database. Throws an exception if the
     * database cannot be deleted.
     * @param nameOfDatabase - the name of the database
     */
    public void create(String nameOfDatabase);

    /**
     * Deletes the given database. Throws an exception if the
     * database cannot be deleted.
     * @param nameOfDatabase - the name of the database
     * @return
     */
    public void delete(String nameOfDatabase);

    /**
     * Create a backup. Throws an exception if the
     * database cannot be deleted.
     * @param nameOfDatabase - name of database to backup.
     * @return
     */
    public EcsDatabaseBackup backup(String nameOfDatabase);

    /**
     * Check if a Database exists
     * @param nameOfDatabase
     * @return true if Database exists, false if Database does not exist
     */
    public boolean doesDatabaseExist(String nameOfDatabase);

    /**
     * Restores a database to a previous version.
     * Throws an exception if the database cannot be deleted.
     * @param backup The backup to restore from.
     * @return
     */
    public void restore(EcsDatabaseBackup backup);

    /**
     * Used for doing SQL operations directly to a database.
     * @param nameOfDatabase
     * @param sql
     * @return
     */
    public String performSql(String nameOfDatabase, String sql);

}

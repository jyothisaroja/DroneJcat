package com.jcat.cloud.fw.fwservices.monitoring.tag;

import java.sql.Timestamp;

import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.Result;
import com.jcat.cloud.fw.fwservices.monitoring.db.MonitoringTag;
import com.jcat.cloud.fw.fwservices.monitoring.util.DatabaseHelper;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 * Singleton class that is user by the LoopHelper to store the timeout tag
 * events in the database.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-07-21 Initial version
 *
 */
public class TagLogger {

    private static TagLogger mTagLogger = null;
    private static boolean mEnabled = false;
    private DatabaseHelper mDbHelper;

    private TestConfiguration testConfig = TestConfiguration.getTestConfiguration();

    /**
     * Default constructor that initializes the database helper.
     */
    private TagLogger() {
        if (!testConfig.isTagLoggerEnabled()) {
            mEnabled = false;
            return;
        }
        if (DatabaseHelper.isDbAccessiable()) {
            mEnabled = true;
        }
        mDbHelper = new DatabaseHelper();
    }

    /**
     * Static getter for the singleton class
     * @return The singleton instance of this class
     */
    public static TagLogger getInstance() {
        if (mTagLogger == null) {
            mTagLogger = new TagLogger();
        }
        return mTagLogger;
    }

    /**
     * Creates a tag object given a set of parameters and stores this tag
     * in the database.
     *
     * There are setters in MonitoringTag that are not used, for data that
     * cannot be accessed (host name and test id). If this information is
     * available, save the date by calling the appropriate setters.
     *
     * @param timeout The timeout event
     * @param result The result enum
     * @param started The start time of the LoopHelper in Unix time (ms)
     * @param finished The end time of the LoopHelper in Unix time (ms)
     */

    public void tag(Timeout timeout, Result result, long started, long finished) {
        if (mEnabled) {
            MonitoringTag tag = new MonitoringTag();
            tag.setTimeout(timeout.getTimeoutInSeconds());
            tag.setTag(timeout.name());
            tag.setResult(result.name());
            tag.setStarted(new Timestamp(started));
            if (result == Result.SUCCESSFUL) {
                tag.setFinished(new Timestamp(finished));
            }
            mDbHelper.save(tag);
        }
    }
}

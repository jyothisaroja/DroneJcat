package com.jcat.cloud.fw.common.utils;

/**
 * Result collection for LoopHelper. Each execution of the LoopHelper's
 * run method may result in any of these results. This enum class is used
 * when storing the LoopHelper tag result in the database.
 *
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-07-21 Initial version
 */
public enum Result {
    SUCCESSFUL, TIMED_OUT, EXCEPTION;
}

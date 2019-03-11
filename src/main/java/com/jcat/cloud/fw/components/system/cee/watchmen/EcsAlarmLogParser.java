/**
 *
 */
package com.jcat.cloud.fw.components.system.cee.watchmen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.jcat.cloud.fw.common.exceptions.EcsWatchmenException;

/**
 * Documentation regarding the different events can be found here:
 *
 * http://calstore.internal.ericsson.com/alexserv?ID=31662&DB=91072-en_lzn7920001_99_p1aq.alx&FN=h3_9.html
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eelimei 2015-05-22 initial version
 *
 */
public class EcsAlarmLogParser {

    private Logger mLogger = Logger.getLogger(EcsAlarmLogParser.class);

    /**
     * Parses the log entry and compares the timestamp existing in the beginning of the log entry
     * to the timestamp that was taken when the test-case started.
     * @param logEntry Current log entry from watchment log.
     * @param timestamp Timestamp from the beginning of the test-case
     * @return true if the logEntry has a timestamp _older_ than the timestamp that was created when test-case started.
     */
    private boolean validTimestamp(String logEntry, DateTime timestamp) {
        // Pattern to match the following (example): <134>May 6 16:59:45
        StringTokenizer st = new StringTokenizer(logEntry);
        String month = st.nextToken(); // Ex: <134>May
        month = month.substring(month.length() - 3, month.length()); // Take the three last letters.

        String dayNr = st.nextToken();
        int day = Integer.parseInt(dayNr);

        String time = st.nextToken();
        st = new StringTokenizer(time, ":");

        int monthNr = -1;
        try {
            Date date;
            date = new SimpleDateFormat("MMM", Locale.ENGLISH).parse(month);
            monthNr = date.getMonth();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int hour = Integer.parseInt(st.nextToken());
        int minute = Integer.parseInt(st.nextToken());
        int seconds = Integer.parseInt(st.nextToken());

        Calendar c1 = GregorianCalendar.getInstance();
        c1.set(Calendar.getInstance().get(Calendar.YEAR), monthNr, day, hour, minute, seconds);
        DateTime entryLogDate = new DateTime(c1.getTime());

        if (entryLogDate.compareTo(timestamp) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Parses the Watchmen alarm log file and returns a HashSet of all
     * alarm and alerts that was found in the log file.
     * @param file File to be parsed
     * @param timestamp The earliest time that log events should be included from.
     * @return A list containing of 0 or more events depending on the content of the log file.
     */
    public List<EcsSnmpEvent> parseLogFile(File file, DateTime timestamp) {

        List<EcsSnmpEvent> gatheredEvents = new ArrayList<EcsSnmpEvent>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));

            String currLine = br.readLine();
            while (currLine != null) {
                if (validTimestamp(currLine, timestamp)) {
                    EcsSnmpEvent event = EcsSnmpFactory.create(currLine);
                    gatheredEvents.add(event);
                }
                currLine = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            mLogger.info("The watchmen-history file was not copied to the local directory.(" + e + ")");
            throw new EcsWatchmenException("The watchmen-history file was not copied to the local directory.(" + e
                    + ")");
        } catch (IOException e) {
            throw new EcsWatchmenException("The watchmen-history file was not copied to the local directory.(" + e
                    + ")");
        }
        return gatheredEvents;
    }

}

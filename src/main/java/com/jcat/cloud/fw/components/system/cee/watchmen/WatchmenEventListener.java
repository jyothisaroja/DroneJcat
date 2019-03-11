package com.jcat.cloud.fw.components.system.cee.watchmen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;

/**
 * This class will start a separate thread to listen to events
 * use addEvent(..) to add event you want to listen to
 * use getResult() to see if all event were catched during listening
 *
 * @author ezhgyin
 * @author zdagjyo 2017-06-23 Modified code to check for alarms/alerts
 *         in alarm-history as everything ends up in alarm history and added
 *         method waitAndVerifyAllEventsTracked
 * @author zdagjyo 2019-01-11 Modified class to support alarm verification in CEE9
 */
public class WatchmenEventListener extends Thread {

    @Inject
    private WatchmenService mWatchmenService;

    @Inject
    private EcsCicList mEcsCicList;
    private Set<EcsSnmpEvent> mEventsToListen = new HashSet<EcsSnmpEvent>();

    private Logger mLogger = Logger.getLogger(this.getClass());

    private boolean mListenerStarted = false;

    /**
     * Add the event to listen to
     */
    public void addEvent(EcsSnmpEvent event) {
        if (!mListenerStarted) {
            // if the listener is not started, start it when first time addEvent() is called
            mListenerStarted = true;
            this.start();
        }
        mEventsToListen.add(event);
    }

    /**
     * Get current result for event listening
     *
     * @return true if all events were found, otherwise return false
     */
    public boolean getResult() {
        boolean result = mEventsToListen.isEmpty();
        if (!result) {
            // there are alarms/alerts not found
            for (EcsSnmpEvent event : mEventsToListen) {
                mLogger.warn(String.format("Could not find alarm/alert %s.", event));
            }
        }
        return result;
    }

    @Override
    public void run() {
        DateTime startTime = new DateTime(mEcsCicList.getRandomCic().getDate());
        mLogger.info("Event Listener Start time: " + startTime);
        List<EcsSnmpEvent> alarmHistory = new ArrayList<EcsSnmpEvent>();
        List<EcsSnmpEvent> activeAlarmList = new ArrayList<EcsSnmpEvent>();
        while (true) {
            try {
                // the try...catch... here is to prevent the event listener from temporary watchmen service issue
                alarmHistory = mWatchmenService.getAlarmHistory();
                activeAlarmList = mWatchmenService.listActiveAlarms();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (EcsSnmpEvent event : alarmHistory) {
                DateTime eventTime = event.getLastEventTime();
                if (eventTime.isAfter(startTime)) {
                    Iterator<EcsSnmpEvent> eventIter = mEventsToListen.iterator();
                    while (eventIter.hasNext()) {
                        EcsSnmpEvent eventToListen = eventIter.next();
                        if (eventToListen.matches(event)) {
                            mLogger.info("Found matching alarm/alert: " + event);
                            mEventsToListen.remove(eventToListen);
                            break;
                        }
                    }
                }
            }
            for (EcsSnmpEvent event : activeAlarmList) {
                DateTime eventTime = event.getLastEventTime();
                if (eventTime.isAfter(startTime)) {
                    Iterator<EcsSnmpEvent> eventIter = mEventsToListen.iterator();
                    while (eventIter.hasNext()) {
                        EcsSnmpEvent eventToListen = eventIter.next();
                        if (eventToListen.matches(event)) {
                            mLogger.info("Found matching alarm/alert: " + event);
                            mEventsToListen.remove(eventToListen);
                            break;
                        }
                    }
                }
            }

            if (mEventsToListen.size() != 0) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                    mLogger.info("check events again after 5 seconds");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method that waits and verifies that all alarms/alerts are tracked
     */
    public void waitAndVerifyAllEventsTracked() {
        if (!getResult()) {
            new LoopHelper<Boolean>(Timeout.ALARM_WAIT_TIMEOUT,
                    "Was not able to verify that all alarms/alerts were found", Boolean.TRUE, () -> {
                        return getResult();
                    }).run();
        }
    }
}

package com.jcat.cloud.fw.fwservices.monitoring.app;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openstack4j.model.telemetry.Meter;
import org.openstack4j.model.telemetry.MeterSample;
import org.openstack4j.openstack.internal.Parser;
import com.google.inject.Inject;
import com.jcat.cloud.fw.components.system.cee.services.ceilometer.CeilometerController;
import com.jcat.cloud.fw.fwservices.monitoring.db.MonitoringSample;
import com.jcat.cloud.fw.fwservices.monitoring.util.DatabaseHelper;

/**
 * This class extracts information from ceilometer-samples and inserts them into a database.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehannyb 2015-06-23 initial version
 * @author ehkiyki 2015-07-06 Changed the database table where the samples are stored.
 * @author ehkiyki 2015-07-08 Added DatabaseHelper to manage the database sessions
 * @author ethssce 2015-08-17 adjusted to namespace structure
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 *
 */
public class VmStatisticCollector {
    private CeilometerController mCeilometerController;
    @Inject
    private DatabaseHelper mDbHelper;
    static long startTime = 0L;

    @Inject
    public VmStatisticCollector(CeilometerController ceilometerController) {
        mCeilometerController = ceilometerController;
    }

    /*
     * addSample for jsonHelper
     * Sets the chosen values for the VmSample and sends is to the database table vm_samples.
     * @param obj
     * @param testId
     */
    public void addSample(String node, MeterSample obj) {
        MonitoringSample sample = new MonitoringSample();
        String countName = obj.getCounterName();
        Float countVolume = obj.getCounterVolume();
        Map<String, Object> metadata = obj.getMetadata();
        Object displayName = obj.getMetadata().get("display_name");
        Date date = Parser.asDate(obj.getTimestamp());

        if (countName != null && countVolume != null && metadata != null && displayName != null && date != null) {

            sample.setNode(node);
            sample.setData(countName);
            sample.setValue(countVolume);
            sample.setHost(displayName.toString());
            Timestamp timestamp = new Timestamp(date.getTime());
            sample.setTimestamp(timestamp);

            mDbHelper.save(sample);
            System.out.println("*************saved " + obj.getCounterName() + "**************");
        }
    }

    /*
     * Collects the Ceilometer information to the database once every minute from all the available samples.
     * @param meterName
     * @param testName
     * @param openStack4jEcs
     */
    public void collect(String node) {
        Set<String> meterNames = getMeterList();

        long startTime = 0L;
        long endTime = 0L;
        int timesYouAddedSmth = 0;
        int timesYouSlept = 0;

        while (true) {
            startTime = System.currentTimeMillis();

            try {
                System.out.println("************* Sleeping for 1 min ************** Times you slept: " + timesYouSlept);
                timesYouSlept = timesYouSlept + 1;
                System.out.println("********** Times you added something ***************** " + timesYouAddedSmth);
                Thread.sleep(60000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            endTime = System.currentTimeMillis();

            for (String meterName : meterNames) {
                List<? extends MeterSample> list = mCeilometerController.getTimedSamples(meterName, startTime, endTime);

                for (int i = 0; i < list.size(); i++) {
                    MeterSample obj = list.get(i);
                    addSample(node, obj);
                    timesYouAddedSmth = timesYouAddedSmth + 1;
                }
            }
        }
    }

    /*
     * getMeterList provides the tester with the available meters:
     * [disk.read.bytes.rate, disk.root.size, volume.size, instance:Flavor--0610_15_14_45.957, image.delete,
     * disk.read.requests, vcpus, image.download, image.update, network.incoming.packets.rate,
     * instance:Flavor--0609_09_24_05.901, disk.write.bytes, instance:NovaControllerIT_CustomFlavor_0612_10_40_20_94,
     * instance:NovaControllerIT_TestFlavor, network.incoming.packets, instance, instance:Flavor--0610_15_34_40.925,
     * disk.write.requests, image.size, network.outgoing.bytes, instance:m1.medium, disk.read.requests.rate,
     * instance:Flavor--0610_15_06_34.476, network.incoming.bytes, disk.write.requests.rate,
     * network.outgoing.bytes.rate, image.upload, disk.read.bytes, instance:Flavor--0612_09_35_06_490, cpu, image,
     * instance:Flavor--0610_15_45_35.348, instance:NovaControllerIT_TestFlavor_0612_10_40_20_94,
     * network.outgoing.packets, instance:Flavor--0612_10_45_28_941, memory, cpu_util, network.outgoing.packets.rate,
     * network.incoming.bytes.rate, instance:Flavor--0605_18_11_01.464, image.serve, volume,
     * instance:Flavor--0609_09_37_21.882, instance:Flavor--0610_14_55_39.434, disk.ephemeral.size,
     * disk.write.bytes.rate]
     * @param openStack4jEcs
     */
    public Set<String> getMeterList() {
        Set<String> returnList = new HashSet<String>();
        List<? extends Meter> list = mCeilometerController.listMeters();
        for (int i = 0; i < list.size(); i++) {
            Meter obj = list.get(i);
            String name = obj.getName();
            returnList.add(name);
        }
        return returnList;
    }
}

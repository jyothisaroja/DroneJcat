package com.jcat.cloud.fw.components.system.cee.services.ceilometer;

import java.util.List;
import org.apache.log4j.Logger;
import org.openstack4j.api.telemetry.MeterService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.telemetry.Meter;
import org.openstack4j.model.telemetry.MeterSample;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.model.telemetry.SampleCriteria.Oper;
import org.openstack4j.model.telemetry.Statistics;
import com.google.inject.Inject;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains methods related with openstack ceilometer.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehannyb 2015-06-23 initial version
 * @author ethssce 2015-08-17 adjusted to namespace structure
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 */
public class CeilometerController {

    /**
     * Logger instance
     */
    private final static Logger mLogger = Logger.getLogger(CeilometerController.class);

    private static OpenStack4jEcs mOpenStack4jEcs;

    /**
     * Main Constructor:
     */
    @Inject
    private CeilometerController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    /**
     * Getter for admin meters.
     */
    private MeterService adminMeters() {
        return mOpenStack4jEcs.getClientForAdminUser(Facing.ADMIN).telemetry().meters();
    }

    /**
     * Getter for public meters.
     */
    private MeterService publicMeters() {
        MeterService meters = mOpenStack4jEcs.getClient(Facing.PUBLIC).telemetry().meters();
        return meters;
    }

    /**
     * Get statistics of specified Ceilometer meter using admin meters.
     *
     * @param meterName - String - name of meter
     * @return List<? extends Statistics> - List of statistics for specified meter
     */
    public List<? extends MeterSample> getAdminSamples(String meterName) {
        mLogger.info("Getting statistics for Celiometer meter " + meterName);
        return adminMeters().samples(meterName);
    }

    /**
     * Get statistics of specified Ceilometer meter using admin meters.
     *
     * @param meterName - String - name of meter
     * @return List<? extends Statistics> - List of statistics for specified meter
     */
    public List<? extends Statistics> getAdminStatistics(String meterName) {
        mLogger.info("Getting statistics for Ceilometer meter " + meterName);
        return adminMeters().statistics(meterName);
    }

    /**
     * Get samples of specified Ceilometer-meter using public meters.
     *
     * @param meterName - String - name of meter
     * @return List<? extends Statistics> - List of statistics for specified meter
     */
    public List<? extends MeterSample> getSamples(String meterName) {
        mLogger.info("Getting statistics for Celiometer meter " + meterName);
        return publicMeters().samples(meterName);
    }

    /**
     * Get statistics of specified Ceilometer meter sing public meters.
     *
     * @param meterName - String - name of meter
     * @return List<? extends Statistics> - List of statistics for specified meter
     */
    public List<? extends Statistics> getStatistics(String meterName) {
        mLogger.info("Getting statistics for Ceilometer meter " + meterName);
        return publicMeters().statistics(meterName);
    }

    /**
     * Get samples of specified Ceilometer-meter using public meters.
     *
     * @param meterName - String - name of meter
     * @return List<? extends Statistics> - List of statistics for specified meter
     */
    public List<? extends MeterSample> getTimedSamples(String meterName, long startTime, long endTime) {
        mLogger.info("Getting statistics for Celiometer meter " + meterName);
        return publicMeters().samples(meterName,
                SampleCriteria.create().timestamp(Oper.GT, startTime).timestamp(Oper.LT, endTime));
    }

    /**
     * Get the list of all Ceilometer meters
     *
     * @param -
     * @return List<? extends Meter> - List of meters
     */
    public List<? extends Meter> listMeters() {
        mLogger.info("Getting list of Ceilometer meters");
        return publicMeters().list();
    }
}

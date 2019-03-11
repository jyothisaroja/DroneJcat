package com.jcat.cloud.fw.common.utils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.components.model.compute.EcsAtlasVm;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.glance.GlanceController;
import com.jcat.cloud.fw.components.system.cee.openstack.keystone.KeystoneController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.openstack.nova.NovaController;
import com.jcat.cloud.fw.components.system.cee.services.crm.CrmService;
import com.jcat.cloud.fw.components.system.cee.services.rabbitmq.RabbitMqService;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import com.jcat.cloud.fw.components.system.cee.target.EcsComputeBladeList;
import com.jcat.cloud.fw.components.system.cee.target.fuel.EcsFuel;
import com.jcat.cloud.fw.components.system.cee.watchmen.WatchmenService;
import com.jcat.cloud.fw.hwmanagement.blademanagement.IEquipmentController;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.BspNetconfLib;
import com.jcat.cloud.fw.hwmanagement.switches.extremeswitch.IExtremeSwitchLib;
import com.jcat.cloud.fw.infrastructure.resources.ExtremeSwitchResourceGroup.ExtremeSwitchName;

/**
 * This class contains health check procedures that are performed during stability test.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2018-03-26 - First revision
 *
 */

public final class StabilityHealthCheck {

    /**
     * Logger instance
     */
    private static final EcsLogger mLogger = EcsLogger.getLogger(StabilityHealthCheck.class);

    @Inject
    private EcsFuel mFuel;

    @Inject
    private EcsComputeBladeList mEcsComputeBladeList;

    @Inject
    private EcsCicList mEcsCicList;

    @Inject
    private CinderController mCinderController;

    @Inject
    private IEquipmentController mEquipmentController;

    @Inject
    private IExtremeSwitchLib mExtremeSwitch;

    @Inject
    private GlanceController mGlanceController;

    @Inject
    private KeystoneController mKeystoneController;

    @Inject
    private NeutronController mNeutronController;

    @Inject
    private NovaController mNovaController;

    @Inject
    private WatchmenService mWatchmenService;

    private boolean mIsBsp = false;

    private Map<String, Date> mExceptionsCaught = new HashMap<String, Date>();

    /**
     * Verifies date on all cics & compute blades.
     */
    private void getDateOnHosts() {
        mLogger.info(EcsAction.FINDING, "", "date", "on all cics");
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            cic.getDate();
        }
        mLogger.info(Verdict.FOUND, "", "date", "on all cics");
        mLogger.info(EcsAction.FINDING, "", "date", "on all computes");
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            blade.getDate();
        }
        mLogger.info(Verdict.FOUND, "", "date", "on all computes");
    }

    /**
     * Verifies system uptime on all cics & compute blades.
     */
    private void getUptimeOnHosts() {
        mLogger.info(EcsAction.FINDING, "", "uptime", "on all cics");
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            cic.getSystemUptime();
        }
        mLogger.info(Verdict.FOUND, "", "uptime", "on all cics");
        mLogger.info(EcsAction.FINDING, "", "uptime", "on all computes");
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            blade.getSystemUptime();
        }
        mLogger.info(Verdict.FOUND, "", "uptime", "on all computes");
    }

    /**
     * Verifies active alarms and alarms in history
     */
    private void verifyAlarms() {
        mLogger.info(EcsAction.FINDING, "", "Alarms", "");
        try {
            mLogger.info(Verdict.FOUND, "", "Active alarms", mWatchmenService.listActiveAlarms().toString());
            mLogger.info(Verdict.FOUND, "", "Alarms in history", mWatchmenService.getAlarmHistory().toString());
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking alarms");
        }
    }

    /**
     * Verifies that atlas VM is active and accessible.
     */
    private void verifyAtlasVm() {
        mLogger.info(EcsAction.FINDING, EcsAtlasVm.class, "");
        try {
            mNovaController.getAtlasVm();
            mLogger.info(Verdict.FOUND, EcsAtlasVm.class, "");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while fetching atlas vm");
        }
    }

    /**
     * Verifies that cinder services are enabled.
     */
    private void verifyCinderServices() {
        mLogger.info(EcsAction.FINDING, "", "Cinder services", "enabled");
        try {
            mCinderController.checkAllServicesStatusEnabled();
            mLogger.info(Verdict.FOUND, "", "Cinder services", "enabled");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking cinder services");
        }
    }

    /**
     * Verifies crash logs on all cics & compute blades.
     */
    private void verifyCrashLogs() {
        mLogger.info(EcsAction.FINDING, "", "Crash logs", "on all cics");
        String directory = "/var/log/crash/";
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            cic.listFiles(directory);
        }
        mLogger.info(Verdict.FOUND, "", "Crash logs", "on all cics");
        mLogger.info(EcsAction.FINDING, "", "Crash logs", "on all computes");
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            blade.listFiles(directory);
        }
        mLogger.info(Verdict.FOUND, "", "Crash logs", "on all computes");
    }

    /**
     * Verifies that RabbitMQ cluster and pacemaker services are up and running.
     */
    private void verifyCrmResources() {
        EcsCic cic = mEcsCicList.getRandomCic();
        CrmService crmService = cic.getCrmService();
        RabbitMqService rabbitMqService = cic.getRabbitMqService();
        mLogger.info(EcsAction.FINDING, "", "Pacemaker services", "up and running");
        try {
            crmService.arePacemakerServicesUp();
            mLogger.info(Verdict.FOUND, "", "Pacemaker services", "up and running");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking pacemaker services");
        }
        mLogger.info(EcsAction.FINDING, "", "RabbitMQ cluster", "up and running");
        try {
            rabbitMqService.isRabbitClusterUp();
            mLogger.info(Verdict.FOUND, "", "RabbitMQ cluster", "up and running");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking RabbitMQ cluster");
        }
    }

    /**
     * Verifies disk usage on all cics & compute blades.
     */
    private void verifyDiskUsage() {
        mLogger.info(EcsAction.FINDING, "", "Disk usage", "on all cics");
        for (EcsCic cic : mEcsCicList.getAllCics()) {
            cic.diskUsage();
        }
        mLogger.info(Verdict.FOUND, "", "Disk usage", "on all cics");
        mLogger.info(EcsAction.FINDING, "", "Disk usage", "on all computes");
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            blade.diskUsage();
        }
        mLogger.info(Verdict.FOUND, "", "Disk usage", " on all computes");
    }

    /**
     * Verifies the time in both extreme switches.
     */
    private void verifyExtremeSwitchTime() {
        mLogger.info(EcsAction.FINDING, "", "Current time", "on exteme switch 1 and 2");
        Date currentTime1 = null;
        Date currentTime2 = null;
        try {
            currentTime1 = mExtremeSwitch.getCurrentTimeOnSwitch();
            mExtremeSwitch.selectExtremeSwitch(ExtremeSwitchName.EXTREMESWITCH_2);
            currentTime2 = mExtremeSwitch.getCurrentTimeOnSwitch();
            mLogger.info(Verdict.FOUND, "", "Current time", "on exteme switch 1:" + currentTime1);
            mLogger.info(Verdict.FOUND, "", "Current time", "on exteme switch 2:" + currentTime2);
        } catch (ParseException e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking time on extreme switch");
        }
    }

    /**
     * Verifies that glance images are listed.
     */
    private void verifyGlanceImages() {
        mLogger.info(EcsAction.FINDING, "", "Glance", "images");
        try {
            mGlanceController.listExistingImages();
            mLogger.info(Verdict.FOUND, "", "Glance", "images");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while listing glance images");
        }
    }

    /**
     * Verifies that keystone endpoints are listed.
     */
    private void verifyKeystoneEndpoints() {
        mLogger.info(EcsAction.FINDING, "", "Keystone", "endpoints");
        try {
            mKeystoneController.listEndpoints();
            mLogger.info(Verdict.FOUND, "", "Keystone", "endpoints");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while listing keystone endpoints");
        }
    }

    /**
     * Verifies multipath status on all compute blades.
     */
    private void verifyMultipath() {
        mLogger.info(EcsAction.FINDING, "", "Multipath", "active on all computes");
        for (EcsComputeBlade blade : mEcsComputeBladeList.getAllComputeBlades()) {
            blade.multipathInstalled();
        }
        mLogger.info(Verdict.FOUND, "", "Multipath", "active on all computes");
    }

    /**
     * Verifies that neutron agents are alive.
     */
    private void verifyNeutronAgents() {
        mLogger.info(EcsAction.FINDING, "", "Neutron Agents", "alive");
        try {
            mNeutronController.areAllNeutronAgentsAlive();
            mLogger.info(Verdict.FOUND, "", "Neutron Agents", "alive");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking neutron agents");
        }
        mLogger.info(EcsAction.FINDING, "", "Neutron net list", "fetching networks");
        try {
            mNeutronController.listNetworks();
            mLogger.info(Verdict.FOUND, "", "Neutron net list", "fetching networks");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while listing neutron networks");
        }
        mLogger.info(EcsAction.FINDING, "", "Neutron port list", "fetching ports");
        try {
            mNeutronController.listPorts();
            mLogger.info(Verdict.FOUND, "", "Neutron port list", "fetching ports");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while listing neutron ports");
        }
    }

    /**
     * Verifies that nova services are enabled.
     */
    private void verifyNovaServices() {
        mLogger.info(EcsAction.FINDING, "", "Nova List", "fetching VMs");
        try {
            mNovaController.listVMsAllTenant();
            mLogger.info(Verdict.FOUND, "", "Nova List", "fetching VMs");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while fetching nova VMs");
        }
        mLogger.info(EcsAction.FINDING, "", "Nova Services", "up");
        try {
            mNovaController.checkAllServicesStateUpStatusEnabled();
            mLogger.info(Verdict.FOUND, "", "Nova Services", "up");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking status of nova services");
        }
    }

    /**
     * Verifies NTP remote and reference IPs on all cics.
     */
    private void verifyNtpOnHosts() {
        mLogger.info(EcsAction.FINDING, "", "NTP Remote Ips", "in all vCICs");
        String[] remoteIps = mFuel.getNtpRemoteIpsforAllHosts(EcsFuel.Host.CIC);
        for (int i = 0; i < remoteIps.length; i++) {
            Matcher matcher = Pattern.compile("[^*]*$").matcher(remoteIps[i]);
            if (matcher.find()) {
                remoteIps[i] = matcher.group(0);
            }
        }
        List<String> remoteIpsList = Arrays.asList(remoteIps);
        remoteIpsList.replaceAll(String::trim);
        mLogger.info(Verdict.FOUND, "", "NTP Remote Ips", remoteIpsList + " in all vCICs ");
        mLogger.info(EcsAction.FINDING, "", "NTP Reference Ips", "in all vCICs");
        String[] referenceIpsBeforeNtpDisable = mFuel.getNtpReferenceIpsforAllHosts(EcsFuel.Host.CIC);
        List<String> referenceIpsListBeforeNtpDisable = Arrays.asList(referenceIpsBeforeNtpDisable);
        referenceIpsListBeforeNtpDisable.replaceAll(String::trim);
        mLogger.info(Verdict.FOUND, "", "NTP Reference Ips",
                referenceIpsListBeforeNtpDisable.toString() + " in all vCICs ");
    }

    /**
     * Prints all exceptions that were caught during health check along with their time stamps.
     */
    public void printAllExceptionsCaught() {
        mLogger.info(EcsAction.LISTING, "", "Exceptions caught", "during health check");
        if (mExceptionsCaught.size() > 0) {
            for (Map.Entry<String, Date> entry : mExceptionsCaught.entrySet()) {
                mLogger.warn("Exception caught: " + entry.getKey() + ", at timestamp: " + entry.getValue());
            }
            mLogger.info(Verdict.LISTED, "", "Exceptions caught", "during health check");
        } else {
            mLogger.warn("No exceptions were caught during healthcheck");
        }
    }

    /**
     * Runs a complete health check on the node.
     */
    public void runHealthCheck() {
        if (mEquipmentController instanceof BspNetconfLib) {
            mIsBsp = true;
        }
        mLogger.info(EcsAction.STARTING, "", "Health Check", "on DC");
        mLogger.info(EcsAction.FINDING, EcsFuel.class, "all hosts online");
        try {
            mFuel.areAllHostsOnline();
            mLogger.info(Verdict.FOUND, EcsFuel.class, "all hosts online");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking fuel hosts");
        }
        mLogger.info(EcsAction.FINDING, EcsFuel.class, "all tasks ready");
        try {
            mFuel.areAllTasksReady();
            mLogger.info(Verdict.FOUND, EcsFuel.class, "all tasks ready");
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while checking fuel tasks");
        }
        try {
            verifyNtpOnHosts();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while verifying NTP hosts");
        }
        try {
            getDateOnHosts();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while finding date on hosts");
        }
        try {
            getUptimeOnHosts();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while finding uptime on hosts");
        }
        verifyAlarms();
        try {
            verifyCrashLogs();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while verifying crash logs");
        }
        try {
            verifyDiskUsage();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while verifying disk usage");
        }
        try {
            verifyMultipath();
        } catch (Exception e) {
            mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
            mLogger.warn("Exception:" + e.getMessage() + " thrown while verifying multipath");
        }
        if (!mIsBsp) {
            try {
                verifyExtremeSwitchTime();
            } catch (Exception e) {
                mExceptionsCaught.put(e.getClass().getSimpleName(), new Date());
                mLogger.warn("Exception:" + e.getMessage() + " thrown while verifying time in extreme switch");
            }
        }
        verifyCrmResources();
        verifyNovaServices();
        verifyKeystoneEndpoints();
        verifyGlanceImages();
        verifyNeutronAgents();
        verifyCinderServices();
        verifyAtlasVm();
        mLogger.info(Verdict.FINISHED, "", "Health Check", "on DC");
    }
}
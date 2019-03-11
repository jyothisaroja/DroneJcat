package com.jcat.cloud.fw.components.system.cee.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsCicException;
import com.jcat.cloud.fw.common.exceptions.EcsConnectionException;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService;
import com.jcat.cloud.fw.common.utils.ParallelExecutionService.Result;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.model.services.database.EcsDatabaseClient;
import com.jcat.cloud.fw.components.model.target.EcsBatCic;
import com.jcat.cloud.fw.components.model.target.EcsBatCicFactory;
import com.jcat.cloud.fw.components.model.target.EcsCic;
import com.jcat.cloud.fw.components.model.target.EcsCic.CicService;
import com.jcat.cloud.fw.components.model.target.EcsCic.Status;
import com.jcat.cloud.fw.components.model.target.EcsCicFactory;
import com.jcat.cloud.fw.components.system.cee.openstack.cinder.CinderController;
import com.jcat.cloud.fw.components.system.cee.openstack.neutron.NeutronController;
import com.jcat.cloud.fw.components.system.cee.services.crm.CrmService;
import com.jcat.cloud.fw.components.system.cee.services.crm.CrmService.ServiceName;
import com.jcat.cloud.fw.components.system.cee.services.database.EcsMysql;
import com.jcat.cloud.fw.components.system.cee.services.rabbitmq.RabbitMqService;
import com.jcat.cloud.fw.infrastructure.resources.BladeSystemResource;
import com.jcat.cloud.fw.infrastructure.resources.OpenStackResource;
import com.kenai.jaffl.annotations.In;

/**
 * This is a class to test if the CIC object is working as intended when sending
 * commands and keeping their sessions alive.
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat - 2014-12-15 - Initial version
 * @author epergat - 2015-03-18 - Add methods to retrieve IP of the active CIC.
 * @author eqinann - 2015-09-09 - Add method to get BAT cic
 * @author eelimei - 2016-01-27 - Add method to get the active_mark cic
 * @author zdagjyo - 2016-11-23 - Add method to get the cic with specific
 *         service running on it.
 * @author zdagjyo - 2017-02-13 - Add methods checkServiceStatusOnCics, getProcessIdAndCic,
 *         waitAndVerifyAllCicsAndServicesOnline and waitForServiceToBeUp
 * @author zdagjyo - 2017-02-17 - Add method isCicTimeSynchronized
 * @author zpralak 2017-03-07 - Add waitAndVerifyRestartAllCics method
 * @author zdagjyo 2017-05-08 - Modify waitAndVerifyRestartAllCics method to use ParallelExecutionService
 * @author zdagjyo 2017-10-16 - Add method getCicWithAtlasImage
 * @author zdagjyo 2017-11-15 - Add method addZeroDiskWeightMultiplierToNovaConf
 * @author zdagjyo 2017-12-01 - Add method updateTimeToLiveInCeilometerConf
 **/
public class EcsCicList extends EcsComponent {

    @Inject
    private EcsBatCicFactory mBatCicFactory;

    private class IPandPort {

        private final String ip;
        private final int port;

        public IPandPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IPandPort)) {
                return false;
            }
            IPandPort pairo = (IPandPort) o;
            return this.ip.equals(pairo.getIpAddress()) && this.port == pairo.getPort();
        }

        public String getIpAddress() {
            return ip;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public int hashCode() {
            return ip.hashCode() ^ port;
        }
    }

    private static final String UPDATE_NOVA_CONF_CMD = "echo 'disk_weight_multiplier=0' >> /etc/nova/nova.conf";

    @Inject
    private EcsCicFactory mCicFactory;

    /**
     * Array containing IPs for all the controllers
     */
    private List<IPandPort> mControllerIPs = new ArrayList<IPandPort>();
    private final BladeSystemResource mControllerResource;
    private final List<EcsCic> mCreatedCicInstances = new ArrayList<EcsCic>();
    private boolean mIsCicsInstantiated = false;
    private final EcsLogger mLogger = EcsLogger.getLogger(EcsCicList.class);
    private final String mRootUser;
    private String mPassword = null;
    private int mNumberOfCics = -1;
    private EcsCic mCicWithAtlasImage;

    private final List<EcsCic> mUniqueCics = new ArrayList<EcsCic>();

    @Inject
    private CinderController mCinderController;

    @Inject
    private NeutronController mNeutronController;

    @Inject
    public EcsCicList(BladeSystemResource controllerResource, OpenStackResource openStackResource) {
        mRootUser = openStackResource.getController().getUserName();
        if (mRootUser != "root") {
            mPassword = openStackResource.getController().getPassword();
        }
        mControllerResource = controllerResource;
        mControllerIPs = getControllerIPs();
    }

    /**
     * Instantiates the cics.
     */
    private void instantiateCics() {
        if (!mIsCicsInstantiated) {
            for (IPandPort cicIp : mControllerIPs) {
                EcsCic cic = mCicFactory.create(mRootUser, mPassword, cicIp.getIpAddress(), cicIp.getPort());
                cic.lazyInitialize();
                mUniqueCics.add(cic);
            }
            mIsCicsInstantiated = true;
        }
    }

    /**
     * Saves the IP addresses for later reference from the resource object.
     */
    protected List<IPandPort> getControllerIPs() {
        List<IPandPort> controllerIPs = new ArrayList<IPandPort>();
        if (mControllerResource.getCic1Ip() != null && mControllerResource.getCic1Port() != 0) {
            controllerIPs.add(new IPandPort(mControllerResource.getCic1Ip(), mControllerResource.getCic1Port()));
        }
        if (mControllerResource.getCic2Ip() != null && mControllerResource.getCic2Port() != 0) {
            controllerIPs.add(new IPandPort(mControllerResource.getCic2Ip(), mControllerResource.getCic2Port()));
        }
        if (mControllerResource.getCic3Ip() != null && mControllerResource.getCic2Port() != 0) {
            controllerIPs.add(new IPandPort(mControllerResource.getCic3Ip(), mControllerResource.getCic3Port()));
        }
        controllerIPs = new ArrayList<>(new HashSet<>(controllerIPs));
        if (!(controllerIPs.size() > 0)) {
            throw new EcsOpenStackException("No IPs were configured for any cic in the BladeSystemResource");
        }
        mNumberOfCics = controllerIPs.size();
        return controllerIPs;
    }

    /**
     * Adds the line "disk_weight_multiplier=0" to nova.conf file and
     * restarts "nova-scheduler" service in all cics.
     *
     * @return boolean
     */
    public boolean addZeroDiskWeightMultiplierToNovaConf() {
        instantiateCics();
        mLogger.info(EcsAction.CHANGING, EcsCic.class, "nova.conf file on all cics");
        for (EcsCic cic : mUniqueCics) {
            cic.sendCommand(UPDATE_NOVA_CONF_CMD);
            if (cic.readFile(null, "/etc/nova/nova.conf").contains("disk_weight_multiplier=0")) {
                int pid = cic.restartService("nova-scheduler");
                if (pid == 0) {
                    mLogger.warn("failed to restart nova-scheduler service on " + cic.getHostname());
                    return false;
                }
            } else {
                mLogger.warn("failed to update nova.conf file on " + cic.getHostname());
                return false;
            }
        }
        mLogger.info(Verdict.CHANGED, EcsCic.class, "nova.conf file on all cics");
        return true;
    }

    /**
     * Checks the status of a service on all CICs
     *
     * @param - service - enum - The crm service whose status is to be checked
     */
    public void checkServiceStatusOnCics(ServiceName service) {
        mLogger.info(EcsAction.FINDING, "status of", service.name(), " on all vCICs");
        switch (service) {
        case RabbitMQ:
            RabbitMqService rabbitMqService = getRandomCic().getRabbitMqService();
            rabbitMqService.verifyRabbitMqIsUp();
            break;
        case MySQL:
            EcsMysql mDbClient = (EcsMysql) getRandomCic().getDatabaseClient();
            mDbClient.verifyStatus("zabbix", EcsDatabaseClient.Status.EXIST);
            break;
        case NeutronOpenvSwitch:
            mNeutronController.waitAndVerifyNeutronAgentsAlive();
            break;
        case NeutronDhcp:
            mNeutronController.waitAndVerifyNeutronAgentsAlive();
            break;
        case NeutronServer:
            mNeutronController.waitAndVerifyNeutronAgentsAlive();
            break;
        case CinderVolume:
            mCinderController.verifyCinderServicesEnabled();
            break;
        case Corosync:
            waitAndVerifyAllCicsAndServicesOnline();
            break;
        default:
            mLogger.warn("Service " + service.name() + " not in use, skipped");
            return;
        }
        mLogger.info(Verdict.FOUND, "status of", service.name(), " as up and running on all vCICs");
    }

    /*
     * (non-Javadoc)
     * @see com.jcat.ecs.lib.controllers.EcsComponent#initialize()
     */
    @Override
    public Boolean deinitialize() {
        mLogger.info(EcsAction.STARTING, "Clean up", EcsCicList.class, "");
        for (EcsCic cic : mUniqueCics) {
            cic.deinitialize();
        }
        for (EcsCic cic : mCreatedCicInstances) {
            cic.deinitialize();
        }
        mIsCicsInstantiated = false;
        mUniqueCics.clear();
        mCreatedCicInstances.clear();
        mLogger.info(Verdict.DONE, "Clean up", EcsCicList.class, "");
        return true;
    }

    /*
     * Returns the CIC where the active_mark is started
     * @return
     */
    public EcsCic getActiveMarkCic() {
        instantiateCics();
        CrmService service = getCic(Status.ACTIVE).getCrmService();
        String hostName = service.activeMark();
        for (EcsCic currentCic : mUniqueCics) {
            if (currentCic.getHostname().equals(hostName)) {
                EcsCic cic = mCicFactory.create(mRootUser, mPassword, currentCic.getIpAddress(), currentCic.getPort());
                cic.lazyInitialize();
                mCreatedCicInstances.add(cic);
                return cic;
            }
        }
        throw new EcsCicException("EcsCicList was not able to find the Active Marker Cic");
    }

    /**
     * Returns all CICs.
     *
     * @return
     */
    public List<EcsCic> getAllCics() {
        instantiateCics();
        List<EcsCic> cics = new ArrayList<EcsCic>();
        for (IPandPort cicIp : mControllerIPs) {
            EcsCic personalCicInstanceForUser = mCicFactory.create(mRootUser, mPassword, cicIp.getIpAddress(),
                    cicIp.getPort());
            personalCicInstanceForUser.lazyInitialize();
            cics.add(personalCicInstanceForUser);
            mCreatedCicInstances.add(personalCicInstanceForUser);
        }
        return cics;
    }

    public List<EcsBatCic> getAllBatCics() {
        List<EcsCic> cicList = new ArrayList<EcsCic>();
        for (EcsCic cic : getAllCics()) {
            if(cic.doesDirectoryExist("/var/lib/glance/BATscripts/")) {
                cicList.add(cic);
            }
        }
        List<EcsBatCic> batCics = new ArrayList<EcsBatCic>();
        for (EcsCic Cic : cicList) {
            EcsBatCic personalBatCicInstanceForUser = mBatCicFactory.create(mRootUser, mPassword, Cic.getIpAddress(),
                    Cic.getPort());
            personalBatCicInstanceForUser.lazyInitialize();
            batCics.add(personalBatCicInstanceForUser);
            mCreatedCicInstances.add(personalBatCicInstanceForUser);
        }
        return batCics;
    }


    public EcsBatCic getBatCic() {
        List<EcsBatCic> listOfBatCics = getAllBatCics();
        if(listOfBatCics.size() != 0) {
        int randIndex = new Random().nextInt(listOfBatCics.size());
            return listOfBatCics.get(randIndex);
        }
        throw new RuntimeException("No cic exists with the requested status");
    }

    /**
     * Retrieves the CIC host with specified service.
     *
     * @param service
     *            The service running on CIC
     *
     * @return EcsCic The CIC host with specified service
     *
     */

    public EcsCic getCic(CicService service) {
        mLogger.info(EcsAction.FINDING, "", EcsCic.class, String.format("CIC with %s", service.toString()));
        EcsCic cic_with_service = null;
        CrmService crmService = getRandomCic().getCrmService();
        String output = null;
        switch (service) {
        case RABBITMQ_MASTER:
            output = crmService.cicHostWithRabbitMaster();
            break;
        case ZABBIX_SERVER:
            output = crmService.cicHostWithZabbixServer();
            break;
        default:
            break;
        }
        for (EcsCic currentCic : mUniqueCics) {
            if (currentCic.getHostname().equals(output)) {
                cic_with_service = currentCic;
                break;
            }
        }
        mLogger.info(Verdict.FOUND, "", EcsCic.class,
                String.format("%s is the CIC with %s", cic_with_service, service.toString()));
        return cic_with_service;
    }

    /**
     * Returns the CIC with the requested status. status - the state of the
     * CIC(s)
     *
     * @return
     */
    public EcsCic getCic(Status status) {
        instantiateCics();
        for (EcsCic currentCic : mUniqueCics) {
            if (currentCic.getStatus() == status) {
                EcsCic cic = mCicFactory.create(mRootUser, mPassword, currentCic.getIpAddress(), currentCic.getPort());
                cic.lazyInitialize();
                mCreatedCicInstances.add(cic);
                return cic;
            }
        }
        throw new RuntimeException("No cic exists with the requested status");
    }

    /**
     * Returns the cic that has atlas image
     *
     * @return EcsCic
     */
    public EcsCic getCicWithAtlasImage() {
        instantiateCics();
        for (EcsCic cic : mUniqueCics) {
            if (cic.doesAtlasImageExist()) {
                mCicWithAtlasImage = cic;
                return cic;
            }
        }
        return null;
    }

    /**
     * Retrieves the process ID of specified service along with the CIC host where it is running.
     *
     * @param service - enum
     *            The service running on CIC
     *
     * @return Map<Integer, EcsCic> The process ID of service along with CIC host where it is running.
     *
     */
    public Map<Integer, EcsCic> getProcessIdOnCic(ServiceName service) {
        Map<Integer, EcsCic> processIdOnCic = new HashMap<Integer, EcsCic>();
        String processId = null;
        for (EcsCic cic : mUniqueCics) {
            processId = cic.getProcessId(service.processIdServiceNotation());
            if (processId == null) {
                continue;
            } else {
                Integer pid = new Integer(Integer.parseInt(processId));
                processIdOnCic.put(pid, cic);
            }
        }
        return processIdOnCic;
    }

    /**
     * Returns a random CIC. Use this when the test case doesn't care about
     * which CIC it's executing its commands to. This is one of the most
     * future-proof ways when writing a test-case since there is no assumption
     * on a specific CIC.
     *
     * @return a randomly selected EcsCic instance.
     */
    public EcsCic getRandomCic() {
        instantiateCics();
        int index = new Random().nextInt(mControllerIPs.size());
        EcsCic personalCicInstanceForUser = mCicFactory.create(mRootUser, mPassword,
                mControllerIPs.get(index).getIpAddress(), mControllerIPs.get(index).getPort());
        personalCicInstanceForUser.lazyInitialize();
        mCreatedCicInstances.add(personalCicInstanceForUser);
        return personalCicInstanceForUser;
    }

    /**
     * Method to check if the system time is synchronized on all cics
     * (The system time is said to be synchronized when the ntp offset value
     * on each cic lies between -50.0 and +50.0)
     *
     * @return  boolean
     */
    public boolean isCicTimeSynchronized() {
        boolean isSynchronized = false;
        int cicCount = 0;
        for (EcsCic cic : mUniqueCics) {
            Float offset = cic.getNtpOffset();
            if (offset > -50F && offset < 50F) {
                cicCount++;
            }
        }
        if (cicCount == mUniqueCics.size()) {
            isSynchronized = true;
        }
        return isSynchronized;
    }

    public int size() {
        if (mNumberOfCics != -1) {
            return mNumberOfCics;
        }
        getControllerIPs();
        return mNumberOfCics;
    }

    /**
     * Updates values of metering and event "time to live" in
     * /etc/ceilometer/ceilometer.conf file on all cics and
     * restarts ceilometer services.
     *
     * @param newValue - int - the new t"ime to live" value
     * @return boolean
     */
    public boolean updateTimeToLiveInCeilometerConf(int newValue) {
        instantiateCics();
        String[] ceilometerServices = new String[] { "ceilometer-agent-central", "ceilometer-agent-notification",
                "ceilometer-api", "ceilometer-collector", "ceilometer-polling" };
        List<String> ceilometerServceList = Arrays.asList(ceilometerServices);
        mLogger.info(EcsAction.CHANGING, EcsCic.class, "ceilometer.conf file on all cics");
        for (EcsCic cic : mUniqueCics) {
            cic.editFile("/etc/ceilometer/ceilometer.conf", "metering_time_to_live=172800",
                    "metering_time_to_live=" + newValue);
            cic.editFile("/etc/ceilometer/ceilometer.conf", "event_time_to_live=172800",
                    "event_time_to_live=" + newValue);
        }
        EcsCic cic = getRandomCic();
        cic.stopService("ceilometer-agent-central");
        for (EcsCic currentCic : mUniqueCics) {
            for (String service : ceilometerServceList) {
                currentCic.restartService(service);
            }
        }
        int pid = cic.startService("ceilometer-agent-central");
        if (pid == 0) {
            mLogger.warn("Failed to start service ceilometer-agent-central");
            return false;
        }
        mLogger.info(Verdict.CHANGED, EcsCic.class, "ceilometer.conf file on all cics");
        return true;
    }

    /**
     * Method to verify that all cics and services running on cics are online
     */
    public void waitAndVerifyAllCicsAndServicesOnline() {
        new LoopHelper<Boolean>(Timeout.PACEMAKER_STATUS_CHANGE,
                "could not verify that pacemaker services running on cic are up", Boolean.TRUE, () -> {
                    try {
                        CrmService crmService = getRandomCic().getCrmService();
                        return !crmService.areServicesOnCicStopped();
                    }  catch (EcsConnectionException e) {
                        mLogger.info(
                                "Could not establish connection to openstack, going to check again after 10 seconds");
                        return false;
                    }
                }).setIterationDelay(10).run();
        CrmService crmService = getRandomCic().getCrmService();
        if (crmService.areAllCicsCrmOnline()) {
            mLogger.info(Verdict.FOUND, EcsCic.class, "all cics are online");
        } else {
            throw new EcsOpenStackException("Few cics are not online");
        }
    }

    /**
     * This method execute waitAndVerifyRestart method paralelly on all cics using executors.
     * Throws exception if not successful.
     *
     * @param cicList
     */
    public void waitAndVerifyRestartCics(List<EcsCic> cicList) throws InterruptedException, ExecutionException {
        ParallelExecutionService service = new ParallelExecutionService();
        // collection that stores tasks along with their expected result(success/failure)
        Map<Runnable, Result> tasks = new HashMap<Runnable, Result>();
        // collection that stores tasks along with the cics being rebooted
        Map<Runnable, EcsCic> taskMap = new HashMap<Runnable, EcsCic>();
        for (EcsCic cic : cicList) {
            Runnable task = () -> {
                cic.waitAndVerifyRestart(Timeout.CIC_REBOOT);
            };
            taskMap.put(task, cic);
        }
        for (Runnable task : taskMap.keySet()) {
            tasks.put(task, Result.SUCCESS);
        }
        // execute tasks simultaneously
        service.executeTasks(tasks);
        String taskResult = null;
        for (Runnable task : taskMap.keySet()) {
            taskResult = service.waitAndGetTaskResult(task);
            if (taskResult == null) {
                mLogger.info(Verdict.VALIDATED, EcsCic.class,
                        String.format("%s is rebooted successfully", taskMap.get(task)));
            }
        }
    }

    /**
     * Waits for the pacemaker service to be up after it is killed
     *
     * @param - service - enum - The service that is killed
     */
    public void waitForServiceToBeUp(ServiceName service) {
        mLogger.info(EcsAction.WAITING, "", service.serviceName(), "is up after killing it, Timeout: "
                + Timeout.PACEMAKER_PROCESS_READY.getTimeoutInSeconds() + "seconds.");
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.PACEMAKER_PROCESS_READY,
                "could not verify that service is up", Boolean.TRUE, () -> {
                    boolean result = false;
                    if (service.serviceName().equals("cinder-volume")
                            || service.serviceName().equals("neutron-server")) {
                        if (getProcessIdOnCic(service).isEmpty()) {
                            return result;
                        }
                        Integer id = getProcessIdOnCic(service).keySet().iterator().next();
                        if (id != null) {
                            result = true;
                        }
                    } else {
                        if (getProcessIdOnCic(service).size() == mUniqueCics.size()
                                && !getProcessIdOnCic(service).containsKey(null)) {
                            result = true;
                        }
                    }
                    return result;
                });
        loopHelper.setIterationDelay(60);
        loopHelper.run();
        mLogger.info(Verdict.FOUND, "status of", service.serviceName(), " as up again after killing it");
    }
}

package com.jcat.cloud.fw.components.system.cee.services.crm;

import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;

/**
 * CRM Service class. Objectize CRM as a Java service
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-11-11 initial version
 * @author eelimei 2016-01-27 Add activeMark method
 * @author zdagjyo 2016-12-02 Add methods awk,cicHostWithRabbitMaster and
 *         getMasterRabbitMQ
 * @author zdagjyo 2016-12-05 Add methods
 *         areServicesOnCicStopped,getQuorumStatus,isQuorumDown and
 *         queryCrmResourceStatusWithFilter
 * @author zdagjyo 2016-12-06 Add method cicHostWithZabbixServer
 * @author zdagjyo 2016-02-06 Add enum ServiceName and methods areAllCicsCrmOnline,
 *         arePacemakerServicesUp, checkServiceStatusOnCics, getCorosyncStatus,
 *         getCrmResourceStatus, getFailCount, getPacemakerServicesStatus and isCrmServiceUp
 * @author zdagjyo 2017-05-05 Add method waitAndVerifyCrmServiceUp, made this class a subclass
 *         of GenericService class and modified method arePacemakerServicesUp to support single
 *         server case
 */
public class CrmService extends GenericService {

    public enum ServiceName {

        RabbitMQ("RabbitMQ", "p_rabbitmq-server", "p_rabbitmq-server", "beam.smp"), MySQL("MySQL", "p_mysqld",
                "p_mysql", "mysqld"), NeutronOpenvSwitch("neutron-openvswitch-agent",
                "p_neutron-plugin-openvswitch-agent", "p_neutron-plugin-openvswitch-agent", "neutron-openvswitch-agent"), CinderVolume(
                "cinder-volume", "p_cinder-volume", "p_cinder-volume", "cinder-volume"), NeutronDhcp(
                "neutron-dhcp-agent", "neutron-dhcp-agent", "p_neutron-dhcp-agent", "neutron-dhcp-agent"), WatchmenZabbix(
                "watchmen-zabbixendpoint", "p_watchmen-zabbixendpoint", "watchmen-zabbixendpoint",
                "watchmen-zabbixendpoint"), NeutronServer("neutron-server", "neutron-server", "neutron-server",
                "neutron-server"), Corosync("corosync", "corosync", "corosync", "corosync");

        private final String mServiceName;
        private final String mCrmServiceNotation;
        private final String mFailCountServiceNotation;
        private final String mProcessIdServiceNotation;

        ServiceName(String serviceName, String crmServiceNotation, String failCountServiceNotation,
                String processIdServiceNotation) {
            mServiceName = serviceName;
            mCrmServiceNotation = crmServiceNotation;
            mFailCountServiceNotation = failCountServiceNotation;
            mProcessIdServiceNotation = processIdServiceNotation;
        }

        public static ServiceName withName(String serviceName) {
            ServiceName[] services = ServiceName.values();
            for (ServiceName sevice : services) {
                if (sevice.serviceName().equals(serviceName)) {
                    return sevice;
                }
            }
            return null;
        }

        public String crmServiceNotation() {
            return mCrmServiceNotation;
        }

        public String failCountServiceNotation() {
            return mFailCountServiceNotation;
        }

        public String processIdServiceNotation() {
            return mProcessIdServiceNotation;
        }

        public String serviceName() {
            return mServiceName;
        }
    }

    private static final String COROSYNC_CMD = "corosync-cfgtool -s";
    private static final String CRM_RESOURCE_STATUS = "crm resource status ";
    private static final String FAILCOUNT_CMD = "crm_failcount -U %s -r %s";
    private static final String MASTER_RABBIT = "-A 1 master_p_rabbitmq-server|sed -n 2p";
    private static final String ZABBIX_SERVER = "zabbix";
    private final EcsLogger mLogger = EcsLogger.getLogger(CrmService.class);

    public CrmService(EcsSession sshSession, EcsCicList cicList) {
        super(sshSession, cicList);
    }

    private String awk(int digit) {
        return " | awk '{print $" + digit + "}'";
    }

    /**
     * Returns the status of corosync service
     *
     * @return - String - The status of corosync service
     */
    private String getCorosyncStatus() {
        return mSshSession.send(COROSYNC_CMD);
    }

    /**
     * Parse CRM status list with []
     *
     * @param text
     * @return
     */
    private String[] parseCrmList(String text) {
        String group = text.substring(text.indexOf("[") + 1, text.indexOf("]"));
        mLogger.debug("Groups:" + group);
        return org.apache.commons.lang.StringUtils.split(group, " ");
    }

    /**
     * Query Crm Resource status
     *
     * @param filters
     * @return
     */
    private String queryCrmResourceStatusWithFilter(String filters) {
        return mSshSession.send(CRM_RESOURCE_STATUS + filters);
    }

    /**
     * Get the hostname of the cic where active_mark is active
     *
     * @return cic hostname
     */
    public String activeMark() {
        String result = queryCrmStatusWithFilter(grep("active_mark"));
        return result.substring(result.indexOf("cic")).trim();
    }

    /**
     * Method to check if all cics are online
     *
     * @return  boolean
     */
    public boolean areAllCicsCrmOnline() {
        if (online().length == mNumberOfCics) {
            return true;
        }
        return false;
    }

    /**
     * Checks if all pacemaker services are up and running.
     *
     * @return - boolean
     */
    public boolean arePacemakerServicesUp() {
        String result = queryCrmStatusWithFilter("-rf");
        if(result.contains("Stopped") | result.contains("FAIL")) {
            return false;
        }
        else return true;
    }

    /**
     * Check if CIC services are stopped
     *
     * @return
     */
    public boolean areServicesOnCicStopped() {
        boolean servicesStopped = false;
        String result = queryCrmResourceStatusWithFilter(grep("Stopped"));
        if (result.contains("Stopped")) {
            servicesStopped = true;
        }
        return servicesStopped;
    }

    /**
     * @return hostname of CIC that hosts RabbitMQ master
     */
    public String cicHostWithRabbitMaster() {
        return queryCrmStatusWithFilter(grep(MASTER_RABBIT) + awk(3));
    }

    /**
     * @return hostname of CIC that hosts Zabbix Server
     */
    public String cicHostWithZabbixServer() {
        return queryCrmStatusWithFilter(grep(ZABBIX_SERVER) + grep("cic") + awk(4));
    }

    /**
     * Gets the crm status of a service.
     *
     * @param - ServiceName -enum - the service whose status is needed
     *
     * @return - String - the crm status of specified service
     */
    public String getCrmResourceStatus(ServiceName service) {
        return queryCrmResourceStatusWithFilter(service.crmServiceNotation());
    }

    /**
     * Gets the fail count for a service
     *
     * @param - service - enum - The service for which the fail count is to be checked
     *
     * @return - String - the fail count for the service
     */
    public String getFailCount(ServiceName service) {
        mLogger.info(EcsAction.FINDING, "", "fail count ", "for " + service.serviceName());
        String result = mSshSession.send(String.format(FAILCOUNT_CMD, mSshSession.getHostname(),
                service.failCountServiceNotation())
                + awk(3));
        mLogger.info(Verdict.FOUND, "fail count ", result.substring(6), "for " + service.serviceName());
        return result.substring(6);
    }

    /**
     * @return Master RabbitMQ on CIC
     */
    public String getMasterRabbitMQ() {
        return queryCrmStatusWithFilter(grep(MASTER_RABBIT));
    }

    /**
     * @return Quorum status
     */
    public String getQuorumStatus() {
        return queryCrmStatusWithFilter(grep("quorum"));
    }

    /**
     * Checks that the specified CRM  service is up and running
     *
     * @param - service - enum - The CRM service whose status is to be checked
     *
     * @return - boolean
     */
    public boolean isCrmServiceUp(ServiceName service) {
        boolean isUp = false;
        String result = null;
        mLogger.info(EcsAction.FINDING, "status of", service.serviceName(), " by pacemaker on a vCIC");
        if (service.equals(ServiceName.Corosync)) {
            result = getCorosyncStatus();
            if (result.contains("active with no faults")) {
                isUp = true;
            }
        } else {
            result = getCrmResourceStatus(service);
            if (!result.contains("NOT running")) {
                isUp = true;
            }
        }
        if (isUp) {
            mLogger.info(Verdict.FOUND, "status of", service.serviceName(), " as up and running");
            return true;
        } else {
            mLogger.warn(service.serviceName() + " is not running");
            return false;
        }
    }

    /**
     * checks if Quorum is down.
     *
     * @return boolean
     */
    public boolean isQuorumDown() {
        boolean quorumDown = false;
        String result = getQuorumStatus();
        if (result.contains("WITHOUT")) {
            quorumDown = true;
        }
        return quorumDown;
    }

    /**
     * Produce a list of CICs where mysql is running
     *
     * @return
     */
    public String[] mySqlStarted() {
        return parseCrmList(queryCrmStatusWithFilter(grep("p_mysql -A1") + grep("Started")));
    }

    /**
     * Waits for the specified crm service to be up and running
     */
    public void waitAndVerifyCrmServiceUp(ServiceName service) {
        new LoopHelper<Boolean>(Timeout.PACEMAKER_PROCESS_READY, "Was not able to verify that " + service + " is up",
                Boolean.TRUE, () -> {
                    return isCrmServiceUp(service);
                }).run();
    }
}

package com.jcat.cloud.fw.common.parameters;

/**
 * <p>
 * Time out collection class for LoopHelper.
 *
 * Time out is defined as the maximum amount of time that one function should wait for. Any time exceeds the time out
 * will be treated as an error or exception, thus will need to be handled in the test case.
 *
 * Please be aware that time out should not be used as a timer, which means the test logic is waiting for a certain
 * period of time. Time out should neither be used as expected execution time of a function.
 *
 * LoopHelper, TimeOut class are parts of the atomic actions of test executor. (will be introduced later)
 *
 *
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2014-07-31 initial version
 * @author ewafred 2014-08-28 added vrrp state change
 * @author eedsla 2014-09-02 added EXTREME_RESTORE_MASTER_STATE
 * @author eedirbo 2014-09-16 NEUTRON_SERVER_PROCESS_RESTART added for neutron-server process
 * @author eedann 2015-03-26 CONNECT_VM_VIA_SSH value changed to 150 (needed for ubuntu image)
 * @author eelimei 2015-06-12 Add PORT_READY
 * @author ehosmol 2015-06-18 Introduce some timeouts
 * @author eelimei 2016-01-11 Add CIC_REBOOT and CIC_REBOOT_MAX, BAT_SCRIPTS_RUN
 * @author eelimei 2016-01-26 Add PM_REPORTS_PRODUCE_CYCLE
 * @author zdagjyo 2016-12-23 Modify PING_VM
 * @author zdagjyo 2016-12-23 Add NOVA_SERVICE_STATUS_CHANGE
 * @author zdagjyo 2017-01-04 Add VNX_READY and VNX_STATE_CHANGE
 * @author zdagjyo 2017-23-01 Add VRF_STARTUP_DURATION
 * @author zdagjyo 2017-02-06 Add CINDER_SERVICE_STATE_CHANGE, PACEMAKER_PROCESS_READY, RABBIT_READY
 * @author zdagjyo 2017-02-17 Add NTP_SEREVR_DOWN and NTP_SEREVR_UP
 * @author zdagjyo 2017-03-08 Add KILL_LOAD_GENERATION
 * @author zdagjyo 2017-03-08 Add FILE_DOWNLOADED
 * @author zpralak 2017-04-27 Modify OMTOOL_MAX_EXECUTION_TIME
 * @author zdagjyo 2017-04-27 Add BACKUP_COMMAND_CEE and SERVICE_AVAILABLE
 * @author zdagjyo 2017-05-10 CONNECT_VM_VIA_SSH value changed to 300 (needed for VM with multiple vlans)
 * @author zpralak 2017-05-16 Add  LIBVIRT_SERVICE_READY
 * @author zpralak 2017-04-05 Add SERVER_STATUS_CHANGE_AFTER_HOST_REBOOT
 * @author zdagjyo 2017-06-05 Add LINK_STATE_CHANGE and ALARM_WAIT_TIMEOUT
 * @author zdagjyo 2017-08-28 Add COPY_FUEL_IMAGE and FUEL_STATE_CHANGE
 * @author zdagjyo 2017-10-10 Add INSTALL_ATLAS_VM and RESTORE_ATLAS_VM
 * @author zdagjyo 2017-11-15 Add CREATE_HEAT_STACK, DELETE_HEAT_STACK, DELETE_OVFT_APPLICATION,
 *         LIST_HEAT_STACK_RESOURCES and UPLOAD_HOT_FILE
 * @author zdagjyo 2017-12-01 Add CONCURRENT_OMTOOL_WAIT_TIME and STABILITY_TEST_12_HRS
 * @author zdagjyo 2017-12-06 Add LOAD_STABILIZE_TIME and MEASUREMENT_SCRIPT_TIME
 * @author zdagjyo 2017-12-27 Add MYSQL_READY
 * @author zdagjyo 2018-01-09 Add NOVA_SERVICE_STATE_CHANGE_AFTER_HOST_RESTART
 * @author zdagjyo 2018-01-25 Add HEALTH_CHECK
 * @author zdagjyo 2018-01-29 Add COMPRESS_FILE
 * @author zdagjyo 2018-02-20 Add BRIDGE_CONFIGURATION_STATUS_CHNAGE
 * @author zmousar 2108-01-11 Add ATLAS_SERVICE_STATE_CHANGE
 * @author zdagjyo 2018-12-28 Add RELEASE_DISK_SPACE
 */
public enum Timeout {
    /*
     * To edit the constants, you might need to change Eclipse's Java Formatter to keep line wraps.
     * Press "Ctrl+z"(Undo) immediately after saving will undo unnecessary line wrappings in Save Actions.
     * Suffix "_SR" should be used when a time out is clearly related to System Requirement.
     * If one constant that only applies to CEE but not to system test or any other test, please add "_CEE" as a suffix.
     * This also applies to other test groups.
     * Please keep in mind that these timeouts might block CI pipeline and increase execution time.
     */

    /**
     * Time to wait before checking the output of an asynchronous command execution.
     */
    ASYNC_CMD_EXECUTION(4),

    /**
     *  Time needed for service to active after atlas_vm reboot
     */
    ATLAS_SERVICE_STATE_CHANGE(20),

    /**
     * Generic timeout to check for alarms.
     */
    ALARM_WAIT_TIMEOUT(300),

    /**
     * Time needed for CEE backup pre-restore and restore commands to execute
     */
    BACKUP_COMMAND_CEE(1200),

    /**
     * CEE backup test
     */
    BACKUP_CREATE_CEE(220),

    /**
     *
     */
    BAT_SCRIPT_SINGLE(600),

    /**
     * Time to wait for a bat script to run to completion
     */
    BAT_SCRIPTS_RUN(1150),

    /**
     * Blade admin state change
     */
    BLADE_ADMIN_STATE(10),

    /**
     * Time to wait for the bridge configuration status in BSP node to be 100
     */
    BRIDGE_CONFIGURATION_STATUS_CHNAGE(300),

    CEILOMETER_RESTART(120),

    /**
     * Time it should take for a controller to change mode.
     */
    CIC_ONLINE_STATUS_CHANGE(60),

    /**
     * Time to wait for a cic to become online after reboot
     */
    CIC_REBOOT(370),

    /**
     * Time to wait for a cic to become online after reboot to see if the cic recovers at all, not only within time
     * limit.
     */
    CIC_REBOOT_MAX_LONG(1800),

    /**
     * Time to wait, until CIC session is recovered after it is dropped
     */
    CIC_SESSION_RECOVER(90),

    /**
     * Time needed for cinder services to be enabled.
     */
    CINDER_SERVICE_STATE_CHANGE(60),

    /**
     *  Maximum time needed to compress a file
     */
    COMPRESS_FILE(300),

    /**
     * Time needed before executing omtool on a cic when omtool is already running on another cic
     */
    CONCURRENT_OMTOOL_WAIT_TIME(30),

    /**
     * Maximum timeout to try and connect to a VM with SSH.
     */
    CONNECT_VM_VIA_SSH(300),

    /**
     * Time to connect to FUEL
     */
    CONNECTION_TIMEOUT(60),

    /**
     *  Time needed to copy fuel image from one compute to another
     */
    COPY_FUEL_IMAGE(1200),

    /**
     * Time needed to launch heat stack inside Atlas VM
     */
    CREATE_HEAT_STACK(360),

    /**
     * Time to wait for a backup of a database to be created by Mysqldump.
     */
    CREATE_MYSQL_BACKUP(10),

    /**
     * Postgres database restart
     */
    DATABASE_RESTART(70),

    /**
     * Time needed to delete heat stack inside Atlas VM
     */
    DELETE_HEAT_STACK(180),

    /**
     * Time needed to delete HOT application (inside Atlas VM)
     */
    DELETE_OVFT_APPLICATION(60),

    /**
     * DHCP agent up and running
     */
    DHCP_AGENT_READY(60),

    /**
     * Time it should take for a target extending EcsOperatingSystem to restart.
     */
    ECS_OS_RESTART(2400),

    /**
     * Extreme switch gets back MASTER role after reboot
     */
    EXTREME_RESTORE_MASTER_STATE(600),

    EXTREME_SWITCH_BONDING_STATE_CHANGE(10),

    FILE_CREATED(5),

    FILE_DOWNLOADED(60),

    /**
     * Time needed for fuel to change its state(up/down)
     */
    FUEL_STATE_CHANGE(60),

    /**
     * Time needed for healthcheck.py script to complete execution
     */
    HEALTH_CHECK(600),

    /**
     * Image in active status. e.g. after upload
     */
    IMAGE_READY(120),

    /**
     * Time needed to install Atlas VM.
     */
    INSTALL_ATLAS_VM(600),

    /**
     * Timeout to kill load generation program in a VM
     */
    KILL_LOAD_GENERATION(30),

    /**
     * Timeout to verify VM boot after killing libvirt.
     */
    LIBVIRT_SERVICE_READY(60),

    /**
     * time needed for SAN link to go up/down
     */
    LINK_STATE_CHANGE(180),

    /**
     * Time needed to list resources of a heat stack (inside Atlas VM)
     */
    LIST_HEAT_STACK_RESOURCES(60),

    /**
     * Time needed for load on node to get stabilized
     */
    LOAD_STABILIZE_TIME(4200),

    /**
     * Test Value to be used for methods involving LoopHelper.
     */
    LOOP_HELPER_UNIT_TEST(3),

    /**
     * Used in LoopHelper unit test
     */
    LOOPHELPER_UT_FALSE(0),

    LOOPHELPER_UT_TRUE(1),

    /**
     * Time to execute measurement script on node
     */
    MEASUREMENT_SCRIPT_TIME(2400),

    /**
     * Monit status for requested service is running
     */
    MONIT_READY(12),

    MONIT_READY_MULTIPLE(25),

    /**
     * Time to wait for MYSQL service to be up on all cics after a cic has been rebooted.
     */
    MYSQL_READY(60),

    /**
     * Time out for the created network to become active
     */
    NETWORK_CREATE(60),

    /**
     * Time out for network to get actually deleted
     */
    NETWORK_DELETE(60),

    /**
     * Time to wait, until the namespace can be found in ip netns list after the PORT=READY
     */
    NETWORK_NAMESPACE_READY(60),

    /**
     * Neutron service is up and running
     */
    NEUTRON_READY(900),

    /**
     * Neutron-Server process restart
     */
    NEUTRON_SERVER_PROCESS_RESTART(80),

    /**
     * Time it should take for a nova service to be up after host restart.
     */
    NOVA_SERVICE_STATE_CHANGE_AFTER_HOST_RESTART(900),

    /**
     * Nova-Service status change
     */
    NOVA_SERVICE_STATUS_CHANGE(240),

    /**
     * Timeout for NTP server to be down
     */
    NTP_SEREVR_DOWN(12000),

    /**
     * Timeout for NTP server to be up
     */
    NTP_SEREVR_UP(6600),

    /**
     * Max Time for OMtool execution
     */
    OMTOOL_MAX_EXECUTION_TIME(10800),

    /**
     * pacemaker process restart
     */
    PACEMAKER_PROCESS_READY(900),

    /**
     * Cic status change in Pacemaker between
     * online and offline
     */
    PACEMAKER_STATUS_CHANGE(660),

    /**
     * Pinging test for 10 minutes
     * Not recommended since it will
     * block test case for a long time
     */
    PING_TEST(600),

    /**
     * Maximum amount of time to try and ping a VM.
     */
    PING_VM(500),

    /**
     * The interval in which pm reports are created.
     */
    PM_REPORTS_PRODUCE_CYCLE(970),

    /**
     * Time out for port to get actually deleted
     */
    PORT_DELETE(60),

    /**
     * Port status change
     */
    PORT_READY(120),

    /**
     * Killing a process
     */
    PROCESS_KILL(120),

    PROCESS_KILL_MULTIPLE(60),

    /**
     * General timeout for restarting a process
     */
    PROCESS_READY(25),

    RABBITMQ_LISTENER_READY(10),

    RABBITMQ_MESSAGE_DELAY(2),

    /**
     * Timeout for RabbitMQ service to be active after restart
     */
    RABBIT_READY(60),

    /**
     * Time to get response FUEL
     */
    READ_TIMEOUT(60),

    /**
     * Time to try and re-establish an SSH connection to either FUEL, CIC or Compute Blade.
     */
    RECONNECT_SSH_CONNECTION(300),

    /**
     * Time needed for disk space to be released.
     */
    RELEASE_DISK_SPACE(60),

    /**
     * Time needed to restore Atlas VM from backup.
     */
    RESTORE_ATLAS_VM(180),

    /**
     * Time out for the created Router to become active
     */
    ROUTER_CREATE(60),

    /**
     * Time out for router to get actually deleted
     */
    ROUTER_DELETE(60),

    /**
     * Server created, not up and running yet
     */
    SERVER_CREATE(20),

    /**
     * Time out for server to get actually deleted
     */
    SERVER_DELETE(90),

    /**
     * Multiple Servers(VMs) deletion
     */
    SERVER_DELETE_MULTIPLE(120),

    /**
     * VM state from active to reboot
     */
    SERVER_IMMEDIATE_STATUS_CHANGE(5),

    /**
     * VM state change
     */
    SERVER_STATUS_CHANGE_AFTER_HOST_REBOOT(300),

    /**
     * Server(VM) boot up and running
     * Other VM than Cirros
     * Temporarily increased to 900 seconds due to failing reports from CI
     * Investigation is needed whether this time out is correct or not
     * Originally 240 seconds is used
     */
    SERVER_READY(240),

    /**
     * Timeout for Cirros VM to come up
     */
    SERVER_READY_CIRROS(60),

    /**
     * Server(VM) snapshotting
     */
    SERVER_SNAPSHOT_READY(240),

    /**
     * Server(VM) Stopping
     */
    SERVER_STOP(60),

    /**
     * Time to wait for openstack services(nova,neutron,etc.,) to be up after ECS backup is restored
     */
    SERVICE_AVAILABLE(660),

    /**
     * Generic timeout for SNMP alarm.
     */
    SNMP_ALARM_WAIT_TIMEOUT(300),

    /**
     * Time to wait for ssh disconnection detected by keep-alive-thread and
     * reconnection.
     */
    SSH_RECONNECT(90),

    /**
     * Time to wait before stopping stability scripts while performing 12 hrs stability test.
     */
    STABILITY_TEST_12_HRS(46800),

    /**
     * Time out for static route to get actually deleted
     */
    STATICROUTE_DELETE(60),

    /**
     * Time out for subnet to get actually deleted
     */
    SUBNET_DELETE(60),

    /**
     * Time for test app traffic to get stable
     */
    TESTAPP_AND_FIO_STABLE( 2 * 120),

    /**
     * Time for test app traffic to get stable
     */
    TESTAPP_STABLE( 2 * 60),

    /**
     * Time needed to upload HOT file to DC (inside Atlas VM)
     */
    UPLOAD_HOT_FILE(60),

    /**
     * Timeout needed to login to VM via virsh console.
     */
    VIRSH_CMD_EXECUTION(180),

    /**
     * Timeout for VNX to come up after reboot
     */
    VNX_READY(1100),

    /**
     * Timeout for VNX to go down during reboot
     */
    VNX_STATE_CHANGE(150),

    /**
     * Volume status change
     */
    VOLUME_READY(120 * 4),

    /**
     * Timeout for VRF to completely start up
     */
    VRF_STARTUP_DURATION(125),

    /**
     * Volume from snapshot status change
     */
    VOLUME_FROM_SNAPSHOT_READY(1200),

    /**
     * VolumeSnapshot status change
     */
    SNAPSHOT_FROM_VOLUME_READY(60),

    /**
     * Timeout for VLAN VRRP state change
     */
    VRRP_STATE_CHANGE(50),

    /**
     * Time to wait for watchdog to perform defined action when it is triggered in VM
     **/
    WATCHDOG_ACTION(60),

    /**
     * Time to wait for watchdog to start at boot
     */
    WATCHDOG_READY(600),

    /**
     * Test Value to be used for methods involving LoopHelper.
     */
    WATCHMEN_QUEUE(120),

    /**
     * Timeout for keystone related updates
     */
    KEYSTONE_CHANGE(60);

    private final int mTimeout;

    /**
     * Initiates each Enum with value given in the ()
     *
     * @param timeout time out value in seconds
     */
    private Timeout(int timeout) {
        mTimeout = timeout;
    }

    /**
     * Get the time out value from each enum
     *
     * @return time out value in milil seconds
     */
    public int getTimeoutInMilliSeconds() {
        return mTimeout * 1000;
    }

    /**
     * Get the time out value from each enum
     *
     * @return time out value in seconds
     */
    public int getTimeoutInSeconds() {
        return mTimeout;
    }
}

package com.jcat.cloud.fw.common.parameters;

/**
 * This class contains global default parameters used as TestNg input parameters.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehosmol 2013-06-27 initial version
 * @author eedmas 2013-07-09 added cirros user and password
 * @author eqinann 2013-09-04 added private constructor to hide default constructor
 * @author eedsla 2013-11-05 added paths to BAT image files in artifactory
 * @author eedmas 2013-12-05 added BAT user/psw/image names. Uncompressed images in Artifactory. m1.small flavor.
 * @author eedmas 2014-01-09 added MSP names and image paths
 * @author eqinann 2014-02-04 added bat subnet and port
 * @author enelwil 2014-04-11 added compute blade for slot3
 * @author eedelk 2014-05-28 added constant ANY_COMPUTE_HOST
 * @author eedann 2016-06-16 added parameter ROOT_USERNAME
 * @author eedsla 2014-08-05 added parameter FLAVOR_MEDIUM
 * @author ezhgyin 2014-10-02 move image source constants to VmImage class, remove unused Constants for BAT image names
 * @author ethssce 2014-11-10 exchanged the value "regionOne" with "RegionOne"
 * @author ethssce 2015-01-21 changed default flavor to medium
 * @author zpralak 2017-09-20 Added ARTIFACTORY_PASSWORD,ARTIFACTORY_URL and ARTIFACTORY_USERNAME
 */
public final class CommonParametersValues {

    /**
     * Paths to BAT image files in artifactory
     */
    public static final String BAT_JEOS_IMAGE_SOURCE = "proj-ecs-dev-local/se/ericsson/ecs/bat/bat-opensuse-12.1-jeos-x86_64-0.0.2.0.img.gz";
    /**
     * Host name to be applied
     */
    public static final String ANY_COMPUTE_HOST = "any";

    /**
     * Artifactory password to be used to download a file from artifactory
     */
    public static final String ARTIFACTORY_PASSWORD = "AP3xkqwBpBoWXFCpEDF5BJxFs3fwykD5mSHefH";

    /**
     * Artifactory url to be used to download a file from artifactory
     */
    public static final String ARTIFACTORY_URL = "https://arm.rnd.ki.sw.ericsson.se/artifactory/";

    /**
     * Artifactory user name to be used to download a file from artifactory
     */
    public static final String ARTIFACTORY_USERNAME = "cloudci";

    /**
     * name of neutron network, which is used by BAT
     */
    public static final String BAT_NETWORK_NAME = "BATnet";

    /**
     * BAT subnet name
     */
    public static final String BAT_SUBNET_NAME = "sBATnet";

    // TODO: this could be stored in Artifactory in defined format
    /**
     * default password for image CirrOS i386
     */
    public static final String CIRROSPASSWORD = "cubswin:)";

    // TODO: this could be stored in Artifactory in defined format
    /**
     * default user for image CirrOS i386
     */
    public static final String CIRROSUSERNAME = "cirros";

    /**
     * default user for bat image
     */
    public static final String BATUSERNAME = "batman";

    /**
     * default password for bat image
     */
    public static final String BATPASSWORD = "passw0rd";

    /**
     * Host name in Openstack
     */
    public static final String COMPUTE_HOST_SR0_SL1 = "p1-sr0-sl1";

    /**
     * Host name in Openstack
     */
    public static final String COMPUTE_HOST_SR0_SL3 = "p1-sr0-sl3";

    // TODO: this name should be more specific
    /**
     * Flavor name in open stack
     */
    public static final String FLAVORNAME = "m1.medium";

    /**
     * Image name in open stack
     */
    public static final String IMAGENAME = "CirrOS_i386";

    /**
     * Port for GEP board in slot 1
     */
    public static final int GEP_SLOT_1_PORT = 20001;

    /**
     * Bat Image name in open stack
     */
    public static final String BAT_IMAGENAME = "bat-ubuntu-12.04-server-64bit_pa2";

    // TODO: remove this, test cases should generate random names
    /**
     * Network name in Openstack
     */
    public static final String NETWORKNAME = "private";

    /**
     * Subnetwork name in Openstack
     */
    public static final String SUBNETNAME = "sPrivate";

    // TODO: remove this, should be auto generated
    /**
     * Server name in open stack
     */
    public static final String SERVERNAME = "jcatDemoServer";

    /**
     * Root user name
     */
    public static final String ROOT_USERNAME = "root";

    /**
     * Zone name in open stack
     */
    public static final String ZONENAME = "RegionOne";

    /**
     * Availability zone in Openstack
     */
    public static final String AVAILABILITYZONE = "nova";

    /**
     * Volume name in Openstack
     */
    public static final String VOLUMENAME = "jcatVolume";

    /**
     * Volume size in Openstack
     */
    public static final String VOLUMESIZE = "20";

    /**
     * Volume type in Openstack
     */
    public static final String VOLUMETYPE = "SATA";

    /**
     * Iteration delay in seconds
     */
    public static final int ITERATION_DELAY = 2;

    /**
     * Iteration delay in seconds
     */
    public static final int ITERATION_DELAY_10_SEC = 10;

    /**
     * Iteration delay in seconds
     */
    public static final int ITERATION_DELAY_30_SEC = 30;

    /**
     * Hide Utility Class Constructor:
     * Utility classes should not have a public or default constructor.
     */
    private CommonParametersValues() {

    }
}

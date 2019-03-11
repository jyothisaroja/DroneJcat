package com.jcat.cloud.fw.common.parameters;

/**
 * Group names to be used for <code>groups</code> parameter within @Test tag
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-08-27 initial version
 * @author eqinann 2013-09-04 added private constructor to hide default constructor
 * @author eedmas 2014-01-20 added SETUP group for Node setup
 * @author enelwil 2014-04-11 added robustness test suite groups
 * @author ethssce 2014-4-30 removed invalid TestNG groups: CAV, POC, STEAM, ICT
 * @author eedann 2014-05-26 added storage group
 * @author emarboc 2015-01-28 added performanceAndCharacteristics group
 * @author ezhgyin 2015-06-16 change group CI_GENERAL to SMOKE_TEST, add two new groups SANITY_TEST and FULL_LEGACY_TEST
 * @author ezhgyin 2015-09-14 add group INCUBATING
 * @author zpralak 2017-04-03 add group OMTOOL
 */
public final class TestNgGroups {
    /**
     * Add to this group when test belongs to fw integration test and should be run for each verification job
     */
    public static final String FW_VERIFICATION = "fw_verification";

    /**
     * Add to this group when test belongs to fw integration test but take longer time to run or have impact on the system so should be executed daily during the night
     */
    public static final String FW_NIGHTLY = "fw_nightly";

    /**
     * Add to this group when test belongs to CI Regression System I&V test group
     */
    public static final String SMOKE_TEST = "smoke_test";

    /**
     * Add to this group when test belongs to sanity test
     */
    public static final String SANITY_TEST = "sanity_test";

    /**
     * Add to this group when test belongs to legacy test
     */
    public static final String FULL_LEGACY_TEST = "full_legacy_test";

    /**
     * Add to this group when test belongs to FLOATINGIP test group
     */
    public static final String FLOATINGIP = "floatingIP";

    /**
     * Add to this group when test belongs to GLANCE test group
     */
    public static final String GLANCE = "glance";

    /**
     * Add to this group when test belongs to NETWORKS test group
     */
    public static final String NETWORKS = "networks";

    /**
     * Add this group when test is not implemented
     */
    public static final String NOTIMPLEMENTED = "notimplemented";

    /**
     * Add this group when test belongs to OMTOOL test group
     */
    public static final String OMTOOL = "omtool";

    /**
     * Add to this group when test belongs to PORTS test group
     */
    public static final String PORTS = "ports";

    /**
     * Add to this group when test belongs to QUOTAS test group
     */
    public static final String QUOTAS = "quotas";

    /**
     * Add to this group when test belongs to REGRESSION test group
     */
    public static final String REGRESSION = "regression";

    /**
     * Destroying data/vms or changing state - affecting other test cases
     */
    public static final String DESTRUCTIVE = "destructive";

    /**
     * Add to this group when test belongs to ROBUSTNESS test group
     */
    public static final String ROBUSTNESS = "robustness";

    /**
     * Add to this group when test belongs to ROBUSTNESS_WITHOUT_BAT_ANALYZER_FROM_START test group
     */
    public static final String ROBUSTNESS_WITHOUT_BAT_ANALYZER_FROM_START = "robustnessWithoutBatAnalyzerFromStart";

    /**
     * Add to this group when test belongs to ROUTER test group
     */
    public static final String ROUTER = "router";

    /**
     * Add to this group when test belongs to the node setup group
     */
    public static final String SETUP = "setup";

    /**
     * Add to this group when test belongs to STORAGE test group
     */
    public static final String STORAGE = "storage";

    /**
     * Add to this group when test belongs to SUBNET test group
     */
    public static final String SUBNET = "subnet";

    /**
     * Add to this group when test belongs to PERFORMANCE_AND_CHARACTERISTICS test group
     */
    public static final String PERFORMANCE_AND_CHARACTERISTICS = "performanceAndCharacteristics";

    /**
     * Add to this group when test needs to be run in the incubator Jenkins job before adding to CI pipeline
     */
    public static final String INCUBATING = "ci-incubating";

    /**
     * Hide Utility Class Constructor:
     * Utility classes should not have a public or default constructor.
     */
    private TestNgGroups() {

    }
}

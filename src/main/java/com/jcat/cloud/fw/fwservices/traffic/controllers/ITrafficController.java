package com.jcat.cloud.fw.fwservices.traffic.controllers;

import se.ericsson.jcat.fw.ng.traffic.exception.TrafficControlPluginException;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2015-08-03 initial version
 *
 */
public interface ITrafficController {

    /**
     * From TrafficPlugin, isAcceptableTraffic() will be called in at least 6 different places:
     * <ul>
     *  <li>beforeSuite afterInvocation</li>
     *  <li>beforeClass afterInvocation </li>
     *  <li>test case afterInvocation</li>
     *  <li>afterMethod afterInvocation</li>
     *  <li>afterClass afterInvocation</li>
     *  <li>afterSuite afterInvocation</li>
     * </ul>
     * For correct result of traffic status, when to enable traffic status check should be considered
     * while designing test case. Typical condition is that traffic statistics check is enabled in "beforeClass"and
     * "after* afterInvocation"s.
     *
     * Thus traffic statistics should be enabled before this method is called, otherwise return true directly.
     *
     * @return
     */
    public boolean isAcceptableTraffic() throws TrafficControlPluginException;

    /**
     * Prepares traffic for starting.
     * The method is used when traffic plugin exists on the SUT but not prepared for start
     * Typically startTraffic.ONSUITE will call this method to prepare traffic for start
     */
    public void prepareTrafficForStart() throws TrafficControlPluginException;

    /**
     * Reset traffic counting and/or start traffic for the coming test case
     *
     * This method should be called at startTraffic(ONTEST)
     *
     * In this method, traffic statistics should be enabled
     */
    public void restartTrafficStatistics() throws TrafficControlPluginException;

    /**
     * Establishing traffic. Plugin should call this method in startTraffic(ONSUTIE)
     * @throws TrafficControlPluginException
     */
    public void startTraffic() throws TrafficControlPluginException;

    /**
     * Finishing traffic. Plugin should call this method in stopTraffic(ONSUITE)
     */
    public void stopTraffic() throws TrafficControlPluginException;

    /**
     * Stop traffic counting and/or stop traffic for each test case
     *
     * This method should be called in stopTraffic(ONTEST), which is before AfterMethod invocation
     *
     * In this method, traffic statistics should be disabled
     */
    public void stopTrafficStatistics() throws TrafficControlPluginException;

}

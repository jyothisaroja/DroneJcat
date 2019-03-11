// Dropping Static Route Support (2016-09-27)
// Static Route is soon to be deprecated in Neutron, according to whom developed it.
// We have no test case using Static Route
// Therefore we are dropping Static Route support by commenting all the lines
//
// This file can be deleted in the future releases
//
//
// package com.jcat.cloud.fw.components.model.network;
//
// import org.openstack4j.api.Builders;
// import org.openstack4j.model.network.StaticRoute;
// import org.openstack4j.model.network.builder.StaticRouteBuilder;
// import com.jcat.cloud.fw.components.model.EcsComponent;
//
// /**
// * Class which represents an StaticRoute in ECS
// * <p>
// * <b>Copyright:</b> Copyright (c) 2014
// * </p>
// * <p>
// * <b>Company:</b> Ericsson
// * </p>
// *
// * @author ehosmol - Initial version
// *
// */
// public class EcsStaticRoute extends EcsComponent {
//
// public static class EcsStaticRouteConcreteBuilder {
//
// private StaticRouteBuilder mStaticRouteBuilder;
//
// EcsStaticRouteConcreteBuilder(String routerId, String destination, String nextHop) {
// this.mStaticRouteBuilder = Builders.staticRoute(routerId, destination, nextHop);
// }
//
// public EcsStaticRoute build() {
// return new EcsStaticRoute(mStaticRouteBuilder.build());
// }
//
// public EcsStaticRouteConcreteBuilder metric(int metric) {
// this.mStaticRouteBuilder = mStaticRouteBuilder.metric(metric);
// return (this);
// }
//
// public EcsStaticRouteConcreteBuilder tenantId(String tenantId) {
// this.mStaticRouteBuilder = mStaticRouteBuilder.tenantId(tenantId);
// return (this);
// }
// }
//
// private final StaticRoute mStaticRoute;
//
// private EcsStaticRoute(StaticRoute staticRoute) {
// this.mStaticRoute = staticRoute;
// }
//
// /**
// * Calls the concrete builder
// *
// * @param routerId
// * @param destination
// * @param nextHop
// *
// * @return {@link EcsStaticRouteConcreteBuilder} concrete builder
// */
// public static EcsStaticRouteConcreteBuilder builder(String routerId, String destination, String nextHop) {
// return new EcsStaticRouteConcreteBuilder(routerId, destination, nextHop);
// }
//
// /**
// * Getter method for StaticRoute instance
// *
// * @return the mStaticRoute
// */
// public StaticRoute get() {
// return mStaticRoute;
// }
//
// @Override
// public String toString() {
// return "Destination: " + mStaticRoute.getDestination() + " RouterId: " + mStaticRoute.getRouterId();
// }
//
// }

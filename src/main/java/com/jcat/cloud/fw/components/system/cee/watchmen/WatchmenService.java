package com.jcat.cloud.fw.components.system.cee.watchmen;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.openstack4j.api.client.CloudProvider;
import org.openstack4j.api.types.Facing;
import org.openstack4j.connectors.jersey2.HttpResponseImpl;
import org.openstack4j.core.transport.ClientConstants;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.UntrustedSSL;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.identity.v3.domain.KeystoneAuth;
import org.openstack4j.openstack.identity.v3.domain.KeystoneAuth.AuthScope;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.OSClientSession.OSClientSessionV3;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.components.model.EcsCloudService;
import com.jcat.cloud.fw.components.system.cee.openstack.keystone.KeystoneController;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;
import com.jcat.cloud.fw.infrastructure.os4j.EcsCustomContextResolver;
import com.jcat.cloud.fw.infrastructure.os4j.EcsKeystoneEndpointDeserializer;
import com.jcat.cloud.fw.infrastructure.resources.OpenStackResource;

/**
 * Class communicates with Watchmen Rest API
 *
 * @author ezhgyin
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo - 2018-12-26 - Modified class to support alarms in CEE9
 */
public class WatchmenService {

    /**
     * Class represents list of active alarms
     */
    private static class ActiveAlarms {
        List<EcsSnmpEvent> active_alarm_list;

        @SuppressWarnings("unused")
        public ActiveAlarms() {
        }

        @SuppressWarnings("unused")
        public ActiveAlarms(List<EcsSnmpEvent> alarmList) {
            this.active_alarm_list = alarmList;
        }

        public List<EcsSnmpEvent> getActive_alarm_list() {
            return active_alarm_list;
        }

        @SuppressWarnings("unused")
        public void setActive_alarm_list(List<EcsSnmpEvent> active_alarm_list) {
            this.active_alarm_list = active_alarm_list;
        }
    }

    /**
     * Class represents list of alarms in alarm history
     */
    private static class AlarmHistory {
        List<EcsSnmpEvent> alarm_history;

        @SuppressWarnings("unused")
        public AlarmHistory() {
        }

        @SuppressWarnings("unused")
        public AlarmHistory(List<EcsSnmpEvent> alarmList) {
            this.alarm_history = alarmList;
        }

        public List<EcsSnmpEvent> getAlarm_history() {
            return alarm_history;
        }

        @SuppressWarnings("unused")
        public void setAlarmHistory(List<EcsSnmpEvent> alarm_history) {
            this.alarm_history = alarm_history;
        }
    }

    private static final String XAUTH_HEADER = "X-Auth-Token";
    private static final String X_TENANT_NAME = "X-Tenant-Name";
    private static final String WATCHMEN_TENANT = "admin";
    // Jersey client for query
    private Client mJerseyClient = ClientBuilder.newBuilder().register(JacksonFeature.class)
            .register(JacksonJsonProvider.class).sslContext(UntrustedSSL.getSSLContext())
            .hostnameVerifier(new NoopHostnameVerifier()).build();

    // Jersey client for authentication
    private Client mJerseyClientAuth;

    private Logger mLogger = Logger.getLogger(WatchmenService.class);

    private OpenStackResource mOpenStackConfig;

    private String mToken = null;

    private String mWatchmenEndpoint;

    private Date mTokenExpiryDate;

    @Inject
    KeystoneController mKeystoneController;

    @Inject
    WatchmenService(OpenStackResource openStackResource) {
        mOpenStackConfig = openStackResource;
        EcsKeystoneEndpointDeserializer endpointDeserializer = new EcsKeystoneEndpointDeserializer(
                TestConfiguration.OsServiceUrlMode.ADMIN, openStackResource.getIpPublic(), true);
        mJerseyClientAuth = ClientBuilder.newBuilder().register(new EcsCustomContextResolver(endpointDeserializer))
                .sslContext(UntrustedSSL.getSSLContext()).hostnameVerifier(UntrustedSSL.getHostnameVerifier()).build();
    }

    /**
     * Authenticate with admin credentials
     */
    private void authenticate() {
        if (mToken == null) {
            String user = mOpenStackConfig.getUser();
            String password = mOpenStackConfig.getPassword();
            Identifier project = Identifier.byName("admin");
            Identifier domain = Identifier.byName("Default");
            KeystoneAuth auth = new KeystoneAuth(user, password, domain, AuthScope.project(project, domain));
            HttpRequest<KeystoneToken> v3request = HttpRequest.builder(KeystoneToken.class)
                    .endpoint(getKeystoneEndpoint()).method(HttpMethod.POST).path("/auth/tokens").entity(auth)
                    .config(Config.newConfig().withSSLVerificationDisabled()).build();

            WebTarget wr = mJerseyClientAuth.target(getKeystoneEndpoint()).path("/auth/tokens");

            Response response = null;
            response = wr.request(MediaType.APPLICATION_JSON).method(v3request.getMethod().name(),
                    Entity.entity(auth, ClientConstants.CONTENT_TYPE_JSON));
            HttpResponse httpResponse = HttpResponseImpl.wrap(response);

            KeystoneToken token = httpResponse.getEntity(KeystoneToken.class);
            token.setId(httpResponse.header(ClientConstants.HEADER_X_SUBJECT_TOKEN));
            token = token.applyContext(getKeystoneEndpoint(), auth);

            if (response.getStatus() != 201 || token == null) {
                throw new EcsOpenStackException("Could not get authentication token for watchmen from Keystone");
            }
            mToken = token.getId().toString();
            OSClientSessionV3 mOSClientSession= OSClientSessionV3.createSession(token, Facing.PUBLIC, CloudProvider.UNKNOWN,
                    Config.newConfig().withSSLVerificationDisabled());
            mTokenExpiryDate = mOSClientSession.getToken().getExpires();
        }
    }

    /**
     * Get https keystone endpoint
     *
     * @return
     */
    private String getKeystoneEndpoint() {
        return mOpenStackConfig.getEndpoint().replaceFirst("http:", "https:");
    }

    /**
     * Get https watchmen endpoint
     *
     * @return
     */
    private String getWatchmenEndpoint() {
        if (mWatchmenEndpoint == null) {
            URI uri = mKeystoneController.getEndpointForService(Facing.ADMIN, EcsCloudService.WATCHMEN);
            String host = uri.getHost();
            String newHost = mOpenStackConfig.getIpPublic();
            return uri.toString().replaceFirst(host, newHost).replaceFirst("http:", "https:");
        }
        throw new EcsOpenStackException("Could not get endpoint for watchmen service");
    }

    /**
     * Send get request
     *
     * @param path
     * @param type
     * @return
     */
    private <T> T sendGet(String path, GenericType<T> type) {
        Date tokenExpiryDate = new Date(mTokenExpiryDate.getTime() - 120 * 1000);
        if (new Date().after(tokenExpiryDate) ) {
            mToken = null;
            authenticate();
        }
        WebTarget wr = mJerseyClient.target(getWatchmenEndpoint()).path(path);
        try {
            return wr.request(MediaType.APPLICATION_JSON).header(XAUTH_HEADER, mToken)
                    .header(X_TENANT_NAME, WATCHMEN_TENANT).get(type);
        } catch (NotAuthorizedException e) {
            mToken = null;
            authenticate();
        }
        return wr.request(MediaType.APPLICATION_JSON).header(XAUTH_HEADER, mToken)
                .header(X_TENANT_NAME, WATCHMEN_TENANT).get(type);
    }

    /**
     * Get watchmen alarm history
     *
     * @return
     */
    public List<EcsSnmpEvent> getAlarmHistory() {
        authenticate();
        AlarmHistory alarms = sendGet("/alarm_history", new GenericType<AlarmHistory>() {
        });
        return alarms.getAlarm_history();
    }

    /**
     * Get watchmen active alarms
     *
     * @return
     */
    public List<EcsSnmpEvent> listActiveAlarms() {
        authenticate();
        ActiveAlarms alarms = sendGet("/active_alarm_list", new GenericType<ActiveAlarms>() {
        });
        return alarms.getActive_alarm_list();
    }
}

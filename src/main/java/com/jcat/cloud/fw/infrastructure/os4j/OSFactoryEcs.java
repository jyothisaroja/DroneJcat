package com.jcat.cloud.fw.infrastructure.os4j;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import org.openstack4j.openstack.internal.OSClientSession.OSClientSessionV3;

import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 * Class that performs keystone authentication.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2018
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2018-06-27 - Initial version
 */
public class OSFactoryEcs {

    private String mEndpoint;
    private final EcsKeystoneEndpointDeserializer endpointDeserializer;

    private static final class RequestFilter implements ClientRequestFilter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().remove(ClientConstants.HEADER_CONTENT_LANGUAGE);
            requestContext.getHeaders().remove(ClientConstants.HEADER_CONTENT_ENCODING);
        }
    }

    /**
     * Constructor that instantiate OSFactory
     *
     * @param endpoint
     * @param adminUser
     * @param adminPassword
     * @param adminTenant
     * @param useNonStrictSSL
     * @param osServiceUrlHandlingMode
     * @param ipPublic
     */
    public OSFactoryEcs(final String endpoint, final String adminUser, final String adminPassword,
            final String adminTenant, final TestConfiguration.OsServiceUrlMode osServiceUrlHandlingMode,
            final String ipPublic) {
        this.mEndpoint = endpoint;
        this.endpointDeserializer = new EcsKeystoneEndpointDeserializer(osServiceUrlHandlingMode, ipPublic,
                "admin".equals(adminTenant));
    }

    /**
     * Authenticate with specific perspective, domain, user and password
     *
     * @param perspective
     * @param user
     * @param password
     * @param domain
     * @return OSClientSession
     */
    public OSClientSessionV3 authenticate(Facing perspective, String user, String password, Identifier domain,
            Identifier project) {
        KeystoneAuth auth = new KeystoneAuth(user, password, domain, AuthScope.project(project, domain));
        HttpRequest<KeystoneToken> request = HttpRequest.builder(KeystoneToken.class)
                .endpoint(mEndpoint.replaceFirst("http:", "https:")).method(HttpMethod.POST).path("/auth/tokens")
                .entity(auth).config(Config.newConfig().withSSLVerificationDisabled()).build();
        Client jerseyClient = ClientBuilder.newBuilder().register(new EcsCustomContextResolver(endpointDeserializer))
                .register(new RequestFilter()).sslContext(UntrustedSSL.getSSLContext())
                .hostnameVerifier(UntrustedSSL.getHostnameVerifier()).build();

        WebTarget wr = jerseyClient.target(mEndpoint.replaceFirst("http:", "https:")).path("/auth/tokens");
        Response response = null;

        response = wr.request(MediaType.APPLICATION_JSON).method(request.getMethod().name(),
                Entity.entity(auth, ClientConstants.CONTENT_TYPE_JSON));

        HttpResponse httpResponse = HttpResponseImpl.wrap(response);
        KeystoneToken token = httpResponse.getEntity(KeystoneToken.class);

        token.setId(httpResponse.header(ClientConstants.HEADER_X_SUBJECT_TOKEN));
        token = token.applyContext(mEndpoint, auth);

        OSClientSessionV3 clientSession = OSClientSessionV3.createSession(token, perspective, CloudProvider.UNKNOWN,
                Config.newConfig().withSSLVerificationDisabled());
        return clientSession;
    }
}

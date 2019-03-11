package com.jcat.cloud.fw.infrastructure.os4j;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.identity.v3.builder.EndpointBuilder;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 * Just because we need to rewrite the service and admin endpoint URLs, since we cant
 * reach them the usual way from test-execution environment (Jenkins), so going through
 * LXC which has port-forwarding setup according to a pattern. Refactored out from
 * Endpoint{Admin}UrlSelector. Fail if the configuration is set to PUBLIC,
 * otherwise only changes/reroutes the publicURL/adminURL.
 *
 * Change/rewrite the URLs at deserialization, since an "endpoint" is meant
 * to be network accessible and not change often, most APIs/Java-bindings to
 * OpenStack make the endpoints immutable. Deserializer offers more control
 * and one-point-1of-change compared to doodling/patching about APIs on
 * higher levels.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eantcvi, Anto Cvitić
 * @author eqinann 2014-10-07 updated to ECS naming convention, added modifier for methods
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 * @author zdagjyo 2017-11-23 Modified method changeUri to support heat/atlas APIs
 * @author zdagjyo 2018-11-16 Modified class to support V3 token
 */
public class EcsKeystoneEndpointDeserializer extends JsonDeserializer<KeystoneEndpoint> {
    private final Logger mLogger = Logger.getLogger(EcsKeystoneEndpointDeserializer.class);
    private final TestConfiguration.OsServiceUrlMode mOsServiceUrlMode;
    private final String mOsConfigPublicIPAddress;
    private final boolean mIsAdmin;

    public EcsKeystoneEndpointDeserializer(final TestConfiguration.OsServiceUrlMode osServiceUrlMode,
            final String osConfigPublicIPAddress, final boolean isAdmin) {
        this.mOsServiceUrlMode = osServiceUrlMode;
        this.mOsConfigPublicIPAddress = osConfigPublicIPAddress;
        this.mIsAdmin = isAdmin;

        if (osServiceUrlMode == TestConfiguration.OsServiceUrlMode.PUBLIC) {
            throw new RuntimeException(TestConfiguration.OsServiceUrlMode.class.getName()
                    + " cannot be of a value PUBLIC at this point !");
        }
    }

    @Override
    public KeystoneEndpoint deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {

        final EndpointBuilder endpointBuilder = KeystoneEndpoint.builder();

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final String fieldName = jp.getCurrentName();
            final String fieldContent = jp.getText();
            if (fieldName == "id") {
                if (fieldContent != "id") {
                    endpointBuilder.id(fieldContent);
                }
            } else if (fieldName == "region") {
                if (fieldContent != "region") {
                    endpointBuilder.region(fieldContent);
                }
            } else if (fieldName == "region_id") {
                if (fieldContent != "region_id") {
                    endpointBuilder.regionId(fieldContent);
                }
            } else if (fieldName == "url") {
                if (fieldContent != "url") {
                    URL url = null;
                    // replace v1 or v2 versions for volume service with v3
                    if (fieldContent.contains("8776")) {
                        url = new URL(fieldContent.replace("public.controller.local", mOsConfigPublicIPAddress)
                                .replace("v1", "v3").replace("v2", "v3"));
                    } else {
                        url = new URL(fieldContent.replace("public.controller.local", mOsConfigPublicIPAddress));
                    }
                    endpointBuilder.url(url);
                }
            } else if (fieldName == "interface") {
                if (fieldContent != "interface") {
                    endpointBuilder.iface(Facing.value(fieldContent));
                }
            } else {
                throw new EcsOpenStackException(
                        "Failed to deserialize keystone endpoint, found an unexpected field in token, field name: "
                                + fieldName);
            }
        }
        return (KeystoneEndpoint) endpointBuilder.build();
    }
}

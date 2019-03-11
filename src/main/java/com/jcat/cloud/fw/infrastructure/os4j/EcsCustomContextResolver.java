package com.jcat.cloud.fw.infrastructure.os4j;

import javax.ws.rs.ext.ContextResolver;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2016
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2016- initial version
 * @author eqinann 2016-09-27 Uplift to Openstack4j 3.0.3 upstream
 *
 */
public class EcsCustomContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mMapper;
    private final ObjectMapper mRootMapper;
    private final SimpleModule mEcsModule;

    public EcsCustomContextResolver(EcsKeystoneEndpointDeserializer endpointDeserializer) {

        mEcsModule = new SimpleModule("ECSEndpointReWrite", new Version(1, 0, 0, null));

        mEcsModule.addDeserializer(KeystoneEndpoint.class, endpointDeserializer);

        mMapper = new ObjectMapper();
        mMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mMapper.enable(SerializationFeature.INDENT_OUTPUT);
        mMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mMapper.registerModule(mEcsModule);

        mRootMapper = new ObjectMapper();
        mRootMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mRootMapper.enable(SerializationFeature.INDENT_OUTPUT);
        mRootMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        mRootMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mRootMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mRootMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mRootMapper.registerModule(mEcsModule);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return type.getAnnotation(JsonRootName.class) == null ? mMapper : mRootMapper;
    }

}

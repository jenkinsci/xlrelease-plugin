package com.xebialabs.xlrelease.ci.util;


import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * Created by jdewinne on 9/3/14.
 */
public class ObjectMapperProvider  {
    ObjectMapper mapper;

    public ObjectMapperProvider(){
        mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT ,true);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}

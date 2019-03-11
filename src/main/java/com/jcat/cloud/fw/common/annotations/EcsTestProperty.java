package com.jcat.cloud.fw.common.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import com.jcat.cloud.fw.infrastructure.configurations.TestConfiguration;

/**
 * Annotation representing ECS property, to be used in {@link TestConfiguration}
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author esauali 2013-03-29 Initial version
 * @author ewubnhn 2013-04-17 Added errorMessage and validValues
 * @author emulign 2014-02-05 Added regexp checker
 */
@Retention(RUNTIME)
public @interface EcsTestProperty {
    String errorMessage() default "";

    boolean optional() default false;

    String regexp() default ".*";

    String[] validValues() default { "" };

    String value() default "";
}

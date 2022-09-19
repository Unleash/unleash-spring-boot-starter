package org.unleash.features.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Toggle {
    String name() default "";
    String alterBean() default "";
    FeatureVariant[] variants() default {};
}

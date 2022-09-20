package org.unleash.features.annotation;

import java.lang.annotation.*;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureVariants {
    String fallbackBean() default "";
    FeatureVariant[] variants() default {};
}

package org.leo.unleash.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Toggle {
    String name() default "";
    String alterBean() default "";
    ContextPath contextPath() default ContextPath.METHOD;
}

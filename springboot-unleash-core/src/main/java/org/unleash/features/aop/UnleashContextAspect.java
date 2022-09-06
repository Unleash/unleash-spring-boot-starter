package org.unleash.features.aop;

import io.getunleash.UnleashContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unleash.features.annotation.Context;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@Aspect
public class UnleashContextAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnleashContextAspect.class);

    @Around("execution(public * *(.., @org.unleash.features.annotation.Context (*), ..))")
    public Object aroundContextAnnotation(final ProceedingJoinPoint pjp) throws Throwable {
        try {
            final MethodSignature signature = (MethodSignature) pjp.getSignature();
            final String methodName = signature.getMethod().getName();
            final Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
            final Object[] params = pjp.getArgs();
            final Annotation[][] annotations;
            final UnleashContext.Builder contextBuilder = UnleashContext.builder();
            final UnleashContext unleashContext;

            annotations = pjp.getTarget().getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();

            int i = 0;

            for (final Object arg : params) {
                for (final Annotation annotation : annotations[i]) {
                    if (annotation.annotationType() == Context.class) {
                        final Context contextAnnotation = (Context) annotation;

                        if (arg != null) {
                            if (parameterTypes[i] != String.class) {
                                throw new IllegalArgumentException("Only string params can be annotated with Context annotation");
                            }

                            contextBuilder.addProperty(contextAnnotation.name(), (String) arg);
                        }
                    }
                }
                i++;
            }

            unleashContext = contextBuilder.build();

            UnleashContextThreadLocal.set(unleashContext);
            return pjp.proceed();
        } finally {
            UnleashContextThreadLocal.unset();
        }
    }
}

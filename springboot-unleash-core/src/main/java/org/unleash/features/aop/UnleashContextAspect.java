package org.unleash.features.aop;

import io.getunleash.UnleashContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.unleash.features.annotation.Context;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.IntStream;

@Aspect
public class UnleashContextAspect {
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

            IntStream.range(0, params.length)
                    .forEach(index -> Arrays.stream(annotations[index])
                            .forEach(annotation -> setUnleashContext(parameterTypes, params, contextBuilder, index, annotation)));

            unleashContext = contextBuilder.build();

            UnleashContextThreadLocal.set(unleashContext);

            return pjp.proceed();
        } finally {
            UnleashContextThreadLocal.unset();
        }
    }

    private void setUnleashContext(final Class<?>[] parameterTypes,
                                   final Object[] params,
                                   final UnleashContext.Builder contextBuilder,
                                   final int index,
                                   final Annotation annotation) {

        if (annotation.annotationType() == Context.class) {
            final Object arg = params[index];
            final Class<?> parameterType = parameterTypes[index];
            final Context contextAnnotation = (Context) annotation;

            if (arg != null) {
                if (parameterType != String.class) {
                    throw new IllegalArgumentException("Only string params can be annotated with Context annotation");
                }

                contextBuilder.addProperty(contextAnnotation.name(), (String) arg);
            }
        }
    }
}

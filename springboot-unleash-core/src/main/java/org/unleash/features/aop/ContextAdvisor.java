package org.unleash.features.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;
import org.unleash.features.annotation.Context;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.IntStream;


@Component("context.advisor")
public class ContextAdvisor implements MethodInterceptor {
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        try {
            final var params = invocation.getArguments();
            final var annotations = invocation.getMethod().getParameterAnnotations();
            final Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();

            IntStream.range(0, params.length)
                    .forEach(index -> Arrays.stream(annotations[index])
                            .forEach(annotation -> setUnleashContext(parameterTypes, params, index, annotation)));

            return invocation.proceed();
        } finally {
            UnleashContextThreadLocal.unset();
        }
    }

    private void setUnleashContext(final Class<?>[] parameterTypes,
                                   final Object[] params,
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

                UnleashContextThreadLocal.addContextProperty(contextAnnotation.name(), (String) arg);
            }
        }
    }
}

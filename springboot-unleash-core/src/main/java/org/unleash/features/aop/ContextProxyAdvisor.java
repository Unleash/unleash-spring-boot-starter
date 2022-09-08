package org.unleash.features.aop;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.stereotype.Component;
import org.unleash.features.annotation.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;


@Component("contextProxyAdvisor")
public class ContextProxyAdvisor extends AbstractAutoProxyCreator {
    public ContextProxyAdvisor() {
        setInterceptorNames(getBeanNameOfFeatureAdvisor());
    }

    private String getBeanNameOfFeatureAdvisor() {
        return ContextAdvisor.class.getAnnotation(Component.class).value();
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(final Class<?> beanClass, final String beanName, final TargetSource customTargetSource) throws BeansException {

        if(!beanClass.isInterface()) {
            final Method[] methods = beanClass.getMethods();
            final boolean isAnnotatedWithContext = Arrays.stream(methods).anyMatch(this::hasAnnotation);

            if(isAnnotatedWithContext) {
                return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
            }
        }
        return DO_NOT_PROXY;
    }

    private boolean hasAnnotation(final Method method) {
        final var params = method.getParameters();
        final var annotationArr = method.getParameterAnnotations();
        boolean present = false;

        for(int i = 0; i < params.length; i++) {
            final Annotation[] annotations = annotationArr[i];

            for (Annotation annotation : annotations) {
                if(annotation.annotationType() == Context.class) {
                    present = true;

                    if(params[i].getType() != String.class) {
                        throw new IllegalArgumentException("Only String type can be annotated with Context");
                    }
                }
            }
        }

        return present;
    }
}

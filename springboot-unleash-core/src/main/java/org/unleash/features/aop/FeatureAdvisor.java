package org.unleash.features.aop;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.unleash.features.annotation.ContextPath;
import org.unleash.features.annotation.Toggle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

@Component("feature.advisor")
public class FeatureAdvisor implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureAdvisor.class);
    private final Unleash unleash;
    private final ApplicationContext applicationContext;

    public FeatureAdvisor(Unleash unleash, ApplicationContext applicationContext) {
        this.unleash = unleash;
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(final MethodInvocation mi) throws Throwable {
        final Toggle toggle = getToggleAnnotation(mi);

        if(toggle != null) {
            final String alterBean = toggle.alterBean();
            final boolean usingAlterBean = StringUtils.hasText(alterBean);
            final boolean isFeatureToggled;

            if(alterBean.equals(getExecutedBeanName(mi))) {
                return mi.proceed();
            }

            isFeatureToggled = check(toggle, mi);

            if(isFeatureToggled) {
                if(usingAlterBean) {
                    return invokeAlterBean(mi, alterBean);
                } else {
                    throw new IllegalArgumentException("alterClass not yet supported");
                }
            }
        }

        return mi.proceed();
    }

    private Object invokeAlterBean(MethodInvocation mi, String alterBeanName) {
        final Method method = mi.getMethod();

        try {
            final Object alterBean = applicationContext.getBean(alterBeanName);

            return method.invoke(alterBean, mi.getArguments());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot invoke method %s on bean %s", method.getName(), alterBeanName), e);
        }
    }

    private boolean check(Toggle toggle, MethodInvocation mi) {
        final var featureId = toggle.name();
        final Optional<UnleashContext> contextOpt;

        if(toggle.contextPath() == ContextPath.METHOD) {
            final var arguments = mi.getArguments();

            contextOpt = Arrays.stream(arguments)
                    .filter(a -> a instanceof UnleashContext)
                    .map(a -> (UnleashContext) a)
                    .findFirst();
        } else {
            contextOpt = Optional.ofNullable(UnleashContextThreadLocal.get());
        }

        return contextOpt
                .map(context -> unleash.isEnabled(featureId, context))
                .orElse(unleash.isEnabled(featureId));
    }

    private String getExecutedBeanName(final MethodInvocation mi) {
        final Class<?> targetClass = getExecutedClass(mi);
        final Component component = targetClass.getAnnotation(Component.class);

        if(component != null) {
            return component.value();
        }

        final Service service = targetClass.getAnnotation(Service.class);

        if(service != null) {
            return service.value();
        }

        final Repository repository = targetClass.getAnnotation(Repository.class);

        if(repository != null) {
            return repository.value();
        }

        try {
            for(final String beanName: applicationContext.getBeanDefinitionNames()) {
                Object bean = applicationContext.getBean(beanName);

                if(AopUtils.isJdkDynamicProxy(bean)) {
                    bean = ((Advised)bean).getTargetSource().getTarget();
                }

                if(bean != null && bean.getClass().isAssignableFrom(targetClass)) {
                    return beanName;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Cannot read behind proxy target");
    }

    private Toggle getToggleAnnotation(MethodInvocation mi) {
        final Method method = mi.getMethod();
        final Class<?> currentInterface;
        final Class<?> currentImplementation;

        if(AnnotatedElementUtils.hasAnnotation(method, Toggle.class)) {
            return AnnotatedElementUtils.findMergedAnnotation(method, Toggle.class);
        }

        currentInterface = method.getDeclaringClass();

        if(AnnotatedElementUtils.hasAnnotation(currentInterface, Toggle.class)) {
            return AnnotatedElementUtils.findMergedAnnotation(currentInterface, Toggle.class);
        }

        currentImplementation = getExecutedClass(mi);

        if(AnnotatedElementUtils.hasAnnotation(currentImplementation, Toggle.class)) {
            return AnnotatedElementUtils.findMergedAnnotation(currentImplementation, Toggle.class);
        }

        return null;
    }

    private Class<?> getExecutedClass(final MethodInvocation mi) {
        final Class<?> executedClass;
        final Object ref = mi.getThis();

        if(ref != null) {
            executedClass = AopUtils.getTargetClass(ref);
        } else {
            executedClass = null;
        }

        if(executedClass == null) {
            throw new IllegalArgumentException("Static methods cannot feature feature flipping");
        }

        return executedClass;
    }
}

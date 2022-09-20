package org.unleash.features.aop;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.Variant;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.unleash.features.UnleashContextPreProcessor;
import org.unleash.features.annotation.FeatureVariant;
import org.unleash.features.annotation.FeatureVariants;
import org.unleash.features.annotation.Toggle;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component("feature.advisor")
public class FeatureAdvisor implements MethodInterceptor {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureAdvisor.class);
    private final Unleash unleash;
    private final ApplicationContext applicationContext;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    private List<UnleashContextPreProcessor> contextPreProcessors;

    public FeatureAdvisor(final Unleash unleash, final ApplicationContext applicationContext) {
        this.unleash = unleash;
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(@NotNull final MethodInvocation mi) throws Throwable {
        final Toggle toggle = getToggleAnnotation(mi);

        if(toggle != null) {
            final var variants = toggle.variants();
            final String alterBean = toggle.alterBean();
            final boolean usingAlterBean = StringUtils.hasText(alterBean);
            final String executedBeanName = getExecutedBeanName(mi);

            if ((variants == null || variants.variants().length == 0) && alterBean.equals(executedBeanName)) {
                return invokePreProcessors(() -> invokeMethodInvocation(mi));
            }

            return invokePreProcessors(() -> checkForFeatureToggle(mi, toggle, alterBean, usingAlterBean, executedBeanName));
        }

        return mi.proceed();
    }

    private Object checkForFeatureToggle(@NotNull final MethodInvocation mi,
                                         final Toggle toggle,
                                         final String alterBean,
                                         final boolean usingAlterBean,
                                         final String executedBeanName) {

        final Optional<UnleashContext> contextOpt;
        final var arguments = mi.getArguments();
        final boolean isFeatureToggled;
        final String variantBeanName;

        //If UnleashContext is explicitly passed as a parameter, it takes precedence over the annotation.
        contextOpt = Arrays.stream(arguments)
                .filter(a -> a instanceof UnleashContext)
                .map(a -> (UnleashContext) a)
                .findFirst();

        isFeatureToggled = check(toggle, contextOpt);

        if(isFeatureToggled) {
            variantBeanName = toggle.variants().variants().length > 0 ? getVariantBeanName(toggle.name(), toggle.variants(), contextOpt) : null;

            if(!StringUtils.hasText(variantBeanName) && toggle.variants().variants().length > 1) {
                LOGGER.warn("Variants present in toggle annotation, but no variants present for feature. Falling back to the default bean");
                return invokeMethodInvocation(mi);
            } if(usingAlterBean && !StringUtils.hasText(variantBeanName)) {
                return invokeAlterBean(mi, alterBean);
            } else if (StringUtils.hasText(variantBeanName)) {
                if(variantBeanName.equals(executedBeanName)) {
                    return invokeMethodInvocation(mi);
                }
                return invokeAlterBean(mi, variantBeanName);
            } else {
                throw new IllegalArgumentException("alterClass not yet supported");
            }
        } else {
            return invokeMethodInvocation(mi);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String getVariantBeanName(final String featureName, final FeatureVariants featureVariants, final Optional<UnleashContext> contextOpt) {
        final String alterBean;
        final Variant variant = contextOpt.map(context -> unleash.getVariant(featureName, context)).orElse(unleash.getVariant(featureName));
        final var featureVariantList = featureVariants.variants();

        if(variant != null && variant.isEnabled()) {
            final Optional<FeatureVariant> featureVariantOpt = Arrays.stream(featureVariantList).filter(featureVariant -> featureVariant.name().equals(variant.getName())).findAny();

            alterBean = featureVariantOpt.map(FeatureVariant::variantBean).orElseGet(() -> {
                LOGGER.warn(String.format("No bean defined for %s in the @FeatureVariants annotation. FallbackBean %s being used", variant.getName(), featureVariants.fallbackBean()));
                return featureVariants.fallbackBean();
            });

            if(!StringUtils.hasText(alterBean)) {
                throw new IllegalArgumentException(String.format("No bean or fallback defined for %s in the @FeatureVariants annotation", variant.getName()));
            }
        } else {
            alterBean = null;
        }

        return alterBean;
    }

    private Object invokeMethodInvocation(final MethodInvocation methodInvocation) {
        try {
            return methodInvocation.proceed();
        } catch (final RuntimeException ex) {
            throw ex;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokePreProcessors(final Supplier<Object> supplier) {
        Supplier<Object> returnValue = supplier;

        if(!CollectionUtils.isEmpty(contextPreProcessors)) {
            for (final UnleashContextPreProcessor contextPreProcessor : contextPreProcessors) {
                returnValue = contextPreProcessor.preProcess(supplier);
            }
        }

        return returnValue.get();
    }

    private Object invokeAlterBean(final MethodInvocation mi, final String alterBeanName) {
        final Method method = mi.getMethod();

        try {
            final Object alterBean = applicationContext.getBean(alterBeanName);

            return method.invoke(alterBean, mi.getArguments());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot invoke method %s on bean %s", method.getName(), alterBeanName), e);
        }
    }

    private boolean check(final Toggle toggle, final Optional<UnleashContext> contextOpt) {
        final var featureId = toggle.name();

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
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Cannot read behind proxy target");
    }

    private Toggle getToggleAnnotation(final MethodInvocation mi) {
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

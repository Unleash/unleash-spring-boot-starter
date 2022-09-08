package org.unleash.features.aop;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.unleash.features.annotation.Toggle;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("MissingSerialAnnotation")
@Component("feature.autoproxy")
public class FeatureProxyAdvisor extends AbstractAutoProxyCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProxyAdvisor.class);
    /** Serial number. */
    private static final long serialVersionUID = -364406999854610869L;

    /** Cache to avoid two-passes on same interfaces. */
    private final Map<String, Boolean> processedInterface = new HashMap<>();


    /**
     * Default constructor invoked by spring.
     */
    public FeatureProxyAdvisor() {
        // Define scanner for classes at startup
        setInterceptorNames(getBeanNameOfFeatureAdvisor());
    }

    /**
     * Read advisor bean name.
     *
     * @return
     *      id of {@link FeatureAdvisor} bean
     */
    private String getBeanNameOfFeatureAdvisor() {
        return FeatureAdvisor.class.getAnnotation(Component.class).value();
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(final Class<?> beanClass, @NotNull final String beanName, final TargetSource targetSource) {
        // Do not use any AOP here as still working with classes and not objects
        if (!beanClass.isInterface()) {
            final Class<?>[] interfaces;
            if (ClassUtils.isCglibProxyClass(beanClass)) {
                interfaces = beanClass.getSuperclass().getInterfaces();
            } else {
                interfaces = beanClass.getInterfaces();
            }
            if (interfaces != null) {
                for (Class<?> currentInterface: interfaces) {
                    final Object[] r = scanInterface(currentInterface);
                    if (r != null) {
                        return r;
                    }
                }
            }
        }
        return DO_NOT_PROXY;
    }

    /**
     * Add current annotated interface.
     *
     * @param currentInterface
     *          class to be scanned
     * @return list of proxies
     */
    private Object[] scanInterface(final Class<?> currentInterface) {
        final String currentInterfaceName = currentInterface.getCanonicalName();
        final Boolean isInterfaceFlipped;
        // Do not scan internals
        if (isJdkInterface(currentInterfaceName)) {
            return null;
        }
        // Never scanned, scan first time
        if (!processedInterface.containsKey(currentInterfaceName)) {
            return scanInterfaceForAnnotation(currentInterface, currentInterfaceName);
        }
        // Already scanned and flipped do not add interceptors
        isInterfaceFlipped = processedInterface.get(currentInterfaceName);
        return isInterfaceFlipped ? PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : null;
    }

    /**
     * Avoid JDK classes.
     *
     * @param currentInterfaceName Interface name. Checks if the interface is a JDK Dynamic Proxy
     * @return check result
     */
    private boolean isJdkInterface(final String currentInterfaceName) {
        return currentInterfaceName.startsWith("java.");
    }

    private Object[] scanInterfaceForAnnotation(final Class<?> currentInterface, final String currentInterfaceName) {
        // Interface never scan
        if (AnnotatedElementUtils.hasAnnotation(currentInterface, Toggle.class)) {
            processedInterface.put(currentInterfaceName, true);
            return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;

        } else {
            // not found on bean, check methods
            for (final Method method : currentInterface.getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(method, Toggle.class)) {
                    processedInterface.put(currentInterfaceName, true);
                    return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;
                }
            }
        }
        // annotation has not been found
        processedInterface.put(currentInterfaceName, false);
        return null;
    }
}

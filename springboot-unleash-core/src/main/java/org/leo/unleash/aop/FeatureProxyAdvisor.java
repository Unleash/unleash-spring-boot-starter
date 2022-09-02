package org.leo.unleash.aop;

import org.leo.unleash.annotation.Toggle;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component("features.autoproxy")
public class FeatureProxyAdvisor extends AbstractAutoProxyCreator {
    /** Serial number. */
    private static final long serialVersionUID = -364406999854610869L;

    /** Cache to avoid two-passes on same interfaces. */
    private final Map<String, Boolean> processedInterface = new HashMap<String, Boolean>();


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
    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
        // Do not used any AOP here as still working with classes and not objects
        if (!beanClass.isInterface()) {
            Class<?>[] interfaces;
            if (ClassUtils.isCglibProxyClass(beanClass)) {
                interfaces = beanClass.getSuperclass().getInterfaces();
            } else {
                interfaces = beanClass.getInterfaces();
            }
            if (interfaces != null) {
                for (Class<?> currentInterface: interfaces) {
                    Object[] r = scanInterface(currentInterface);
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
     * @return
     */
    private Object[] scanInterface(Class<?> currentInterface) {
        String currentInterfaceName = currentInterface.getCanonicalName();
        // Do not scan internals
        if (isJdkInterface(currentInterfaceName)) {
            return null;
        }
        // Never scanned, scan first time
        if (!processedInterface.containsKey(currentInterfaceName)) {
            return scanInterfaceForAnnotation(currentInterface, currentInterfaceName);
        }
        // Already scanned and flipped do not add interceptors
        Boolean isInterfaceFlipped = processedInterface.get(currentInterfaceName);
        return isInterfaceFlipped ? PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : null;
    }

    /**
     * Avoid JDK classes.
     *
     * @param currentInterfaceName
     * @return
     */
    private boolean isJdkInterface(String currentInterfaceName) {
        return currentInterfaceName.startsWith("java.");
    }

    private Object[] scanInterfaceForAnnotation(Class<?> currentInterface, String currentInterfaceName) {
        // Interface never scan
        if (AnnotatedElementUtils.hasAnnotation(currentInterface, Toggle.class)) {
            processedInterface.put(currentInterfaceName, true);
            return PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS;

        } else {
            // not found on bean, check methods
            for (Method method : currentInterface.getDeclaredMethods()) {
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

package org.unleash.features.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class UnleashContextAspect {
    @Around("execution(public * *(.., @org.unleash.features.annotation.Context (*), ..))")
    public Object aroundContextAnnotation(final ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}

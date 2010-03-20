package org.hyperic.hq.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Performs classpath scanning to lookup all implementations of
 * {@link HQAuthenticationProvider} for use by the @{link
 * {@link InternalAuthenticationProvider}
 * @author jhickey
 * 
 */
public class HQAuthenticationProviderLocator implements FactoryBean<Set<HQAuthenticationProvider>>,
    ApplicationContextAware {
    private ApplicationContext applicationContext;

    public Set<HQAuthenticationProvider> getObject() throws Exception {
        return new HashSet<HQAuthenticationProvider>(applicationContext.getBeansOfType(
            HQAuthenticationProvider.class).values());
    }

    public Class<?> getObjectType() {
        return Set.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}

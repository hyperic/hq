package org.hyperic.hq.security;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import java.util.Collections;

/**
 * Performs classpath scanning to lookup all implementations of
 * {@link HQAuthenticationProvider} for use by the @{link
 * {@link InternalAuthenticationProvider}
 * @author jhickey
 * 
 */
public class HQAuthenticationProviderLocator implements FactoryBean<List<HQAuthenticationProvider>>,
    ApplicationContextAware {
    private ApplicationContext applicationContext;

    public List<HQAuthenticationProvider> getObject() throws Exception {
        Map<String,HQAuthenticationProvider> providers= applicationContext.getBeansOfType(
            HQAuthenticationProvider.class);
        List<HQAuthenticationProvider> orderedProviders = new ArrayList<HQAuthenticationProvider>(providers.values());
        Collections.sort(orderedProviders, new OrderComparator());
        return orderedProviders;
    }

    public Class<?> getObjectType() {
        return List.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}

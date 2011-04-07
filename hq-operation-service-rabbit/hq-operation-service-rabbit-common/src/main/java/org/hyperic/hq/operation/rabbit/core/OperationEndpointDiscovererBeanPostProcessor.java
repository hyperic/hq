package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.OperationRegistry;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Helena Edelson
 */
public final class OperationEndpointDiscovererBeanPostProcessor implements BeanPostProcessor {

    private final OperationDiscoverer operationEndpointDiscoverer;

    private final OperationRegistry operationEndpointRegistry;

    /**
     * Creates an instance
     *
     * @param operationEndpointRegistry The registry
     */
    public OperationEndpointDiscovererBeanPostProcessor(OperationRegistry operationEndpointRegistry) {
        this(new AnnotatedOperationEndpointDiscoverer(), operationEndpointRegistry);
    }

    OperationEndpointDiscovererBeanPostProcessor(OperationDiscoverer operationEndpointDiscoverer,
        OperationRegistry operationEndpointRegistry) {
        this.operationEndpointDiscoverer = operationEndpointDiscoverer;
        this.operationEndpointRegistry = operationEndpointRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        try {
            this.operationEndpointDiscoverer.discover(bean, this.operationEndpointRegistry);
        } catch (Exception e) {
            throw new FatalBeanException("Unable to scan bean for annotations", e);
        }
        return bean;
    }

}

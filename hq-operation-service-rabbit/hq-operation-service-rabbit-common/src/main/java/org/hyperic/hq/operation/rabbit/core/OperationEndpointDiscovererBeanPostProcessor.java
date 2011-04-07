package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationEndpointDiscoverer;
import org.hyperic.hq.operation.OperationEndpointRegistry;
import org.hyperic.hq.operation.rabbit.mapping.AnnotatedOperationEndpointMapper;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Helena Edelson
 */
public final class OperationEndpointDiscovererBeanPostProcessor implements BeanPostProcessor {

    private final OperationEndpointDiscoverer operationEndpointDiscoverer;

    private final OperationEndpointRegistry operationEndpointRegistry;

    /**
     * Creates an instance
     *
     * @param operationEndpointRegistry The registry
     */
    public OperationEndpointDiscovererBeanPostProcessor(OperationEndpointRegistry operationEndpointRegistry) {
        this(new AnnotatedOperationEndpointMapper(), operationEndpointRegistry);
    }

    OperationEndpointDiscovererBeanPostProcessor(OperationEndpointDiscoverer operationEndpointDiscoverer,
        OperationEndpointRegistry operationEndpointRegistry) {
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

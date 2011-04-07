package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationDispatcherDiscoverer;
import org.hyperic.hq.operation.OperationDispatcherRegistry;
import org.hyperic.hq.operation.rabbit.mapping.AnnotatedOperationDispatcherMapper;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Helena Edelson
 */
public class OperationDispatcherDiscovererBeanPostProcessor implements BeanPostProcessor {

    private final OperationDispatcherDiscoverer operationDispatcherDiscoverer;

    private final OperationDispatcherRegistry operationDispatcherRegistry;

    /**
     * Creates an instance
     *
     * @param operationDispatcherRegistry The registry
     */
    public OperationDispatcherDiscovererBeanPostProcessor(OperationDispatcherRegistry operationDispatcherRegistry) {
        this(new AnnotatedOperationDispatcherMapper(), operationDispatcherRegistry);
    }

    OperationDispatcherDiscovererBeanPostProcessor(OperationDispatcherDiscoverer operationDispatcherDiscoverer,
        OperationDispatcherRegistry operationDispatcherRegistry) {
        this.operationDispatcherDiscoverer = operationDispatcherDiscoverer;
        this.operationDispatcherRegistry = operationDispatcherRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        try {
            this.operationDispatcherDiscoverer.discover(bean, this.operationDispatcherRegistry);
        } catch (Exception e) {
            throw new FatalBeanException("Unable to scan bean for annotations", e);
        }
        return bean;
    }
}

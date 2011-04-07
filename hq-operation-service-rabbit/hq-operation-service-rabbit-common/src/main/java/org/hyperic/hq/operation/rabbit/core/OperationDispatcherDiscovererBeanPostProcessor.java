package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.OperationRegistry;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Helena Edelson
 */
public class OperationDispatcherDiscovererBeanPostProcessor implements BeanPostProcessor {

    private final OperationDiscoverer operationDiscoverer;

    private final OperationRegistry operationRegistry;

    /**
     * Creates an instance
     *
     * @param operationRegistry The registry
     */
    public OperationDispatcherDiscovererBeanPostProcessor(OperationRegistry operationRegistry) {
        this(new AnnotatedOperationDispatcherDiscoverer(), operationRegistry);
    }

    OperationDispatcherDiscovererBeanPostProcessor(OperationDiscoverer operationDiscoverer,
        OperationRegistry operationRegistry) {
        this.operationDiscoverer = operationDiscoverer;
        this.operationRegistry = operationRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        try {
            this.operationDiscoverer.discover(bean, this.operationRegistry);
        } catch (Exception e) {
            throw new FatalBeanException("Unable to scan bean for annotations", e);
        }
        return bean;
    }
}

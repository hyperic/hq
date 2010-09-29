package org.hyperic.hq.plugin.rabbitmq.configure;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitUtils;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

import java.util.List;

/**
 * DynamicSpringBeanConfigurerTest
 * @author Helena Edelson
 */
@Ignore
public class DynamicSpringBeanConfigurerTest {

    protected static final Log logger = LogFactory.getLog(DynamicSpringBeanConfigurerTest.class);


    @Test
    public void createDynamicBeans() throws InterruptedException {
        ConfigResponse serviceConfig = new ConfigResponse();
        serviceConfig.setValue(DetectorConstants.HOST, "localhost");
        serviceConfig.setValue(DetectorConstants.USERNAME, "guest");
        serviceConfig.setValue(DetectorConstants.PASSWORD, "guest");
        serviceConfig.setValue(DetectorConstants.PLATFORM_TYPE, "Linux");

        serviceConfig.setValue(DetectorConstants.NODE_COOKIE_VALUE, RabbitUtils.configureCookie(serviceConfig));

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        GenericBeanDefinition connectionFactoryBean = ConnectionFactoryBeanDefinitionBuilder.build(serviceConfig);
        DynamicSpringBeanConfigurer.registerBean(connectionFactoryBean, (DefaultListableBeanFactory) ctx.getBeanFactory());
        SingleConnectionFactory cf = ctx.getBean(SingleConnectionFactory.class);
        Assert.notNull(cf, "SingleConnectionFactory must not be null.");

        GenericBeanDefinition adminBean = BrokerAdminBeanDefinitionBuilder.build(serviceConfig, connectionFactoryBean);
        DynamicSpringBeanConfigurer.registerBean("rabbitBrokerAdmin", adminBean, (DefaultListableBeanFactory) ctx.getBeanFactory());
        RabbitBrokerAdmin rabbitBrokerAdmin = ctx.getBean(RabbitBrokerAdmin.class);
        Assert.notNull(ctx.getBean("rabbitBrokerAdmin"), "rabbitBrokerAdmin must not be null.");

        ctx.register(RabbitConfiguration.class);
        ctx.refresh();
        RabbitGateway rabbitGateway = ctx.getBean(RabbitGateway.class);
        assertNotNull(rabbitGateway.getRabbitStatus());

        List<QueueInfo> queues = rabbitBrokerAdmin.getQueues();
        ctx.close();
    }

}

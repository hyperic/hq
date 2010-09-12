package org.hyperic.hq.plugin.rabbitmq.configure;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitBrokerGateway;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory; 
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;


import java.util.Arrays;

/**
 * DynamicSpringBeanConfigurerTest
 *
 * @author Helena Edelson
 */
public class DynamicSpringBeanConfigurerTest {

    protected static final Log logger = LogFactory.getLog(DynamicSpringBeanConfigurerTest.class);

     
    @Test
    public void createDynamicBeans() throws InterruptedException {
        ConfigResponse config = new ConfigResponse();
        config.setValue("host", "localhost");
        config.setValue("username", "guest");
        config.setValue("password", "guest");

        assertTrue(ConnectionFactoryBeanDefinitionBuilder.hasConfigValues(config));

        GenericBeanDefinition beanDefinition = ConnectionFactoryBeanDefinitionBuilder.build(config);

        ApplicationContext context = ApplicationContextCreator.createApplicationContext(beanDefinition);
        assertNotNull(context);
        assertTrue(Arrays.asList(context.getBeanDefinitionNames()).contains(generateBeanName(SingleConnectionFactory.class)));
        SingleConnectionFactory singleConnectionFactory = context.getBean(SingleConnectionFactory.class);
        assertNotNull(singleConnectionFactory);
        assertEquals(context.getBean(SingleConnectionFactory.class), singleConnectionFactory);

        ApplicationContext childContext = ApplicationContextCreator.getChildApplicationContext();
        RabbitBrokerAdmin rabbitBrokerAdmin = childContext.getBean(RabbitBrokerAdmin.class);
        assertNotNull(rabbitBrokerAdmin);
        assertTrue(rabbitBrokerAdmin.getStatus().getRunningApplications() != null);

        RabbitBrokerGateway gateway = childContext.getBean(RabbitBrokerGateway.class);
        assertNotNull(gateway);
    }

    @Test
    public void createAndRegisterBean(){
        /* ToDo
        DynamicSpringBeanConfigurer.createPropertyValues();
        DynamicSpringBeanConfigurer.registerBean();*/
    }

    private String generateBeanName(Class type) {
        String tmp = type.getSimpleName();
        String replace = String.valueOf(tmp.charAt(0));
        return StringUtils.replace(tmp, replace, replace.toLowerCase());
    }
}

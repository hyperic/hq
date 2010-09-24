package org.hyperic.hq.plugin.rabbitmq.configure;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * DynamicSpringBeanConfigurerTest
 *
 * @author Helena Edelson
 */
@Ignore
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
        assertNotNull(beanDefinition);
         
        RabbitGateway rabbitGateway = ApplicationContextCreator.createBeans(config);
        assertNotNull(rabbitGateway);

        //assertTrue(Arrays.asList(context.getBeanDefinitionNames()).contains(generateBeanName(SingleConnectionFactory.class)));
        RabbitStatus status = rabbitGateway.getRabbitStatus();
        assertTrue(status != null);
        logger.debug(status);
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

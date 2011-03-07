package org.hyperic.hq.amqp.distribution;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SingleConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Helena Edelson
 */
@Configuration
@ImportResource("classpath:META-INF/spring/rabbit.xml")
public class DefaultAmqpConfiguration {

    @Value("${prototype.connection.host}") private String host;

    @Bean
    public ConnectionFactory connectionFactory() {
        return new SingleConnectionFactory();
    }

    @Bean
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }
}

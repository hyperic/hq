package org.hyperic.hq.operation.rabbit.mapping;

import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.core.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.util.DiscoveryValidator;
import org.hyperic.hq.operation.rabbit.util.Routings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public abstract class AbstractRabbitOperationEntity extends DiscoveryValidator {

    protected final RabbitTemplate rabbitTemplate;

    protected final Routings routings;

    protected final Converter<Object,String> converter;

    /**
     * @param rabbitTemplate Used to listen for messages
     */
    public AbstractRabbitOperationEntity(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = new JsonMappingConverter();
        this.routings = new Routings();
    }
 
    public static final class MethodInvoker {

        private final Method method;

        private final Object instance;

        private final Converter<Object, String> converter;

        public MethodInvoker(Method method, Object instance, Converter<Object, String> converter) {
            this.method = method;
            this.instance = instance;
            this.converter = converter;
        }

        /**
         * @param context
         * @return the result of dispatching the method represented by this object
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public Object invoke(String context) throws IllegalAccessException, InvocationTargetException {
            Object data = this.converter.read(context, this.method.getParameterTypes()[0]);
            return this.method.invoke(this.instance, data);
        }

        @Override
        public String toString() {
            return new StringBuilder("method=").append(this.method)
                    .append(" instance=").append(this.instance).append(" converter").append(this.converter).toString();
        }
    }
    

}

package org.hyperic.hq.operation.rabbit.handler;

import org.hyperic.hq.operation.*;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.core.RabbitTemplate;
import org.hyperic.hq.operation.rabbit.mapping.Routings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
public class RabbitOperationHandler implements OperationHandlerRegistry, OperationHandler, OperationSupported {

    private static final long STARTUP_TIMEOUT = 5000;

    private final RabbitTemplate rabbitTemplate;

    private final Routings routings;

    /**
     * temporary: explicit for no spring.
     */
    private final Converter<Object,String> converter;

    private final Map<String, MethodInvoker> operationHandlers = new ConcurrentHashMap<String, MethodInvoker>();

    public RabbitOperationHandler(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, new JsonMappingConverter());
    }

    /**
     * @param rabbitTemplate Used to listen for messages
     */
    public RabbitOperationHandler(RabbitTemplate rabbitTemplate, Converter<Object,String> converter) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.routings = new Routings();
    }

    public void registerOperationHandler(String operationName, Method handlerMethod, Object instance) {
        this.operationHandlers.put(operationName, new MethodInvoker(handlerMethod, instance, this.converter));
    }

    public void handle(Envelope envelope) throws EnvelopeHandlingException {
        MethodInvoker methodInvoker = this.operationHandlers.get(envelope.getOperationName());

        try {
            Object response = methodInvoker.invoke(envelope.getContext());
            String data = this.converter.fromObject(response);

            //routings.getOperationResponse()
           // Envelope responseMessage = new Message(envelope.getOperationId(), "", data, null);
           // this.rabbitTemplate.send(envelope.getReplyTo(), responseMessage);

        }
        catch (IllegalAccessException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
        catch (InvocationTargetException e) {
            throw new EnvelopeHandlingException("Exception invoking operation handler method", e);
        }
        /*catch (IOException e) {
            throw new EnvelopeHandlingException("Exception sending response to operation", e);
        }*/
    }
 
    /**
     * Tests whether the operation has been registered with this handler
     * @param operation The operation name
     * @return Returns true if the operation name is a key in the handler's mapping, false if not
     */
    public boolean supports(Operation operation) {
       return this.operationHandlers.containsKey(operation.getOperationName());
    }

    private static final class MethodInvoker {

        private final Method handlerMethod;

        private final Object instance;

        private final Converter<Object, String> converter;

        MethodInvoker(Method handlerMethod, Object instance, Converter<Object, String> converter) {
            this.handlerMethod = handlerMethod;
            this.instance = instance;
            this.converter = converter;
        }

        Object invoke(String context) throws IllegalAccessException, InvocationTargetException {
            Object data = this.converter.toObject(context, this.handlerMethod.getParameterTypes()[0]);
            return this.handlerMethod.invoke(this.instance, data);
        }

        @Override
        public String toString() {
            return new StringBuilder("handlerMethod=").append(this.handlerMethod)
                    .append(" instance=").append(this.instance).append(" converter").append(this.converter).toString();
        }
    }
}
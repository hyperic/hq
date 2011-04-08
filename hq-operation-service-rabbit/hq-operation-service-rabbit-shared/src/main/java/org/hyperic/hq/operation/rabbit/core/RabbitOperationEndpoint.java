package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
public class RabbitOperationEndpoint extends AbstractRabbitOperationEntity implements OperationRegistry, Handler {
 
    private final Map<String, MethodInvoker> operationEndpoints = new ConcurrentHashMap<String, MethodInvoker>();

    /**
     * @param rabbitTemplate
     */
    public RabbitOperationEndpoint(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    } 

    /**
     * @param operationName The name operation name that this method can handle
     * @param endpointMethod The method
     * @param endpointCandidate The instance to invoke the method on
     */
    public void register(String operationName, Method endpointMethod, Object endpointCandidate) {
        if (!this.operationEndpoints.containsKey(operationName))
            this.operationEndpoints.put(operationName, new MethodInvoker(endpointMethod, endpointCandidate, this.converter));
    }

    /**
     * Handles incoming async messages
     * @param envelope The envelope to handle
     * @throws EnvelopeHandlingException
     */
    public void handle(Envelope envelope) throws EnvelopeHandlingException {
        if (!this.operationEndpoints.containsKey(envelope.getOperationName()))
           throw new OperationNotSupportedException(envelope.getOperationName());

        MethodInvoker methodInvoker = this.operationEndpoints.get(envelope.getOperationName());

        try {
            Object response = methodInvoker.invoke(envelope.getContext());
            String data = this.converter.write(response);

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
}
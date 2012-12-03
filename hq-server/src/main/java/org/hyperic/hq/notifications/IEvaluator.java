package org.hyperic.hq.notifications;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

public interface IEvaluator<T> {
    /**
     * produces messages with destinations where each message contains the collection of entities which passed a filter chain of a certain destination, 
     * and the destination it is supposed to be sent to
     * 
     * @param entities
     * @return
     * @throws JMSException
     */
    public List<ObjectMessage> evaluate(final List<T> entities) throws JMSException;
}

package org.hyperic.hq.common.util;

import java.io.Serializable;
/**
 * Responsible for publishing messages to a topic
 * @author jhickey
 *
 */

// TODO: Get rid of? Given that we now use JmsTemplate, its implementation add hardly any convenience
public interface MessagePublisher {

    /**
     *
     * @param name The topic name
     * @param sObj The message to publish
     */
    void publishMessage(String name, Serializable sObj);
}

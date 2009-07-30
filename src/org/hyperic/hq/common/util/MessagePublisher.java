package org.hyperic.hq.common.util;

import java.io.Serializable;
/**
 * Responsible for publishing messages to a topic
 * @author jhickey
 *
 */
public interface MessagePublisher {

    /**
     *
     * @param name The topic name
     * @param sObj The message to publish
     */
    void publishMessage(String name, Serializable sObj);
}

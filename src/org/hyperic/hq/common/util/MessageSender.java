package org.hyperic.hq.common.util;

import java.io.Serializable;

public interface MessageSender {

    void publishMessage(String name, Serializable sObj);
}

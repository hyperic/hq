 
package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;

 
public interface ConsumerCallback extends ChannelCallback<Object> {

    void stop();

}

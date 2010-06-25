/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.rabbitmq.objs;

import com.ericsson.otp.erlang.OtpErlangBinary;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangTuple;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.RabbitMQUtils;

/**
 *
 * @author administrator
 */
public class Queue {
    private static Log log = LogFactory.getLog(Queue.class);
    private String name,vHost;
    private Map props;

//    private static final String[] metrics={"messages_ready","messages_unacknowledged","messages_uncommitted","messages,acks_uncommitted","consumerstransactions","memory"};

    public Queue(OtpErlangList args) {
        log.debug(args);
        props=RabbitMQUtils.tupleListToMap(args);

        Map nameProps=RabbitMQUtils.tupleToMap((OtpErlangTuple) props.get("name"));
        name=RabbitMQUtils.toString((OtpErlangBinary)nameProps.get("queue"));
        vHost=RabbitMQUtils.toString((OtpErlangBinary)nameProps.get("resource"));
    }

    public String toString(){
        return "queue: [name='"+name+"' vHost='"+vHost+"']";
    }
    public String getFullName() {
        return vHost+name;
    }

    public String getName() {
        return name;
    }

    public String getVHost() {
        return vHost;
    }

    public Map getProperties() {
        return props;
    }

}

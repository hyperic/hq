/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.operation.rabbit.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Operation
public @interface OperationEndpoint {
  //@OperationEndpoint(exchange = "to.server", routingKey = "request.register", binding = "request.*", responseExchange = "to.agent", responseRoutingKey = "response.register")

    /**
    * The exchange to bind the listener queue to.
    * Messages sent to this exchange will be bound
    * and routed to the designated queue.
    * The queue name will be the method name (operation)
    * that this annotation decorates.
     */
    String exchange() default "";

    /**
     * The routing key that the sender will use to
     * send the message which this will receive.
     */
    String routingKey() default "";

    /**
     * The binding pattern to use when the above exchange
     * name and queue (method name this annotation decorates).
     * @return
     */
    String binding() default "";

    String queue() default "";

    /**
     * Where applicable, the response exchange to send a response to.
     */
    String responseExchange() default "";

    /**
     * Where applicable, the response routing key to use to send a response to. 
     */
    String responseRoutingKey() default "";

    String responseQueue() default "";
}

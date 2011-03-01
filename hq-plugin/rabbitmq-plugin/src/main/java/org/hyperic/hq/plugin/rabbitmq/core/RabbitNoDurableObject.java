/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

/**
 *
 * @author administrator
 */
public abstract class RabbitNoDurableObject implements RabbitObject {

    public final boolean isDurable() {
        return false;
    }

    public final void setDurable(boolean durable) {
        throw new RuntimeException(this.getClass().getName()+" is RabbitNoDurableObject");
    }
}

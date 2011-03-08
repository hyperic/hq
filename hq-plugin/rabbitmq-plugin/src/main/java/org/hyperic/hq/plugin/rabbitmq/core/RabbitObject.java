/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public interface RabbitObject {

    public String getName();

    public String getServiceType();

    public String getServiceName();

    public boolean isDurable();

    public ConfigResponse getProductConfig();

    public ConfigResponse getCustomProperties();
}

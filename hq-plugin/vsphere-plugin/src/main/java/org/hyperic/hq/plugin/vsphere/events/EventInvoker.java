package org.hyperic.hq.plugin.vsphere.events;

import com.vmware.vim25.mo.PropertyCollector;
 
/**
 * EventInvoker
 *
 * @author Helena Edelson
 */
public interface EventInvoker {

    Object invoke(PropertyCollector propertyCollector, EventHandler eventHandler, long duration);
 
}

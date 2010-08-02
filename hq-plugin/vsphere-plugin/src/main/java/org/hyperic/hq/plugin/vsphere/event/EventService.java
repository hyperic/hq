/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.vsphere.event;

import com.vmware.vim25.Event;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.mo.EventManager;
import com.vmware.vim25.mo.PropertyCollector;


/**
 * EventService
 *
 * @author Helena Edelson
 */
public interface EventService {

    void invoke(PropertyCollector propertyCollector, long duration) throws Exception;
    
    Event getLatestEvent(EventManager eventManager);

    Event[] queryEvents(EventManager eventManager, ManagedObjectReference mof) throws Exception;

    Event[] queryHistoricalEvents(EventManager eventManager, ManagedObjectReference rootFolder) throws Exception;

    void publishEvent(Event event, TaskInfo taskInfo, EventManager eventManager) throws Exception;
 
}

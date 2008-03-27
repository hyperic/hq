/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.events.server.session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.shared.EventObjectDeserializer;

/**
 * Deserializes an <code>AbstractEvent</code> referenced by a <code>TriggerEvent</code>. 
 * As part of the deserialization, links the <code>AbstractEvent</code> to the 
 * <code>TriggerEvent</code> by setting the <code>AbstractEvent</code> Id equal 
 * to the <code>TriggerEvent</code> Id.
 * 
 * This class is not thread safe.
 */
class EventToTriggerEventLinker implements EventObjectDeserializer {
    
    private final ObjectInput _eventObjectInput;
    private final TriggerEvent _triggerEvent;
    private AbstractEvent _event;
    
    /**
     * Creates an instance.
     *
     * @param triggerEvent The <code>TriggerEvent</code>.
     * @throws IOException if the <code>AbstractEvent</code> stream header cannot be read.
     */
    public EventToTriggerEventLinker(TriggerEvent triggerEvent) throws IOException {
        ByteArrayInputStream istream = 
            new ByteArrayInputStream(triggerEvent.getEventObject());
        _eventObjectInput = new ObjectInputStream(istream);
        _triggerEvent = triggerEvent;
    }
    
    /**
     * @see org.hyperic.hq.events.shared.EventObjectDeserializer#deserializeEventObject()
     */
    public AbstractEvent deserializeEventObject() 
        throws ClassNotFoundException, IOException {
        
        if (_event == null) {
            _event = (AbstractEvent) _eventObjectInput.readObject();
            _event.setId(_triggerEvent.getId());            
        }
        
        return _event;
    }

}

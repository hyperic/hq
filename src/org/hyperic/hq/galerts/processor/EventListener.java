package org.hyperic.hq.galerts.processor;

import java.util.List;

import org.hyperic.hq.zevents.ZeventListener;

class EventListener 
    implements ZeventListener
{
    private final GalertProcessor _aProc;
    
    EventListener(GalertProcessor aProc) {
        _aProc = aProc;
    }
    
    public void processEvents(List events) {
        _aProc.processEvents(events);
    }
}

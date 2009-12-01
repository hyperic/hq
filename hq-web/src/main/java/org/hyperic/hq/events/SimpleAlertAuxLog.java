package org.hyperic.hq.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleAlertAuxLog 
    implements AlertAuxLog
{
    private String _descr;
    private List   _children = new ArrayList();
    private long   _timestamp;
    
    public SimpleAlertAuxLog(String descr, long timestamp){
        _descr     = descr;
        _timestamp = timestamp;
    }
    
    public long getTimestamp() {
        return _timestamp;
    }

    public void addChild(AlertAuxLog child) {
        _children.add(child);
    }

    public List getChildren() {
        return Collections.unmodifiableList(_children);
    }

    public String getDescription() {
        return _descr;
    }

    public AlertAuxLogProvider getProvider() {
        return null;
    }

    public String getURL() {
        return null;
    }
}

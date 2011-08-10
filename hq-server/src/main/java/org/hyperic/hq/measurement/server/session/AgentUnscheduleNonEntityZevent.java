package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.Collections;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class AgentUnscheduleNonEntityZevent extends Zevent {

    private final String agentToken;
    private final Collection<AppdefEntityID> aeids;
    
    @SuppressWarnings("serial")
    public AgentUnscheduleNonEntityZevent(String agentToken, Collection<AppdefEntityID> aeids) {
        super(new ZeventSourceId() {}, new ZeventPayload() {});
        this.agentToken = agentToken;
        this.aeids = Collections.unmodifiableCollection(aeids);
    }
    
    public Collection<AppdefEntityID> getAppdefEntities() {
        return aeids;
    }

    public String getAgentToken() {
        return agentToken;
    }

}

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class AgentUnscheduleNonEntityZevent extends Zevent {
    
    private static class AgentUnscheduleNonEntityZeventSource implements ZeventSourceId {
        private static final long serialVersionUID = 1983501797805394163L;
        private AgentUnscheduleNonEntityZeventSource() { }
    }
    
    private static class AgentUnscheduleNonEntityZeventPayload implements ZeventPayload {
        private Collection<AppdefEntityID> aeids;
        private String agentToken;
        private AgentUnscheduleNonEntityZeventPayload(String agentToken, Collection<AppdefEntityID> aeids) {
            this.agentToken = agentToken;
            this.aeids = aeids;
        }
        private Collection<AppdefEntityID> getIds() {
            return aeids;
        }
        private String getAgentToken() {
            return agentToken;
        }
    }
    
    public AgentUnscheduleNonEntityZevent(String agentToken, Collection<AppdefEntityID> aeids) {
        super(new AgentUnscheduleNonEntityZeventSource(),
            new AgentUnscheduleNonEntityZeventPayload(agentToken, aeids));
    }
    
    public Collection<AppdefEntityID> getAppdefEntities() {
        return ((AgentUnscheduleNonEntityZeventPayload)getPayload()).getIds();
    }

    public String getAgentToken() {
        return ((AgentUnscheduleNonEntityZeventPayload)getPayload()).getAgentToken();
    }

}

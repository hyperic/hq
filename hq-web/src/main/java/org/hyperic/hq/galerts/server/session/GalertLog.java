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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.server.session.Action;

public class GalertLog
    extends PersistedObject 
    implements AlertInterface
{     
    public static int MAX_SHORT_REASON = 256;
    public static int MAX_LONG_REASON  = 2048;

    private boolean            _fixed;
    private GalertDef          _def;
    private long               _timestamp;
    private String             _shortReason;
    private String             _longReason;
    private GalertDefPartition _partition;
    private Collection         _actionLog = new ArrayList();
    private List               _auxLogs = new ArrayList();
    private Long               _stateId;
    private Long               _ackedBy;
    
    protected GalertLog() {}
    
    GalertLog(GalertDef def, ExecutionReason reason, long timestamp) {
        if (reason.getShortReason().length() > MAX_SHORT_REASON ||
            reason.getLongReason().length() > MAX_LONG_REASON)
        {
            throw new IllegalArgumentException("Reason is too long");
        }
        
        _def         = def;
        _timestamp   = timestamp;
        _shortReason = reason.getShortReason();
        _longReason  = reason.getLongReason();
        _partition   = reason.getPartition();
    }

    GalertActionLog createActionLog(String detail, Action action, 
                                    AuthzSubject subject) 
    {
        GalertActionLog res = new GalertActionLog(this, detail, action, 
                                                  subject);
        
        _actionLog.add(res);
        return res;
    }
    
    public PerformsEscalations getDefinition() {
        return getAlertDef();
    }

    public GalertDef getAlertDef() {
        return _def;
    }
    
    protected void setAlertDef(GalertDef def) {
        _def = def;
    }

    public AlertDefinitionInterface getAlertDefinitionInterface() {
        return getAlertDef();
    }

    public long getTimestamp() {
        return _timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public String getShortReason() {
        return _shortReason;
    }
    
    protected void setShortReason(String txt) {
        _shortReason = txt;
    }
    
    public String getLongReason() {
        return _longReason;
    }
    
    protected void setLongReason(String txt) {
        _longReason = txt;
    }
    
    protected void setPartitionEnum(int code) {
        _partition = GalertDefPartition.findByCode(code);
    }
    
    protected int getPartitionEnum() {
        return _partition.getCode();
    }
    
    protected void setActionLogBag(Collection actionLog) {
        _actionLog = actionLog;
    }

    protected Collection getActionLogBag() {
        return _actionLog;
    }

    public Collection getActionLog() {
        return Collections.unmodifiableCollection(_actionLog);
    }

    protected void setAuxLogBag(List auxLogs) {
        _auxLogs = auxLogs;
    }

    protected List getAuxLogBag() {
        return _auxLogs;
    }

    /**
     * Gets the {@link GalertAuxLog}s associated with this alert.
     */
    public List getAuxLogs() {
        return Collections.unmodifiableList(getAuxLogBag());
    }
    
    GalertAuxLog addAuxLog(AlertAuxLog auxLog, GalertAuxLog parent) { 
        GalertAuxLog res = new GalertAuxLog(this, auxLog, parent); 
        
        getAuxLogBag().add(res);
        return res;
    }

    public boolean isFixed() {
        return _fixed;
    }

    protected void setFixed(boolean fixed) {
        _fixed = fixed;
    }
    
    protected void setAckedBy(Long ackedBy) {
        _ackedBy = ackedBy;
    }
    
    protected Long getAckedBy() { 
        return _ackedBy;
    }
    
    protected void setStateId(Long stateId) {
        _stateId = stateId;
    }
    
    protected Long getStateId() {
        return _stateId;
    }
    
    public boolean isAcknowledged() {
        return getAckedBy() != null;
    }

    public boolean hasEscalationState() {
        return getStateId() != null;
    }

    public boolean isAcknowledgeable() {
        return getStateId() != null && getAckedBy() == null;
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + (int)getTimestamp();
        hash = hash * 31 + getShortReason().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof GalertLog == false)
            return false;
        
        GalertLog oe = (GalertLog)o;

        return oe.getTimestamp() == getTimestamp() && 
               oe.getShortReason().equals(getShortReason());
    }
}

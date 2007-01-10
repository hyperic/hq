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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;

public class GalertLog
    extends PersistedObject 
    implements AlertInterface, Escalatable
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

    public ExecutionReason getExecutionReason() {
        return new ExecutionReason(getShortReason(), getLongReason(),
                                   _partition);
    }

    public boolean isFixed() {
        return _fixed;
    }

    protected void setFixed(boolean fixed) {
        _fixed = fixed;
    }

    public AlertInterface getAlertInfo() {
        return this;
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

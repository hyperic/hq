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

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.Action;

public class GalertActionLog  
    extends PersistedObject
{
    private String       _detail;
    private GalertLog    _galertLog;
    private Action       _action;
    private AuthzSubject _subject;
    private long         _timeStamp;
    
    protected GalertActionLog() {
    }
   
    GalertActionLog(GalertLog alert, String detail, Action action,
                    AuthzSubject subject) 
    {
        _detail    = detail;
        _galertLog = alert;
        _action    = action;
        _subject   = subject;
        _timeStamp = System.currentTimeMillis();
    }
    
    public String getDetail() {
        return _detail;
    }
    
    protected void setDetail(String detail) {
        _detail = detail;
    }
    
    public GalertLog getGalertLog() {
        return _galertLog;
    }
    
    protected void setGalertLog(GalertLog alert) {
        _galertLog = alert;
    }
    
    public Action getAction() {
        return _action;
    }
    
    protected void setAction(Action action) {
        _action = action;
    }
    
    public AuthzSubject getSubject() {
        return _subject;
    }
    
    protected void setSubject(AuthzSubject subject) {
        _subject = subject;
    }

    protected int getAlertTypeEnum() {
        return GalertEscalationAlertType.GALERT.getCode();
    }
    
    protected void setAlertTypeEnum(int v) {
        // Do nothing
    }
    
    public EscalationAlertType getAlertType() {
        return GalertEscalationAlertType.GALERT;
    }

    public long getTimeStamp() {
        return _timeStamp;
    }
    
    protected void setTimeStamp(long stamp) {
        _timeStamp = stamp;
    }
}

/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2008], Hyperic, Inc. 
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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;

@Entity
@Table(name="EAM_ALERT_ACTION_LOG")
public class AlertActionLog  implements Serializable
{
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="DETAIL",nullable=false,length=500)
    private String              detail;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ALERT_ID")
    @Index(name="ALERT_ACTION_LOG_IDX")
    private Alert               alert;
    
    @SuppressWarnings("unused")
    @Column(name="ALERT_TYPE",nullable=false)
    private int alertTypeEnum;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ACTION_ID")
    @Index(name="ALERT_ACTION_ID_IDX")
    private Action              action;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="SUBJECT_ID")
    @Index(name="ALERT_ACTION_SUBJ_ID_IDX")
    private AuthzSubject        subject;
    
    @Column(name="TIMESTAMP",nullable=false)
    private long                timeStamp;
    
    protected AlertActionLog() {
    }
   
    public AlertActionLog(Alert alert, String detail, Action action,
                             AuthzSubject subject) 
    {
        this.alert     = alert;
        this.action    = action;
        this.subject   = subject;
        timeStamp = System.currentTimeMillis();
        setDetail(detail);
    }
    
    
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDetail() {
        return detail;
    }
    
    protected void setDetail(String detail) {
        if (detail != null && detail.length() > 500) {
            this.detail = detail.substring(0, 499);
        }
        else if (detail == null || detail.length() == 0) {
            // detail cannot be null and oracle treats empty strings as null
            this.detail = " ";
        }
        else {
            this.detail = detail;
        }
    }
    
    protected Alert getAlert() {
        return alert;
    }
    
    protected void setAlert(Alert alert) {
        this.alert = alert;
    }
    
    public Action getAction() {
        return action;
    }
    
    protected void setAction(Action action) {
        this.action = action;
    }
    
    public AuthzSubject getSubject() {
        return subject;
    }
    
    protected void setSubject(AuthzSubject subject) {
        this.subject = subject;
    }
    
    protected int getAlertTypeEnum() {
        return ClassicEscalationAlertType.CLASSIC.getCode();
    }
    
    protected void setAlertTypeEnum(int v) {
        // Do nothing
    }
    
    public EscalationAlertType getAlertType() {
        return ClassicEscalationAlertType.CLASSIC;
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    protected void setTimeStamp(long stamp) {
        timeStamp = stamp;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AlertActionLog)) {
            return false;
        }
        Integer objId = ((AlertActionLog)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }
}

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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.Action;

@Entity
@Table(name = "EAM_GALERT_ACTION_LOG")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GalertActionLog implements Serializable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACTION_ID")
    @Index(name = "GALERT_ACTION_ID_IDX")
    private Action action;

    @SuppressWarnings("unused")
    @Column(name = "ALERT_TYPE", nullable = false)
    private int alertType;

    @Column(name = "DETAIL", nullable = false, length = 1024)
    private String detail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GALERT_ID", nullable = false)
    @Index(name = "GALERT_ACTION_LOG_IDX")
    private GalertLog galertLog;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUBJECT_ID")
    @Index(name = "GALERT_ACTION_SUBJECT_ID_IDX")
    private AuthzSubject subject;

    @Column(name = "TIMESTAMP", nullable = false)
    private long timeStamp;

    protected GalertActionLog() {
    }

    public GalertActionLog(GalertLog alert, String detail, Action action, AuthzSubject subject) {
        this.detail = detail;
        galertLog = alert;
        this.action = action;
        this.subject = subject;
        timeStamp = System.currentTimeMillis();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof GalertActionLog)) {
            return false;
        }
        Integer objId = ((GalertActionLog) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public Action getAction() {
        return action;
    }

    public EscalationAlertType getAlertType() {
        return GalertEscalationAlertType.GALERT;
    }

    public String getDetail() {
        return detail;
    }

    public GalertLog getGalertLog() {
        return galertLog;
    }

    public Integer getId() {
        return id;
    }

    public AuthzSubject getSubject() {
        return subject;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    protected void setAction(Action action) {
        this.action = action;
    }

    protected void setAlertType(int v) {
        // Do nothing
    }

    protected void setDetail(String detail) {
        this.detail = detail;
    }

    protected void setGalertLog(GalertLog alert) {
        galertLog = alert;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setSubject(AuthzSubject subject) {
        this.subject = subject;
    }

    protected void setTimeStamp(long stamp) {
        timeStamp = stamp;
    }

}

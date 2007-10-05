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

package org.hyperic.hq.ui.taglib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.beans.RecentAlertBean;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * <p>A JSP tag that will get the recent alerts for a user and put them
 * in the request context.</p>
 *
 * <p>Attributes:
 * <table border="1">
 * <tr><th> name      </th><th> required </th><th> default </th></tr>
 * <tr><td> var       </th><th> true     </th><th> N/A     </th></tr>
 * <tr><td> sizeVar   </th><th> true     </th><th> N/A     </th></tr>
 * <tr><td> maxAlerts </th><th> false    </th><th> 2       </th></tr>
 * </table>
 *
 */
public class RecentAlertsTag extends TagSupport {
    Log log = LogFactory.getLog( RecentAlertsTag.class.getName() );

    //----------------------------------------------------instance variables

    private String var;
    private String sizeVar;
    private int maxAlerts = 2;

    //----------------------------------------------------constructors

    public RecentAlertsTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the request attribute that should hold the list
     * of alerts.
     *
     * @param var the name of the request attribute
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    /**
     * Set the name of the request attribute that should hold the size
     * of the list of alerts.
     *
     * @param sizeVar the name of the request attribute
     */
    public void setSizeVar(String sizeVar) {
        this.sizeVar = sizeVar;
    }
    
    /**
     * Set the max number of alerts to get.
     *
     * @param maxAlerts the max number of alerts
     */
    public void setMaxAlerts(String maxAlerts) {
        this.maxAlerts = Integer.parseInt(maxAlerts);
    }
    
    /**
     * Process the tag, generating and formatting the list.
     *
     * @exception JspException if the scripting variable can not be
     * found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        try {
            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();
            ServletContext ctx = pageContext.getServletContext();

            EventsBoss eb = ContextUtils.getEventsBoss(ctx);
            AuthzBoss ab = ContextUtils.getAuthzBoss(ctx);

            int sessionId = RequestUtils.getSessionId(request).intValue();
            // Recent alerts in the last two hours
            List userAlerts =
                eb.findRecentAlerts(sessionId, maxAlerts, 0,
                                    2 * MeasurementConstants.HOUR, null);

            if ( log.isTraceEnabled() ) {
                log.trace("found " + userAlerts.size() + " recent alerts");
            }

            ArrayList alertArr = new ArrayList();
            Iterator it = userAlerts.iterator();
            AuthzSubject subject = ab.getCurrentSubject(sessionId); 
            for (int i = 0; i < maxAlerts && it.hasNext(); i++) {
                Escalatable alert = (Escalatable)it.next();
                AlertDefinitionInterface defInfo = 
                    alert.getDefinition().getDefinitionInfo();
                AppdefEntityID adeId = 
                    new AppdefEntityID(defInfo.getAppdefType(),
                                       defInfo.getAppdefId());
                AppdefEntityValue aVal = new AppdefEntityValue(adeId, subject); 

                alertArr.add(
                    new RecentAlertBean(alert.getId(),
                                        alert.getAlertInfo().getTimestamp(),
                                        defInfo.getId(),
                                        defInfo.getName(),
                                        0,
                                        adeId.getId(),
                                        new Integer( adeId.getType() ),
                                        aVal.getName(),
                                        alert.getAlertInfo().isFixed()));
            }

            Object[] recentAlerts = alertArr.toArray(new RecentAlertBean[0]);

            request.setAttribute(var, recentAlerts);
            request.setAttribute(sizeVar, new Integer(recentAlerts.length) );

            return SKIP_BODY;
        } catch (Exception e) {
            log.warn("Error while generating recent alerts tag", e);
            throw new JspTagException( e.getMessage() );
        }
    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;        
    }

    public void release() {
        maxAlerts = 2;
        var = null;
        sizeVar = null;
        super.release();
    }
}

// EOF

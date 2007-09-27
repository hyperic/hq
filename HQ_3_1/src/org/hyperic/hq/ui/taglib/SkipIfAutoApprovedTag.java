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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.SessionUtils;

public class SkipIfAutoApprovedTag extends BodyTagSupport {

    private String aiserver = null;

    public SkipIfAutoApprovedTag () { super(); }

    public int doStartTag() throws JspException {

        AIServerValue aiServer;
        try {
            aiServer = (AIServerValue)
                ExpressionUtil.evalNotNull("spider", "aiserver", getAiserver(),
                                           AIServerValue.class, this,
                                           pageContext);
        } catch (NullAttributeException ne) {
            throw new JspTagException("typeId not found: " + ne);
        } catch (JspException je) {
            throw new JspTagException( je.toString() );
        }

        ServletContext ctx = pageContext.getServletContext();
        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        WebUser user
            = SessionUtils.getWebUser
            (((HttpServletRequest)pageContext.getRequest()).getSession());
        int sessionId = user.getSessionId().intValue();
        if (BizappUtils.isAutoApprovedServer(sessionId, appdefBoss, aiServer)) {
            return SKIP_BODY;
        }
        return EVAL_BODY_INCLUDE;
    }

    public String getAiserver() {
        return this.aiserver;
    }    
    public void setAiserver(String aiserver) {
        this.aiserver = aiserver;
    }
}

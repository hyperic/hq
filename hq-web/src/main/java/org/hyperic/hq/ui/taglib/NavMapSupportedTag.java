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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * Set a scoped variable containing a boolean flag as to whether or
 * not the running server and current page context both support the
 * navigation map functionality.
 *
 */
public class NavMapSupportedTag extends VarSetterBaseTag {
    private Log log = LogFactory.getLog( NavMapSupportedTag.class.getName() );

    public final int doStartTag() throws JspException {
    
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
           
            AppdefBoss ab = Bootstrap.getBean(AppdefBoss.class);

            boolean navMapSupported = true;

            // first try to get the resource ids as eids, then as rid / type
            AppdefEntityID[] eids = null;
            try {
                eids = RequestUtils.getEntityIds(request);
            } catch (ParameterNotFoundException e) {
                // either an auto-group of platforms or an individual resource
                try {
                    eids = new AppdefEntityID[] {
                            RequestUtils.getEntityId(request) };
                } catch (ParameterNotFoundException pnfe) {
                    // auto-group of platforms
                    eids = null;
                }
            }

            if (null == eids) {
                // auto-group of platforms
                navMapSupported = true;
            } else {
                if (0 == eids.length) {
                    // ERROR, not supported
                    log.error("Couldn't find any entity ids.  Assuming NavMap not supported.");
                    navMapSupported = false;
                } else if (1 == eids.length) {
                    if ( eids[0].isGroup() ) {
                        try {
                            int sessionId = RequestUtils.getSessionId(request).intValue();
                            AppdefGroupValue group =
                                ab.findGroup(sessionId, eids[0].getId());

                            if ( group.isGroupAdhoc() || group.isDynamicGroup() ) {
                                // mixed-group, not supported
                                navMapSupported = false;
                            } else {
                                // compatible-group or cluster, supported
                                navMapSupported = true;
                            }
                        } catch (SessionException e) {
                            // ERROR, not supported
                            log.error("Session timeout.  Assuming NavMap not supported.", e);
                            navMapSupported = false;
                        } catch (PermissionException e) {
                            // ERROR, not supported
                            log.error("No rights to this group.  Assuming NavMap not supported.", e);
                            navMapSupported = false;
                        } catch (ServletException e) {
                            // ERROR, not supported
                            log.error("Couldn't check group type w/o session id.  Assuming NavMap not supported.", e);
                            navMapSupported = false;
                        }

                    } else {
                        // non-group, supported
                        navMapSupported = true;
                    }
                } else {
                    // auto-group
                    navMapSupported = true;
                }
            }

            
            setScopedVariable( new Boolean(navMapSupported) );
        
        return SKIP_BODY;
    }
}

// EOF

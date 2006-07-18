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

package org.hyperic.hq.ui.action;

import java.io.IOException;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.TilesRequestProcessor;

import org.hyperic.hq.ui.Constants;

/**
 * An <code>RequestProcessor</code> subclass that adds properties to the request
 * for help and page name
 * these properties live in struts-config.xml
 */
public class BaseRequestProcessor extends TilesRequestProcessor {

    protected ActionForward
        processActionPerform(HttpServletRequest request,
                             HttpServletResponse response,
                             Action action,
                             ActionForm form,
                             ActionMapping mapping)
        throws IOException, ServletException {
        
        BaseActionMapping smapping = null;
        try {
            smapping = (BaseActionMapping) mapping;                
            if(smapping.getTitle() != null){
                request.setAttribute(Constants.PAGE_TITLE_KEY, smapping.getTitle() );
            }
            return (action.execute(mapping, form, request, response));
        } catch (Exception e) {
            return (processException(request, response,
                                     e, form, mapping));
        }
    }
}


/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
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
package org.hyperic.hq.ui.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.hyperic.hq.bizapp.server.session.ProductBossEJBImpl;
import org.hyperic.hq.bizapp.shared.ProductBossLocal;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.View;
import org.hyperic.hq.ui.RequestKeyConstants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.URLUtils;

public abstract class Plugin extends MenuPage implements PageBeginRenderListener {
    
    private static Log log = LogFactory.getLog(Attachment.class);

    @Persist
    public abstract void setPluginURL(String url);
    public abstract String getPluginURL();

    @Persist
    public abstract void setTitle(String title);
    public abstract String getTitle();

    @Persist
    public abstract void setHelpLink(String tag);
    public abstract String getHelpLink();

    public void pageBeginRender(PageEvent event) {
        super.pageBeginRender(event);
        // Grab the query param for the plugin identifier
        String pluginId = event.getRequestCycle()
                .getParameter(RequestKeyConstants.PLUGIN_ID_PARAM);
        
        // Lookup the plugin
        ProductBossLocal pBoss = ProductBossEJBImpl.getOne();
        String baseUrl = null;
        int sessionId;
        AttachmentDescriptor attachDesc = null;
        try {
            baseUrl = URLUtils.getHQBaseURL(getServletContext());
            sessionId = RequestUtils.getSessionIdInt(getRequest());
            attachDesc = pBoss.findAttachment(sessionId, Integer
                    .valueOf(pluginId));
        } catch (Exception e) {
            log.error("Error finding attachment descriptor for attachment "
                    + pluginId + " " + e.getLocalizedMessage());
        }
        
        // Get the attributes of the plugin and build its url
        if (attachDesc != null) {
            View view = attachDesc.getAttachment().getView();
            setHelpLink(attachDesc.getHelpTag());
            setTitle(attachDesc.getHTML());
            String path = view.getPath();
            String name = view.getPlugin().getName();
            setPluginURL(URLUtils.buildPluginAbsoluteURL(name, path, pluginId, baseUrl, getRequest().getSession().getId()));
        } else {
            log.error("Cannot find attachment descriptor for attachment "
                    + pluginId);
            return;
        }
    }

}

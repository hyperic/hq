/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
package org.hyperic.hq.hqu.rendit;

import java.io.File;
import java.util.Properties;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPlugin;

/**
 * Implementors of this interface (notably, HQUPlugin) are able to be 
 * dispatched by the HQU {@link RenditServer}.
 *
 * This interface primarily deals with how the UI plugins are deployed,
 * and attached to UI areas
 */
public interface IHQUPlugin {
    /**
     * Called before anything else, to inform the plugin where it is
     * located on disk.
     */
    void initialize(File pluginDir);
    
    /**
     * Returns information about the plugin.  The properties here are
     * from RenditServer.PROP_*
     */
    Properties getDescriptor();
    
    /**
     * Get the name of the plugin.  This is a unique, short-name (such as
     * live_exec, or auditcenter), which likely apply to a directory on
     * disk.
     */
    String getName();
    
    /**
     * Returns a localized string, containing the description of the plugin.
     */
    String getDescription();
    
    /**
     * Called when HQ deploys a plugin.  
     * 
     * @param me  HQ's internal representation of the plugin
     */
    void deploy(UIPlugin me);
   
    /**
     * Get an attachment descriptor for the attachment and associated 
     * resource.
     * 
     * @param a An attachment, previously created via deploy (or some other
     *          means)
     * @param r The resource that is currently being viewed, when the
     *          attachment is being rendered.  For attachments that are
     *          global, this will be the root resource (id=0)
     *         
     * @return a descriptor if one should be displayed, else null, indicating
     *         that the attachment should not be displayed.
     */
    AttachmentDescriptor getAttachmentDescriptor(Attachment a, Resource r); 
}

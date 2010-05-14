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

package org.hyperic.hq.hqu.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.hqu.ViewDescriptor;

public abstract class View
    extends PersistedObject 
{ 
    private UIPlugin   _plugin;
    private String     _path;
    private String     _descr;
    private AttachType _attachType;
    private Collection _attachments = new ArrayList();
    
    protected View() {}
    
    protected View(UIPlugin plugin, ViewDescriptor view, AttachType attach) {
        _plugin     = plugin;
        _path       = view.getPath();
        _descr      = view.getDescription();
        _attachType = attach;
    }
    
    public UIPlugin getPlugin() {
        return _plugin;
    }
    
    protected void setPlugin(UIPlugin plugin) {
        _plugin = plugin;
    }
    
    public String getPath() {
        return _path;
    }
    
    protected void setPath(String path) {
        _path = path;
    }

    public String getDescription() {
        return _descr;
    }
    
    protected void setDescription(String descr) {
        _descr = descr;
    }
    
    protected int getAttachTypeEnum() {
        return _attachType.getCode();
    }
    
    protected void setAttachTypeEnum(int code) {
        _attachType = AttachType.findByCode(code);
    }
    
    public AttachType getAttachType() {
        return _attachType;
    }
    
    void addAttachment(Attachment a) {
        getAttachmentsBag().add(a);
    }
    
    void removeAttachment(Attachment a) {
        getAttachmentsBag().remove(a);
    }
    
    protected Collection getAttachmentsBag() {
        return _attachments;
    }
    
    protected void setAttachmentsBag(Collection a) {
        _attachments = a;
    }
    
    public Collection getAttachments() {
        return Collections.unmodifiableCollection(_attachments);
    }
    
    public String toString() {
        return getPath() + " [" + getDescription() + "] attachable to [" +
            getAttachType().getDescription() + "]";
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof View)) {
            return false;
        }
        
        View o = (View)obj;
        return o.getPlugin().equals(getPlugin()) &&
               o.getPath().equals(getPath());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getPlugin().hashCode();
        result = 37 * result + getPath().hashCode();

        return result;
    }
}

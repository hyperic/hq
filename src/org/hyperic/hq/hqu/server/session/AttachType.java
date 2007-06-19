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

import java.util.ResourceBundle;

import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.ViewDescriptor;
import org.hyperic.util.HypericEnum;

public abstract class AttachType 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.hqu.Resources";
    
    public static AttachType ADMIN = new AttachType(0, "admin",
                                                    "attachType.admin") 
    {
        View createView(UIPlugin plugin, ViewDescriptor viewInfo) { 
            return new ViewAdmin(plugin, viewInfo);
        }

        Attachment attach(View view, AttachmentDescriptor d) { 
            assert view.getAttachments().isEmpty();
            Attachment a = new Attachment(view);
            
            view.addAttachment(a);
            return a;
        }

        boolean isAutoAttachable() {
            return true;
        }
    };

    public static AttachType MASTHEAD = new AttachType(1, "masthead",
                                                       "attachType.masthead") 
    {
        View createView(UIPlugin plugin, ViewDescriptor viewInfo) { 
            return new ViewMasthead(plugin, viewInfo);
        }

        Attachment attach(View view, AttachmentDescriptor d) { 
            assert view.getAttachments().isEmpty();
            Attachment a = new Attachment(view);
            
            view.addAttachment(a);
            return a;
        }

        boolean isAutoAttachable() {
            return true;
        }
    };

    abstract View createView(UIPlugin plugin, ViewDescriptor viewInfo); 
    
    abstract Attachment attach(View view, AttachmentDescriptor d);
    
    abstract boolean isAutoAttachable();
    
    public static AttachType findByDescription(String desc) {
        if (desc.equals("admin")) 
            return ADMIN;
        else if (desc.equals("masthead"))
            return MASTHEAD;

        throw new IllegalArgumentException("Unknown AttachType [" + desc + "]");
    }
    
    public static AttachType findByCode(int code) {
        return (AttachType)HypericEnum.findByCode(AttachType.class, code);
    }
    
    private AttachType(int code, String desc, String localeProp) {
        super(AttachType.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
}

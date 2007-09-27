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

package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

class PlatformTag extends ResourceTag implements XmlEndAttrHandler {

    private static final String[] REQUIRED_ATTRS =
        new String[] { ATTR_NAME };
    
    PlatformTypeInfo type;
    
    PlatformTag(BaseTag parent) {
        super(parent);
    }

    int getResourceType() {
        return TypeInfo.TYPE_PLATFORM;
    }
    
    public String getName() {
        return "platform";
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public XmlTagInfo[] getSubTags() {
        XmlTagInfo[] tags = new XmlTagInfo[] {
            new XmlTagInfo(new ServerTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
        };
        return getMergedSubTags(super.getSubTags(), tags);
    }

    public void endAttributes() throws XmlAttrException {
        String name = getAttribute(ATTR_NAME);
        this.type = new PlatformTypeInfo(name);
        
        this.data.addTypes(new TypeInfo[] { this.type });
        this.typeName = name;
        setNameProperty(name);
    }
}

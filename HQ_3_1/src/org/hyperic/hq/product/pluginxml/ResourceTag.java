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

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.xmlparser.XmlTagInfo;

abstract class ResourceTag extends BaseTag {
    
    String typeName;
    //set by ConfigTag if this resource contains:
    //<config include="foo"/>
    String configName;

    ResourceTag(BaseTag parent) {
        super(parent);
    }
    
    abstract int getResourceType();
    
    boolean isService() {
        return getResourceType() == TypeInfo.TYPE_SERVICE;
    }
    
    boolean isServer() {
        return getResourceType() == TypeInfo.TYPE_SERVER;
    }
    
    boolean isPlatform() {
        return getResourceType() == TypeInfo.TYPE_PLATFORM;
    }

    String getPlatformName(BaseTag tag) {
        String platform = tag.getAttribute(ATTR_PLATFORM);
        if (platform == null) {
            return this.typeName;
        }
        return this.typeName + " " + platform;
    }
    
    String getServerTypeName() {
        if (isServer()) {
            return ((ServerTag)this).typeName;
        }
        else if (isService()) {
            return ((ServiceTag)this).serverType;
        }
        return null;
    }
    
    //save value of name="..." attribute as a property
    //for plugins to get later w/ getTypeNameProperty()
    protected void setNameProperty(String name) {
        setNameProperty(this.typeName, name);
    }
    
    protected void setNameProperty(String typeName, String name) {
        String key = typeName + "." + GenericPlugin.PROP_NAME;
        this.data.setGlobalProperty(key, name);        
    }

    protected String getNameProperty(TypeInfo type) {
        String key = type.getName() + "." + GenericPlugin.PROP_NAME;
        return this.data.getProperty(key);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new FilterTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new PropertyTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ConfigTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new CustomPropertiesTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new PluginTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new HelpTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new MetricsTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new MetricTag(this), XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ActionsTag(this), XmlTagInfo.ZERO_OR_MORE)
        };
    }
}

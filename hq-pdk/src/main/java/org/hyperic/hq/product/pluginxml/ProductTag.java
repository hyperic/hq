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

import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

class ProductTag
    extends BaseTag
    implements XmlEndAttrHandler { 
    
    static final String DEFAULT_PACKAGE =
        "org.hyperic.hq.plugin";
    
    static final String ATTR_PACKAGE = "package";
    
    private static final String[] OPTIONAL_ATTRS = {
        ATTR_CLASS, ATTR_PACKAGE, ATTR_NAME
    };
    
    private ProductTag() {}
    
    ProductTag(PluginData data) {
        this.data = data;
    }
    
    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new PropertyTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new FilterTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new EmbedTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ScriptTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new PlatformTag(this),
                           XmlTagInfo.ZERO_OR_MORE),                           
            new XmlTagInfo(new ServerTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ServiceTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new MetricsTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new HelpTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ConfigTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new CustomPropertiesTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ClassPathTag(this), 
                           XmlTagInfo.ZERO_OR_MORE),
        };
    }

    public String getName() {
        return "plugin";
    }
    
    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }
    
    public void endAttributes() throws XmlAttrException {
        String name = getAttribute(ATTR_NAME);
        String packageName = getAttribute(ATTR_PACKAGE);
        
        if (name != null) {
            this.data.name = name;
        } else {
            name = this.data.name;
        }

        if ((packageName == null) && (name != null)) {
            packageName = DEFAULT_PACKAGE + "." + name;
        }
        
        if (packageName != null) {
            this.data.setProperty(ATTR_PACKAGE, packageName);
            this.data.addFilter(ATTR_PACKAGE, packageName);
        }
    }
    
    public void endTag() {
        String plugin = getAttribute(ATTR_CLASS);
        if (plugin != null) {
            this.data.addPlugin(ProductPlugin.TYPE_PRODUCT,
                                ProductPlugin.TYPE_PRODUCT,
                                plugin);            
        }
    }
}

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

import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTextHandler;

//for filtering within hq-plugin.xml only
//after hq-plugin.xml has been processed this value are cleared
class FilterTag extends BaseTag
    implements XmlTextHandler, XmlEndAttrHandler {

    private static final String[] REQUIRED_ATTRS = 
        { ATTR_NAME };

    private static final String[] OPTIONAL_ATTRS =
        { ATTR_VALUE };

    protected PluginData data;
    protected String name, value="";
    
    public FilterTag(BaseTag parent) {
        super(parent);
        this.data = parent.data;
    }
    
    public String getName() {
        return "filter";
    }
    
    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS; 
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public void handleText(String text) {
        text = text.trim();
        if (text.length() != 0) {
            this.value = text;
        }
    }

    public void endAttributes() throws XmlAttrException {
        this.name = getAttribute(ATTR_NAME);
        this.value = getAttribute(ATTR_VALUE, "true");
    }

    void endTag() {
        this.data.addFilter(this.name, this.value);
    }
}

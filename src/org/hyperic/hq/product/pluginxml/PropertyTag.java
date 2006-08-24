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

//apply as a filter as well as saving the property for 
//plugin use via GenericPlugin.getProperty
class PropertyTag extends FilterTag {

    private static final String[] OPTIONAL_ATTRS =
        { ATTR_VALUE, ATTR_TYPE };
    
    private BaseTag parent;
    
    public PropertyTag(BaseTag parent) {
        super(parent);
        this.parent = parent;
    }
    
    public String getName() {
        return "property";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    void endTag() {
        super.endTag(); //make filter-able too

        if (isResourceParent()) {
            ResourceTag resource = (ResourceTag)this.parent;
            resource.setAttribute(this.name, this.value);
            //plugins get this value using GenericPlugin.getTypeProperty()
            //resource names are unique and we want to be able to share between
            //plugins, so put these in the global property table
            String name = resource.typeName + "." + this.name;
            this.data.setGlobalProperty(name, this.value);
        }
        else {
            this.data.setProperty(this.name, this.value);
        }

        if (isGlobalType()) {
            this.data.setGlobalProperty(this.name, this.value);
        }
    }
}

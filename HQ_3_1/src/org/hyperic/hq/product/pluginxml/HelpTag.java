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

class HelpTag
    extends BaseTag
    implements XmlTextHandler,
               XmlEndAttrHandler {

    private static final String ATTR_APPEND = "append";
    
    private static final String[] OPTIONAL_ATTRS =
        new String[] {
            ATTR_NAME, ATTR_APPEND, ATTR_INCLUDE, ATTR_PLATFORM
        };

    private String helpName;
    
    HelpTag(BaseTag parent) {
        super(parent);
    }
    
    public String getName() {
        return "help";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }
    
    public void handleText(String text) {
        if (!this.collectHelp) {
            return;
        }

        StringBuffer sb = new StringBuffer();

        includeHelp(getAttribute(ATTR_INCLUDE), sb);

        sb.append(this.data.applyFilters(text));

        includeHelp(getAttribute(ATTR_APPEND), sb);

        String data = sb.toString();
        if (data.trim().length() == 0) {
            //e.g. <help/> to clear help inherited from another server
            //such as Apache-ERS inherits from Apache but already includes
            //the snmp module.
            data = "";
        }
        this.data.help.put(this.helpName, data);
    }

    public void endAttributes() throws XmlAttrException {
        if (!this.collectHelp) {
            return;
        }
        
        String name = getAttribute(ATTR_NAME);
        
        if (isResourceParent()) {
            if (name != null) {
                String msg =
                    "help 'name' attribute not allowed " +
                    "when nested in a " + this.parent.getName() + " tag";
                throw new XmlAttrException(msg);
            }

            this.helpName =
                ((ResourceTag)this.parent).getPlatformName(this);
        }
        else {
            if (name == null) {
                throw new XmlAttrException("missing help 'name' attribute");
            }

            this.helpName = name;
        }
    }
    
    private void includeHelp(String include, StringBuffer sb) {
        String[] includes = getList(include);
        for (int i=0; i<includes.length; i++) {
            Object hep = this.data.help.get(includes[i]);
            if (hep == null) {
                //XXX should throw
                //log.warn("help include not found: " + include);
            }
            else {
                sb.append(hep);
            }
        }
    }
}

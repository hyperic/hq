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

import java.util.List;

import org.hyperic.util.xmlparser.XmlTagException;

class ScanTag extends ContainerTag {
    
    private static final String ATTR_REGISTRY = "registry";
    
    private static final int TYPE_FILE = 1;
    private static final int TYPE_REGISTRY = 2;
    
    private static final String[] OPTIONAL_ATTRS = {
        ATTR_INCLUDE, ATTR_TYPE, ATTR_REGISTRY
    };
    
    private ServerTag server;
    
    ScanTag(BaseTag parent) {
        super(parent);
        this.server = (ServerTag)parent;
    }
    
    public String getName() {
        return "scan";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    //if this server type was inherited from another server type
    //and the clone defines scan config then clear the inherited scan config
    private void override(List sigs) {
        if (sigs == null) {
            return;
        }
        if (this.server.isIncluded) {
            sigs.clear();
        }
    }

    void endTag() throws XmlTagException {
        int type;
        String scanType = getAttribute(ATTR_TYPE, "file");
        String include = getAttribute(ATTR_INCLUDE);
        String registry = getAttribute(ATTR_REGISTRY);
        String name = this.server.typeName;

        if (registry != null) {
            //just handle the common case of 1 key for now
            scanType = "registry";
        }

        if (scanType.equals("file")) {
            type = TYPE_FILE;
            override(this.data.getFileScanIncludes(name));
        }
        else if (scanType.equals("registry")) {
            type = TYPE_REGISTRY;
            override(this.data.getRegistryScanKeys(name));
            override(this.data.getRegistryScanIncludes(name));
            if (registry != null) {
                this.data.addRegistryScanKey(name, registry);
            }
        }
        else {
            throw new XmlTagException("Unsupported scan type: " +
                                           scanType);
        }
        
        if (include != null) {
            List sigs;
            if (type == TYPE_FILE) {
                sigs = this.data.getFileScanIncludes(include);
            }
            else {
                sigs = this.data.getRegistryScanIncludes(include);
            }
            
            if (sigs != null) {
                this.includes.addAll(sigs);
            }
            else {
                //XXX throw ex?
            }
        }

        if (this.includes.size() > 0) {
            if (type == TYPE_FILE) {
                this.data.addFileScanIncludes(name,
                                              this.includes);
            }
            else {
                this.data.addRegistryScanIncludes(name,
                                                  this.includes);
            }
        }

        super.endTag();
    }
}

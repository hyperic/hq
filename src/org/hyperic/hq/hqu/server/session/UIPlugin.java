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

public class UIPlugin
    extends PersistedObject 
{ 
    private String     _name;
    private String     _pluginVersion;
    private Collection _views = new ArrayList();

    protected UIPlugin() {}
    
    UIPlugin(String name, String pluginVersion) {
        _name          = name;
        _pluginVersion = pluginVersion;
    }
    
    public String getName() {
        return _name;
    }
    
    protected void setName(String name){
        _name = name;
    }
    
    public String getPluginVersion() {
        return _pluginVersion;
    }
    
    protected void setPluginVersion(String pluginVer) {
        _pluginVersion = pluginVer;
    }
    
    public Collection getViews() {
        return Collections.unmodifiableCollection(_views);
    }
    
    void addView(View v) {
        getViewsBag().add(v);
    }
    
    protected Collection getViewsBag() {
        return _views;
    }
    
    protected void setViewsBag(Collection v) {
        _views = v;
    }
    
    public String toString() {
        return getName() + " version " + getPluginVersion();
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof UIPlugin)) {
            return false;
        }
        
        UIPlugin o = (UIPlugin)obj;
        return o.getName().equals(getName());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getName().hashCode();

        return result;
    }
}

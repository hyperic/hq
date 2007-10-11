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
package org.hyperic.hq.hqu;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.server.session.View;

/**
 * Returned from a UI plugin when the dispatcher is asked to load it.
 */
public class UIPluginDescriptor {
    private String  _pluginName;
    private String  _description;
    private String  _version;
    private int     _apiMajor;
    private int     _apiMinor;
    private boolean _dumpScripts=false;
    private List    _viewDescriptors = new ArrayList();
    
    public String getName() {
        return _pluginName;
    }
    
    public void setName(String name) {
        _pluginName = name;
    }
    
    public String getDescription() {
        return _description;
    }
    
    public void setDescription(String desc) {
        _description = desc;
    }
    
    public void setVersion(String version) {
        _version = version;
    }

    public String getVersion() {
        return _version;
    }
    
    public void setApiMajor(int major) {
        _apiMajor = major;
    }
    
    public int getApiMajor() {
        return _apiMajor;
    }
    
    public void setApiMinor(int minor) {
        _apiMinor = minor;
    }
    
    public int getApiMinor() {
        return _apiMinor;
    }

    public void setDumpScripts(boolean dump) {
        _dumpScripts = dump;
    }
    
    public boolean getDumpScripts() {
        return _dumpScripts;
    }
    
    public void addView(String path, String descr, String attachType,
                        boolean autoAttach) 
    {
        ViewAttacher at = new ViewAttacher() {
            public void attach(View v) {
                if (v.getAttachments().isEmpty())
                    UIPluginManagerEJBImpl.getOne().attachView(v, 
                                                             v.getPrototype());
            }
        };
        
        addView(path, descr, attachType, autoAttach ? at : null);
    }

    public void addView(String path, String descr, String attachType) {
        addView(path, descr, attachType, false);
    }
    
    public void addView(String path, String desc, String attachType,
                        ViewAttacher attacher) 
    {
        AttachType t = AttachType.findByDescription(attachType);
        _viewDescriptors.add(new ViewDescriptor(path, desc, t, attacher)); 
    }
    
    public List getViews() {
        return _viewDescriptors;
    }
}

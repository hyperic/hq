/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.vim;

//derived from SDK/samples/Axis/java/com/vmware/apputils/vim/ServiceConnection.java
import java.net.URL;

import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.ServiceContent;
import com.vmware.vim.VimPortType;
import com.vmware.vim.VimServiceLocator;

public class VimServiceConnection {
    private static final int STATE_CONNECTED = 0;
    private static final int STATE_DISCONNECTED = 1;
    private static final boolean _ignoreCerts = true;

    private VimServiceLocator _locator;
    private VimPortType _service;
    private int _svcState;
    private ServiceContent _sic;
    private ManagedObjectReference _svcRef;

    public VimServiceConnection() {
        _svcState = STATE_DISCONNECTED;

        _svcRef = new ManagedObjectReference();
        _svcRef.setType("ServiceInstance");
        _svcRef.set_value(_svcRef.getType());
    }
   
    public void connect(String url, String username, String password) throws Exception {
        if (_service != null) {
            disconnect();
        }
      
        if (_ignoreCerts) {
            ignoreCert();
        }
      
        _locator = new VimServiceLocator();
        _locator.setMaintainSession(true);      
        _service = _locator.getVimPort(new URL(url));
        _sic = _service.retrieveServiceContent(_svcRef);
        if (_sic.getSessionManager() != null) {          
            _service.login(_sic.getSessionManager(), username, password, null);
        }
        _svcState = STATE_CONNECTED;
    }
   
    private void ignoreCert() {
        System.setProperty("org.apache.axis.components.net.SecureSocketFactory",
                           "org.apache.axis.components.net.SunFakeTrustSocketFactory");
    }

    public boolean isConnected() {
        return _svcState == STATE_CONNECTED;
    }

    public VimPortType getService() {     
        return _service;
    }
   
    public ManagedObjectReference getServiceInstanceRef() {
        return _svcRef;
    }

    public ServiceContent getServiceContent() {
        return _sic;
    }

    public ManagedObjectReference getPropCol() {
        return _sic.getPropertyCollector();
    }

    public ManagedObjectReference getRootFolder() {
        return _sic.getRootFolder();
    }

    public void disconnect() throws Exception {
        if (_service != null) {
            _service.logout(_sic.getSessionManager());
            _service = null;
            _sic = null;
            _svcState = STATE_DISCONNECTED;
        }
    }

    public static void main(String[] args) throws Exception {
        VimServiceConnection conn = new VimServiceConnection();
        conn.connect(args[0], args[1], args[2]);
        System.out.println("connected=" + conn.isConnected());
    }
}

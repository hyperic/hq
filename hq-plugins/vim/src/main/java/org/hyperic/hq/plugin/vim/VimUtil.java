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

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

public class VimUtil extends ServiceInstance {

    static final String HOST_SYSTEM = "HostSystem";
    static final String POOL = "ResourcePool";
    static final String VM = "VirtualMachine";

    static final String PROP_URL = VimCollector.PROP_URL;
    static final String PROP_HOSTNAME = VimCollector.PROP_HOSTNAME;
    static final String PROP_USERNAME = VimCollector.PROP_USERNAME;
    static final String PROP_PASSWORD = VimCollector.PROP_PASSWORD;

    private static final Log _log =
        LogFactory.getLog(VimUtil.class.getName());
    private InventoryNavigator _nav;
    private String _url;

    public VimUtil(URL url, String username, String password, boolean ignoreCert)
        throws RemoteException, MalformedURLException {
        super(url, username, password, ignoreCert);
        _url = url.toString();
    }

    public static VimUtil getInstance(Properties props)
        throws PluginException {

        String url = getURL(props);
        if (url == null) {
            throw new PluginException(VimCollector.PROP_URL +
                                      " not configured");
        }

        String username = props.getProperty(VimCollector.PROP_USERNAME);
        String password = props.getProperty(VimCollector.PROP_PASSWORD); 

        try {
            return new VimUtil(new URL(url), username, password, true);
        } catch (Exception e) {
            throw new PluginException("ServiceInstance(" + url + ", " +
                                      username + "): " + e, e);
        }

    }

    public static VimUtil getInstance(ConfigResponse config)
        throws PluginException {
    
        return getInstance(config.toProperties());
    }

    public boolean isSessionValid() {
        try {
            //make sure session is still valid. XXX better way?
            Calendar clock = getServerClock();
            if (clock == null) {
                _log.debug(_url + " session invalid, clock=NULL");
                return false;
            }
            else {
                _log.debug(_url + " session valid, clock=" +
                           new Date(clock.getTimeInMillis()));
                return true;
            }
        } catch (Exception e) {
            _log.debug(_url + " session invalid, clock=" + e, e);
            return false;
        }
    }

    public boolean isESX() {
        return !"gsx".equals(getAboutInfo().getProductLineId());
    }

    public InventoryNavigator getNavigator() {
        if (_nav == null) {
            _nav = new InventoryNavigator(getRootFolder());
        }
        return _nav;
    }

    public ManagedEntity find(String type, String name)
        throws PluginException {

        ManagedEntity obj;
        try {
            obj = getNavigator().searchManagedEntity(type, name);
        } catch (Exception e) {
            throw new PluginException(type + "/" + name + ": " + e, e);
        }
        if (obj == null) {
            throw new PluginException(type + "/" + name + ": not found");
        }
        return obj;
    }

    public ManagedEntity[] find(String type) throws PluginException {
        ManagedEntity[] obj;
        try {
            obj = getNavigator().searchManagedEntities(type);
        } catch (Exception e) {
            throw new PluginException(type + ": " + e, e);
        }
        if (obj == null) {
            throw new PluginException(type + ": not found");
        }
        return obj;
    }

    public HostSystem getHost(String host) throws PluginException {
        return (HostSystem)find(HOST_SYSTEM, host);
    }

    public static String getURL(Properties props) {
        return props.getProperty(VimCollector.PROP_URL);
    }

    public static void dispose(VimUtil vim) {
        if (vim != null) {
            vim.getServerConnection().logout();
        }
    }
}

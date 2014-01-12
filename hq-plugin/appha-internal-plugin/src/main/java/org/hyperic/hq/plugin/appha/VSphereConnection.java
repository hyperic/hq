/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMWare, Inc.
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

package org.hyperic.hq.plugin.appha;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

public class VSphereConnection {
    private static final Log _log = LogFactory.getLog(VSphereConnection.class);
    private static final Map<String, List<VSphereConnection>> _conns =
        new HashMap<String, List<VSphereConnection>>();
    private static final int POOL_SIZE =
        Integer.valueOf(System.getProperty("vsphere.pool.size", "2"));

    final Object LOCK = new Object();
    VSphereUtil vim;
    private String _user = "", _pass = "";
    private String url;
    
    public VSphereConnection(String url) {
        this.url = url;
    }
   
    private static String address(Object obj) {
        if (obj == null) {
            return "@NULL";
        }
        return "@" + Integer.toHexString(obj.hashCode());
    }

    private static String mask(String val) {
        return val.replaceAll(".", "*");
    }

    private static String diff(String old, String cur) {
        return "'" + old + "'->'" + cur + "'";  
    }
    
    /**
     * @return {@link VSphereConnection} from the connection pool.  Once a connection is grabbed
     * it must be returned to the pool via the release() method
     */
    public static VSphereConnection getPooledInstance(Properties props) throws PluginException {
        synchronized (_conns) {
            VSphereConnection rtn;
            String url = VSphereUtil.getURL(props);
            List<VSphereConnection> tmp;
            if (null == (tmp = _conns.get(url))) {
                tmp = new ArrayList<VSphereConnection>();
                for (int ii=0; ii<POOL_SIZE; ii++) {
                    tmp.add(new VSphereConnection(url));
                }
                _conns.put(url, tmp);
            } else {
                while (tmp.size() == 0) {
                    try {
                        _conns.wait();
                    } catch (InterruptedException e) {
                        _log.warn(e,e);
                    }
                }
            }
            rtn = tmp.remove(0);
            rtn.validate(props);
            return rtn;
        }
    }
    
    public static void evict(String url) throws PluginException {
    	synchronized (_conns) {
    		try {
	    		List<VSphereConnection> instances = _conns.get(url);
	    		if (instances != null) {
	    			for (VSphereConnection v : instances) {
	    				v.dispose();
	    			}
	        		_conns.remove(url);
	    		}
    		} catch (Exception e) {
    			throw new PluginException(e);
    		}
    	}
    }

    public void release() {
        synchronized (_conns) {
            List<VSphereConnection> tmp;
            if (null != (tmp = _conns.get(getUrl()))) {
                tmp.add(this);
                _conns.notifyAll();
            }
        }
    }
   
    public void dispose() {
    	if (vim != null) {
	        VSphereUtil.dispose(vim);
	        if (_log.isDebugEnabled()) {
	        	_log.debug("Closed previous connection (" +
	        				address(vim) + ") for: " + url); 
	        }
	        vim = null;
    	}
    }
    
    private String getUrl() {
        return url;
    }

    private void validate(Properties props) throws PluginException {
        String url = VSphereUtil.getURL(props);
        String username = props.getProperty(VSphereUtil.PROP_USERNAME, "");
        String password = props.getProperty(VSphereUtil.PROP_PASSWORD, "");
        boolean requiresReconnect = false;
        boolean usernameChanged = !username.equals(_user);
        boolean passwordChanged = !password.equals(_pass);
        if (usernameChanged || passwordChanged) {
            requiresReconnect = true;
            if (_log.isDebugEnabled()) {
                String diff = "";
                if (usernameChanged) {
                    diff += "user:" + diff(_user, username);
                }
                if (passwordChanged) {
                    if (diff.length() != 0) {
                        diff += ",";
                    }
                    diff += "pass:" + diff(mask(_pass), mask(password));
                }
                _log.debug("Credentials changed (" + diff +
                           ") reconnecting cached connection for: " + url);                    
            }
        }
        else {
            if (vim != null) {
                requiresReconnect = !vim.isSessionValid();
            }
            // Else, some previous error must have happened -- expect it to be already logged.
        }
        if (requiresReconnect) {
            dispose();
        }
        if (vim == null) {
            try {
                vim = VSphereUtil.getInstance(props);
            } catch (Exception e) {
                throw new PluginException(e);
            }
            if (_log.isDebugEnabled()) {
            	_log.debug("Opened new connection (" +
                       address(vim) + "/" +
                       address(LOCK) + ") for: " + url);
            }
            _user = username;
            _pass = password;
        }
    }
}

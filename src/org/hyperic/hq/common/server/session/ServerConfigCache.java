package org.hyperic.hq.common.server.session;

import java.util.Enumeration;
import java.util.Properties;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.ConfigPropertyException;

/**
 * This class is an in-memory map of HQ server settings
 */
public class ServerConfigCache {
    private Log log = LogFactory.getLog(ServerConfigCache.class);
    private final Cache _cache;

    public static final String CACHENAME = "ServerConfigCache";
    private static final ServerConfigCache singleton = new ServerConfigCache();

    private ServerConfigCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }

    public String getProperty(String key) {        
        String val = null;
        Element el = _cache.get(key);
        
        if (el != null) {
            val = (String) el.getObjectValue();
        } else {
            loadConfig();
            if (_cache.get(key) != null) {
                val = getProperty(key);
            }
        }
        return val;
    }
        
    public Boolean getBooleanProperty(String key) {
        Boolean bool = null;
        String prop = getProperty(key);
        
        if (prop != null) {
            bool = Boolean.valueOf(prop);
        }
        return bool;
    }

    public void put(String key, String value) {
        Element el = new Element(key, value);
                                                          
        _cache.put(el);
    }
    
    public void remove(String key) {
        _cache.remove(key);
    }

    private void loadConfig() {
        try {
            Properties config = ServerConfigManagerEJBImpl.getOne().getConfig();
            String key = null;
            
            for (Enumeration e = config.propertyNames(); e.hasMoreElements() ;) {
                key = (String) e.nextElement();
                singleton.put(key, config.getProperty(key));
            }
        } catch (ConfigPropertyException e) {
            throw new SystemException(e);
        }
    }

    public static ServerConfigCache getInstance() {
        return ServerConfigCache.singleton;
    }

}

package org.hyperic.hq.measurement.server.session;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticThread;

import java.util.List;

public class SRNCache {
    private static Log _log = LogFactory.getLog(SRNCache.class);

    // The cache name, must match the definition in ehcache.xml
    private static String CACHENAME = "SRNCache";

    private static Cache _cache;

    private static SRNCache _singleton = new SRNCache();

    public static SRNCache getInstance() {
        return _singleton;
    }

    private SRNCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);

        DiagnosticObject cacheDiagnostics = new DiagnosticObject() {
            public String getStatus() {
                String separator = System.getProperty("line.separator");

                StringBuffer buf = new StringBuffer();
                buf.append(separator)
                    .append(CACHENAME)
                    .append(" elements=")
                    .append(_cache.getSize())
                    .append(" (")
                    .append(_cache.calculateInMemorySize())
                    .append(" bytes) hits=")
                    .append(_cache.getHitCount())
                    .append(" misses=")
                    .append(_cache.getMissCountNotFound());
                return buf.toString();
            }
        };
        DiagnosticThread.addDiagnosticObject(cacheDiagnostics);
    }

    /**
     * Get the current cache size.
     * 
     * @return The size of the cache
     */
    public long getSize() {
        return _cache.getSize();
    }

    public void put(ScheduleRevNum val) {
        Element el = new Element(val.getId(), val);
        _cache.put(el);
    }

    public ScheduleRevNum get(AppdefEntityID aid) {
        SrnId id = new SrnId(aid.getType(), aid.getID());
        return get(id);
    }

    public ScheduleRevNum get(SrnId id) {
        Element el = _cache.get(id);
        if (el != null) {
            return (ScheduleRevNum)el.getObjectValue();
        }
        return null;
    }

    public boolean remove(SrnId id) {
        return _cache.remove(id);
    }

    public List getKeys() {
        return _cache.getKeys();
    }
}

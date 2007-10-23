package org.hyperic.hq.measurement.server.session;

import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.MetricValue;

public class DownMetricsCacheEventListenerFactory extends
        CacheEventListenerFactory {
    private Log _log =
        LogFactory.getLog(DownMetricsCacheEventListenerFactory.class);

    public class DownMetricsCacheEventListener implements CacheEventListener {

        public void dispose() {}

        public void notifyElementEvicted(Ehcache cache, Element el) {}

        public void notifyElementExpired(Ehcache cache, Element el) {}

        public void notifyElementPut(Ehcache cache, Element el)
            throws CacheException {}

        public void notifyElementRemoved(Ehcache cache, Element el)
            throws CacheException {}

        public void notifyElementUpdated(Ehcache cache, Element el)
            throws CacheException {
            Integer mid = (Integer) el.getKey();
            MetricValue val = (MetricValue) el.getValue();

            // Look up the actual down time
            if (_log.isDebugEnabled()) {
                _log.debug("Look up earliest down time data point for " + mid);
            }
            
            long lastTime = DataManagerEJBImpl.getOne()
                .getLastNonZeroTimestamp(mid, val.getTimestamp());

            // Set the time if it's less than the current timestamp
            if (lastTime < val.getTimestamp()) {
                val.setTimestamp(lastTime);
            }
        }

        public void notifyRemoveAll(Ehcache cache) {}
        
        public Object clone() {
            return new DownMetricsCacheEventListener();
        }

    }

    public CacheEventListener createCacheEventListener(Properties props) {
        return new DownMetricsCacheEventListener();
    }

}

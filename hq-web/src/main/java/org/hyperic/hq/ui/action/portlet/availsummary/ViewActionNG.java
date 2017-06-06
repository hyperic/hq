package org.hyperic.hq.ui.action.portlet.availsummary;

import java.util.Comparator;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

@Component("availsummaryViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	
    private final Log log = LogFactory.getLog(ViewActionNG.class.getName());
    @Resource
    private AuthzBoss authzBoss;
    @Resource
    private MeasurementBoss measurementBoss;
    @Resource
    private AppdefBoss appdefBoss;
    @Resource
    private DashboardManager dashboardManager;
    
    
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		
		try {
			this.request = getServletRequest();
	        HttpSession session = request.getSession();
	        WebUser user = RequestUtils.getWebUser(session);
	        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
	            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
	        
	        ConfigResponse dashPrefs = dashConfig.getConfig();
	
	        String token;
	        long ts = System.currentTimeMillis();
	
	        try {
	            token = RequestUtils.getStringParameter(request, "token");
	            if (token != null){
	               //token should be alpha-numeric
	               if (!token.matches("^[\\w-]*$")){
	                   log.warn("Token cleared by xss filter: "+token);
	                   token=null;
	               }
	            }
	        } catch (ParameterNotFoundException e) {
	            token = null;
	        }
	
	        String resKey = PropertiesFormNG.RESOURCES;
	        String numKey = PropertiesFormNG.NUM_TO_SHOW;
	        String titleKey = PropertiesFormNG.TITLE;
	        
	        if (token != null) {
	            resKey += token;
	            numKey += token;
	            titleKey += token;
	        }
	
	        log.debug("Availability summary loaded in " + (System.currentTimeMillis() - ts) + " ms");	        
	        // request.setAttribute("titleDescription", dashPrefs.getValue(titleKey, ""));
	        
		} catch (Exception ex) {
			// TODO add handling for exception
		}

	}
	
	   private class AvailSummaryNG {
	        private final AppdefResourceTypeValue _type;
	        private int _numUp = 0;
	        private int _numDown = 0;

	        public AvailSummaryNG(AppdefResourceTypeValue type) {
	            _type = type;
	        }

	        public void setAvailability(double avail) {
	            if (avail == MeasurementConstants.AVAIL_UP) {
	                _numUp++;
	            } else {
	                _numDown++;
	            }
	        }

	        public String getTypeName() {
	            return _type.getName();
	        }

	        public int getAppdefType() {
	            return _type.getAppdefType();
	        }

	        public Integer getAppdefTypeId() {
	            return _type.getId();
	        }

	        public int getNumUp() {
	            return _numUp;
	        }

	        public int getNumDown() {
	            return _numDown;
	        }

	        public double getAvailPercentage() {
	            return (double) _numUp / (_numDown + _numUp);
	        }
	    }

	    private class AvailSummaryComparatorNG implements Comparator<AvailSummaryNG> {

	        public int compare(AvailSummaryNG s1, AvailSummaryNG s2) {

	            if (s1.getAvailPercentage() == s2.getAvailPercentage()) {
	                // Sort on the actual number
	                if (s1.getNumDown() != s2.getNumDown()) {
	                    return s1.getNumDown() < s2.getNumDown() ? 1 : -1;
	                }
	                // Sort on type name if equal avail percentage
	                return s1.getTypeName().compareTo(s2.getTypeName());
	            } else if (s1.getAvailPercentage() < s2.getAvailPercentage()) {
	                return -1;
	            } else {
	                return 1;
	            }
	        }
	    }

	    private CacheEntry loadData(int sessionId, AppdefEntityID id) throws AppdefEntityNotFoundException {
	        Cache cache = CacheManager.getInstance().getCache("AvailabilitySummary");
	        Element e = cache.get(id);

	        if (e != null) {
	            return (CacheEntry) e.getObjectValue();
	        }

	        // Otherwise, load from the backend

	        try {
	            Measurement m = measurementBoss.findAvailabilityMetric(sessionId, id);

	            CacheEntry res = new CacheEntry(m);
	            cache.put(new Element(id, res));
	            return res;
	        } catch (Exception ex) {
	            log.debug("Caught exception loading data: " + ex, ex);
	            return null;
	        }
	    }

	    // Classes for caching dashboard data
	    private class CacheEntry {
	        private AppdefResourceTypeValue _type;
	        private final Measurement _m;

	        public CacheEntry(Measurement m) {
	            _m = m;
	        }

	        public AppdefResourceTypeValue getType() {
	            return _type;
	        }

	        public void setType(AppdefResourceTypeValue type) {
	            _type = type;
	        }

	        public Measurement getMeasurement() {
	            return _m;
	        }
	    }

}

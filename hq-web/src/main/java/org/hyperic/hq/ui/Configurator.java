/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.util.config.ConfigResponse;

/**
 * A class that loads the application properties file (found at
 * <code>/WEB-INF/classes/hq.properties</code>) and configures the
 * web application. All properties in the file are exposed as servlet
 * context attributes.
 *
 */
public class Configurator implements ServletContextListener {

    private final Log log;
    
    public Configurator() {
        log = LogFactory.getLog(Configurator.class.getName());    
    }
    
    protected String getPreferenceFile() {
        return Constants.PROPS_USER_PREFS;
    }

    protected String getRoleDashboardPreferenceFile(){
    	return "";
    }
    
    protected String getUserDashboardPreferenceFile(){
    	return "/WEB-INF/classes/DefaultUserDashboardPreferences.properties";
    }
    
    /**
     * Respond to the <em>context initialized</em> container event by
     * loading the application properties file and the portals
     * definition file.
     *
     */
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        loadConfig(ctx);
        loadPreferences(ctx);
        loadTablePreferences(ctx);
        loadBuildNumber(ctx);
    }

    /**
     * Respond to the <em>context destroyed</em> container event by
     * doing nothing.
     *
     */
    public void contextDestroyed(ServletContextEvent sce) {
    }

    /**
     * Load the application properties file (found at
     * <code>/WEB-INF/classes/hq.properties</code>) and configure
     * the web application. All properties in the file are exposed as
     * servlet context attributes.
     *
     */
    public void loadConfig(ServletContext ctx) {
        Properties props = null;
        try {
            props = loadProperties(ctx,
                                                Constants.PROPS_FILE_NAME);
        }
        catch (Exception e) {
            error("unable to load application properties file [" +
                  Constants.PROPS_FILE_NAME + "]", e);
            return;
        }

        if (props == null) {
            debug("application properties file [" +
                  Constants.PROPS_FILE_NAME + "] does not exist");
            return;
        }

        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.startsWith(Constants.SYSTEM_VARIABLE_PATH)) {
                System.setProperty(name,props.getProperty(name));
            } else {
                ctx.setAttribute(name, props.getProperty(name));
            }
        }

        debug("loaded application configuration [" +
              Constants.PROPS_FILE_NAME + "]");
    }   

    private void debug (String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    private void error (String msg, Exception e) {
        if (log.isErrorEnabled()) {
            log.error(msg, e);
        }
    }
    
    private void loadPreferences(ServletContext ctx){
        ConfigResponse userPrefs = new ConfigResponse();
        ConfigResponse userDashPrefs = new ConfigResponse();
        ConfigResponse roleDashPrefs = new ConfigResponse();
        
        try{
        	// Load User Preferences
            Properties userProps =
                loadProperties(ctx, getPreferenceFile()); 
            Enumeration keys = userProps.keys();
            while (keys.hasMoreElements()){
                String key = (String) keys.nextElement();
                userPrefs.setValue( key, userProps.getProperty(key) );
            }
            ctx.setAttribute(Constants.DEF_USER_PREFS, userPrefs);
            
            // Load User Dashboard Preferences
            Properties userDashProps = loadProperties(ctx, getUserDashboardPreferenceFile());
            keys = userDashProps.keys();
            while(keys.hasMoreElements()){
            	String key = (String) keys.nextElement();
            	userDashPrefs.setValue(key, userDashProps.getProperty(key));
            }
            ctx.setAttribute(Constants.DEF_USER_DASH_PREFS, userDashPrefs);
            
            // Load Role Dashboard Preferences
            Properties roleDashProps;
			try {
				roleDashProps = loadProperties(ctx,
						getRoleDashboardPreferenceFile());
			} catch (Exception e) {
				roleDashProps = null;
			}
            if (roleDashProps != null) {
				keys = roleDashProps.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					roleDashPrefs.setValue(key, roleDashProps.getProperty(key));
				}
			}
			ctx.setAttribute(Constants.DEF_ROLE_DASH_PREFS, roleDashPrefs);
        } 
        catch (Exception e) {
            error("loading table properties file " + Constants.PROPS_TAGLIB + "failed: ", e);
            
        }
    
    }

    private void loadTablePreferences(ServletContext ctx){
        try{
            
            Properties tableProps = loadProperties(ctx, Constants.PROPS_TAGLIB);
            ctx.setAttribute(Constants.PROPS_TAGLIB_NAME, tableProps );
        }
        catch (Exception e) {
            error("loading table properties file " + Constants.PROPS_TAGLIB + "failed: ", e);
            
        }
    
    }
    
    /**
     * Load the specified properties file and return the properties.
     *
     * @param ctx the <code>ServletContext</code>
     * @param filename the fully qualifed name of the properties file
     * @exception Exception if a problem occurs while loading the file
     */
    private  Properties loadProperties(ServletContext ctx,
                                            String filename)
        throws Exception {
        Properties props = new Properties();
        InputStream is = ctx.getResourceAsStream(filename);
        if (is != null) {
            props.load(is);
            is.close();
        }
    
        return props;
    }
        
    private void loadBuildNumber(ServletContext ctx){
		final String SNAPSHOT_IDENTIFIER = "BUILD-SNAPSHOT";
		final String BUILD_DATE_FORMAT_OUTPUT = "yyyy-MM-dd";
		final String BUILD_DATE_FORMAT_INPUT = "MMM dd, yyyy";
		
		try {
			String version = ProductProperties.getVersion();

			if (version.contains(SNAPSHOT_IDENTIFIER)) {
				try {
					// Get build date, format into Date object...
					String date = ProductProperties.getBuildDate();
					DateFormat format = new SimpleDateFormat(BUILD_DATE_FORMAT_INPUT);
					Date buildDate = format.parse(date);

					// Take date object and format into different format for
					// display...
					format = new SimpleDateFormat(BUILD_DATE_FORMAT_OUTPUT);

					version += "-" + format.format(buildDate);
				} catch (ParseException e) {
					// Couldn't parse the date, so we fall back to using just
					// the version string...
					log.info("Couldn't parse the build date for display", e);
				}
			}

			ctx.setAttribute(Constants.APP_VERSION, version);
            ctx.setAttribute(Constants.APP_BUILD, ProductProperties.getBuildNumber());
		} catch (Exception e) {
			error("Unable to load product version", e);
		}
    }
    
    
}

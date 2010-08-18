package org.hyperic.hq.web;

import org.hyperic.hq.ui.Constants;

/**
 * This interface provides public string constants that map to existing session parameters used throughout the web app.
 * As we move things around this list should change and hopefully mirror the list of parameters that are actually in use.
 *
 * String should be ordered alphabetical for ease of maintenance.
 *
 * @author David Crutchfield
 *
 */

// TODO break dependency on Constants class as this progress...
public interface SessionParameterKeys {
	public final static String WEB_USER = Constants.WEBUSER_SES_ATTR; 
	public final static String SELECTED_DASHBOARD_ID = Constants.SELECTED_DASHBOARD_ID; 
}
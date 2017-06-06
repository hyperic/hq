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

package org.hyperic.hq.ui.action.admin.config;

import java.util.Properties;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;
import org.hyperic.hq.vm.VCConfig;

public class SystemConfigFormNG
    extends BaseValidatorFormNG {

    private String senderEmail = "";
    private String baseUrl = "";
    private String arcURL = "";
    private String helpUserId = "";
    private String helpPassword = "";
    private Integer deleteUnitsVal = 0;
    private String deleteUnits = "";
    private Integer maintIntervalVal = 0;
    private String maintInterval = "";
    private Long alertPurgeVal = 0l;
    private String alertPurge = "";
    private boolean reindex = false;
    private int updateMode = 0;
    private Long elPurgeVal = 0l;
    private boolean _alertsAllowed = true;
    private boolean _alertNotificationsAllowed = true;
    private String vCenterURL = "https://localhost/sdk";
    private String vCenterUser = "";
    private String vCenterPassword = "";
    

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(" senderEmail=").append(senderEmail);
        buf.append(" baseUrl=").append(baseUrl);
        buf.append(" arcURL=").append(arcURL);
        buf.append(" helpUserId=").append(helpUserId);
        buf.append(" helpPassword=").append(helpPassword);
        buf.append(" deleteUnits=").append(deleteUnits);
        buf.append(" updateMode=").append(updateMode);
        buf.append(" alertsAllowed=").append(_alertsAllowed);
        buf.append(" alertNotificationsAllowed=").append(_alertNotificationsAllowed);
        buf.append(" vCenterURL=").append(getVCenterURL());
        buf.append(" vCenterUser=").append(getVCenterUser());
        buf.append(" vCenterPassword=").append(getVCenterPassword());

        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.struts.action.ActionForm#reset(org.apache.struts.action.
     * ActionMapping, javax.servlet.http.HttpServletRequest)
     */
    public void reset() {
        super.reset();

        helpUserId = "";
        helpPassword = "";
        deleteUnits = "";
        deleteUnitsVal = null;
        maintInterval = "";
        maintIntervalVal = null;
        alertPurge = "";
        alertPurgeVal = null;
        elPurgeVal = 0l;
        updateMode = 0;
        resetVCenterValues();
    }

    public void resetVCenterValues() {
        setVCenterURL("https://localhost/sdk");
        setVCenterUser("");
        setVCenterPassword("");
    }

    public void loadConfigProperties(Properties prop) {
        senderEmail = prop.getProperty(HQConstants.EmailSender);
        baseUrl = prop.getProperty(HQConstants.BaseURL);
        arcURL = prop.getProperty(Constants.CONFIG_PROP_ARC_SERVER_URL);
        helpUserId = prop.getProperty(HQConstants.HelpUser);
        helpPassword = prop.getProperty(HQConstants.HelpUserPassword);

        String deleteUnitsValStr = prop.getProperty(HQConstants.DataPurgeRaw);
        Long deleteUnitInt = new Long(deleteUnitsValStr);
        deleteUnits = findTimeUnit(deleteUnitInt.longValue());
        deleteUnitsVal = (int) calcTimeUnit(deleteUnitInt.longValue());

        String maintIntervalValStr = prop.getProperty(HQConstants.DataMaintenance);
        Long maintIntervalLong = new Long(maintIntervalValStr);
        maintInterval = findTimeUnit(maintIntervalLong.longValue());
        maintIntervalVal = (int) calcTimeUnit(maintIntervalLong.longValue());

        String nightlyReindexStr = prop.getProperty(HQConstants.DataReindex);
        reindex = Boolean.valueOf(nightlyReindexStr).booleanValue();

        String alertPurgeValStr = prop.getProperty(HQConstants.AlertPurge);
        Long alertPurgeLong = new Long(alertPurgeValStr);
        alertPurge = findTimeUnit(alertPurgeLong.longValue());
        alertPurgeVal = calcTimeUnit(alertPurgeLong);

        String elPurgeValStr = prop.getProperty(HQConstants.EventLogPurge);
        Long elPurgeLong = new Long(elPurgeValStr);
        elPurgeVal = calcTimeUnit(elPurgeLong);

        String alertsAllowedStr = prop.getProperty(HQConstants.AlertsEnabled, "true");
        _alertsAllowed = Boolean.valueOf(alertsAllowedStr).booleanValue();

        String alertNotificationsAllowedStr = prop.getProperty(HQConstants.AlertNotificationsEnabled, "true");
        _alertNotificationsAllowed = Boolean.valueOf(alertNotificationsAllowedStr).booleanValue();

    }
    
    public void loadVCProps(VCConfig vc) {
        if (null == vc) {
            return;
        }
        vCenterURL = vc.getUrl();
        vCenterUser = vc.getUser();
        vCenterPassword = vc.getPassword();
    }

    /**
     * find the proper time unit associated with the timeUnit
     * 
     * @return time unit label
     */
    private String findTimeUnit(long timeUnitInt) {
        if (timeUnitInt % Constants.DAYS == 0) {
            return Constants.DAYS_LABEL;
        } else if (timeUnitInt % Constants.HOURS == 0) {
            return Constants.HOURS_LABEL;
        } else {
            return Constants.MINUTES_LABEL;
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     * 
     * @return time unit label
     */
    private long calcTimeUnit(long timeUnitInt) {
        if (timeUnitInt % Constants.DAYS == 0) {
            return (long)(timeUnitInt / Constants.DAYS);
        } else if (timeUnitInt % Constants.HOURS == 0) {
            return (long)(timeUnitInt / Constants.HOURS);
        } else {
            return (long)(timeUnitInt / Constants.MINUTES);
        }
    }

    /**
     * find the proper time unit associated with the timeUnit
     * 
     * @return time unit label
     */
    private long convertToMillisecond(long val, String timeLabel) {
        if (timeLabel.equalsIgnoreCase(Constants.DAYS_LABEL)) {
            return val * Constants.DAYS;
        } else if (timeLabel.equalsIgnoreCase(Constants.HOURS_LABEL)) {
            return val * Constants.HOURS;
        } else {
            return val * Constants.MINUTES;
        }
    }

    public Properties saveConfigProperties(Properties prop) {
        prop.setProperty(HQConstants.EmailSender, senderEmail);
        prop.setProperty(HQConstants.BaseURL, baseUrl);
        prop.setProperty(Constants.CONFIG_PROP_ARC_SERVER_URL, arcURL);
        prop.setProperty(HQConstants.HelpUser, helpUserId);
        prop.setProperty(HQConstants.HelpUserPassword, helpPassword);

        long deleteUnitInt = convertToMillisecond(deleteUnitsVal, Constants.DAYS_LABEL);
        prop.setProperty(HQConstants.DataPurgeRaw, String.valueOf(deleteUnitInt));
        prop.setProperty(HQConstants.DataReindex, String.valueOf(reindex));

        long maintIntervalLong = convertToMillisecond(maintIntervalVal, Constants.HOURS_LABEL);
        prop.setProperty(HQConstants.DataMaintenance, String.valueOf(maintIntervalLong));

        long alertPurgeLong = convertToMillisecond(alertPurgeVal, Constants.DAYS_LABEL);

        prop.setProperty(HQConstants.AlertPurge, String.valueOf(alertPurgeLong));

        long elPurgeLong = convertToMillisecond(elPurgeVal, Constants.DAYS_LABEL);

        prop.setProperty(HQConstants.EventLogPurge, String.valueOf(elPurgeLong));

        prop.setProperty(HQConstants.AlertsEnabled, String.valueOf(_alertsAllowed));
        prop.setProperty(HQConstants.AlertNotificationsEnabled, String.valueOf(_alertNotificationsAllowed));

        return prop;
    }

    public String getHelpPassword() {
        return helpPassword;
    }

    public String getHelpUserId() {
        return helpUserId;
    }

    public void setHelpPassword(String string) {
        helpPassword = string;
    }

    
    public String getDeleteUnits() {
        return deleteUnits;
    }

    public void setDeleteUnits(String s) {
        deleteUnits = s;
    }

    
    public String getMaintInterval() {
        return maintInterval;
    }

    public void setMaintInterval(String s) {
        maintInterval = s;
    }

    
    public String getAlertPurge() {
        return alertPurge;
    }

    public void setAlertPurge(String s) {
        alertPurge = s;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setBaseUrl(String string) {
        baseUrl = string;
    }

    public String getArcURL() {
        return arcURL;
    }

    public void setArcURL(String arcURL) {
        this.arcURL = arcURL;
    }

    public void setSenderEmail(String string) {
        senderEmail = string;
    }

    public boolean getReindex() {
        return reindex;
    }

    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }

    
    public Integer getDeleteUnitsVal() {
		return deleteUnitsVal;
	}

	public void setDeleteUnitsVal(Integer deleteUnitsVal) {
		this.deleteUnitsVal = deleteUnitsVal;
	}

	public Integer getMaintIntervalVal() {
		return maintIntervalVal;
	}

	public void setMaintIntervalVal(Integer maintIntervalVal) {
		this.maintIntervalVal = maintIntervalVal;
	}

	public Long getAlertPurgeVal() {
		return alertPurgeVal;
	}

	public void setAlertPurgeVal(Long alertPurgeVal) {
		this.alertPurgeVal = alertPurgeVal;
	}

	public Long getElPurgeVal() {
		return elPurgeVal;
	}

	public void setElPurgeVal(Long elPurgeVal) {
		this.elPurgeVal = elPurgeVal;
	}

	public void setHelpUserId(String helpUserId) {
		this.helpUserId = helpUserId;
	}

	public int getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(int updateMode) {
        this.updateMode = updateMode;
    }

    public boolean isAlertsAllowed() {
        return _alertsAllowed;
    }

    public void setAlertsAllowed(boolean alertsAllowed) {
        _alertsAllowed = alertsAllowed;
    }

    public boolean isAlertNotificationsAllowed() {
        return _alertNotificationsAllowed;
    }

    public void setAlertNotificationsAllowed(boolean alertNotificationsAllowed) {
        _alertNotificationsAllowed = alertNotificationsAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.struts.action.ActionForm#validate(org.apache.struts.action
     * .ActionMapping, javax.servlet.http.HttpServletRequest)
     */
/*    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

        ActionErrors errors = super.validate(mapping, request);

        return errors;
    }

*/	public String getVCenterURL() {
		return vCenterURL;
	}

	public void setVCenterURL(String vCenterURL) {
		this.vCenterURL = vCenterURL;
	}

	public String getVCenterUser() {
		return vCenterUser;
	}

	public void setVCenterUser(String vCenterUser) {
		this.vCenterUser = vCenterUser;
	}

	public String getVCenterPassword() {
		return vCenterPassword;
	}

	public void setVCenterPassword(String vCenterPassword) {
		this.vCenterPassword = vCenterPassword;
	}


}

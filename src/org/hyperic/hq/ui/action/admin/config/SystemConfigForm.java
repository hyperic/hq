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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorForm;

public class SystemConfigForm extends BaseValidatorForm {

    private String senderEmail = "";
    private String baseUrl = "";
    private String helpUserId = "";
    private String helpPassword = "";
    private String deleteUnitsVal = "0";
    private String deleteUnits = "";
    private String maintIntervalVal = "0";
    private String maintInterval = "";
    private String alertPurgeVal = "0";
    private String alertPurge    = "";
    private boolean reindex = false;
    private int    updateMode = 0;
    private String elPurgeVal = "0";
    private boolean externDocs = true;

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());

        buf.append(" senderEmail=").append(senderEmail);
        buf.append(" baseUrl=").append(baseUrl);
        buf.append(" helpUserId=").append(helpUserId);
        buf.append(" helpPassword=").append(helpPassword);
        buf.append(" deleteUnits=").append(deleteUnits);
        buf.append(" updateMode=").append(updateMode);
        buf.append(" externDocs=").append(externDocs);

        return buf.toString();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.struts.action.ActionForm#reset(org.apache.struts.action.ActionMapping,
     *      javax.servlet.http.HttpServletRequest)
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);

        helpUserId = "";
        helpPassword = "";
        deleteUnits = "";
        deleteUnitsVal = null;
        maintInterval = "";
        maintIntervalVal = null;
        alertPurge = "";
        alertPurgeVal = null;
        elPurgeVal = "0";
        updateMode = 0;
    }

    public void loadConfigProperties (Properties prop){
        senderEmail = prop.getProperty(HQConstants.EmailSender);
        baseUrl = prop.getProperty(HQConstants.BaseURL);
        helpUserId = prop.getProperty(HQConstants.HelpUser);
        helpPassword = prop.getProperty(HQConstants.HelpUserPassword);
        
        String deleteUnitsValStr = prop.getProperty(HQConstants.DataPurgeRaw);
        Long deleteUnitInt = new Long(deleteUnitsValStr);
        deleteUnits = findTimeUnit(deleteUnitInt.longValue());
        deleteUnitsVal = calcTimeUnit(deleteUnitInt.longValue());

        String maintIntervalValStr =
            prop.getProperty(HQConstants.DataMaintenance);
        Long maintIntervalLong = new Long(maintIntervalValStr);
        maintInterval = findTimeUnit(maintIntervalLong.longValue());
        maintIntervalVal = calcTimeUnit(maintIntervalLong.longValue());

        String nightlyReindexStr = prop.getProperty(HQConstants.DataReindex);
        reindex = Boolean.valueOf(nightlyReindexStr).booleanValue();
        
        String alertPurgeValStr = prop.getProperty(HQConstants.AlertPurge);
        Long alertPurgeLong = new Long(alertPurgeValStr);
        alertPurge = findTimeUnit(alertPurgeLong.longValue());
        alertPurgeVal = calcTimeUnit(alertPurgeLong.longValue());

        String elPurgeValStr = prop.getProperty(HQConstants.EventLogPurge);
        Long elPurgeLong = new Long(elPurgeValStr);
        elPurgeVal = calcTimeUnit(elPurgeLong.longValue());
        
        String externDocsStr = prop.getProperty(HQConstants.ExternalHelp);
        if (externDocsStr != null) {
            externDocs = Boolean.valueOf(externDocsStr).booleanValue();
        }
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
    private String calcTimeUnit(long timeUnitInt) {
        if (timeUnitInt % Constants.DAYS == 0) {
            return String.valueOf(timeUnitInt / Constants.DAYS);
        } else if (timeUnitInt % Constants.HOURS == 0) {
            return String.valueOf(timeUnitInt / Constants.HOURS);
        } else {
            return String.valueOf(timeUnitInt / Constants.MINUTES);
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
    
    public Properties saveConfigProperties(Properties prop)
    {
        prop.setProperty(HQConstants.EmailSender, senderEmail);
        prop.setProperty(HQConstants.BaseURL, baseUrl);
        prop.setProperty(HQConstants.HelpUser, helpUserId);
        prop.setProperty(HQConstants.HelpUserPassword, helpPassword);

        long deleteUnitInt =
            convertToMillisecond(Integer.parseInt(deleteUnitsVal), deleteUnits);
        prop.setProperty(HQConstants.DataPurgeRaw, String.valueOf(deleteUnitInt));
        prop.setProperty(HQConstants.DataReindex, String.valueOf(reindex));

        long maintIntervalLong =
            convertToMillisecond(Integer.parseInt(maintIntervalVal),
                                 maintInterval);
        prop.setProperty(HQConstants.DataMaintenance,
                 String.valueOf(maintIntervalLong));

        long alertPurgeLong =
            convertToMillisecond(Long.parseLong(alertPurgeVal), alertPurge);
        
        prop.setProperty(HQConstants.AlertPurge, String.valueOf(alertPurgeLong));

        long elPurgeLong =
            convertToMillisecond(Long.parseLong(elPurgeVal),
                                 Constants.DAYS_LABEL);
        
        prop.setProperty(HQConstants.EventLogPurge,
                         String.valueOf(elPurgeLong));
        prop.setProperty(HQConstants.ExternalHelp, String.valueOf(externDocs));

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

    public void setHelpUserId(String string) {
        helpUserId = string;
    }

    public String getDeleteUnitsVal() {
        return deleteUnitsVal;
    }

    public void setDeleteUnitsVal(String v) {
        deleteUnitsVal = v;
    }

    public String getDeleteUnits() {
        return deleteUnits;
    }

    public void setDeleteUnits(String s) {
        deleteUnits = s;
    }

    public String getMaintIntervalVal() {
        return maintIntervalVal;
    }

    public void setMaintIntervalVal(String v) {
        maintIntervalVal = v;
    }

    public String getMaintInterval() {
        return maintInterval;
    }

    public void setMaintInterval(String s) {
        maintInterval = s;
    }

    public String getAlertPurgeVal() {
        return alertPurgeVal;
    }

    public void setAlertPurgeVal(String v) {
        alertPurgeVal = v;
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

    public void setSenderEmail(String string) {
        senderEmail = string;
    }

    public boolean getReindex() {
        return reindex;
    }
    
    public void setReindex(boolean reindex) {
        this.reindex = reindex;
    }

    /* (non-Javadoc)
     * @see org.apache.struts.action.ActionForm#validate(org.apache.struts.action.ActionMapping, javax.servlet.http.HttpServletRequest)
     */
    public ActionErrors validate(
        ActionMapping mapping,
        HttpServletRequest request) {
        
        ActionErrors errors = super.validate(mapping, request);
                    
        return errors;
    }

    public String getElPurgeVal() {
        return elPurgeVal;
    }

    public void setElPurgeVal(String elPurgeVal) {
        this.elPurgeVal = elPurgeVal;
    }

    public int getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(int updateMode) {
        this.updateMode = updateMode;
    }

    public boolean isExternDocs() {
        return externDocs;
    }

    public void setExternDocs(boolean externDocs) {
        this.externDocs = externDocs;
    }
}

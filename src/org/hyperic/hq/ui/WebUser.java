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

package org.hyperic.hq.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.util.StringUtil;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.ui.util.MonitorUtils;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * A representation of the person currently interacting with the
 * application.
 */
public class WebUser {

    public static final String PREF_FAV_RESOURCE_METRICS_PREFIX =
    ".resource.common.monitor.visibility.favoriteMetrics";

    public static final String PREF_METRIC_RANGE =
    ".resource.common.monitor.visibility.metricRange";

    public static final String PREF_METRIC_RANGE_LASTN =
    ".resource.common.monitor.visibility.metricRange.lastN";

    public static final String PREF_METRIC_RANGE_UNIT =
    ".resource.common.monitor.visibility.metricRange.unit";

    public static final String PREF_METRIC_RANGE_RO =
    ".resource.common.monitor.visibility.metricRange.ro";

    public static final String PREF_METRIC_THRESHOLD =
    ".resource.common.monitor.visibility.metricThreshold";

    // delimiter for preferences that are muti-valued and stringified
    public static final String PREF_LIST_DELIM = ",";

    // preference key namespace delimiter
    private static final String DOT = ".";

    private AuthzSubjectValue subject;
    private Integer sessionId;
    
    private String password;
    
    private ConfigResponse preferences;

    /** Indicates whether or not the user has an entry in the
      * principals table */
    private boolean hasPrincipal;

    public WebUser() {
        this.sessionId = null;
    }
    
    public WebUser(AuthzSubjectValue subject) {
        this.subject = subject;
        this.sessionId = null;
        this.hasPrincipal = false;
    }
    
    public WebUser(AuthzSubjectValue subject, Integer sessionId,
                   String password, ConfigResponse preferences,
                   boolean hasPrincipal) {
                       
        this.subject = subject;
        this.sessionId = sessionId;
        this.setPassword(password);
        this.setPreferences(preferences);
        this.hasPrincipal = hasPrincipal;
    }
    
    
    /** Return the <code>AuthzSubjectValue</code> represented by this
     * web user.
     */
    public AuthzSubjectValue getSubject() {
        return this.subject;
    }

    /** Return the id as an <code>Integer</code> for this web user */
    public Integer getId() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getId();
    }
    
    /** Return the BizApp session id as an <code>Integer</code> for
     * this web user
     */
    public Integer getSessionId() {
        return this.sessionId;
    }
    
    /** Set the BizApp session id as an <code>Integer</code> for this
     * web user
     * @param sessionId the new session id
     */
    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }
    
    /** Facade method for AuthzSubjectValue.getName */
    public String getUsername() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getName();
    }
    
    /** Facade method for AuthzSubjectValue.setName
     * @param username the username
     */
    public void setUsername(String username) {
        this.subject.setName(username);
    }

    /** Facade method for AuthzSubjectValue.getName */
    public String getName() {
        return getUsername();
    }
    
    public String getSmsaddress() {
        return this.subject.getSMSAddress();
    }
    
    public void setSmsaddress(String s) {
        this.subject.setSMSAddress(s);
    }
    
    /** Facade method for AuthzSubjectValue.getFirstName */
    public String getFirstName() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getFirstName();
    }
    
    /** Facade method for AuthzSubjectValue.setFirstName
     * @param name the firstName
     */
    public void setFirstName(String name) {
        this.subject.setFirstName(name);
    }

    /** Facade method for AuthzSubjectValue.getLastName */
    public String getLastName() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getLastName();
    }
    
    /** Facade method for AuthzSubjectValue.setLastName
     * @param name the lastName
     */
    public void setLastName(String name) {
        this.subject.setLastName(name);
    }

    /** Facade method for AuthzSubjectValue.getEmailAddress */
    public String getEmailAddress(){
        if (this.subject == null) {
            return null;
        }
        return this.subject.getEmailAddress();
    }
    
    /** Facade method for AuthzSubjectValue.setEmailAddress
     * @param name the emailAddress
     */
    public void setEmailAddress(String emailAddress) {
        this.subject.setEmailAddress(emailAddress);
    }

    /** Facade method for AuthzSubjectValue.getAuthDsn */
    public String getAuthDsn() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getAuthDsn();
    }

    /** Facade method for AuthzSubjectValue.setAuthDsn
     * @param name the phoneNumber
     */
    public void setAuthDsn(String phoneNumber) {
        this.subject.setAuthDsn(phoneNumber);
    }
    
    /** Facade method for AuthzSubjectValue.getPhoneNumber */
    public String getPhoneNumber() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getPhoneNumber();
    }

    /** Facade method for AuthzSubjectValue.setPhoneNumber
     * @param name the phoneNumber
     */
    public void setPhoneNumber(String phoneNumber) {
        this.subject.setPhoneNumber(phoneNumber);
    }
    
    /** Facade method for AuthzSubjectValue.getDepartment */
    public String getDepartment() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getDepartment();
    }

    /** Facade method for AuthzSubjectValue.setDepartment
     * @param name the department
     */
    public void setDepartment(String department) {
        this.subject.setDepartment(department);
    }
    
    /** Facade method for AuthzSubjectValue.getActive */
    public boolean getActive() {
        if (this.subject == null) {
            return false;
        }
        return this.subject.getActive();
    }

    /** Facade method for AuthzSubjectValue.setActive
     * @param active the flag
     */
    public void setActive(boolean active) {
        this.subject.setActive(active);
    }
    
    /** Return a human readable serialization of this object */
    public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("id=").append(getId()).append(" ");
        str.append("sessionId=").append(getSessionId()).append(" ");
        str.append("hasPrincipal=").append(getHasPrincipal()).append(" ");
        str.append("subject=").append(getSubject()).append(" ");
        str.append("}");
        return(str.toString());
    }
    
    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean getHasPrincipal() { return this.hasPrincipal; }
    public void setHasPrincipal(boolean hasPrincipal) { 
        this.hasPrincipal = hasPrincipal;
    }
    
    public ConfigResponse getPreferences() { return this.preferences; }
    public void setPreferences(ConfigResponse preferences) {
        this.preferences = preferences;
    }
    
    public String getPreference(String key) throws InvalidOptionException {
        String value = preferences.getValue(key);
        
        if (value == null)
            throw new InvalidOptionException("preference" + key +
                                             " requested is not valid");
        return value.trim();
    }

    public String getPreference(String key, String def) {
        String value = preferences.getValue(key);
        
        if (value == null)
            return def;
        
        return value.trim();
    }

    /**
     * Break the named preference into tokens delimited by
     * <code>PREF_LIST_DELIM</code>.
     *
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List getPreferenceAsList(String key) throws InvalidOptionException {
        return getPreferenceAsList(key, PREF_LIST_DELIM );
    }
    
    /**
     * Break the named preference  
     * @param delimiter the delimeter to break it up by
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List getPreferenceAsList(String key, String delimiter)
        throws InvalidOptionException {
        return StringUtil.explode(getPreference(key), delimiter);
    }
    
    public void setPreference(String key, List values)
        throws InvalidOptionValueException, InvalidOptionException {
        setPreference(key, values, PREF_LIST_DELIM);                  
        
    }

    public void setPreference(String key, List values, String delim)
        throws InvalidOptionValueException, InvalidOptionException {
        String stringified = StringUtil.listToString(values, delim);
        setPreference(key,stringified);
    }
    
    public void setPreference(String key, Object value)
        throws InvalidOptionValueException, InvalidOptionException {
        String val = null;
        if (value == null) {
            val = "";
        }
        else if (value instanceof String) {
            val = (String) value;
        }
        else {
            val = value.toString();
        }
        this.preferences.setValue(key, val);
    }

    public void unsetPreference(String key) {
        this.preferences.unsetValue(key);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public List getResourceFavoriteMetricsPreference(String appdefTypeName) 
            throws InvalidOptionException {
        return getPreferenceAsList(getResourceFavoriteMetricsKey(appdefTypeName));
    }

    /**
     * Method getResourceFavoriteMetricsKey.
     * 
     * Encapsulates the logic for how the favorite metrics key for a particular appdef
     * type is calculated
     * 
     * @param appdefTypeName i.e. application, platform, server, service
     * @return String the calculated preferences key
     */
    public String getResourceFavoriteMetricsKey(String appdefTypeName) {
        StringBuffer sb = new StringBuffer(PREF_FAV_RESOURCE_METRICS_PREFIX);
        sb.append(DOT).append(appdefTypeName);
        return sb.toString();        
    }
    
    /**
     * Returns a Map of pref values:
     *
     * <ul>
     *   <li><code>MonitorUtils.RO</code>: Boolean
     *   <li><code>MonitorUtils.LASTN</code>: Integer
     *   <li><code>MonitorUtils.UNIT</code>: Unit
     *   <li><code>MonitorUtils.BEGIN</code>: Long
     *   <li><code>MonitorUtils.END</code>: Long
     * </ul>
     */
    public Map getMetricRangePreference(boolean defaultRange)
        throws InvalidOptionException {
        Map m = new HashMap();

        //  properties may be empty or unparseable strings (ex:
        //  "null"). if so, use their default values.
        Boolean ro = null;
        try {
            ro = new Boolean(getPreference(PREF_METRIC_RANGE_RO));
        }
        catch (NumberFormatException nfe) {
            ro = MonitorUtils.DEFAULT_VALUE_RANGE_RO;
        }
        m.put(MonitorUtils.RO, ro);

        Integer lastN = null;
        try {
            lastN = new Integer(getPreference(PREF_METRIC_RANGE_LASTN));
        }
        catch (NumberFormatException nfe) {
            lastN = MonitorUtils.DEFAULT_VALUE_RANGE_LASTN;
        }
        m.put(MonitorUtils.LASTN, lastN);

        Integer unit = null;
        try {
            unit = new Integer(getPreference(PREF_METRIC_RANGE_UNIT));
        }
        catch (NumberFormatException nfe) {
            unit = MonitorUtils.DEFAULT_VALUE_RANGE_UNIT;
        }
        m.put(MonitorUtils.UNIT, unit);

        List range = getPreferenceAsList(PREF_METRIC_RANGE);
        Long begin = null;
        Long end = null;
        if (range != null && range.size() > 0) {
            try {
                begin = new Long((String) range.get(0));
                end = new Long((String) range.get(1));
            }
            catch (NumberFormatException nfe) {
                begin = null;
                end = null;
            }
        }

        // sometimes we are satisfied with no range. other times we
        // need to calculate the "last n" units range and return
        // that.
        if (defaultRange && begin == null && end == null) {
            range = MonitorUtils.calculateTimeFrame(lastN.intValue(),
                                                    unit.intValue());

            begin = (Long) range.get(0);
            end = (Long) range.get(1);
        }

        m.put(MonitorUtils.BEGIN, begin);
        m.put(MonitorUtils.END, end);

        return m;
    }
    
    public Map getMetricRangePreference() throws InvalidOptionException {
        return getMetricRangePreference(true);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public Integer getMetricThresholdPreference() 
        throws InvalidOptionException {
        return new Integer(getPreference(PREF_METRIC_THRESHOLD));
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public boolean getBooleanPref(String key, boolean ifNull) {
        String val;
        try {
            val = getPreference(key);
        } catch (InvalidOptionException e) {
            return ifNull;
        }
        return Boolean.valueOf(val).booleanValue();
    }
    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public int getIntPref(String key, int ifNull) {
        String val;
        try {
            val = getPreference(key);
        } catch (InvalidOptionException e) {
            return ifNull;
        }
        return Integer.parseInt(val);
    }
}

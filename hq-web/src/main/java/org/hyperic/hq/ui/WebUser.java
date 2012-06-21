/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import org.hyperic.hq.api.common.InterfaceUser;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * A representation of the person currently interacting with the
 * application.
 */
public class WebUser implements InterfaceUser {

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

    private AuthzSubjectValue _subject;
    private Integer _sessionId;
    private ConfigResponse _preferences;
    /** Indicates whether or not the user has an entry in the
      * principals table */
    private boolean _hasPrincipal;

    public WebUser() {
        _sessionId = null;
    }
    
    public WebUser(AuthzSubjectValue subject) {
        _subject = subject;
        _sessionId = null;
        _hasPrincipal = false;
    }
    
    public WebUser(AuthzSubject subject, Integer sessionId,
                   ConfigResponse preferences, boolean hasPrincipal) {
                       
        this(subject.getAuthzSubjectValue(), sessionId, preferences,
             hasPrincipal);
    }

    public WebUser(AuthzSubjectValue subject, Integer sessionId,
                   ConfigResponse preferences, boolean hasPrincipal) {
                       
        _subject = subject;
        _sessionId = sessionId;
        setPreferences(preferences);
        _hasPrincipal = hasPrincipal;
    }

    /**
     * Return the AuthzSubjectValue represented by this web user.
     */
    public AuthzSubjectValue getSubject() {
        return _subject;
    }

    public Integer getId() {
        if (_subject == null) {
            return null;
        }
        return _subject.getId();
    }
    
    /**
     * Return the BizApp session id as an Integer for this web user
     */
    public Integer getSessionId() {
        return _sessionId;
    }
    
    /**
     * Set the BizApp session id as an Integer for this web user
     * @param sessionId the new session id
     */
    public void setSessionId(Integer sessionId) {
        _sessionId = sessionId;
    }

    public String getUsername() {
        if (_subject == null) {
            return null;
        }
        return _subject.getName();
    }

    public void setUsername(String username) {
        _subject.setName(username);
    }

    public String getName() {
        return getUsername();
    }
    
    public String getSmsaddress() {
        return _subject.getSMSAddress();
    }
    
    public void setSmsaddress(String s) {
        _subject.setSMSAddress(s);
    }

    public String getFirstName() {
        if (_subject == null) {
            return null;
        }
        return _subject.getFirstName();
    }

    public void setFirstName(String name) {
        _subject.setFirstName(name);
    }

    public String getLastName() {
        if (_subject == null) {
            return null;
        }
        return _subject.getLastName();
    }

    public void setLastName(String name) {
        _subject.setLastName(name);
    }

    public String getEmailAddress(){
        if (_subject == null) {
            return null;
        }
        return _subject.getEmailAddress();
    }

    public void setEmailAddress(String emailAddress) {
        _subject.setEmailAddress(emailAddress);
    }
    
    public boolean isHtmlEmail() {
        if (_subject == null) {
            return false;
        }
        return _subject.isHtmlEmail();
    }
    
    public void setHtmlEmail(boolean htmlEmail) {
        _subject.setHtmlEmail(htmlEmail);
    }

    public String getAuthDsn() {
        if (_subject == null) {
            return null;
        }
        return _subject.getAuthDsn();
    }

    public void setAuthDsn(String phoneNumber) {
        _subject.setAuthDsn(phoneNumber);
    }

    public String getPhoneNumber() {
        if (_subject == null) {
            return null;
        }
        return _subject.getPhoneNumber();
    }

    public void setPhoneNumber(String phoneNumber) {
        _subject.setPhoneNumber(phoneNumber);
    }

    public String getDepartment() {
        if (_subject == null) {
            return null;
        }
        return _subject.getDepartment();
    }

    public void setDepartment(String department) {
        _subject.setDepartment(department);
    }

    public boolean getActive() {
        if (_subject == null) {
            return false;
        }
        return _subject.getActive();
    }

    public void setActive(boolean active) {
        _subject.setActive(active);
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
    
    public boolean getHasPrincipal() {
        return _hasPrincipal;
    }

    public void setHasPrincipal(boolean hasPrincipal) {
        _hasPrincipal = hasPrincipal;
    }
    
    public ConfigResponse getPreferences() {
        return _preferences;
    }

    public void setPreferences(ConfigResponse preferences) {
        _preferences = preferences;
    }
    
    public String getPreference(String key)
        throws InvalidOptionException
    {
        String value = _preferences.getValue(key);
        
        if (value == null)
            throw new InvalidOptionException("preference" + key +
                                             " requested is not valid");
        return value.trim();
    }

    public String getPreference(String key, String def) {
        String value = _preferences.getValue(key);
        
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
    public List<String> getPreferenceAsList(String key, String delimiter)
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
        String val;
        if (value == null) {
            val = "";
        }
        else if (value instanceof String) {
            val = (String) value;
        }
        else {
            val = value.toString();
        }
        _preferences.setValue(key, val);
    }

    public void unsetPreference(String key) {
        _preferences.unsetValue(key);
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
     * Encapsulates the logic for how the favorite metrics key for a particular
     * appdef type is calculated
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
    public Map<String,Object> getMetricRangePreference(boolean defaultRange)
        throws InvalidOptionException {
        Map<String, Object> m = new HashMap<String,Object>();

        //  properties may be empty or unparseable strings (ex:
        //  "null"). if so, use their default values.
        Boolean ro;
        try {
            ro = Boolean.valueOf(getPreference(PREF_METRIC_RANGE_RO));
        }
        catch (NumberFormatException nfe) {
            ro = MonitorUtils.DEFAULT_VALUE_RANGE_RO;
        }
        m.put(MonitorUtils.RO, ro);

        Integer lastN;
        try {
            lastN = new Integer(getPreference(PREF_METRIC_RANGE_LASTN));
        }
        catch (NumberFormatException nfe) {
            lastN = MonitorUtils.DEFAULT_VALUE_RANGE_LASTN;
        }
        m.put(MonitorUtils.LASTN, lastN);

        Integer unit;
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
    
    public Map<String,Object> getMetricRangePreference() throws InvalidOptionException {
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

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

package org.hyperic.hq.bizapp.shared.action;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;

public class EmailActionConfig implements ActionConfigInterface {
    public static final String CFG_TYPE  = "listType";
    public static final String CFG_NAMES = "names";
    public static final String CFG_SMS   = "sms";
    
    public static final int TYPE_EMAILS  = 1;
    public static final int TYPE_USERS   = 2;
    public static final int TYPE_ROLES   = 3;
    
    private static String implementor =
        "org.hyperic.hq.bizapp.server.action.email.EmailAction";
    
    private int     _type;
    private String  _names;
    private List    _users;
    private boolean _sms = false;
    
    public EmailActionConfig() {
    }
       
    public ConfigSchema getConfigSchema() {
        IntegerConfigOption type;
        StringConfigOption recip;
        BooleanConfigOption sms;
        ConfigSchema res = new ConfigSchema();

        // Determine the type of recipients
        type = new IntegerConfigOption(CFG_TYPE, 
                                       "Recipient Type ([1] Emails [2] Users)",
                                       new Integer(1));
        type.setMinValue(TYPE_EMAILS);
        type.setMaxValue(TYPE_USERS);
        res.addOption(type);

        // Recipients
        recip = new StringConfigOption(CFG_NAMES, 
                                  "Recipients (comma-delimited Emails or ID's)", 
                                  "");
        recip.setMinLength(0);
        recip.setOptional(true);
        res.addOption(recip);

        // If SMS
        sms = new BooleanConfigOption(CFG_SMS, "Send to users' SMS", false);
        res.addOption(sms);

        return res;
    }

    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException 
    {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_TYPE, String.valueOf(getType()));
        response.setValue(CFG_NAMES, getNames().trim());
        response.setValue(CFG_SMS, isSms());
        return response;
    }

    /** 
     * Initialize the action instance
     */
    public void init(ConfigResponse config) throws InvalidActionDataException {
        // First, let's set the type
        String sType = config.getValue(CFG_TYPE);
        if (sType == null)
            throw new InvalidActionDataException(CFG_TYPE +
                                                 " is a required option");

        _type       = Integer.parseInt(sType);
        String sSms = config.getValue(CFG_SMS);
        _sms        = Boolean.valueOf(sSms).booleanValue();
        _users      = new ArrayList();

        // Parse the recipients
        _names = config.getValue(CFG_NAMES);
        StringTokenizer st = new StringTokenizer(_names, ",;");
        if (sType == null)
            throw new InvalidActionDataException(CFG_NAMES +
                                                 " is a required option");

        while (st.hasMoreTokens()) {
            String input = st.nextToken();
            
            switch (_type) {
            case TYPE_USERS:
            case TYPE_ROLES:
                try {
                    _users.add(new Integer(input));
                } catch (NumberFormatException e) {
                    throw new InvalidActionDataException(
                            "ID is not a valid integer");
                }
                break;
            default:
            case TYPE_EMAILS:
                _users.add(input);
                break;
            }
        }
    }
    
    public int getType() {
        return _type;
    }

    public void setType(int type) {
        _type = type;
    }

    public String getNames() {
        return _names;
    }

    public void setNames(String names) {
        _names = names;
    }

    /**
     * Returns the users.  This is the list of emails, user ID's, or role ID's
     */
    public List getUsers() {
        return _users;
    }

    public boolean isSms() {
        return _sms;
    }

    public void setSms(boolean sms) {
        _sms = sms;
    }

    public void setImplementor(String impl) {
        implementor = impl;
    }

    public String getImplementor() {
        return implementor;
    }
}

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

package org.hyperic.util.i18n;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MessageBundle {
    private ResourceBundle _bundle;
    
    private MessageBundle(String baseName) {
        _bundle = ResourceBundle.getBundle(baseName);
    }
    
    public String format(String propName) {
        return _bundle.getString(propName);
    }

    public String format(String propName, Object[] args) {
        MessageFormat fmt = new MessageFormat(_bundle.getString(propName));
        
        return fmt.format(args);
    }
    
    public String format(String propName, Object arg) {
        return format(propName, new Object[] { arg });
    }
    
    public String format(String propName, Object arg, Object arg2) {
        return format(propName, new Object[] { arg, arg2 });
    }

    public String format(String propName, Object arg, Object arg2, Object arg3) 
    {
        return format(propName, new Object[] { arg, arg2, arg3 });
    }

    public String format(String propName, Object arg, Object arg2, Object arg3,
                         Object arg4) 
    {
        return format(propName, new Object[] { arg, arg2, arg3, arg4 });
    }

    public String format(String propName, Object arg, Object arg2, Object arg3,
                         Object arg4, Object arg5) 
    {
        return format(propName, new Object[] { arg, arg2, arg3, arg4, arg5 });
    }

    public ResourceBundle getResourceBundle() {
        return _bundle;
    }
    
    public static MessageBundle getBundle(String baseName) {
        return new MessageBundle(baseName);
    }
}

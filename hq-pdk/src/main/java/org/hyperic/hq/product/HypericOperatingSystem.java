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

package org.hyperic.hq.product;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class HypericOperatingSystem {
    /**
     * 
     */
    
    public static final String NAME_HYPER_V_WIN32   = "Win-Hyper-V";
    public static final String[] WIN32_NAMES = {
        OperatingSystem.NAME_WIN32,NAME_HYPER_V_WIN32
    };

    public static final String[] NAMES;

    public static final boolean IS_WIN32 =
        OperatingSystem.IS_WIN32;
    
    public static final boolean IS_HYPER_V = isHyperV();
    
    private static HypericOperatingSystem instance = null;

    private static final Map<String,Boolean> supportedPlatforms = new HashMap<String,Boolean>();
    
    static {
        int len = OperatingSystem.UNIX_NAMES.length + WIN32_NAMES.length;
        String[] all = new String[len];
        System.arraycopy(OperatingSystem.UNIX_NAMES, 0, all, 0, OperatingSystem.UNIX_NAMES.length);
        all[len-2] = OperatingSystem.NAME_WIN32;
        all[len-1] = NAME_HYPER_V_WIN32;
        NAMES = all;

        for (int i=0; i<NAMES.length; i++) {
            supportedPlatforms.put(NAMES[i], Boolean.TRUE);
        }
    }

    public static boolean isSupported(String name) {
        return supportedPlatforms.get(name) == Boolean.TRUE;
    }

    public static boolean isWin32(String name) {
        return OperatingSystem.NAME_WIN32.equals(name) || NAME_HYPER_V_WIN32.equals(name);
    }

    private HypericOperatingSystem() {        
    }

    public static synchronized HypericOperatingSystem getInstance() {
        if (instance == null) {
            instance = new HypericOperatingSystem();
        }
        return instance;        
    }
    
    public String getName() {
        // is is hyper-v return hyper -v
        if (!IS_HYPER_V) {
            return OperatingSystem.getInstance().getName();
        }

         return NAME_HYPER_V_WIN32;
    }
    
    public String getDescription() {
        if (!IS_HYPER_V) {
            return OperatingSystem.getInstance().getDescription();
        }

         return "Hyper-V " + OperatingSystem.getInstance().getDescription();  
    }
    
    
    public String getArch() {
        return OperatingSystem.getInstance().getArch();
    }
    
    public String getVersion() {
        return OperatingSystem.getInstance().getVersion();
    }
    
    public String getVendor() {
        return OperatingSystem.getInstance().getVendor();
    }
    
    public String getVendorVersion() {
        return OperatingSystem.getInstance().getVendorVersion();
    }
    
    private static RegistryKey openRootKey(String key) {
        try {
            return RegistryKey.LocalMachine.openSubKey(key);
        }
        catch (Win32Exception e) {             
            return null;
        }
    }
    
    private static boolean isHyperV() {
        if (!OperatingSystem.IS_WIN32) {
            return false;
        }
        RegistryKey rootReg = openRootKey("SYSTEM\\CurrentControlSet\\Services\\vmms");
        if (rootReg == null) {
            return false;
        }
        try {
             String path = rootReg.getStringValue("ImagePath");
             if (path != null && path.length() > 0) {             
                 return true;
             }
             return false;
        }catch(Exception e) {               
             return false;
         }
    }
}


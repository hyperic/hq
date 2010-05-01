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

package org.hyperic.hq.autoinventory.scanimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.PluginLoader;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ServerSignature;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This implementation of ScanMethod knows how to scan the Windows Registry.
 * It can be configurated to scan the whole registry or only certain keys
 * while ignoring others.
 */
public class WindowsRegistryScan extends ScanMethodBase {

    private ConfigResponse platformConfig;
    
    private Log _log = LogFactory.getLog(WindowsRegistryScan.class.getName());

    public WindowsRegistryScan () {
        // we'll give the registry a reliability factor of 7
        _authorityLevel = 7;
    }

    protected ConfigOption[] getOptionsArray(){
        return new ConfigOption[] { };
    }

    public String getName () { return "RegistryScan"; }
    public String getDisplayName () { return "Registry Scan"; }
    public String getDescription () { return "Scan the Windows registry"; }

    private String getPluginName(Object detector) {
        //XXX these plugins should extend GenericPlugin
        //like everything else and then use the
        //GenericPlugin.getName() method.
        String name = detector.getClass().getName();
        int ix = name.lastIndexOf(".");
        return name.substring(ix+1, name.length());
    }

    public void scan (ConfigResponse platformConfig, ServerDetector[] serverDetectors) 
        throws AutoinventoryException {

        this.platformConfig = platformConfig;
        String scanName = "Windows Registry Scan";
        HashMap detectorMap = new HashMap();
        StopWatch timer = null, totalTimer = null;
        boolean isDebug = _log.isDebugEnabled();

        if (isDebug) {
            totalTimer = new StopWatch();
            timer = new StopWatch();
        }

        _log.debug(scanName + " starting...");
        _state.setScanStatus(this, "scan started");
        
        //build a map of RegistryKeys => [detectors]
        //in the case where multiple detectors want to scan
        //the same keys, for example WebLogic 7.0 and 8.1 
        //will scan the same keys.
        for (int i=0; i<serverDetectors.length; i++) {
            if (!(serverDetectors[i] instanceof RegistryServerDetector)) {
                continue;
            }

            RegistryServerDetector detector = 
                (RegistryServerDetector)serverDetectors[i];

            List keys = detector.getRegistryScanKeys();

            for (int j=0; j<keys.size(); j++) {
                String key = (String)keys.get(j);
                List detectors = (List)detectorMap.get(key);

                if (detectors == null) {
                    detectors = new ArrayList();
                    detectorMap.put(key, detectors);
                }

                detectors.add(detector);
            }
        }

        for (Iterator it = detectorMap.entrySet().iterator();
             it.hasNext();)
        {
            if (_scanner.getIsInterrupted()) {
                _log.info("Scanner interrupted.");
                return;
            }

            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            List detectors = (List)entry.getValue();

            String status =
                "scanning: " + key + " with " +
                detectors.size() + " detectors";

            _state.setScanStatus(this, status);

            if (isDebug) {
                timer.reset();
            }

            recursiveSearch(detectors, key);

            if (isDebug) {
                _log.debug(status + " (took " + timer + ")");
            }
        }

        if (isDebug) {
            _log.debug(scanName + " completed, took: " + totalTimer);
        }

        _state.setScanStatus(this, scanName + " completed");
    }

    private RegistryKey openRootKey(String key) {
        try {
            return RegistryKey.LocalMachine.openSubKey(key);
        }
        catch (Win32Exception e) {
            _log.debug("Could not open registry key '" +
                       key + "': " + e.getMessage());
            return null;
        }
    }

    private void recursiveSearch(List detectors,
                                 String key) {
        RegistryKey start;

        if (!key.endsWith("*")) {
            if ((start = openRootKey(key)) == null) {
                return;
            }
            searchKey(detectors, start);
            return;
        }

        //simple pattern match for now is all the existing plugins need.
        //for example if given:
        //"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\MySQL*"
        //only dig into subkeys of "...\\Uninstall" that startWith("MySQL")
        String root;
        int ix = key.lastIndexOf("\\");
        if (ix != -1) {
            root = key.substring(0, ix);
            key = key.substring(ix+1, key.length()-1);
        }
        else {
            _log.error("Malformed key pattern: " + key);
            return;
        }

        if ((start = openRootKey(root)) == null) {
            return;
        }

        _log.debug("Narrowing search of '" + root + "' to ^" +
                   key + "*");

        String[] keyNames = start.getSubKeyNames();
        for (int i=0; i<keyNames.length; i++) {
            RegistryKey subkey;

            if (!keyNames[i].startsWith(key)) {
                //skip search of these keys
                continue;
            }

            try {
                subkey = start.openSubKey(keyNames[i]);
            } catch (Win32Exception e) {
                _log.debug("Could not open registry key '" +
                           key + "'");
                continue;
            }

            searchKey(detectors, subkey);
        }
    }

    // A couple of helper functions to recursively search the registry.
    private void searchValues(List detectors,
                              RegistryKey curr)
    {
        List detectedServers;

        for (int i=0; i<detectors.size(); i++) {
            RegistryServerDetector detector = 
                (RegistryServerDetector)detectors.get(i);

            ServerSignature sig = 
                ((ServerDetector)detector).getServerSignature();

            String[] patterns = sig.getRegistryMatchPatterns();

            for (int j=0; j<patterns.length; j++) {
                String matchValue;

                try {
                    matchValue = curr.getStringValue(patterns[j]);
                    if (matchValue != null) {
                        //trim possible trailing NULL byte
                        matchValue = matchValue.trim();
                    }
                }
                catch (Win32Exception e) {
                    //ok, key does not exist.
                    continue;
                }

                String msg =
                    "Error running " + getPluginName(detector) + ": ";

                PluginLoader.setClassLoader(detector);
                try {
                    detectedServers =
                        detector.getServerResources(this.platformConfig, matchValue, curr);
                } catch (PluginException e) {
                    _log.error(msg + e.getMessage(), e);
                    continue;
                } catch (Exception e) {
                    detectedServers = null;
                    _log.error("Unexpected " + msg + e.getMessage(), e);
                } catch (NoClassDefFoundError e) {
                    //this is possible in the case of running the agent
                    //with the IBM WebSphere 4 jre on a machine that
                    //has WebLogic servers that will be detected since
                    //the WebLogic code uses jdk 1.4 xml apis.
                    //however, this is very unlikely in the real world
                    //and regardless, not possible to monitor WebLogic
                    //using the IBM 1.3 jdk anyhow.
                    detectedServers = null;
                    _log.error("NoClassDefFoundError " + msg + e.getMessage(), e);
                } finally {
                    PluginLoader.resetClassLoader(detector);
                }

                if (detectedServers != null &&
                    detectedServers.size() > 0) {
                    // Add servers to stat
                    // We had a match, save the path and server value.
                    _log.debug("DETECTED SERVERS="
                               + StringUtil.listToString(detectedServers));
                    _state.addServers(this, detectedServers);
                }
            }
        }
    }

    private void searchKey(List detectors,
                           RegistryKey curr)
    {
        searchValues(detectors, curr);
        searchNextKey(detectors, curr);
    }

    private void searchNextKey(List detectors,
                               RegistryKey curr)
    {
        String[] keyNames = curr.getSubKeyNames();

        for (int i = 0; i < keyNames.length; i++) {
            RegistryKey next = null;
            try {
                next = curr.openSubKey(keyNames[i]);
            } catch (Win32Exception e) {
                _log.debug("Unable to open registry key: " + keyNames[i]);
                continue;
            }
            searchValues(detectors, next);
            searchNextKey(detectors, next);
        }
    }
}

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

package org.hyperic.hq.plugin.weblogic;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.timer.StopWatch;

public class WeblogicFinder {

    private static final HashMap skipDirs = new HashMap();

    static {
        skipDirs.put("ldap", Boolean.TRUE);
        skipDirs.put("logs", Boolean.TRUE);
        skipDirs.put(".wlnotdelete", Boolean.TRUE);
    }

    //this is all we have to go on.
    //weblogic doesnt put anything else useful in the registry.
    public static final String UNINSTALL_KEY =
        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall";

    public static final String PRODUCT_NAME =
        "BEA WebLogic Platform";

    private static final String[] VERSIONS = {
        WeblogicProductPlugin.VERSION_81,
        WeblogicProductPlugin.VERSION_70,
    };

    /**
     * guess WebLogic installpath using the registry.
     * returns most recent version found.
     */
    public static File getRegistryInstallPath() {
        List dirs = getRegistryInstallPaths();

        if (dirs.size() == 0) {
            return null;
        }

        return (File)dirs.get(0);
    }

    static File getRegistryInstallPath(String version) {
        String uninstallKey = 
            WeblogicFinder.UNINSTALL_KEY + "\\" +
            WeblogicFinder.PRODUCT_NAME + " " +
            version;

        RegistryKey key = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(uninstallKey);
            String val = key.getStringValue("DisplayIcon");
            if (val == null) {
                return null;
            }
            val = val.trim();
            
            File dir = getInstallRoot(new File(val).getParentFile());
            if (dir != null) {
                return dir;
            }
        } catch (Win32Exception e) {
        } catch (UnsatisfiedLinkError e) {
        } finally {
            if (key != null) {
                key.close();
            }
        }

        return null;
    }
    
    public static List getRegistryInstallPaths() {
        List dirs = new ArrayList();

        for (int i=0; i<VERSIONS.length; i++) {
            File dir =
                getRegistryInstallPath(VERSIONS[i]);
            if (dir != null) {
                dirs.add(dir);
            }
        }

        return dirs;
    }

    public static File getInstallRoot(String dir) {
        return getInstallRoot(new File(dir));
    }

    public static File getInstallRoot(File dir) {
        while (dir != null) {
            if (new File(dir, "server").exists()) {
                return dir;
            }
            dir = dir.getParentFile();
        }

        return null;
    }

    //for example, if we find:
    //C:\bea\weblogic81\samples\domains\medrec\MedRecServer\.internal\console.war
    //then the config.xml should be at:
    //C:\bea\weblogic81\samples\domains\medrec\config.xml
    //the List of configs contains the console.war files since
    //this is what getServerValues expects as defined in
    //cam-server-sigs.properties
    private static File[] find(File dir, final List configs) {
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.getName().equals("console.war") ||
                    file.getName().equals("console"))
                {
                    configs.add(file);
                    return false;
                }
                return file.isDirectory() &&
                    (skipDirs.get(file.getName()) != Boolean.TRUE);
            }
        });

        return dirs;
    }

    //there is no server instance info in the registry,
    //so we have to look on the disk.
    public static void search(File dir, final List configs) {
        File[] dirs = find(dir, configs);
        for (int i=0; i<dirs.length; i++) {
            search(dirs[i], configs);
        }
    }

    public static List getSearchDirs(File root) {
        List dirs = new ArrayList();

        //look for the real stuff first.
        dirs.add(new File(root.getParentFile(), "user_projects"));

        //examples after that.
        File samples = new File(root, "samples");

        File versionSamples = new File(samples, "domains");
        if (versionSamples.exists()) {
            //8.1
            dirs.add(versionSamples);
        }
        else {
            //7.0
            versionSamples =
                new File(samples, "server" + File.separator + "config");
            dirs.add(versionSamples);
        }

        return dirs;
    }

    private static void seekAndDisplay(File dir)  throws Exception {
        if (!dir.exists()) {
            System.out.println(dir + " does not exist.");
            return;
        }

        System.out.println("Scanning " + dir + "\n");
        List configs = new ArrayList();

        StopWatch timer = new StopWatch();
        search(dir, configs);
        System.out.println("Search " + dir + " took: " + timer);

        String[] cArgs = new String[1];

        for (int i=0; i<configs.size(); i++) {
            File war = (File)configs.get(i);
            File path =
                war.getParentFile().getParentFile().getParentFile();
            File configXML = new File(path, "config.xml");

            System.out.println("-->" + configXML);
            cArgs[0] = configXML.getAbsolutePath();
            WeblogicConfig.main(cArgs);
            System.out.println("");
        }
    }

    public static void main(String[] args) throws Exception {
        List dirs = getRegistryInstallPaths();

        StopWatch timer = new StopWatch();

        for (int i=0; i<dirs.size(); i++) {
            File dir = (File)dirs.get(i);

            List searchDirs = getSearchDirs(dir);
            for (int j=0; j<searchDirs.size(); j++) {
                seekAndDisplay((File)searchDirs.get(j));
            }
        }

        System.out.println("Entire search took: " + timer);
    }
}

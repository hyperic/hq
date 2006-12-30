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
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.timer.StopWatch;

public class WeblogicFinder {

    private static final String REG_SERVICES =
        "System\\CurrentControlSet\\Services\\";

    private static final HashMap skipDirs = new HashMap();

    private static List windowsServices;

    static {
        skipDirs.put("ldap", Boolean.TRUE);
        skipDirs.put("logs", Boolean.TRUE);
        skipDirs.put(".wlnotdelete", Boolean.TRUE);
    }

    private static String getCanonicalPath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    private static class WeblogicService {
        private ServiceConfig _config;
        private String _cmdLine;
        private String _execDir;
        private String _binaryPath;

        private WeblogicService(ServiceConfig config)
            throws Win32Exception {

            _config = config;
            String path =
                REG_SERVICES + config.getName() + "\\Parameters";
            RegistryKey key = 
                RegistryKey.LocalMachine.openSubKey(path);
            try {
                _cmdLine = key.getStringValue("CmdLine").trim();
                _execDir = key.getStringValue("ExecDir").trim();
                _execDir = getCanonicalPath(_execDir);
            } finally {
                key.close();
            }
        }

        private String getBinaryPath() {
            if (_binaryPath == null) {
                _binaryPath =
                    getCanonicalPath(_config.getPath().trim());
            }
            return _binaryPath;
        }

        private String getExecDir() {
            return _execDir;
        }

        private boolean isServer() {
            return _cmdLine.endsWith("weblogic.Server");
        }

        private boolean isManagedServer() {
            return _cmdLine.indexOf("-Dweblogic.management.server") != -1;
        }

        private boolean isAdminServer() {
            return isServer() && !isManagedServer();
        }
    }

    private static List getWindowsServices() {
        if (windowsServices == null) {
            windowsServices = new ArrayList();
        }
        else {
            return windowsServices;
        }
        List names;

        try {
             names = Service.getServiceNames();
        } catch (Win32Exception e) {
            return windowsServices;
        }

        for (int i=0; i<names.size(); i++) {
            Service service = null;
            try {
                service = new Service((String)names.get(i));
                ServiceConfig config = service.getConfig();
                String path = config.getPath().trim();
                if (!path.endsWith("beasvc.exe")) {
                    continue;
                }
                WeblogicService beaSvc =
                    new WeblogicService(config);
                windowsServices.add(beaSvc);
            } catch (Win32Exception e){
                continue;
            } finally {
                if (service != null) {
                    service.close();
                }
            }
        }

        return windowsServices;
    }

    static File getServiceInstallPath() {
        return getServiceInstallPath(null);
    }

    static File getServiceInstallPath(String version) {
        WeblogicService service = getWeblogicService(version, false);
        if (service == null) {
            return null;
        }

        File dir = getInstallRoot(service.getBinaryPath());
        return dir;
    }

    static File getAdminServicePath(String version) {
        WeblogicService service = getWeblogicService(version, true);
        if (service == null) {
            return null;
        }

        return new File(service.getExecDir());
    }

    private static WeblogicService getWeblogicService(String version,
                                                      boolean adminOnly) {
        List services = getWindowsServices();
        int size = services.size();
        if (size == 0) {
            return null;
        }

        int v = 0;
        if (version != null) {
            v = version.charAt(0);
        }

        for (int i=0; i<size; i++) {
            WeblogicService service =
                (WeblogicService)services.get(i);

            if (adminOnly && !service.isAdminServer()) {
                continue;
            }

            if (v != 0) {
                if (service.getBinaryPath().indexOf(v) == -1) {
                    continue;
                }
            }

            return service;
        }

        return null;
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

        if (dirs == null) {
            return new File[0];
        }

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
        String path;
        if (args.length == 0) {
            path = WeblogicDetector.getRunningInstallPath();
        }
        else {
            path = args[0];
        }

        File dir = new File(path);
        List searchDirs = getSearchDirs(dir);
        for (int j=0; j<searchDirs.size(); j++) {
            seekAndDisplay((File)searchDirs.get(j));
        }
    }
}

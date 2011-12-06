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

package org.hyperic.hq.plugin.apache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class ErsApacheServerDetector
    extends ApacheServerDetector
    implements RegistryServerDetector {

    private static final String[] PTQL_QUERIES_20 = {
        "State.Name.eq=httpsd.prefork,State.Name.Pne=$1",
        "State.Name.eq=httpsd.worker,State.Name.Pne=$1",
    };

    private static final String[] PTQL_QUERIES_13 = {
        "State.Name.eq=httpsd,State.Name.Pne=$1",
    };

    public ErsApacheServerDetector() {
        super();
    }

    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain an AIServerExtValue
     * for each server defined in the covalent/ers-2.x/servers
     * directory.
     */
    public List getServerList(String installpath,ApacheBinaryInfo binary)
        throws PluginException {

        getLog().debug("[getServerList] installpath="+installpath);
        File serversDir = new File(installpath, "servers");
        File[] serverList = serversDir.listFiles();
        List servers;

        //seen on windows, uninstall ers-2.3.2 leaves tird in the
        //registry but directory is gone from disk.
        if (serverList == null) {
            return null;
        }
        servers = new ArrayList();
        String type = getTypeInfo().getName();

        for (int i = 0; i < serverList.length; i++) {
            if (!serverList[i].isDirectory()) {
                continue;
            }
            String serverRoot = serverList[i].getAbsolutePath();
            File pidFile=new File(serverRoot, getDefaultPidFile());
            if (!pidFile.exists()) {
                //filters non-apache servers (i.e. tomcat) and
                //unused servers, such as "default1.3"
                getLog().debug("[getServerList] pidFile ("+pidFile+") not found.");
                continue;
            }
            String serverName = serverList[i].getName();
            
            ServerResource server = createServerResource(serverRoot);
            String name = server.getName();
            //e.g. name == "hammer Apache-ERS 2.4", serverName == "hammer"
            if (!name.equals(serverName + " " + type)) {
                server.setName(name + " (" + serverName + ")");                
            }

            server.setIdentifier(getAIID(serverRoot));

            if (configureServer(server, binary)) {
                if (isWin32()) {
                    ConfigResponse cf = new ConfigResponse();
                    String sname = getWindowsServiceName(serverName);
                    if (sname != null) {
                        cf.setValue(Win32ControlPlugin.PROP_SERVICENAME, sname);
                        setControlConfig(server, cf);
                    }
                }
                getLog().debug("[getServerList] serverRoot="+serverRoot+" serverName="+serverName+" OK");
                servers.add(server);
            }else{
                getLog().debug("[getServerList] serverRoot="+serverRoot+" serverName="+serverName+" no configured");
            }
        }

        return servers;
    }

    @Override
    protected String getWindowsServiceName() {
        return null;
    }

    protected String getWindowsServiceName(String serverName) {
        String name =  "ERS"+serverName+"httpsd";
        try {
            List services = Service.getServiceNames();
            if (!services.contains(name)) {
                name = "Covalent" + getPlatformName() + "ApacheERS" + getTypeInfo().getVersion();
                if (!services.contains(name)) {
                    name = null;
                }
            }
        } catch (Win32Exception ex) {
            getLog().debug("[getWindowsServiceName] " + ex.getMessage(), ex);
            name = null;
        }
        getLog().debug("[getWindowsServiceName] name=" + name);
        return name;
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        if (isWin32()) {
            return null;
        }

        List servers = new ArrayList();
        List binaries = new ArrayList();
        
        final String version = getTypeInfo().getVersion();

        if (version.equals("4.x")) {
            binaries.addAll(getServerProcessList("2.2", PTQL_QUERIES_20));
        }else if (version.equals("3.x")) {
            binaries.addAll(getServerProcessList("2.0", PTQL_QUERIES_20));
            List binaries_13 =
                getServerProcessList("1.3", PTQL_QUERIES_13);
            if (binaries_13 != null) {
                if (binaries == null) {
                    binaries = binaries_13;
                }
                else {
                    binaries.addAll(binaries_13);
                }
            }
        }

        //ERS has no version info, so we just check for the
        //existance of a unique file
        String versionFile = getTypeProperty("VERSION_FILE");
        if (versionFile == null) {
            return null; //2.3 unsupported
        }

        for (int i=0; i<binaries.size(); i++) {
            ApacheBinaryInfo info =
                (ApacheBinaryInfo)binaries.get(i);

            // /usr/local/covalent/ers-2.4/apache/bin/httpsd.prefork
            // -->
            // /usr/local/covalent/ers-2.4
            String path = getParentDir(info.binary, 3);
            if (!new File(path, versionFile).exists()) {
                continue;
            }
            List found = getServerList(path,info);
            if (found != null) {
                servers.addAll(found);
            }
        }

        return servers;
    }

    /**
     * The path argument here is a path to an apache_startup.sh script.
     * So the corresponding ERS server base dir is two dirs up from that.
     */
    @Override
    public List getServerResources(ConfigResponse platformConfig, String path) throws PluginException {
        String version = getTypeInfo().getVersion();
        ApacheBinaryInfo binary = ApacheBinaryInfo.getInfo(path);

        if ((binary == null) || (binary.version == null)) {
            getLog().debug("[getServerResources] no Binary Info path=" + path + " version=" + version);
            return null; //does not match our server type version
        }
        if (version.equals("4.x") && !binary.version.startsWith("2.2")) {
            getLog().debug("[getServerResources] Binary Info path=" + path + " version=" + version + " no ERS4.x");
            return null; //does not match our server type version
        }
        if (version.equals("3.x") && !binary.version.startsWith("2.0")) {
            getLog().debug("[getServerResources] Binary Info path=" + path + " version=" + version + " no ERS4.x");
            return null; //does not match our server type version
        }

        getLog().debug("[getServerResources] Binary Info path=" + path + " version=" + version + " IS ERS ="+version);
        
        return getServerList(getParentDir(binary.binary, 3), binary);
    }

    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain a single
     * AIServerValue (if a server was found).  Currently the
     * DotOrgDetector does not support detecting multiple instances
     * of Apache in a single directory.
     */
    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current)
        throws PluginException {
        return getServerResources(platformConfig, path);
    }

    private String getAIID (String serverRoot) {
        // prepend the type because ERS-tomcat will use the same AIID
        return "ERS-Apache " + serverRoot;
    }
}

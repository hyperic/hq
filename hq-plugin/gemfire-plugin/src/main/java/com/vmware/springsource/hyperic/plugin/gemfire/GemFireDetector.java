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
package com.vmware.springsource.hyperic.plugin.gemfire;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.jmx.MxServerDetector;

public class GemFireDetector extends MxServerDetector {

    //XXX make public on ServerDetector
    private static final String VERSION_FILE = "VERSION_FILE";
    private static final String VERSION = "VERSION";
    private static final String JAR_FILE = "JAR_FILE";
    private static final String DEF_URL = "service:jmx:rmi://localhost/jndi/rmi://:1099/jmxconnector";
    private static final Log log = LogFactory.getLog(GemFireDetector.class);

    @Override
    protected List getServerProcessList() {
        String jarFile = getTypeProperty(JAR_FILE);
        List procs = new ArrayList();
        long[] pids = getPids(getProcQuery());
        if (log.isDebugEnabled()) {
            log.debug(getProcQuery() + " matched " + pids.length + " processes");
        }

        for (int i = 0; i < pids.length; i++) {
            long pid = pids[i];
            String[] args = getProcArgs(pid);
            String path = null;
            String url = DEF_URL;
            for (int j = 0; j < args.length; j++) {
                String arg = args[j];
                if (arg.equals("-classpath")) {
                    List<String> classpath = Arrays.asList(args[j + 1].split(File.pathSeparator));
                    for (String jar : classpath) {
                        //XXX support regexp...
                        if (jar.endsWith(jarFile)) {
                            path = jar.substring(0, jar.length() - jarFile.length());
                        }
                    }
                } else if (arg.startsWith("rmi-bind-address")) {
                    String host = arg.split("=")[1];
                    url = url.replaceFirst("localhost", host);
                } else if (arg.startsWith("rmi-port")) {
                    String port = arg.split("=")[1];
                    url = url.replaceFirst("1099", port);
                }
            }

            if (path != null) {
                MxProcess process = new MxProcess(pid, args, path);
                process.setURL(url);
                procs.add(process);
            }
        }
        return procs;
    }

    @Override
    protected String getProcQuery(String path) {
        log.debug("[getProcQuery] path=" + path);
        String query=super.getProcQuery(path);
        if(path!=null){
            File jar=new File(path,"/lib/gemfire.jar");
            query+=",Args.*.ct="+jar.getAbsolutePath();
        }
        log.debug("[getProcQuery] query=" + query);
        return query;
    }

    @Override
    protected boolean isInstallTypeVersion(String installpath) {
        log.debug("[isInstallTypeVersion] " + getTypeInfo().getVersion());
        String versionFile = getTypeProperty(VERSION_FILE);
        String version = getTypeProperty(VERSION);

        boolean res = false;
        if (super.isInstallTypeVersion(installpath)) {
            try {
                String v = readFileAsString(new File(installpath, versionFile));
                res = v.contains("gemfire.jar=" + version);  // XXX make regexpr.
                if (!res) {
                    log.debug("[isInstallTypeVersion] (" + version + ") verison=" + v);
                }
            } catch (IOException ex) {
                log.error("Error!!!", ex);
            }
        }
        return res;
    }

    private static String readFileAsString(File filePath) throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.sigar.CpuTimer;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.file.FileUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheBinaryInfo {

    private static final Log log =
        LogFactory.getLog(ApacheBinaryInfo.class.getName());

    private static final String[] CTL_SCRIPTS = {
        "apachectl", "apache2ctl"
    };

    private static HashMap cache = null;
    private static final String APACHE_VERSION = "Apache/";
    private static final String SERVER_VERSION = "Server version:";
    private static final String SERVER_BUILT = "Server built:";
    private static final String MPM_DIR = "-D APACHE_MPM_DIR=\"";
    
    public String version = null;
    public String root = null;
    public String binary;
    public String ctl;
    public String built;
    public String mpm;
    public String name;

    public String errmsg = "";
    private long lastModified = 0;

    private ApacheBinaryInfo() {}

    public static synchronized ApacheBinaryInfo getInfo(String binary) {
        if (cache == null) {
            cache = new HashMap();
        }

        ApacheBinaryInfo info = (ApacheBinaryInfo)cache.get(binary);

        long lastModified = new File(binary).lastModified();

        if ((info == null) || (lastModified != info.lastModified)) {
            info = new ApacheBinaryInfo();
            info.binary = binary;
            info.ctl = binary;
            info.lastModified = lastModified;
            cache.put(binary, info);

            try {
                info.getApacheBinaryInfo(binary);
            } catch (IOException e) {
                info.errmsg = e.getMessage();
            }
        }

        return info;
    }

    public static ApacheBinaryInfo getInfo(String binary,
                                           String wantedVersion) {

        ApacheBinaryInfo info = getInfo(binary);

        if (info.version == null) {
            return null;
        }

        if (info.version.startsWith(wantedVersion)) {
            return info;
        }

        return null;
    }

    public static String getVersion(String binary,
                                    String wantedVersion) {

        ApacheBinaryInfo info = getInfo(binary);

        if (info == null) {
            return null;
        }

        if (info.version.startsWith(wantedVersion)) {
            return info.version;
        }

        return null;
    }

    public static String getRootDir(String binary,
                                    String wantedVersion) {

        ApacheBinaryInfo info = getInfo(binary);

        if (info == null) {
            return null;
        }

        if (info.version.startsWith(wantedVersion)) {
            return info.root;
        }

        return null;
    }

    private boolean findVersion(String binary) throws IOException {
        if (this.version != null) {
            return true;
        }

        this.errmsg = "";
        String line = FileUtil.findString(binary, APACHE_VERSION);

        if (line == null) {
            this.errmsg =
                "Unable to find '" + APACHE_VERSION + "' in: " +
                binary;
            return false;
        }

        int ix = line.indexOf(" ");
        if (ix != -1) {
            line = line.substring(0, ix);
        }

        ix = line.lastIndexOf('/');
        this.version = line.substring(ix+1);
        return true;
    }
    
    private String findDefine(String binary, String name)
        throws IOException {
        
        this.errmsg = "";
        String define = "-D " + name + "=\"";
        String line = FileUtil.findString(binary, define);
        if (line == null) {
            this.errmsg =
                "Unable to find -D " + name + " in: " +
                binary;
            return null;
        }

        String value =
            line.substring(define.length(),
                           line.length()-1);

        if (value.length() == 0) {
            this.errmsg =
                "Found -D " + name + " in: " +
                binary + " but value is empty";
            value = null; //e.g. debian's apache2
        }
        
        return value;
    }
    
    private boolean findRoot(String binary) throws IOException {
        if ((this.root = findDefine(binary, "HTTPD_ROOT")) == null) {
            String file = findDefine(binary, "SERVER_CONFIG_FILE");
            if (file != null) {
                File conf = new File(file);
                if (conf.isAbsolute() && conf.exists()) {
                    //e.g. debian is /etc/apache2
                    this.root = conf.getParent();
                }
            }
        }

        if (this.root == null) {
            return false;
        }
        else {
            return true;
        }
    }

    private void getVersionCmdInfo(String binary) {
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

        ExecuteWatchdog watchdog = 
            new ExecuteWatchdog(1000);

        Execute ex =
            new Execute(new PumpStreamHandler(stdOut, stdErr),
                        watchdog);

        ex.setCommandline(new String[] { binary, "-V" });
        BufferedReader is = null;
        
        try {
            String line;
            ex.execute();
            is = new BufferedReader(new StringReader(stdOut.toString()));

            while ((line = is.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(SERVER_VERSION)) {
                    line = line.substring(SERVER_VERSION.length()).trim();
                    int ix = line.indexOf('/');
                    if (ix != -1) {
                        line = line.substring(ix+1);
                        ix = line.indexOf(' ');
                        if (ix != -1) {
                            line = line.substring(0, ix);
                        }
                        this.version = line;
                    }
                }
                else if (line.startsWith(SERVER_BUILT)) {
                    line = line.substring(SERVER_BUILT.length()).trim();
                    this.built = line;
                }
                else if (line.startsWith(MPM_DIR)) {
                    line = line.substring(MPM_DIR.length()).trim();
                    int ix = line.lastIndexOf('"');
                    if (ix != -1) {
                        line = line.substring(0, ix);
                    }
                    ix = line.lastIndexOf("/");
                    if (ix != -1) {
                        line = line.substring(ix+1);
                    }
                    this.mpm = line;
                }
            }
        } catch (Exception e) {
            String msg =
                "Error running binary '" + binary + "': " +
                e.getMessage();
            log.error(msg, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
    }

    private void getApacheBinaryInfo(String binary)
        throws IOException {

        File binaryFile = new File(binary);
        if (binaryFile.isDirectory()) {
            this.errmsg = binaryFile + " is a directory";
            return;
        }

        getVersionCmdInfo(binary);

        boolean isDLL = false;
        File libhttpd = null;
        File bindir = binaryFile.getParentFile();
        if (bindir == null) {
            throw new IOException(binary + " has no parent directory");
        }

        if (ApacheServerDetector.isWin32()) {
            //on windows Apache/version is in libhttpd.dll
            //the other -D FOO=BAR props are in httpd.exe
            libhttpd = new File(bindir, "libhttpd.dll");
            if (libhttpd.exists()) {
                isDLL = true;
            }
        }
        else {
            //If libhttpd.so exists in libexec, then we need
            //to search that file instead of the Apache binary.
            if ((libhttpd = bindir.getParentFile()) != null) {
                libhttpd = new File(libhttpd, "libexec/libhttpd.so");
                if (libhttpd.exists()) {
                    binary = libhttpd.getAbsolutePath();
                }
            }
        }

        for (int i=0; i<CTL_SCRIPTS.length; i++) {
            File script = new File(bindir, CTL_SCRIPTS[i]);
            if (script.exists()) {
                this.ctl = script.toString();
            }
        }

        if (!(findVersion(binary) ||
              (isDLL && findVersion(libhttpd.getAbsolutePath()))))
        {
            return;
        }

        if (!findRoot(binary)) {
            return;
        }
    }
    
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("exe", this.binary);
        if (this.mpm != null) {
            props.setProperty("mpm", this.mpm);
        }
        if (this.built != null) {
            props.setProperty("built", this.built);
        }
        return props;
    }

    public String toString() {
        String info =
            "version=" + this.version +
            ", root=" + this.root +
            ", binary=" + this.binary +
            ", ctl=" + this.ctl;

        if (this.mpm != null) {
            info += ", mpm=" + this.mpm;
        }
        
        if (this.built != null) {
            info += ", build=" + this.built;
        }

        if (this.errmsg.length() > 0) {
            info += ", errmsg=" + this.errmsg;
        }

        return info;
    }

    public static void main(String[] args) {
        CpuTimer cpu = new CpuTimer();
        cpu.start();

        String binary = args[0];
        System.out.println(getInfo(binary));

        cpu.stop();
        cpu.list(System.out);
    }
}

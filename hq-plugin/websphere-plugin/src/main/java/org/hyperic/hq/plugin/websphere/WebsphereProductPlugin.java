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
package org.hyperic.hq.plugin.websphere;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class WebsphereProductPlugin extends ProductPlugin {

    public static boolean VALID_JVM = true;
    public static final String NAME = "websphere";
    public static final String SERVER_NAME = "WebSphere";
    public static final String SERVER_DESC = "Application Server";
    public static final String ADMIN_NAME = "Admin";
    public static final String THRPOOL_NAME = "Thread Pool";
    public static final String CONNPOOL_NAME = "Connection Pool";
    public static final String APP_NAME = "Application";
    public static final String WEBAPP_NAME = "Webapp";
    public static final String EJB_NAME = "EJB";
    public static final String SERVER_ADMIN_NAME =
            SERVER_NAME + " " + ADMIN_NAME;
    public static final String VERSION_50 = "5.0";
    public static final String PROP_INSTALL_ROOT = "was.install.root";
    public static final String PROP_ADMIN_HOST = "admin.host";
    public static final String PROP_ADMIN_PORT = "admin.port";
    public static final String PROP_USERNAME = "username";
    public static final String PROP_PASSWORD = "password";
    public static final String PROP_SERVER_NODE = "server.node";
    public static final String PROP_SERVER_NAME = "server.name";
    public static final String PROP_SERVER_PORT = "server.port";
    public static final String PROP_SERVER_CELL = "server.cell";
    public static final String PROP_THRPOOL_NAME = "thrpool";
    public static final String PROP_CONNPOOL_NAME = "connpool";
    public static final String PROP_APP_NAME = "app";
    public static final String PROP_WEBAPP_NAME = "webapp";
    public static final String PROP_EJB_NAME = "ejb";
    public static final String PROP_WEBAPP_DISPLAY_NAME =
            PROP_WEBAPP_NAME + ".display";
    public static final String PROP_WEBAPP_CONTEXT =
            PROP_WEBAPP_NAME + ".context";
    public static final String PROP_EJB_JNDI_NAME =
            PROP_EJB_NAME + ".jndi";
    //XXX rid need to configure this in agent.properties
    public static final String PROP_INSTALLPATH =
            "websphere." + ProductPlugin.PROP_INSTALLPATH;
    private static final String[] REG_VERSIONS = {
        "5.1.0.0",
        "5.0.0.0",
        "6.0.0.0",
        "6.1.0.0",
        "7.0.0.0",
        "8.0.0.0",
        "8.5.0.0"};
    private static Log log = LogFactory.getLog("WebsphereProductPlugin");
    private static String REG_KEY;
    private static boolean hasSoapConfig = false;
    private static boolean isOSGi = false;
    private static boolean useExt = false;
    static boolean useJMX = true;
    private static String tmpdir;

    //if we are running with the ibm jdk we can configure
    //websphere.installpath ourselves.
    private static String getInstallPathFromJDK() {
        String res = null;
        if (VALID_JVM) {
            String javaHome = System.getProperty("java.home");
            File dir = new File(javaHome);
            do {
                if (!new File(dir, "profiles").exists()) {
                    dir = dir.getParentFile();
                } else {
                    res = dir.getAbsolutePath();
                }
            } while ((res == null) && (dir.getParentFile() != null));
        }
        return res;
    }

    /**
     * guess WebSphere installpath using the registry. returns most recent
     * version found.
     */
    public static File getRegistryInstallPath() {
        List<File> dirs = getRegistryInstallPaths();

        if (dirs.size() == 0) {
            return null;
        }

        return dirs.get(0);
    }

    public static List<File> getRegistryInstallPaths() {
        List<File> dirs = new ArrayList<File>();

        try {
            RegistryKey key =
                    RegistryKey.LocalMachine.openSubKey(REG_KEY);

            for (int i = 0; i < REG_VERSIONS.length; i++) {
                String name = REG_VERSIONS[i];

                try {
                    RegistryKey ver = key.openSubKey(name);
                    String val = ver.getStringValue("InstallLocation");
                    if (val != null) {
                        dirs.add(new File(val.trim()));
                    }
                } catch (Win32Exception e) {
                    log.debug(e, e);
                }
            }
        } catch (Win32Exception e) {
            log.debug(e, e);
        } catch (UnsatisfiedLinkError e) {
            log.debug(e, e);
        } //dll is not on server-side

        return dirs;
    }

    private static String findInstallDir() {
        String dir = null;
        String where = "unknown";

        if (isWin32()) {
            File path = getRegistryInstallPath();
            if (path != null) {
                dir = path.getAbsolutePath();
                where = "registry";
            }
        }

        if (dir == null) {
            dir = WebsphereDetector.getRunningInstallPath();
            if (dir != null) {
                where = "process table";
            } else {
                dir = getInstallPathFromJDK();
                if (dir != null) {
                    where = "JRE";
                }
            }

            if (dir == null) {
                String root;
                if (isWin32()) {
                    root = "C:/Program Files";
                } else {
                    root = "/opt";
                }
                //default WebSphere installpath(s)
                dir = root + "/IBM/WebSphere/AppServer";
                if (!new File(dir).isDirectory()) {
                    dir = root + "/WebSphere/AppServer";
                    if (!new File(dir).isDirectory()) {
                        dir = null;
                    }
                }
                where = "default location";
            }
        }

        if (dir == null) {
            log.debug("Unable to determine " + PROP_INSTALLPATH);
            return null;
        } else {
            log.debug(PROP_INSTALLPATH + " configured using " + where + " (" + dir + ")");
            return dir;
        }
    }

    public static boolean hasSoapConfig() {
        return hasSoapConfig;
    }

    public static boolean isOSGi() {
        return isOSGi;
    }

    private void addClassPath(List<String> path,
            String dir,
            String[] jars) {

        if (jars == null) {
            return;
        }

        for (int j = 0; j < jars.length; j++) {
            if (!jars[j].endsWith(".jar")) {
                continue;
            }

            File f = new File(dir, jars[j]);
            log.debug("- Classpath += " + f);
            try {
                path.add(f.getCanonicalPath());
            } catch (IOException ex) {
                path.add(f.getAbsolutePath());
            }
        }
    }

    //jar names minus version "_6.1.0.jar"
    private void addClassPathOSGi(List<String> path, String dir, String[] jars) {
        log.debug("Adding OSGi packages from " + dir);
        final HashMap<String, Boolean> wantedJars = new HashMap<String, Boolean>();
        for (int i = 0; i < jars.length; i++) {
            wantedJars.put(jars[i], Boolean.TRUE);
        }

        String[] files =
                new File(dir).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                int ix = name.indexOf('_');
                if (ix != -1) {
                    name = name.substring(0, ix);
                }
                return wantedJars.get(name) != null;
            }
        });

        addClassPath(path, dir, files);
    }
    
    private void copyToTmpAndAddClassPath(List<String> path, String dir, String[] jars) {
        final HashMap<String, Boolean> wantedJars = new HashMap<String, Boolean>();
        for (int i = 0; i < jars.length; i++) {
            wantedJars.put(jars[i], Boolean.TRUE);
        }

        String[] files = new File(dir).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                int ix = name.indexOf('_');
                if (ix != -1) {
                    name = name.substring(0, ix);
                }
                return wantedJars.get(name) != null;
            }
        });

        String tmp = dir;
        if (tmp.contains(" ")) {
            tmp = tmpdir;
            if (tmp.contains(" ")) {
                tmp = isWin32() ? "c:\\tmp" : "/tmp";
            }
            log.debug("Websphere install path (" + dir + ") contains a ' ', copying necesary jars to '" + tmp + "' (websphere.tmpdir)");
        }

        for (String jar : jars) {
            try {
                copyFile(new File(dir,jar), new File(tmp,jar));
            } catch (IOException ex) {
                log.debug(ex, ex);
            }
        }
        
        addClassPathOSGi(path, tmp, jars);
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        log.debug("[copyFile] " + sourceFile.getCanonicalPath() + " => " + destFile.getCanonicalPath());

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        InputStream in = new FileInputStream(sourceFile);
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    //XXX bigmac hackattack
    //org.apache.commons.logging is embedded in this .jar
    //conflicts with our pdk/lib/commons-logging.jar
    //rewrite the .jar and remove the conflicting package
    private File runtimeJarHack(File file) throws Exception {
        String tmp = System.getProperty("java.io.tmpdir"); //agent/tmp
        File newJar = new File(tmp, file.getName());
        if (newJar.exists()) {
            return newJar;
        }
        log.debug("Creating " + newJar);
        JarFile jar = new JarFile(file);
        JarOutputStream os = new JarOutputStream(new FileOutputStream(newJar));
        byte[] buffer = new byte[1024];
        try {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                int n;
                JarEntry entry = e.nextElement();

                if (entry.getName().startsWith("org/apache/commons/logging/")) {
                    continue;
                }
                InputStream entryStream = jar.getInputStream(entry);

                os.putNextEntry(entry);
                while ((n = entryStream.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
                entryStream.close();
            }
        } finally {
            jar.close();
            os.close();
        }
        return newJar;
    }

    //XXX HHQ-2044 emulate -Djava.ext.dirs
    private void setExtDirs(String installDir) throws Exception {
        ClassLoader parent = getClass().getClassLoader().getParent();
        URLClassLoader loader = null;

        //rewind to top most parent, which should (must) be ExtClassLoader
        while (parent != null) {
            if (parent instanceof URLClassLoader) {
                loader = (URLClassLoader) parent;
            }
            parent = parent.getParent();
        }

        log.debug("Using java.ext.dirs loader=" + loader);

        //bypass protected access.
        Method addURL =
                URLClassLoader.class.getDeclaredMethod("addURL",
                new Class[]{
                    URL.class
                });

        addURL.setAccessible(true);

        String[] dirs = {
            "lib",
            "plugins",
            "runtimes"
        };

        for (int i = 0; i < dirs.length; i++) {
            File dir = new File(installDir, dirs[i]);
            String[] jars = dir.list();
            if (jars != null) {
                for (int j = 0; j < jars.length; j++) {
                    File jar = new File(dir, jars[j]);
                    if (isValidJar(jar)) {
                        log.debug("classpath += " + jar);
                        if (jar.getName().startsWith("com.ibm.ws.runtime_")) {
                            jar = runtimeJarHack(jar);
                        }
                        URL url = sun.net.www.ParseUtil.fileToEncodedURL(jar);
                        addURL.invoke(loader, new Object[]{url});
                    } else {
                        log.debug("jar '" + jars[j] + "' skipped");
                        continue;
                    }
                }
            }
        }
    }

    private boolean isValidJar(File jar) {
        return jar.getName().startsWith("com.ibm")
                && jar.getName().endsWith(".jar")
                && !jar.getName().contains("thinclient")
                && jar.isFile();
    }

    private String[] getClassPathOSGi(String installDir) {
        ArrayList<String> path = new ArrayList<String>();

        String v = WebsphereDetector.getComponentVersion(new File(installDir, "properties/version/WAS.product"));
        if (v.startsWith("6")) {

            final String[] lib = {
                "bootstrap.jar",
                "j2ee.jar",
                "launchclient.jar",
                "mail-impl.jar"
            };
            addClassPathOSGi(path, installDir + "/lib/", lib);

            final String[] plugins = {
                "com.ibm.ws.bootstrap",
                "com.ibm.ws.emf",
                "com.ibm.ws.security.crypto",
                "com.ibm.ws.wccm",
                "org.eclipse.core.runtime",
                "org.eclipse.osgi",
                "com.ibm.ws.runtime",};
            addClassPathOSGi(path, installDir + "/plugins/", plugins);

            final String[] runtimes = {
                "com.ibm.ws.admin.client",};
            addClassPathOSGi(path, installDir + "/runtimes/", runtimes);

        } else if (v.startsWith("7") || v.startsWith("8")) {
            final String[] lib = {
                "bootstrap.jar",};
            addClassPathOSGi(path, installDir + "/lib/", lib);

            final String[] plugins = {
                "com.ibm.ffdc.jar",
                "com.ibm.ws.admin.core.jar",
                "com.ibm.ws.runtime.jar",
                "com.ibm.ws.emf.jar",
                "com.ibm.hpel.logging.jar",
                "org.eclipse.emf.ecore.jar",
                "org.eclipse.emf.common.jar",
                "com.ibm.ws.prereq.soap.jar",
                "com.ibm.ws.prereq.javamail.jar",
                "com.ibm.wsfp.main.jar",
                "javax.j2ee.management.jar",};
            addClassPathOSGi(path, installDir + "/plugins/", plugins);
            
            final String[] jarToCopy = {"com.ibm.ws.security.crypto.jar"};
            copyToTmpAndAddClassPath(path, installDir + "/plugins/", jarToCopy);
        } else {
            if (log.isDebugEnabled()) {
                log.error("Unknown version '" + v + "'");
            }
        }

        path.add("pdk/lib/mx4j/hq-jmx.jar");
        String[] cp = new String[path.size()];
        path.toArray(cp);
        return cp;
    }

    @Override
    public String[] getClassPath(ProductPluginManager manager) {
        OperatingSystem os = OperatingSystem.getInstance();
        boolean testIBMJDK = (os.getName().equals(OperatingSystem.NAME_LINUX)
                || os.getName().equals(OperatingSystem.NAME_WIN32));
        //assert testIBMJDK : os.getName();
        if (testIBMJDK) {
            VALID_JVM = System.getProperty("java.vm.vendor").toUpperCase().indexOf("IBM") != -1;
            if (!VALID_JVM) {
                log.debug("The WebSphere plugin needs a IBM JVM !!! "
                        + "(agent jvm=" + System.getProperty("java.vm.vendor") + ")");
            }
            assert VALID_JVM;
        }

        if (isWin32()) {
            String prop = "websphere.regkey";
            REG_KEY = getPluginProperty(prop);
            if (REG_KEY == null) {
                log.error("Prop " + prop + "undefined");
                return new String[]{};
            }
        }

        Properties managerProps = manager.getProperties();

        tmpdir = managerProps.getProperty("websphere.tmpdir", System.getProperty("java.io.tmpdir"));
        useJMX = Boolean.valueOf(managerProps.getProperty("websphere.usejmx", "true"));
        useExt = Boolean.valueOf(managerProps.getProperty("websphere.useext", "false"));
        String installDir = managerProps.getProperty(PROP_INSTALLPATH,findInstallDir());

        log.debug("websphere.tmpdir = " + tmpdir);
        log.debug("websphere.usejmx = " + useJMX);
        log.debug("websphere.useext = " + useExt);
        log.debug(PROP_INSTALLPATH + " = " + installDir);


        if (installDir == null) {
            return new String[]{"pdk/lib/mx4j/hq-jmx.jar"};
        }

        //required for authentication
        String defaultCorbaConfig =
                installDir + File.separator
                + "properties" + File.separator + "sas.client.props";

        String corbaConfig =
                managerProps.getProperty("websphere.CORBA.ConfigURL",
                defaultCorbaConfig);

        System.setProperty("com.ibm.CORBA.ConfigURL",
                "file:" + corbaConfig);

        String defaultSoapProps =
                "properties" + File.separator + "soap.client.props";

        File installPropertiesDir =
                getInstallPropertiesDir(installDir, defaultSoapProps);
        String soapConfig = null;
        File soapConfigFile = null;
        if (installPropertiesDir != null && installPropertiesDir.exists()) {
            String defaultSoapConfig = installPropertiesDir.getPath();
            log.debug("default soap config is " + defaultSoapConfig);

            soapConfig =
                    managerProps.getProperty("websphere.SOAP.ConfigURL",
                    defaultSoapConfig);

            soapConfigFile = new File(soapConfig);
            hasSoapConfig = soapConfigFile.exists();
        }

        if (hasSoapConfig) {
            log.debug("Using soap properties: " + soapConfig);
            System.setProperty("com.ibm.SOAP.ConfigURL",
                    "file:" + soapConfig);
        } else {
            log.debug("Unable to find soap.client.props in " + soapConfig);
        }

        isOSGi =
                new File(installDir, "/plugins").isDirectory();
        log.debug("isOSGi=" + isOSGi);

        //required for 6.1
        File sslConfigFile = null;
        if (hasSoapConfig && soapConfigFile != null) {
            sslConfigFile =
                    new File(soapConfigFile.getParent(),
                    "ssl.client.props");
        }

        if (sslConfigFile != null && sslConfigFile.exists()) {
            log.debug("Using ssl properties: " + sslConfigFile);
            System.setProperty("com.ibm.SSL.ConfigURL",
                    "file:" + sslConfigFile);
        } else if (isOSGi) {
            log.debug("Unable to find ssl.client.props");
        }

        //required for 6.x
        System.setProperty(PROP_INSTALL_ROOT, installDir);

        if (isOSGi) {
            if (useExt) {
                try {
                    setExtDirs(installDir);
                    return new String[]{"pdk/lib/mx4j/hq-jmx.jar"};
                } catch (Exception e) {
                    log.error("setExtDirs: " + e, e);
                }
            }
            return getClassPathOSGi(installDir);
        }

        return new String[]{
                    //5.0 + 5.1
                    installDir + "/java/jre/lib/core.jar",
                    installDir + "/java/jre/lib/ext/log.jar",
                    installDir + "/lib/bootstrap.jar",
                    installDir + "/lib/wssec.jar",
                    installDir + "/lib/pmiclient.jar",
                    installDir + "/lib/wasjmx.jar",
                    installDir + "/lib/soap.jar",
                    installDir + "/lib/sas.jar",
                    installDir + "/lib/pmi.jar",
                    installDir + "/lib/wsexception.jar",
                    installDir + "/lib/jmxc.jar",
                    installDir + "/lib/jmxx.jar",
                    installDir + "/lib/jmxext.jar",
                    installDir + "/lib/jflt.jar",
                    installDir + "/lib/nls.jar",
                    installDir + "/lib/ffdc.jar",
                    installDir + "/lib/idl.jar",
                    //5.0
                    installDir + "/java/jre/lib/ext/ibmjsse.jar",
                    installDir + "/java/jre/lib/ext/ibmjssefips-ob.jar",
                    //5.1
                    installDir + "/java/jre/lib/security.jar",
                    //5.1.1
                    installDir + "/java/jre/lib/ibmjsseprovider.jar",
                    //4.0
                    installDir + "/lib/perf.jar",
                    installDir + "/lib/websphere.jar",
                    installDir + "/lib/ujc.jar",
                    installDir + "/lib/ns.jar",
                    installDir + "/lib/iwsorb.jar",
                    //4.0 AE
                    installDir + "/lib/ejbcontainer.jar",
                    installDir + "/lib/jts.jar",
                    installDir + "/lib/csicpi.jar",
                    installDir + "/lib/repository.jar",
                    //both 4.0 + 5.0
                    installDir + "/lib/admin.jar",
                    installDir + "/lib/xerces.jar",
                    installDir + "/lib/j2ee.jar",
                    installDir + "/lib/ras.jar",
                    installDir + "/lib/utils.jar",
                    installDir + "/java/jre/lib/ext/mail.jar",
                    //6.0
                    installDir + "/lib/management.jar",
                    installDir + "/lib/mail-impl.jar",
                    installDir + "/lib/emf.jar",
                    installDir + "/lib/utils.jar",
                    installDir + "/lib/runtime.jar",
                    installDir + "/lib/classloader.jar",
                    installDir + "/lib/security.jar",
                    installDir + "/lib/wasproduct.jar",
                    installDir + "/lib/pmij2ee.jar",
                    installDir + "/java/jre/lib/ibmcertpathprovider.jar",
                    installDir + "/java/jre/lib/ext/ibmjceprovider.jar",
                    installDir + "/java/jre/lib/ext/ibmjcefips.jar",
                    installDir + "/etc/tmx4jTransform.jar",
                    "pdk/lib/mx4j/hq-jmx.jar"
                };
    }

    File getInstallPropertiesDir(String installDir, String lookFor) {
        File result = getInstallPropertiesDirForWAS61(lookFor);

        if (result == null) {
            result = getInstallPropertiesDirForWAS60(installDir, lookFor);
        }

        return result;
    }

    File getInstallPropertiesDirForWAS61(String lookFor) {

        File result = null;

        List servers = getServerProcessList();
        if (servers.size() != 0) {
            for (Iterator it = servers.iterator(); it.hasNext();) {
                WebSphereProcess process = (WebSphereProcess) it.next();

                if (process.getServerRoot() != null) {
                    File candidate = new File(process.getServerRoot(), lookFor);
                    if (candidate.exists()) {
                        log.debug("Getting WAS properties from profile under server.root: "
                                + candidate);
                        result = candidate;
                        break;
                    }
                }
            }
        }

        return result;
    }

    List getServerProcessList() {
        return WebsphereDetector.getServerProcessList();
    }

    File getInstallPropertiesDirForWAS60(String installDir, String lookFor) {

        File result = null;

        File profileDir = new File(installDir + File.separator + "profiles");
        if (profileDir.exists()) {
            File candidateDir = new File(profileDir
                    + File.separator + "default");
            if (candidateDir.exists()) {
                File candidate = new File(candidateDir, lookFor);
                if (candidate.exists()) {
                    log.debug("Getting WAS properties from default profile location: "
                            + candidate);
                    result = candidate;
                }
            }

            if (result == null) {
                // Look under non-default profiles.
                if (profileDir.isDirectory()) {
                    String[] profiles = profileDir.list();
                    if (profiles != null) {
                        for (int i = 0; i < profiles.length; i++) {
                            candidateDir = new File(profileDir, profiles[i]);
                            if (candidateDir.exists()) {
                                File candidate = new File(candidateDir, lookFor);
                                if (candidate.exists()) {
                                    log.debug("Getting WAS properties from profile non-default location: "
                                            + candidate);
                                    result = candidate;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}

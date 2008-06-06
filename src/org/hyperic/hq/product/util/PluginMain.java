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

package org.hyperic.hq.product.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.util.PluginLoader;

/**
 * Run the main method for a plugin class.
 * Example:
 * java -jar pdk/lib/hq-product.jar apache ApacheServerDetector
 */
//cut-n-pasted-n-chopped from sigar.cmd.Runner class
public class PluginMain {

    private static final String DEFAULT_PACKAGE =
        "org.hyperic.hq.plugin";

    private static URL jarURL(String jar) throws Exception {
        return new URL("jar", null, "file:" + jar + "!/");
    }

    private static URL[] getLibJars(String dir) throws Exception {
        File[] jars = new File(dir).listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".jar");
            }
        });

        if (jars == null) {
            return new URL[0];
        }

        URL[] urls = new URL[jars.length];

        for (int i=0; i<jars.length; i++) {
            urls[i] = jarURL(jars[i].getAbsolutePath());
        }

        return urls;
    }

    private static URLClassLoader getLoader() {
        return (URLClassLoader)Thread.currentThread().getContextClassLoader();
    }

    private static String getPdkDir() {
        URL[] urls = getLoader().getURLs();
        for (int i=0; i<urls.length; i++) {
            String url = urls[i].getFile();
            if (!url.endsWith(PluginDumper.PRODUCT_JAR)) {
                continue;
            }
            url = URLDecoder.decode(url); //"%20" -> " "
            //strip lib/hq-product.jar
            return new File(url).getParentFile().getParent();
        }
        return "pdk";
    }

    private static void addURLs(URL[] jars) throws Exception {
        URLClassLoader loader = getLoader();

        //bypass protected access.
        Method addURL =
            URLClassLoader.class.getDeclaredMethod("addURL",
                                                   new Class[] {
                                                       URL.class
                                                   });

        addURL.setAccessible(true); //pound sand.

        for (int i=0; i<jars.length; i++) {
            addURL.invoke(loader, new Object[] { jars[i] });
        }
    }

    private static void addJarDir(String dir) throws Exception {
        URL[] jars = getLibJars(dir);
        addURLs(jars);
    }

    private static Class getPluginClass(PluginDumper pd,
                                        String plugin,
                                        String className) throws Exception {

        pd.init();
        ProductPlugin productPlugin = pd.ppm.getProductPlugin(plugin);
        
        String packageName = productPlugin.getPluginProperty("package");
        if (packageName == null) {
            packageName = DEFAULT_PACKAGE + "." + plugin;
        }

        String mainClass = packageName + "." + className;
        PluginLoader.setClassLoader(productPlugin);
        try {
            return Class.forName(mainClass, true,
                                 productPlugin.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            System.out.println("Invalid ClassName: " + mainClass);
            return null;
        } finally {
            PluginLoader.resetClassLoader(productPlugin);
        }
    }

    private static void runMain(PluginDumper pd,
                                String[] args) throws Exception {
        int offset;
        ProductPlugin productPlugin = null;
        String plugin, className=null, mainClass=null;
        final String usage = 
            "Usage: PluginMain plugin ClassName";

        if (args.length < 1) {
            throw new IllegalArgumentException(usage);
        }

        plugin = args[0];
        if (plugin.indexOf(".") != -1) {
            //example:
            //java -jar pdk/lib/hq-product.jar \
            //org.hyperic.hq.product.URLMetric https://localhost/
            mainClass = plugin;
            plugin = null;
            offset = 1;
        }
        else {
            if (args.length < 2) {
                throw new IllegalArgumentException(usage);
            }
            //example:
            //java -jar pdk/lib/hq-product.jar \
            //apache ApacheServerDetector
            className = args[1];
            offset = 2;
        }

        String[] pargs = new String[args.length - offset];
        System.arraycopy(args, offset, pargs, 0, args.length-offset);

        Class cmd = null;
        if (plugin != null) {
            cmd = getPluginClass(pd, plugin, className);
            if (cmd == null) {
                return;
            }
            productPlugin = pd.ppm.getProductPlugin(plugin);
        }
        else {
            try {
                cmd = Class.forName(mainClass);
            } catch (ClassNotFoundException e) {
                System.out.println("Invalid ClassName: " + mainClass);
                return;    
            }
        }

        Method main = cmd.getMethod("main",
                                    new Class[] {
                                        String[].class
                                    });

        if (productPlugin != null) {
            PluginLoader.setClassLoader(productPlugin);
        }
        try {
            main.invoke(null, new Object[] { pargs });
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof NoClassDefFoundError) {
                System.out.println("Class Not Found: " +
                                   t.getMessage());
            }
            else {
                t.printStackTrace();
            }
        }
        finally {
            if (productPlugin != null) {
                PluginLoader.resetClassLoader(productPlugin);
            }
        }
    }

    private static final String[][] LOG_PROPS = {
        { "log4j.appender.R", "org.apache.log4j.ConsoleAppender" },
        { "log4j.appender.R.layout.ConversionPattern", "%-5p [%t] [%c{1}] %m%n" },
        { "log4j.appender.R.layout", "org.apache.log4j.PatternLayout" }        
    };

    private static void configureLogging(String pdkDir, String level) {
        if (new File(level).exists()) {
            PropertyConfigurator.configure(level);
            return;
        }
        Properties props = new Properties();
        Properties agentProps = new Properties();
        //pickup categories from from agent.properties
        File agentProperties =
            new File(pdkDir, "../../../conf/agent.properties");
        if (agentProperties.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(agentProperties);
                agentProps.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try { is.close(); } catch (Exception e) {}
                }
            }
        }
        for (Iterator it = agentProps.keySet().iterator();
             it.hasNext();)
        {
            String key = (String)it.next();
            if (key.startsWith("log4j.logger.") ||
                key.startsWith("log4j.category."))
            {
                props.setProperty(key, agentProps.getProperty(key));
            }
        }
        props.setProperty("log4j.rootLogger", level.toUpperCase() + ", R");
        for (int i=0; i<LOG_PROPS.length; i++) {
            props.setProperty(LOG_PROPS[i][0], LOG_PROPS[i][1]);
        }
        props.putAll(System.getProperties());
        PropertyConfigurator.configure(props);
    }

    public static void main(String[] args) throws Exception {
        String pdkDir =
            System.getProperty(ProductPluginManager.PROP_PDK_DIR, getPdkDir());

        File tmpDir = new File(new File(pdkDir).getParentFile(), "tmp");
        //point the the agent tmp dir which gets cleaned out everytime
        //the agent is started.  a must for windows where the files will
        //not get deleted because they are still open.
        if (tmpDir.exists() && tmpDir.canWrite()) {
            System.setProperty("java.io.tmpdir", tmpDir.toString());
        }

        String pdkLib = pdkDir + File.separator + "lib";
        String logLevel = System.getProperty("log", "error");

        //bleh, make sure we get log level right.
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-Dlog=")) {
                logLevel = args[i].substring(6);
            }
        }

        addJarDir(pdkLib);

        System.setProperty("org.hyperic.sigar.path", pdkLib);
        ProductPluginManager.setPdkDir(pdkDir);

        configureLogging(pdkDir, logLevel);

        PluginDumper pd = new PluginDumper(args);

        //XXX this is hackish, but dwim is more important
        if (!pd.config.hasSwitches) {
            runMain(pd, pd.config.args);
        }
        else {
            pd.init();
            pd.invoke();
            pd.shutdown();
        }
    }
}

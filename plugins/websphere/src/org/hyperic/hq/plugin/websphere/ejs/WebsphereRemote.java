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

package org.hyperic.hq.plugin.websphere.ejs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import java.rmi.RemoteException;

import javax.rmi.PortableRemoteObject;

import javax.ejb.CreateException;
import javax.ejb.ObjectNotFoundException;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ejs.sm.active.ActiveModuleConfig;
import com.ibm.ejs.sm.beans.ClientAccess;
import com.ibm.ejs.sm.beans.ClientAccessHome;
import com.ibm.ejs.sm.beans.DataSource;
import com.ibm.ejs.sm.beans.DataSourceConfig;
import com.ibm.ejs.sm.beans.EJBServer;
import com.ibm.ejs.sm.beans.EJBServerAttributes;
import com.ibm.ejs.sm.beans.EnterpriseApp;
import com.ibm.ejs.sm.beans.J2EEResourceConfig;
import com.ibm.ejs.sm.beans.LiveRepositoryObject;
import com.ibm.ejs.sm.beans.Module;
import com.ibm.ejs.sm.beans.Node;
import com.ibm.ejs.sm.beans.NodeAttributes;
import com.ibm.ejs.sm.beans.RepositoryObject;
import com.ibm.ejs.sm.beans.RepositoryObjectName;
import com.ibm.ejs.sm.beans.RepositoryObjectNameElem;
import com.ibm.ejs.sm.beans.TransportConfig;
import com.ibm.ejs.sm.exception.AttributeNotSetException;
import com.ibm.ejs.sm.exception.OpException;

import com.ibm.ejs.sm.util.act.Act;
/*
import com.ibm.ejs.sm.util.act.ActException;
import com.ibm.ejs.sm.util.act.ActTimeoutException;
*/

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;


import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

import org.hyperic.hq.plugin.websphere.wscp.AppServerCommand;
import org.hyperic.hq.plugin.websphere.wscp.ApplicationCommand;
import org.hyperic.hq.plugin.websphere.wscp.DataSourceCommand;
import org.hyperic.hq.plugin.websphere.wscp.EjbCommand;
import org.hyperic.hq.plugin.websphere.wscp.ModuleCommand;
import org.hyperic.hq.plugin.websphere.wscp.NodeCommand;
import org.hyperic.hq.plugin.websphere.wscp.WebappCommand;
import org.hyperic.hq.plugin.websphere.wscp.WebsphereCommand;
import org.hyperic.hq.plugin.websphere.wscp.WebsphereJMXCommand;

/**
 * Wrapper around WebSphere beans to provide support for control,
 * availability and discovery.
 */

public class WebsphereRemote {

    private static final int STATE_RUNNING = 3;
    private static final int STATE_STOPPED = 5;

    private String node;
    private String port;
    private HashMap cache = new HashMap();
    private static HashMap instances = new HashMap();
    private Properties types = new Properties();
    private Properties props = new Properties();
    private ClientAccess client;
    private EJBServer ejbServer = null;
    private boolean doModuleLookup = true;

    public WebsphereRemote(String node, String port) {
        this.node = node;
        this.port = port;

        this.props.put("java.naming.factory.initial",
                       "com.ibm.websphere.naming.WsnInitialContextFactory");

        this.props.put("com.ibm.CORBA.BootstrapHost", node);
        this.props.put("com.ibm.CORBA.BootstrapPort", port);
        this.props.put("com.ibm.websphere.naming.jndicache.cacheobject",
                       "none");

        this.props.put("java.naming.provider.url",
                       "iiop://" + node + ":" + port);

        //map wscp name to ejs name
        this.types.put("ApplicationServer", "EJBServer");
        this.types.put("EJBServer", "ApplicationServer");
    }

    public static void teardown() {
        Iterator it = instances.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            WebsphereRemote remote = (WebsphereRemote)entry.getValue();
            remote.shutdown();
        }

        instances.clear();
    }

    public static WebsphereRemote getInstance(String node, String port)
        throws PluginException {
        return getInstance(node, port, false);
    }

    public static WebsphereRemote getInstance(String node, String port,
                                              boolean doCache)
        throws PluginException {

        String key = node + port;
        WebsphereRemote remote = null;

        if (doCache) {
            remote = (WebsphereRemote)instances.get(key);
        }

        if (remote == null) {
            remote = new WebsphereRemote(node, port);

            try {
                remote.init();
            } catch (NamingException e) {
                String msg = e.getMessage();
                if (e instanceof CommunicationException) {
                    msg = "Failed to bind to admin server " + node + ":" + port;
                }
                throw new PluginException(msg, e);
            } catch (RemoteException e) {
                throw new PluginException(e.getMessage(), e);
            } catch (CreateException e) {
                throw new PluginException(e.getMessage(), e);
            }

            if (doCache) {
                instances.put(key, remote);
            }
        }

        return remote;
    }

    public void init()
        throws NamingException, RemoteException, CreateException {

        InitialContext context = null;
        Context ctx = null;
        Object obj;

        try {
            context = new InitialContext(this.props);

            //XXX AE only?
            ctx = (Context)context.lookup("ejsadmin/homes");

            obj = ctx.lookup("ClientAccessHome");
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
        }

        ClientAccessHome home = (ClientAccessHome)
            PortableRemoteObject.narrow(obj, ClientAccessHome.class);

        this.client = home.create();
    }

    public void shutdown() {
        this.cache.clear();
    }

    //map ejs name to wscp name
    private String objectNameToString(RepositoryObjectName name) {
        StringBuffer sb = new StringBuffer("/");

        for (Enumeration e = name.enumerate();
             e.hasMoreElements(); )
        {
            RepositoryObjectNameElem elem =
                (RepositoryObjectNameElem)e.nextElement();
            String home = elem.getHomeName();
            String type = home.substring(0, home.length()-4);
            String ename = elem.getName();
            sb.append(this.types.getProperty(type, type));
            sb.append(":").append(ename).append("/");
        }

        return sb.toString();
    }

    //map wscp name to RepositoryObjectName
    //type =~ s/ApplicationServer/EJBServer/g
    //and "Home" is appended to the type
    private RepositoryObjectName cmdToObjectName(WebsphereCommand cmd) {
        RepositoryObjectName name = new RepositoryObjectName();
        ArrayList names = cmd.getNames();

        for (int i=0; i<names.size(); i+=2) {
            String type = (String)names.get(i);
            String value = (String)names.get(i+1);
            String home = this.types.getProperty(type, type) + "Home";
            name.addElement(new RepositoryObjectNameElem(home, value));
        }

        return name;
    }

    private int getServerModuleStatus(ApplicationCommand cmd, Module mod)
        throws RemoteException, OpException {
        Hashtable t = mod.getServerModuleStatus();

        for (Iterator it = t.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry ent = (Map.Entry)it.next();
            String key = (String)ent.getKey();
            if (key.endsWith(cmd.getStatusKey())) {
                //XXX use app server name too.
                return ((Integer)ent.getValue()).intValue();
            }
        }

        return STATE_STOPPED;
    }

    //cannot use client.findRepositoryObjectByName to find a Module,
    //instead listModules for the EnterpriseApp in which the Module
    //is contained.  cache them all for future lookups.
    private RepositoryObject lookupModule(ModuleCommand cmd)
        throws RemoteException, OpException, ObjectNotFoundException {

        EnterpriseApp app = (EnterpriseApp)lookup(cmd.getApplication());

        for (Enumeration e = app.listModules();
             e.hasMoreElements(); )
        {
            Object obj = e.nextElement();
            Module mod = (Module)PortableRemoteObject.narrow(obj,
                                                             Module.class);

            String mName = objectNameToString(mod.getModuleFullName());

            this.cache.put(mName, mod);
        }

        return (RepositoryObject)this.cache.get(cmd.getFullName());
    }

    private RepositoryObject lookup(WebsphereCommand cmd)
        throws RemoteException, OpException, ObjectNotFoundException {

        String fullname = cmd.getFullName();
        RepositoryObject obj =
            (RepositoryObject)this.cache.get(fullname);

        if (obj == null) {
            if (cmd instanceof ModuleCommand) {
                return lookupModule((ModuleCommand)cmd);
            }

            RepositoryObjectName name = cmdToObjectName(cmd);
            obj = (RepositoryObject)
                client.findRepositoryObjectByName(name);

            this.cache.put(fullname, obj);
        }

        return obj;
    }

    public static boolean isRunning(Metric metric) {
        Properties props = metric.getProperties();

        String host =
            props.getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST);
        String port =
            props.getProperty(WebsphereProductPlugin.PROP_ADMIN_PORT);

        WebsphereRemote remote;

        try {
            remote = getInstance(host, port);
        } catch (PluginException e) {
            return false;
        }

        WebsphereCommand cmd =
            WebsphereJMXCommand.convert(metric.getObjectProperties());

        return remote.isRunning(cmd);
    }

    public boolean isRunning(WebsphereCommand cmd) {
        try {
            RepositoryObject obj = lookup(cmd);

            if (obj instanceof LiveRepositoryObject) {
                LiveRepositoryObject repo = 
                    (LiveRepositoryObject)obj;
                int state = repo.getCurrentState();

                if (state != STATE_RUNNING) {
                    ApplicationCommand appCmd;

                    if (obj instanceof Module) {
                        appCmd = ((ModuleCommand)cmd).getApplication();
                        //getCurrentState does not work with Server Groups
                        state =
                            getServerModuleStatus(appCmd,
                                                  (Module)obj);
                    }
                    else if (obj instanceof EnterpriseApp) {
                        EnterpriseApp app = (EnterpriseApp)obj;
                        appCmd = (ApplicationCommand)cmd;

                        //getCurrentState does not work with Server Groups
                        //so if a Module within the application is running
                        //then the application must be too.
                        for (Enumeration mods = app.listContainedObjects();
                             mods.hasMoreElements();)
                        {
                            Module mod = (Module)mods.nextElement();

                            if (getServerModuleStatus(appCmd, mod) ==
                                STATE_RUNNING)
                            {
                                state = STATE_RUNNING;
                                break;
                            }
                        }
                    }
                }

                return state == STATE_RUNNING;
            }
            else if (obj instanceof DataSource) {
                Object[] retval = ((DataSource)obj).testConnection();
                Boolean result = (Boolean)retval[0];
                return result.booleanValue();
            }
            return false; //XXX
        } catch (RemoteException e) {
            return false;
        } catch (OpException e) {
            return false;
        } catch (ObjectNotFoundException e) {
            return false;
        }
    }

    private void setErrorStr(String msg,
                             ControlPlugin plugin,
                             WebsphereCommand cmd,
                             String action,
                             Exception e) {

        String fullname = cmd.getFullName();
        plugin.setMessage(msg + " " + action + "ing " +
                          fullname + ": " + e.getMessage());
    }

    public void doAction(ControlPlugin plugin,
                         WebsphereCommand cmd,
                         String action) {

        LiveRepositoryObject obj;

        plugin.setResult(ControlPlugin.RESULT_FAILURE);

        try {
            obj = (LiveRepositoryObject)lookup(cmd);
        } catch (RemoteException e) {
            setErrorStr("RemoteException", plugin, cmd, action, e);
            return;
        } catch (OpException e) {
            setErrorStr("OpException", plugin, cmd, action, e);
            return;
        } catch (ObjectNotFoundException e) {
            setErrorStr("ObjectNotFound", plugin, cmd, action, e);
            return;
        }

        Act act;
        try {
            if (action.equals("stop")) {
                this.cache.clear();
                act = obj.stop(0); //0x1d4c0);
                plugin.setResult(ControlPlugin.RESULT_SUCCESS);
            }
            else if (action.equals("start")) {
                act = obj.start();
                plugin.setResult(ControlPlugin.RESULT_SUCCESS);
            }
            else {
                throw new IllegalArgumentException(action);
            }
        } catch (RemoteException e) {
            setErrorStr("RemoteException", plugin, cmd, action, e);
            return;
        } catch (OpException e) {
            setErrorStr("OpException", plugin, cmd, action, e);
            return;
        }

        //XXX seems act.waitForCompletion spawns a thread that
        //never dies; ok the control plugin will waitForState
        /*
        try {
            act.waitForCompletion(plugin.getTimeoutMillis());
            plugin.setResult(ControlPlugin.RESULT_SUCCESS);
            return;
        } catch (ActTimeoutException e) {
            setErrorStr("Timeout", plugin, cmd, action, e);
        } catch (ActException e) {
            setErrorStr("Error", plugin, cmd, action, e);
        } catch (RemoteException e) {
            setErrorStr("RemoteException", plugin, cmd, action, e);
        }
        */
    }

    private String leafName(RepositoryObject obj)
        throws RemoteException, OpException {
        return obj.getFullName().getLeafElement().getName();
    }

    private String rootName(RepositoryObject obj)
        throws RemoteException, OpException {
        return obj.getFullName().getRootElement().getName();
    }

    //pmi uses the jndi name
    private String getEjbBinding(byte[] bindings) {
        BufferedReader reader = 
            new BufferedReader(
                new InputStreamReader(
                    new ByteArrayInputStream(bindings)));

        //some sort of markup languange
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<ejbBindings")) {
                    final String tok = "jndiName=\"";
                    int idx = line.indexOf(tok);
                    if (idx > 0) {
                        line = line.substring(idx+tok.length());
                    }
                    idx = line.indexOf("\">");
                    if (idx > 0) {
                        line = line.substring(0, idx);
                    }
                    return line;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    //pmi uses the webapp display-name
    private String getWebappDisplayName(File dir) {
        File webxml = new File(new File(dir, "WEB-INF"), "web.xml");

        if (!webxml.exists()) {
            return null;
        }
        
        FileInputStream is = null;
        String line;

        try {
            is = new FileInputStream(webxml);
            
            BufferedReader reader = 
                new BufferedReader(
                    new InputStreamReader(is));

            //more of this sort of markup languange
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                final String tok = "<display-name>";
                if (line.startsWith(tok)) {
                    line = line.substring(tok.length());
                    int idx = line.indexOf("</display-name>");
                    if (idx > 0) {
                        line = line.substring(0, idx);
                    }
                    return line;
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }

        return null;
    }

    private int getServerPort(EJBServer server) {
        EJBServerAttributes attrs = new EJBServerAttributes();
        try {
            attrs.request("WebContainerConfig");
            attrs = (EJBServerAttributes)this.ejbServer.getAttributes(attrs);
            Vector transports =
                attrs.getWebContainerConfig().getTransports();

            for (int i=0; i<transports.size(); i++) {
                TransportConfig config =
                    (TransportConfig)transports.get(i);
                if (config.getProtocol().equals("http")) {
                    return config.getPort();
                }
            }
        } catch (Exception ae) {
            ae.printStackTrace();
        }
    
        return 9080;
    }
    
    private void discover(ArrayList objects,
                          Node node,
                          RepositoryObject robj)
        throws RemoteException, OpException {

        String nodeName = leafName(node);

        if (robj == null) {
            robj = node;
        }

        for (Enumeration e = robj.listContainedObjects();
             e.hasMoreElements(); ) {

            RepositoryObject obj = (RepositoryObject)
                PortableRemoteObject.narrow(e.nextElement(),
                                            RepositoryObject.class);
            if (obj instanceof EJBServer) {
                this.ejbServer = (EJBServer)obj;

                AppServerCommand cmd =
                    new AppServerCommand(rootName(obj), leafName(obj));

                int port = getServerPort(this.ejbServer);
                String prop = WebsphereProductPlugin.PROP_SERVER_PORT;
                cmd.getMetricProperties().setProperty(prop,
                                                      String.valueOf(port));

                objects.add(cmd);

                discover(objects, node, obj);
            }
            else if (obj instanceof Module) {
                Module mod = (Module)obj;
                EnterpriseApp app = mod.getEnterpriseApp();
                String appName = leafName(app);

                objects.add(new ApplicationCommand(nodeName, appName));

                if (!this.doModuleLookup) {
                    continue;
                }

                ActiveModuleConfig config;

                try {
                    config = (ActiveModuleConfig)mod.getConfig(this.ejbServer);
                } catch (RemoteException ae) {
                    String msg = 
                        "Error getting ActiveModuleConfig for " +
                        nodeName + ":" + appName;
                    System.err.println(msg);
                    ae.printStackTrace();
                    this.doModuleLookup = false;
                    continue;
                } catch (OpException ae) {
                    continue;
                }

                String type = config.getModuleType();

                Vector configs = config.getJ2EEResourceConfigs();

                for (int i=0; i<configs.size(); i++) {
                    J2EEResourceConfig cfg =
                        (J2EEResourceConfig)configs.get(i);

                    if (cfg instanceof DataSourceConfig) {
                        objects.add(new DataSourceCommand(cfg.getName()));
                    }
                }

                String modName = leafName(mod);
                String pmiValue = null;
                String pmiName;
                ModuleCommand modCmd = null;
                
                if (type.equals("ejb")) {
                    pmiName = WebsphereProductPlugin.PROP_EJB_JNDI_NAME;
                    pmiValue = getEjbBinding(config.getBindings());
                    modCmd = new EjbCommand(nodeName, appName, modName);
                }
                else if (type.equals("web")) {
                    pmiName = WebsphereProductPlugin.PROP_WEBAPP_DISPLAY_NAME;
                    File war = new File(app.getEarfile(node),
                                        config.getRelativeURI());
                    pmiValue = getWebappDisplayName(war);
                    modCmd = new WebappCommand(nodeName, appName, modName);
                    //context for metric hq adds to websphere
                    String context = war.getName();
                    if (context.endsWith(".war")) {
                        context = context.substring(0, context.length()-4);
                    }
                    String ctxName = WebsphereProductPlugin.PROP_WEBAPP_CONTEXT;
                    modCmd.getMetricProperties().put(ctxName,
                                                     File.separator+context);
                }
                else {
                    continue;
                }

                if (pmiValue == null) {
                    pmiValue = modName;
                }

                modCmd.getMetricProperties().put(pmiName, pmiValue);

                objects.add(modCmd);
            }
        }
    }

    private class DiscoverArrayList extends ArrayList {
        public boolean add(Object o) {
            if (this.contains(o)) {
                return false;
            }
            super.add(o);
            return true;
        }
    }

    //leave it up to autodiscovery plugin to convert this
    //return list to server/service values.
    public WebsphereCommand[] discover(String node)
        throws PluginException {

        ArrayList objects = new DiscoverArrayList();

        NodeCommand nodeCmd = new NodeCommand(node);

        try {
            discover(objects, (Node)lookup(nodeCmd), null);
        } catch (OpException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (RemoteException e) {
            String msg = e.getMessage();
            //try to turn otherwise useless error message into something meaningful
            if (e.detail instanceof RemoteException) {
                Throwable de = ((RemoteException)e.detail).detail;
                if (de instanceof InvocationTargetException) {
                    InvocationTargetException ie = (InvocationTargetException)de;
                    if (ie.getTargetException() instanceof ObjectNotFoundException) {
                        msg = "Node name not found: " + node;
                    }
                }
            }
            throw new PluginException(msg, e);
        } catch (ObjectNotFoundException e) {
            throw new PluginException(e.getMessage(), e);
        }

        return (WebsphereCommand[])objects.toArray(new WebsphereCommand[0]);
    }

    private void getNodeProperties(Properties props, Node node) {
        NodeAttributes attrs = new NodeAttributes();

        try {
            attrs.request(NodeAttributes.hostName);
            attrs.request(NodeAttributes.osName);
            attrs.request(NodeAttributes.hostSystemType);
            attrs.request(NodeAttributes.installRoot);
            attrs = (NodeAttributes)node.getAttributes(attrs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            props.setProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                              attrs.getHostName());
        } catch (AttributeNotSetException e) {
            //XXX default
        }

        try {
            props.setProperty(ProductPlugin.PROP_INSTALLPATH,
                              attrs.getInstallRoot());
        } catch (AttributeNotSetException e) {
            //XXX default
        }

        try {
            props.setProperty(NodeAttributes.osName,
                              attrs.getOsName());
        } catch (AttributeNotSetException e) {
            //XXX default
        }

        try {
            props.setProperty(NodeAttributes.hostSystemType,
                              attrs.getHostSystemType());
        } catch (AttributeNotSetException e) {
            //XXX default
        }
    }

    public NodeCommand[] getNodes() {
        ArrayList nodes = new ArrayList();

        try {
            for (Enumeration e = this.client.getTypeTree();
                 e.hasMoreElements(); )
            {
                ClientAccess.TypeInfo type =
                    (ClientAccess.TypeInfo)e.nextElement();

                for (Enumeration inst =
                         this.client.listInstances(type.type, false);
                     inst.hasMoreElements(); )
                {
                    ClientAccess.TypeInstanceInfo info =
                        (ClientAccess.TypeInstanceInfo)inst.nextElement();

                    if (info.repO instanceof Node) {
                        NodeCommand node =
                            new NodeCommand(leafName(info.repO));

                        getNodeProperties(node.getProperties(),
                                          (Node)info.repO);

                        nodes.add(node);
                    }
                }

                if (nodes.size() != 0) {
                    //TypeInfo does not actually give any useful
                    //info about the type.  so once we have more than
                    //one node, we've hit that type, stop looking.
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (NodeCommand[])nodes.toArray(new NodeCommand[0]);
    }

    //for testing standalone outside of the agent
    //${websphere.installpath}/java/bin/java -jar pdk/lib/spider-pdk.jar \
    //websphere ejs.WebSphereRemote ...
    public static void main(String[] args) throws Exception {
        WebsphereRemote remote;
        String host="localhost", port="900";

        if (args.length >= 2) {
            host = args[0];
            port = args[1];
        }

        remote = getInstance(host, port, false);

        if (args.length == 3) {
            discoverDump(remote, args[2]);
            return;
        }

        NodeCommand[] nodes = remote.getNodes();

        for (int i=0; i<nodes.length; i++) {
            System.out.println(nodes[i]);
            nodes[i].getProperties().list(System.out);

            discoverDump(remote, nodes[i].getLeafName());
        }
    }

    private static void discoverDump(WebsphereRemote remote, String node)
        throws Exception {

        WebsphereCommand[] objects = remote.discover(node);

        for (int i=0; i<objects.length; i++) {
            boolean isRunning = remote.isRunning(objects[i]);

            System.out.println(objects[i] +
                               " (Running=" + isRunning + ")");

            Properties props = new Properties();
            props.putAll(objects[i].getProperties());
            props.putAll(objects[i].getMetricProperties());
            props.list(System.out);
        }
    }
}

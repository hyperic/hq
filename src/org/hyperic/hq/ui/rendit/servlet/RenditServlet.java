package org.hyperic.hq.ui.rendit.servlet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.rendit.PluginWrapper;
import org.hyperic.hq.ui.rendit.RenditServer;
import org.hyperic.hq.ui.rendit.servlet.DirWatcher.DirWatcherCallback;
import org.hyperic.util.StringUtil;

public class RenditServlet 
    extends HttpServlet
{
    private static final Log _log = LogFactory.getLog(RenditServlet.class);
    private static final Object INIT_LOCK = new Object();
    private static boolean INITIALIZED;
    
    private static DirWatcher _watcher;
    private static Thread _watcherThread;
    
    protected void doGet(HttpServletRequest req, HttpServletResponse response) 
        throws ServletException, IOException 
    {
        String servPath = req.getServletPath();
        String reqUri = req.getRequestURI();
        
        initPlugins();
        
        if (!reqUri.startsWith(servPath)) {
            _log.warn("Request path [" + reqUri + "] does not start with " + 
                      "servlet [" + servPath + "]");
            return;
        }

        // XXX:  Make sure the following is sane -- needs to be escaped?  
        // any weird attacks?
        reqUri = reqUri.substring(servPath.length());
        List path = StringUtil.explode(reqUri, "/");
        
        if (path.size() < 1) {
            throw new ServletException("Illegal request path");
        }
        
        String plugin = (String)path.get(0);
        _log.info("Request for [" + plugin + "]: " + req.getRequestURI() + 
                  "?" + req.getQueryString());
                  
        try {
            RenditServer.getInstance().handleRequest(plugin, req, response);
        } catch(Exception e) {
            throw new ServletException(e);
        }
    }
    
    public void init() throws ServletException {
        super.init();
        initPlugins();
    }

    private void initPlugins() {
        synchronized(INIT_LOCK) {
            if (INITIALIZED)
                return;
        
            String home = System.getProperty("jboss.home.url");
                
            if (home == null) {
                _log.info("Can't find JBOSS Home");
                return;
            }
            
            URL url;
            try {
                url = new URL(home);
            } catch (MalformedURLException e) {
                _log.error("Malformed jboss.home.url=" + home);
                return;
            }
            
            File homeDir = new File(url.getFile());
            
            // XXX:  Hardcoded sysdir for now
            String sysPath = System.getProperty("jboss.server.home.dir") +
                    "/deploy/hq.ear/rendit_sys";
            File sysDir = new File(sysPath);
            RenditServer.getInstance().setSysDir(sysDir);

            _log.info("Watching for HQU plugins in [" + 
                      homeDir.getAbsolutePath() + "]");
            _watcher = new DirWatcher(homeDir, new DirWatcherCallback() {
                public void fileAdded(File f) {
                    if (PluginWrapper.isValidPlugin(f)) {
                        RenditServer.getInstance().addPluginDir(f.getName(), f);
                    }
                }

                public void fileRemoved(File f) {
                    if (PluginWrapper.isValidPlugin(f)) {
                        RenditServer.getInstance().removePluginDir(f.getName());
                    }
                }
            });
            
            _watcherThread = new Thread(_watcher);
            _watcherThread.setDaemon(true);
            _watcherThread.start();
                
            INITIALIZED = true;
        }
    }
}

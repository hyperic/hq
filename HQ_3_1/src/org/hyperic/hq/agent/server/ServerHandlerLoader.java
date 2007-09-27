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

package org.hyperic.hq.agent.server;

import org.hyperic.hq.agent.AgentLoaderException;
import org.hyperic.util.PluginLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The ServerHandlerLoader utilizes the ComponentLoader to load .jars
 * which are to be plugged into the Agent's AgentServerHandler interface.
 * This object is a bit of a machine which takes paths to .jars, and returns
 * the AgentServerHandler object associated with the main-class in the .jar.
 */

class ServerHandlerLoader {
    private Log logger;

    ServerHandlerLoader(){
        this.logger = LogFactory.getLog(ServerHandlerLoader.class);
    }

    /**
     * Load a .jar into the VM and return the requested AgentServerHandler.
     * This routine loads the .jar, extracts the Main-Class, verifies that
     * it implements the correct interface, and returns it to the caller.
     *
     * @param jarPath path to the .jar file
     *
     * @return an AgentServerHandler object as implemented by the Main-Class
     *          in the .jar
     *
     * @throws AgentLoaderException indicating there was an error loading
     *                              the class.
     */

    AgentServerHandler loadServerHandler(String jarPath) 
        throws AgentLoaderException 
    {
        AgentServerHandler handler = null;
        Class pluginClass = null;

        try {
            PluginLoader loader = 
                PluginLoader.create(jarPath,
                                    this.getClass().getClassLoader());
            PluginLoader.setClassLoader(loader);

            pluginClass = loader.loadPlugin();

            handler = (AgentServerHandler)pluginClass.newInstance();
        } catch(Exception e) {
            this.logger.error("Error loading server handler jar", e);
            throw new AgentLoaderException("Unable to load server handler " +
                                           "jar: " + e.getMessage());

        } finally {
            if (handler != null) {
                PluginLoader.resetClassLoader(handler);
            }
        }

        validateServerHandler(handler, pluginClass.getName());

        return handler;
    }
    
    private void validateServerHandler(Object sHandler, String sHandlerName)
        throws AgentLoaderException
    {
        Class tc;

        tc = org.hyperic.hq.agent.server.AgentServerHandler.class;

        if(!tc.isInstance(sHandler)){
            throw new AgentLoaderException(sHandlerName + " is not a valid " +
                                           "server handler (it does not " +
                                           "implement AgentServerHandler");
        }
    }
}

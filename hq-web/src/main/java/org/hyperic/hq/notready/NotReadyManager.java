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

package org.hyperic.hq.notready;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

/**
 * A simple MBean which calls the NotReadyFilter class to set the
 * application ready or not.  The reason that we don't make the filter
 * also an MBean is that it requires that the MBean can load things
 * like the Filter class and such.  This also makes it a bit more
 * clear what the purposes of the objects are.
 */
@Service
public class NotReadyManager
    
{
    private Log         _log = LogFactory.getLog(NotReadyManager.class);
   

    public NotReadyManager(){
    }

    public void setReady(boolean ready){
        NotReadyFilter.setReady(ready);
    }

    public boolean isReady(){
        return NotReadyFilter.getReady();
    }

    

    /**
     * JBoss waits until the server is fully started to start the connectors.  
     * We start them early to avoid long server startup times.
     */
    public void postRegister(Boolean registrationDone) {
        Runnable r = new WebServerConnectorStarter();
        Thread t = new Thread(r);
        t.start();
    }

   
    
    private class WebServerConnectorStarter implements Runnable {
        public void run() {
            try {
                // HHQ-2739: Short-term hack for JBoss 4.2.3.GA
                // Execute in new thread and sleep for 10 seconds so that
                // the jboss.web service has time to start first
                Thread.sleep(10 * 1000);

                _log.info("Starting WebServer connectors");

                ObjectName service = 
                    new ObjectName("jboss.web:service=WebServer");

                //_server.invoke(service, "startConnectors",
                  //             new Object[0], new String[0]);
            } catch (Exception e) {
                _log.error("Unable to start WebServer connectors: " 
                            + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }            
        }
    }
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.util.thread.MultiRunnable;

public class AgentRunner 
    implements Runnable, MultiRunnable
{
    private int         _threadNo;
    private Properties  _bootProps;

    public void configure(int threadNo, Properties props) {
        System.out.println("Configuring thread " + threadNo);
        _threadNo  = threadNo;
        _bootProps = props;
    }
    
    public void run() {
        File clonesDir = new File("clones");
        File cloneDir = new File(clonesDir, "clone_" + _threadNo);
        File dataDir  = new File(cloneDir, "data");
        File agentCfg = new File(cloneDir, "agent.properties");
        FileInputStream fIs = null;
        Properties p = new Properties();
        
        p.putAll(_bootProps);
        try {
            fIs = new FileInputStream(agentCfg);
            p.load(fIs);
        } catch(IOException exc) {
            exc.printStackTrace();
        } finally {
            try {fIs.close();} catch(Exception e) {}
        }

        p.setProperty(AgentConfig.PROP_STORAGEPROVIDERINFO[0],
                      dataDir.getAbsolutePath() + "|m|100|20|50");  
        p.setProperty(AgentConfig.PROP_DATADIR[0], dataDir.getAbsolutePath());
        p.setProperty(AgentConfig.PROP_KEYSTORE[0], 
                      new File(dataDir, AgentConfig.PROP_KEYSTORE[0]).getAbsolutePath());
        
        try {
            AgentConfig cfg = AgentConfig.newInstance(p);
            AgentDaemon.newInstance(cfg).start();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }
}

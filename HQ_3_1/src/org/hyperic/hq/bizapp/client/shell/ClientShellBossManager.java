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

package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.hq.bizapp.shared.AppdefBoss;

import org.hyperic.hq.bizapp.shared.AppdefBossUtil;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthBossUtil;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.AuthzBossUtil;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.bizapp.shared.ConfigBossUtil;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBossUtil;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.bizapp.shared.ProductBossUtil;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AIBossUtil;
import org.hyperic.hq.bizapp.shared.LiveDataBoss;
import org.hyperic.hq.bizapp.shared.LiveDataBossUtil;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.ControlBossUtil;

import java.util.Hashtable;
import javax.naming.NamingException;

public class ClientShellBossManager {
    private AppdefBoss      appdefBoss;
    private AuthBoss        authBoss;
    private AuthzBoss       authzBoss;
    private ProductBoss     productBoss;
    private MeasurementBoss measurementBoss;
    private ConfigBoss      configBoss;
    private EventsBoss      eventsBoss;
    private AIBoss          aiBoss;
    private LiveDataBoss    liveDataBoss;
    private ControlBoss     controlBoss;

    protected ClientShellAuthenticator auth;
    
    public ClientShellBossManager(ClientShellAuthenticator auth){
        this.auth = auth;
        this.resetBosses();
    }

    public ClientShellAuthenticator getAuthenticator(){
        return this.auth;
    }

    public void resetBosses(){
        this.appdefBoss      = null;
        this.authBoss        = null;
        this.authzBoss       = null;
        this.productBoss     = null;
        this.measurementBoss = null;
        this.eventsBoss      = null;
        this.aiBoss          = null;
        this.liveDataBoss    = null;
        this.controlBoss     = null;
    }

    public AppdefBoss getAppdefBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.appdefBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.appdefBoss = AppdefBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get AppdefBoss: " + exc);
            }
        }
        return this.appdefBoss;
    }

    public AuthBoss getAuthBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.authBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.authBoss = AuthBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get AuthBoss: " + exc);
            }
        }
        return this.authBoss;
    }

    public AuthzBoss getAuthzBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.authzBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.authzBoss = AuthzBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get AuthzBoss: " + exc);
            }
        }
        return this.authzBoss;
    }

    public ProductBoss getProductBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.productBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.productBoss = ProductBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get ProductBoss: " + exc);
            }
        }
        return this.productBoss;
    }

    public MeasurementBoss getMeasurementBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.measurementBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();
                
                this.measurementBoss = 
                    MeasurementBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get MeasurementBoss: " + 
                                          exc);
            }
        }
        return this.measurementBoss;
    }

    public ConfigBoss getConfigBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.configBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();
                
                this.configBoss = 
                    ConfigBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get ConfigBoss: " + 
                                          exc);
            }
        }
        return this.configBoss;
    }

    public EventsBoss getEventsBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.eventsBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();
                
                this.eventsBoss = EventsBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get EventsBoss: " + exc);
            }
        }
        return this.eventsBoss;
    }

    public AIBoss getAIBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.aiBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.aiBoss = AIBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get AIBoss: " + exc);
            }
        }
        return this.aiBoss;
    }

    public LiveDataBoss getLiveDataBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if (this.liveDataBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.liveDataBoss = LiveDataBossUtil.getHome(env).create();
            } catch (ClientShellAuthenticationException e) {
                throw e;
            } catch (Exception e) {
                throw new NamingException("Could not get LiveDataBoss: " + e);
            }
        }

        return this.liveDataBoss;
    }

    public ControlBoss getControlBoss()
        throws NamingException, ClientShellAuthenticationException
    {
        if(this.controlBoss == null) {
            try {
                Hashtable env = this.auth.getNamingEnv();

                this.controlBoss =
                    ControlBossUtil.getHome(env).create();
            } catch(ClientShellAuthenticationException exc){
                throw exc;
            } catch(Exception exc) {
                throw new NamingException("Could not get ControlBoss: " +
                                          exc);
            }
        }
        return this.controlBoss;
    }    
}

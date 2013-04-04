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

package org.hyperic.hq.product;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Base class for control plugins.
 */
public abstract class ControlPlugin extends GenericPlugin {

    public static final String PROP_TIMEOUT   = "timeout";

    protected static final int DEFAULT_TIMEOUT = 30;
    private int timeout = DEFAULT_TIMEOUT;

    private int result;
    private String message;

    // Control plugin states
    public static final String STATE_UNKNOWN    = "unknown";
    public static final String STATE_STARTING   = "starting";
    public static final String STATE_STARTED    = "started";
    public static final String STATE_STOPPING   = "stopping";
    public static final String STATE_STOPPED    = "stopped";
    public static final String STATE_RESTARTING = "restarting";

    // Result codes for actions
    public static final int RESULT_SUCCESS      =  0;
    public static final int RESULT_FAILURE      = -1;

    protected ControlPluginManager manager;
    public ControlPlugin() {
    }

    public List<String> getActions() {
        List<String> actions = null;
        if (this.data != null) {
            actions = this.data.getControlActions(getTypeInfo());
        }
        if (actions == null) {
            getLog().debug(getName() +
                           " does not specify any control actions");
            actions = new ArrayList<String>();
        }
        return actions;
    }

    protected void setExceptionMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            msg = t.toString();
        }
        if (getMessage() == null) {
            setMessage(msg);
        }
        getLog().debug(msg, t);
        setResult(RESULT_FAILURE);
    }
    
    public void doAction(String action, final ControlSendCommandResult_args resultsMetadata) throws PluginException {
        doAction(action);
    }
      
    public void doAction(String action) throws PluginException {
        doAction(action, new String[0]);
    }
    
    
    
    public void doAction(final String action, final String[] args, final ControlSendCommandResult_args resultsMetadata) throws PluginException{
        doAction(action, args);
    }
    
   
    
    /**
     * Invokes plugin method with the name of param action.
     */
    public void doAction(String action, String[] args) throws PluginException {
        
        setResult(RESULT_SUCCESS); //ControlPluginManager defaults to FAILURE

        Method method = null;
        Object[] sigs = {
            new Class[] { args.getClass() },
            new Class[0]
        };

        final Class clz = getClass() ; 
        for (int i=0; i<sigs.length; i++) {
            try {
                method = clz.getDeclaredMethod(action, (Class[])sigs[i]);
                break;
            } catch (NoSuchMethodException e) {
                continue;
            }
        }

        if (method == null) {
            String msg =
                "Action '" + action + "' not supported";
            throw new PluginException(msg);
        }

        Object[] methodArgs =
            (method.getParameterTypes().length == 0) ?
             new Object[0] : new Object[] { args };

        try {
            method.invoke(this, methodArgs);
        } catch (InvocationTargetException e) {
            setExceptionMessage(e.getTargetException());
        } catch (Exception e) {
            setExceptionMessage(e);
            throw new PluginException("Error invoking " + getName() +
                                      " action '" + action + "': " + e, e);
        }
    }

    protected boolean isRunning() {
        getLog().error(getName() + " does not implement isRunning");
        return false; //FIXME temporary during refactoring
    }

    public int getTimeout() {
        return this.timeout;
    }

    public int getTimeoutMillis() {
        return this.timeout * 1000;
    }

    public void setTimeout(int val) {
        this.timeout = val;
    }

    public void setTimeout(String val) {
        setTimeout(Integer.parseInt(val));
    }

    protected ControlPluginManager getManager() {
        return this.manager;
    }

    protected void setManager(ControlPluginManager manager) {
        this.manager = manager;
    }

    public void init(PluginManager manager)
        throws PluginException
    {
        this.manager = (ControlPluginManager)manager;
    }

    public int getResult()
    {
        return this.result;
    }

    public void setResult(int result)
    {
        this.result = result;
    }

    public String getMessage()
    {
        return this.message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    protected String detectState() {
        if (isRunning()) {
            return STATE_STARTED;
        }
        else {
            return STATE_STOPPED;
        }
    }

    protected String waitForState(String wantedState)
    {
        int timeout = getTimeoutMillis();
        long timeStart = System.currentTimeMillis();
        String state = detectState();

        while (!state.equals(wantedState) &&
               (System.currentTimeMillis() - timeStart) < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
            state = detectState();
        }

        return state;
    }
    
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        if (this.data != null) {
            ConfigSchema schema =
                this.data.getConfigSchema(info,
                                          ProductPlugin.CFGTYPE_IDX_CONTROL);
            if (schema != null) {
                return schema;
            }
        }
        return super.getConfigSchema(info, config);
    }
}

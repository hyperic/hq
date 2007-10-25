/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.product.jmx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class MxControlPlugin 
    extends ControlPlugin
{
    private static final Log log =
        LogFactory.getLog(MxControlPlugin.class);

    private String objectName;

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        this.objectName = configureObjectName(this);
    }

    static String configureObjectName(GenericPlugin plugin)
        throws PluginException {

        String objectName = plugin.getTypeProperty(MxQuery.PROP_OBJECT_NAME);
        if (objectName == null) {
            throw new PluginException(MxQuery.PROP_OBJECT_NAME +
                                      " not defined for " +
                                      plugin.getTypeInfo().getName());
        }
        objectName = MxUtil.expandObjectName(objectName,
                                             plugin.getConfig());
        
        return objectName;
    }

    protected boolean isRunning() {
        Integer avail;
        //XXX get attribute from hq-plugin.xml
        try {
            avail = (Integer)MxUtil.getValue(getConfig().toProperties(),
                                             this.objectName,
                                             Metric.ATTR_AVAIL);
        } catch (Exception e) {
            return false;
        }
        
        return avail.intValue() == Metric.AVAIL_UP;
    }

    public void doAction(String action, String[] args)
        throws PluginException {

        invokeMethod(action, args);
    }

    protected void invokeMethod(String action, String[] args) {
        //<property name="OBJECT_NAME.gc" value="java.lang:Type=Memory"/>
        String objectName =
            getTypeProperty(MxQuery.PROP_OBJECT_NAME + "." + action);

        if (objectName == null) {
            //allow ObjectName embedded in action name for cmd-line testing:
            //-m control -a hyperic:Name=MxServerTest::invokeFoo
            int ix = action.indexOf("::");
            if (ix != -1) {
                objectName = action.substring(0, ix);
                action = action.substring(ix+2);
            }
            else {
                //default to OBJECT_NAME property
                objectName = this.objectName;
            }
        }
        else {
            objectName = Metric.translate(objectName, getConfig());
        }

        invokeMethod(this, objectName, action, args);
    }

    static void invokeMethod(ControlPlugin plugin,
                             String objectName,
                             String action, String[] args) {

        log.debug("invoking " + action + " " + MBeanUtil.anyToString(args));

        try {
            String result = null;
            Object obj =
                MxUtil.invoke(plugin.getConfig().toProperties(),
                              objectName,
                              action, args, new String[0]);
            if (obj != null) {
                result = MBeanUtil.anyToString(obj);
            }
            log.debug(objectName + "." + action +
                      "() returned: " + obj);
            plugin.setResult(RESULT_SUCCESS);
            if (result != null) {
                plugin.setMessage(result);
            }
        } catch (PluginException e) {
            log.error(e.getMessage(), e);
            plugin.setMessage(e.getMessage());
            plugin.setResult(RESULT_FAILURE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // anything not explicitly thrown by MxUtil.invoke
            // needs to have the full stack trace logged for debugging.
            plugin.setMessage(e.getMessage());
            plugin.setResult(RESULT_FAILURE);
        }
    }
}

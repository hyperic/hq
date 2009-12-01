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

package org.hyperic.hq.plugin.weblogic;

import java.util.Map;

import java.security.PrivilegedAction;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class WeblogicMeasurementPlugin
    extends MeasurementPlugin
    implements PrivilegedAction {

    private static final boolean useJAAS = WeblogicProductPlugin.useJAAS();
    private Metric metric;

    //same attribute used for server control state
    private static final String SERVER_AVAIL_ATTR =
        WeblogicMetric.SERVER_RUNTIME_STATE;

    private static final String SERVER_AVAIL_ATTR_61 = "State";

    //same attribute used for application service control state
    private static final String APP_AVAIL_ATTR = 
        WeblogicMetric.APPLICATION_STATE;

    private static final String EJB_AVAIL_ATTR = 
        WeblogicMetric.EJB_COMPONENT_RUNTIME_STATUS;

    private static final String WEBAPP_AVAIL_ATTR = 
        WeblogicMetric.WEBAPP_COMPONENT_RUNTIME_STATUS;

    private static final String JDBC_CONN_AVAIL_ATTR = 
        WeblogicMetric.JDBC_CONNECTION_POOL_RUNTIME_STATE;

    private static final String EXQ_AVAIL_ATTR = 
        "Name";

    public WeblogicMeasurementPlugin() {
        setName(WeblogicProductPlugin.NAME);
    }

    private static final String[][] PLATFORM_HELP_PROPS = {
        {
            "weblogic.prefix",
            "/usr/local/bea", "C:\\\\bea"
        }
    };

    protected String[][] getPlatformHelpProperties() {
        return PLATFORM_HELP_PROPS;
    }

    /**
     * used for replacement in etc/hq-plugin.xml
     */
    protected Map getMeasurementProperties() {
        TypeInfo info = getTypeInfo();
        Map props =
            WeblogicMetric.getMetricProps(info.getVersion());

        return props;
    }

    private boolean isAvail(Metric metric) {
        String attr = metric.getAttributeName();
        //XXX this is ugly.
        boolean isAvail =
            attr.equals(SERVER_AVAIL_ATTR) ||
            attr.equals(SERVER_AVAIL_ATTR_61) ||
            attr.equals(EJB_AVAIL_ATTR) ||
            attr.equals(WEBAPP_AVAIL_ATTR) ||
            attr.equals(APP_AVAIL_ATTR) ||
            attr.equals(JDBC_CONN_AVAIL_ATTR) ||
            attr.equals(EXQ_AVAIL_ATTR);
        return isAvail;
    }

    public MetricValue getValue(Metric metric)
        throws PluginException, MetricNotFoundException,
        MetricUnreachableException {

        boolean isAvail = isAvail(metric);

        try {
            if (useJAAS) {
                return getValueAs(metric);
            }
            else {
                return getWeblogicValue(metric);
            }
        } catch (MetricUnreachableException e) {
            if (!isAvail) {
                throw e;
            }
        } catch (MetricNotFoundException e) {
            if (!isAvail) {
                throw e;
            }
        } catch (Exception e) {
            if (!isAvail) {
                throw new PluginException(e.getMessage(), e);
            }
        }

        return new MetricValue(Metric.AVAIL_DOWN);
    }

    private MetricValue getWeblogicValue(Metric metric)
        throws PluginException, MetricNotFoundException,
        MetricUnreachableException {

        Double val = null;
        boolean isAvail = isAvail(metric);

        Object obj = WeblogicUtil.getRemoteMBeanValue(metric);

        if (isAvail) {
            val = new Double(WeblogicUtil.convertStateVal(obj));
        }
        else {
            //XXX: when we have the flag, we can mark NumberFormatException
            val = new Double(obj.toString());
        }

        return new MetricValue(val, System.currentTimeMillis()); 
    }

    private MetricValue getValueAs(Metric metric)
        throws PluginException, MetricNotFoundException,
        MetricUnreachableException {
        
        WeblogicAuth auth = WeblogicAuth.getInstance(metric);

        this.metric = metric;

        Object obj;

        try {
            obj = auth.runAs(this);
        } catch (SecurityException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        }

        if (obj instanceof MetricValue) {
            return (MetricValue)obj;
        }

        if (obj instanceof PluginException) {
            throw (PluginException)obj;
        }
        if (obj instanceof MetricNotFoundException) {
            throw (MetricNotFoundException)obj;
        }
        if (obj instanceof MetricUnreachableException) {
            throw (MetricUnreachableException)obj;
        }
        if (obj instanceof Exception) {
            Exception e = (Exception)obj;
            throw new PluginException(e.getMessage(), e);
        }

        throw new IllegalArgumentException();
    }

    public Object run() {
        try {
            return getWeblogicValue(this.metric);
        } catch (Exception e) {
            return e;
        }
    }

    private String getServiceName() {
        ServiceTypeInfo service =
            (ServiceTypeInfo)getTypeInfo();

        String name = service.getName();
        String sname = service.getServerName();

        return name.substring(sname.length() + 1);        
    }

    public String translate(String template, ConfigResponse config){
        TypeInfo info = getTypeInfo();
        //measurement always goes directly to the node
        //which may also be the admin server itself
        template = WeblogicMetric.translateNode(template, config);
        template = super.translate(template, config);

        //allows *EJB to share templates
        final String beanTok = "${BeanType}";
        if (template.indexOf(beanTok) != -1) {
            String beanType = getServiceName();
            //"Entity EJB" -> "EntityEJB"
            beanType = StringUtil.replace(beanType, " ", "");
            //"${BeanType}Runtime=..." -> "EntityEJBRuntime=..."
            template = StringUtil.replace(template, beanTok, beanType);
        }
        else if (info.getType() == TypeInfo.TYPE_SERVER) {
            if (info.getVersion().equals(WeblogicProductPlugin.VERSION_61)) {
                // s/StateVal/State/ for 6.1
                template = StringUtil.replace(template,
                                              SERVER_AVAIL_ATTR,
                                              SERVER_AVAIL_ATTR_61);
            }
        }

        return template;
    }
}

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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAIJobFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAIScheduleFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllAppsFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllPlatformTypesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllPlatformsFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServerTypesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServersFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServiceTypesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServicesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindClosestMetricDataFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindControlJobFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindControlScheduleFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindMetricDataFetcher;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.LiveDataBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.resourceImport.BatchImportData;
import org.hyperic.hq.bizapp.shared.resourceImport.BatchImportException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.livedata.shared.LiveDataException;
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.measurement.EvaluationException;
import org.hyperic.hq.measurement.MeasurementConfigException;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.data.DataNotAvailableException;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.schedule.ScheduleException;
import org.quartz.SchedulerException;

public class ClientShellEntityFetcher {
    protected ClientShellBossManager   bossManager;
    protected ClientShellAuthenticator auth;

    public ClientShellEntityFetcher(ClientShellBossManager bossManager,
                                    ClientShellAuthenticator auth)
    {
        this.bossManager = bossManager;
        this.auth        = auth;
    }

    public PlatformValue getPlatformValue(String platformTag)
        throws NamingException, ClientShellAuthenticationException,
               AppdefEntityNotFoundException, SessionTimeoutException,
               SessionNotFoundException, PermissionException,
               RemoteException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        Integer id;
        int token;

        token = this.auth.getAuthToken();

        try {
            id = Integer.valueOf(platformTag);
        } catch(NumberFormatException exc){
            return boss.findPlatformByName(token, platformTag);
        }
        return boss.findPlatformById(token, id);
    }

    public ApplicationValue getApplicationValue(String appTag)
        throws NamingException, AppdefEntityNotFoundException, RemoteException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        Integer id;
        int token;
        
        token = this.auth.getAuthToken();

        try {
            id = Integer.valueOf(appTag);
        } catch(NumberFormatException exc){
            return boss.findApplicationByName(token, appTag);
        }
        return boss.findApplicationById(token, id);
    }

    public ServerValue getServerValue(String serverTag)
        throws NamingException, ClientShellAuthenticationException,
               AppdefEntityNotFoundException, SessionTimeoutException,
               SessionNotFoundException, PermissionException, RemoteException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        Integer id;
        int token;

        token = this.auth.getAuthToken();
        try {
            id = Integer.valueOf(serverTag);
        } catch(NumberFormatException exc){
            ServerValue[] servers = boss.findServersByName(token, serverTag);
            if (servers.length == 1) {
                return servers[0];
            } else {
                StringBuffer sb = new StringBuffer();
                for (int i=0; i<servers.length; i++) {
                    sb.append(servers[i].getId().toString());
                    if (i+1 != servers.length)
                        sb.append(", ");
                }
                
                throw new ServerNotFoundException("Multiple servers " +
                                                  "found for '" +
                                                  serverTag + "'," +
                                                  " possible matches: " +
                                                  sb.toString());
            }
        }
        return boss.findServerById(token, id);
    }

    public AppdefGroupValue getGroupValue (String groupTag)
        throws NamingException,FinderException, 
               SessionTimeoutException, SessionNotFoundException,
               RemoteException, PermissionException, 
               ClientShellAuthenticationException, AppdefGroupNotFoundException
    {
        AppdefGroupValue retVal = null;
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();
        try {
            Integer gid = Integer.valueOf(groupTag);
            retVal = boss.findGroup(token, gid);
        } catch (NumberFormatException e) {
            return boss.findGroupByName(token, groupTag);
        }
        return retVal;
    }

    public ServiceValue getServiceValue(String serviceTag)
        throws NamingException, ClientShellAuthenticationException,
               AppdefEntityNotFoundException, SessionTimeoutException,
               SessionNotFoundException, PermissionException,
               RemoteException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        Integer id;
        int token;

        token = this.auth.getAuthToken();

        try {
            id = Integer.valueOf(serviceTag);
        } catch(NumberFormatException exc){
            ServiceValue[] services = 
                boss.findServicesByName(token, serviceTag);
            if (services.length == 1) {
                return services[0];
            } else {
                StringBuffer sb = new StringBuffer();
                for (int i=0; i<services.length; i++) {
                    sb.append(services[i].getId().toString());
                    if (i+1 != services.length)
                        sb.append(", ");
                }

                throw new ServiceNotFoundException("Multiple services " +
                                                   "found for '" +
                                                   serviceTag + "'," +
                                                   " possible matches: " +
                                                   sb.toString());
            }
        }
        return boss.findServiceById(token, id);
    }

    public List findAllPlatformTypes()
        throws NamingException, PermissionException, FinderException,
               ClientShellAuthenticationException, SessionTimeoutException,
               SessionNotFoundException, RemoteException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();

        return boss.findAllPlatformTypes(token, PageControl.PAGE_ALL);
    }

    public List findAllServerTypes()
        throws NamingException, FinderException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, PermissionException,
               ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();

        return boss.findAllServerTypes(token, PageControl.PAGE_ALL);
    }

    public List findAllServiceTypes()
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();

        return boss.findAllServiceTypes(token, PageControl.PAGE_ALL);
    }

    public List findAllGroups()
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException,
               RemoteException, ClientShellAuthenticationException, 
               ApplicationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();

        return boss.findAllGroups(token, PageControl.PAGE_ALL);
    }

    public List findAllAlertDefinitions()
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException,
               ClientShellAuthenticationException, PermissionException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.findAllAlertDefinitions(this.auth.getAuthToken());
    }

    public List findAllAlerts()
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException, ClientShellAuthenticationException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.findAllAlerts(this.auth.getAuthToken());
    }

    public List findResourceAlerts(AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException, ClientShellAuthenticationException,
               PermissionException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.findAlerts(this.auth.getAuthToken(), id, 
                               PageControl.PAGE_ALL);
    }

    public int deleteResourceAlerts(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, RemoteException, RemoveException {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.deleteAlerts(this.auth.getAuthToken(), id);
    }

    public int deleteAlertsInTimeRange(long begin, long end)
        throws NamingException, ClientShellAuthenticationException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, RemoteException, RemoveException {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.deleteAlerts(this.auth.getAuthToken(), begin, end);
    }
    
    public void activateAlertDefinitions(Integer[] ids, boolean activate)
        throws ClientShellAuthenticationException, NamingException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, RemoteException, FinderException {
        EventsBoss boss = bossManager.getEventsBoss();
        boss.activateAlertDefinitions(auth.getAuthToken(), ids, activate);
    }
    
    public void flushRegisteredTriggers()
        throws ClientShellAuthenticationException, NamingException,
               SessionNotFoundException, SessionTimeoutException,
               RemoteException {
        EventsBoss boss = bossManager.getEventsBoss();
        boss.flushRegisteredTriggers(auth.getAuthToken());
    }

    public List findResourceAlertDefinitions(AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException,
               ClientShellAuthenticationException, PermissionException 
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.findAlertDefinitions(this.auth.getAuthToken(), id, 
                                         PageControl.PAGE_ALL);
    }



    public PlatformTypeValue getPlatformTypeValue(String platformTag)
        throws NamingException, PlatformNotFoundException, RemoteException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, ClientShellAuthenticationException
               
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();
        Integer id;

        try {
            id = Integer.valueOf(platformTag);
        } catch(NumberFormatException exc){
            return boss.findPlatformTypeByName(token, platformTag);
        }
        return boss.findPlatformTypeById(token, id);
    }

    public ServerTypeValue getServerTypeValue(String serverTag)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, RemoteException, PermissionException,
               ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();
        Integer id;

        try {
            id = Integer.valueOf(serverTag);
        } catch(NumberFormatException exc){
            return boss.findServerTypeByName(token, serverTag);
        }
        return boss.findServerTypeById(token, id);
    }

    public ServiceTypeValue getServiceTypeValue(String serviceTag)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, RemoteException, PermissionException,
               ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        int token = this.auth.getAuthToken();
        Integer id;

        try {
            id = Integer.valueOf(serviceTag);
        } catch(NumberFormatException exc){
            return boss.findServiceTypeByName(token, serviceTag);
        }
        return boss.findServiceTypeById(token, id);
    }

    public AIPlatformValue getAIPlatformValue(String aiplatformTag)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, RemoteException, PermissionException,
               CreateException, ClientShellAuthenticationException, RemoveException
    {
        AIBoss boss = this.bossManager.getAIBoss();
        int id, token;

        token = this.auth.getAuthToken();

        try {
            id = Integer.parseInt(aiplatformTag);
        } catch(NumberFormatException exc){
            return boss.findAIPlatformByFqdn(token, aiplatformTag);
        }
        return boss.findAIPlatformById(token, id);
    }

    public AIServerValue getAIServerValue(String aiserverTag)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, RemoteException, PermissionException,
               ClientShellAuthenticationException
    {
        AIBoss boss = this.bossManager.getAIBoss();
        int id, token;

        token = this.auth.getAuthToken();

        try {
            id = Integer.parseInt(aiserverTag);
        } catch(NumberFormatException exc){
            return boss.findAIServerByName(token, aiserverTag);
        }
        return boss.findAIServerById(token, id);
    }

    public AIIpValue getAIIpValue(String aiipTag)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, RemoteException, PermissionException,
               ClientShellAuthenticationException
    {
        AIBoss boss = this.bossManager.getAIBoss();
        int id, token;

        token = this.auth.getAuthToken();

        try {
            id = Integer.parseInt(aiipTag);
        } catch(NumberFormatException exc){
            return boss.findAIIpByAddress(token, aiipTag);
        }
        return boss.findAIIpById(token, id);
    }

    public List getMetricsForID(AppdefEntityID id)
        throws NamingException, PermissionException, 
               ClientShellAuthenticationException, AppdefEntityNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               GroupNotCompatibleException, RemoteException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.findMeasurements(this.auth.getAuthToken(), id,
                                     PageControl.PAGE_ALL);
    }

    public List getMetricTemplatesForMonitorableType(String mType)
        throws SessionTimeoutException, SessionNotFoundException,
               NamingException, RemoteException, ClientShellAuthenticationException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.findMeasurementTemplates(this.auth.getAuthToken(), 
                                             mType, PageControl.PAGE_ALL);
    }

    public List getMetricTemplatesForID(AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException, 
               RemoteException, PermissionException,
               AppdefEntityNotFoundException, NamingException,
               FinderException, ClientShellAuthenticationException
    {
        // First get the appdef resource type
        AppdefResourceValue res = this.findResourceByID(id);
        return this.getMetricTemplatesForMonitorableType(
            res.getAppdefResourceTypeValue().getName());
    }

    public AppdefEntityID getID(int type, String resourceTag)
            throws NamingException, FinderException,
            AppdefEntityNotFoundException, SessionTimeoutException,
            SessionNotFoundException, RemoteException, PermissionException,
            CreateException, ClientShellAuthenticationException,
            RemoveException {        
        return getID(type, resourceTag, true);
    }

    public AppdefEntityID getID(int type, String resourceTag, boolean shortcut)
        throws NamingException, FinderException, AppdefEntityNotFoundException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, PermissionException, CreateException, 
               ClientShellAuthenticationException, RemoveException
    {
        int id;

        if (shortcut) {
            try {
                return new AppdefEntityID(type, Integer.parseInt(resourceTag));
            } catch(NumberFormatException exc){
                // passthrough
            }
        }

        switch(type){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            id = this.getPlatformValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            id = this.getServerValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            id = this.getServiceValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            id = this.getApplicationValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            id = this.getGroupValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM:
            id = this.getAIPlatformValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_AISERVER:
            id = this.getAIServerValue(resourceTag).getId().intValue();
            break;
        case AppdefEntityConstants.APPDEF_TYPE_AIIP:
            id = this.getAIIpValue(resourceTag).getId().intValue();
            break;
        default:
            throw new IllegalArgumentException("Unknown ID type");
        }
        
        return new AppdefEntityID(type, id);
    }

    public DerivedMeasurementValue getMetric(int metricID)
        throws SessionTimeoutException, SessionNotFoundException,
               MeasurementNotFoundException, NamingException,
               RemoteException, ClientShellAuthenticationException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.getMeasurement(this.auth.getAuthToken(),
                                   new Integer(metricID));
    }

    public DerivedMeasurementValue getMetricByAliasAndID(AppdefEntityID id,
                                                         String alias)
        throws NamingException, MeasurementNotFoundException, RemoteException,
               ClientShellAuthenticationException, SessionTimeoutException,
               SessionNotFoundException, MeasurementNotFoundException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.getMeasurement(this.auth.getAuthToken(), id, alias);
    }

    public MetricValue getLiveMetricValue(int metricID)
        throws NamingException, ClientShellAuthenticationException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, MeasurementNotFoundException,
               EvaluationException, LiveMeasurementException, RemoteException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.getLiveMeasurementValue(this.auth.getAuthToken(),
                                            new Integer(metricID));
    }

    public void enableMetrics(Integer[] templates, AppdefEntityID id,
                              int interval)
        throws ClientShellAuthenticationException, NamingException,
               SessionTimeoutException, SessionNotFoundException,
               TemplateNotFoundException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, MeasurementCreateException,
               ConfigFetchException, PermissionException, RemoteException,
               EncodingException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        boss.createMeasurements(this.auth.getAuthToken(), id, templates, 
                                (long)interval * 1000L);
    }

    public void disableMetrics(AppdefEntityID id)
        throws SessionTimeoutException, SessionNotFoundException,
               NamingException, MeasurementConfigException, PermissionException,
               RemoteException, ClientShellAuthenticationException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        boss.disableMeasurements(this.auth.getAuthToken(), id);
    }

    public void disableMetrics(Integer[] ids)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, NamingException, RemoteException,
               MeasurementNotFoundException, ClientShellAuthenticationException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        boss.disableMeasurements(this.auth.getAuthToken(), ids);
    }

    public void deleteAIJob(Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, ClientShellAuthenticationException,
               AutoinventoryException
    {
        AIBoss boss;
        try {
            boss = this.bossManager.getAIBoss();
            boss.deleteAIJob(this.auth.getAuthToken(), ids);
        } catch ( NamingException ne ) {
            throw new SystemException(ne);
        } catch ( RemoteException re ) {
            throw new SystemException(re);
        }
    }

    public Properties getResourceProperties(AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException, ClientShellAuthenticationException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.getResourceProperties(this.auth.getAuthToken(), id);
    }

    public AlertDefinitionValue createAlertDefinition(
        AlertDefinitionValue alertdef)
        throws ClientShellAuthenticationException, NamingException,
               TriggerCreateException, AlertDefinitionCreateException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException, RemoteException {
        
        EventsBoss boss = this.bossManager.getEventsBoss();
        alertdef =
            boss.createAlertDefinition(this.auth.getAuthToken(), alertdef);
        
        return alertdef;
    }

    public void deleteAlertDefinitions(Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException, RemoveException,
               FinderException, PermissionException,
               ClientShellAuthenticationException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        boss.deleteAlertDefinitions(this.auth.getAuthToken(), ids);
    }

    public ConfigSchema getConfigSchema(AppdefEntityID id, String type)
        throws ConfigFetchException, PermissionException, EncodingException,
               PluginNotFoundException, PluginException,
               SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, ClientShellAuthenticationException,
               FinderException, CreateException, ApplicationException
    {
        ProductBoss boss;
        
        try {
            boss = this.bossManager.getProductBoss();
            return boss.getConfigSchema(this.auth.getAuthToken(), id, type);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (RemoteException e) {
            throw new SystemException(e);
        }
    }

    public String importBatchData(BatchImportData data)
        throws ClientShellAuthenticationException, NamingException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, RemoteException, BatchImportException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.importBatchData(this.auth.getAuthToken(), data);
    }

    public ResourceTree getResourceTree(AppdefEntityID[] ids, int traversal)
        throws ClientShellAuthenticationException, NamingException,
               AppdefEntityNotFoundException, PermissionException,
               SessionTimeoutException, SessionNotFoundException,
               RemoteException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.getResourceTree(this.auth.getAuthToken(),
                                    ids, traversal);
    }

    public PageList findResourcesByTypeId(AppdefEntityTypeID typeId) 
        throws NamingException, AppdefEntityNotFoundException, RemoteException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, ClientShellAuthenticationException
    {
        AppdefBoss boss = this.bossManager.getAppdefBoss();
        return boss.findCompatInventory(this.auth.getAuthToken(), 
                                        typeId.getType(), 
                                        typeId.getId().intValue(),
                                        null,
                                        null,
                                        null,
                                        PageControl.PAGE_ALL);
        
    }
    
    public AppdefResourceValue findResourceByID(AppdefEntityID id)
        throws NamingException, AppdefEntityNotFoundException, RemoteException,
               SessionTimeoutException, SessionNotFoundException,
               PermissionException, ClientShellAuthenticationException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.findById(this.auth.getAuthToken(), id);
    }

    public ConfigResponseDB getConfigResponse(AppdefEntityID id)
        throws NamingException, AppdefEntityNotFoundException, RemoteException,
               SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException
    {
        ProductBoss boss;

        boss = this.bossManager.getProductBoss();
        return boss.getConfigResponse(this.auth.getAuthToken(), id);
    }

    public ConfigSchema getActionConfigSchema(String actionClass)
        throws SessionNotFoundException, SessionTimeoutException, 
               EncodingException, ClientShellAuthenticationException, 
               RemoteException, NamingException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.getActionConfigSchema(this.auth.getAuthToken(), 
                                          actionClass);
    }

    public ActionValue createAction(Integer adid, String actionClass,
                                    ConfigResponse response)
        throws SessionNotFoundException, SessionTimeoutException,
               NamingException, ActionCreateException, RemoveException,
               FinderException, RemoteException,
               ClientShellAuthenticationException, PermissionException
    {
        EventsBoss boss;

        boss = this.bossManager.getEventsBoss();
        return boss.createAction(this.auth.getAuthToken(), adid, actionClass,
                                 response);
    }

    public void setConfigResponse(AppdefEntityID id, ConfigResponse response, 
                                  String type)
        throws SessionNotFoundException, SessionTimeoutException,
               CreateException, NamingException, EncodingException,
               FinderException, PermissionException, RemoteException,
               ClientShellAuthenticationException, ScheduleException,
               ConfigFetchException, ApplicationException
    {
        ProductBoss boss;

        boss = this.bossManager.getProductBoss();
        boss.setConfigResponse(this.auth.getAuthToken(), id, response, type);
    }

    public ConfigResponse getMergedConfigResponse(String productType,
                                                  AppdefEntityID id,
                                                  boolean required)
        throws NamingException, AppdefEntityNotFoundException, CreateException,
               ConfigFetchException, SessionNotFoundException,
               SessionTimeoutException, EncodingException, PermissionException,
               ClientShellAuthenticationException, RemoteException
    {
        ProductBoss boss;

        boss = this.bossManager.getProductBoss();
        return boss.getMergedConfigResponse(this.auth.getAuthToken(), 
                                            productType, id, required);
    }

    public FindAllAppsFetcher findAllAppsFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllAppsFetcher(this.bossManager.getAppdefBoss(),
                                      this.auth.getAuthToken());
    }

    public FindAllPlatformsFetcher findAllPlatformsFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllPlatformsFetcher(this.bossManager.getAppdefBoss(),
                                           this.auth.getAuthToken());
    }

    public FindAllServersFetcher findAllServersFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllServersFetcher(this.bossManager.getAppdefBoss(),
                                         this.auth.getAuthToken());
    }

    public FindAllServicesFetcher findAllServicesFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllServicesFetcher(this.bossManager.getAppdefBoss(),
                                          this.auth.getAuthToken());
    }

    public FindAllPlatformTypesFetcher findAllPlatformTypesFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllPlatformTypesFetcher(this.bossManager.getAppdefBoss(),
                                               this.auth.getAuthToken());
    }

    public FindAllServerTypesFetcher findAllServerTypesFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllServerTypesFetcher(this.bossManager.getAppdefBoss(),
                                             this.auth.getAuthToken());
    }

    public FindAllServiceTypesFetcher findAllServiceTypesFetcher()
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindAllServiceTypesFetcher(this.bossManager.getAppdefBoss(),
                                              this.auth.getAuthToken());
    }

    public FindAIScheduleFetcher 
        findAIScheduleFetcher(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException
    {
        return new 
            FindAIScheduleFetcher(this.bossManager.getAIBoss(),
                                  this.auth.getAuthToken(),
                                  id);
    }

    public FindAIJobFetcher findAIJobFetcher(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException
    {   
        return new FindAIJobFetcher(this.bossManager.getAIBoss(),
                                    this.auth.getAuthToken(),
                                    id);
    }

    public FindMetricDataFetcher findAllMetricDataFetcher(int metricID)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, ClientShellAuthenticationException
    {
        return new
            FindMetricDataFetcher(this.bossManager.getMeasurementBoss(),
                                  this.auth.getAuthToken(),
                                  metricID);
    }

    public FindMetricDataFetcher findAllMetricDataFetcher(int metricID,
                                                          long begin, long end)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, ClientShellAuthenticationException
    {
        return new
            FindMetricDataFetcher(this.bossManager.getMeasurementBoss(),
                                  this.auth.getAuthToken(),
                                  metricID, begin, end);
    }

    public FindMetricDataFetcher findClosestMetricDataFetcher(int metricID,
                                                              long begin,
                                                              long end,
                                                              long interval)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, ClientShellAuthenticationException
    {
        return new
            FindClosestMetricDataFetcher(this.bossManager.getMeasurementBoss(),
                                         this.auth.getAuthToken(),
                                         metricID, begin, end, interval);
    }

    public void scheduleAIScan(AppdefEntityID id, 
                               ScanConfigurationCore scanConfig,
                               String scanName, String scanDesc,
                               ScheduleValue schedule)
        throws ScheduleException, AutoinventoryException,
               FinderException, NamingException, AppdefEntityNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, RemoteException,
               ClientShellAuthenticationException, GroupNotCompatibleException,
               AgentConnectionException, AgentNotFoundException,
               DuplicateAIScanNameException
    {
        AIBoss boss;

        boss = this.bossManager.getAIBoss();

        if (id.getType()==AppdefEntityConstants.APPDEF_TYPE_GROUP)
            boss.startGroupScan(this.auth.getAuthToken(), id.getID(), 
                                scanConfig, scanName, scanDesc, schedule);
        else
            boss.startScan(this.auth.getAuthToken(), id.getID(), 
                           scanConfig, scanName, scanDesc, schedule);
    }

    public void toggleRuntimeScan(AppdefEntityID id, boolean doEnable)
        throws AutoinventoryException,
               FinderException, NamingException, AppdefEntityNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, RemoteException, 
               ConfigFetchException, EncodingException,
               ClientShellAuthenticationException, GroupNotCompatibleException,
               UpdateException
    {
        AIBoss boss;
        boss = this.bossManager.getAIBoss();
        boss.toggleRuntimeScan(this.auth.getAuthToken(), id, 
                               doEnable);
    }

    public Integer createDerivedMeasurementTemplate(String name, String alias,
                                                    String monType, String cat,
                                                    String expr, String units,
                                                    int collectionType,
                                                    MeasurementArgValue[] args)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        return boss.createDerivedMeasurementTemplate(this.auth.getAuthToken(),
                                                     name, alias, monType, cat,
                                                     expr, units, 
                                                     collectionType, args);
    }

    public void removeDerivedMeasurementTemplate(int metricId)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException, RemoveException
    {
        MeasurementBoss boss;

        boss = this.bossManager.getMeasurementBoss();
        boss.removeDerivedMeasurementTemplate(this.auth.getAuthToken(),
                                              new Integer(metricId));
                                                     
    }

    public Properties getCPropEntries(AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException, PermissionException, 
               AppdefEntityNotFoundException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.getCPropEntries(this.auth.getAuthToken(), id);
    }

    public void setCPropValue(AppdefEntityID id, String key, String value)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException, AppdefEntityNotFoundException, 
               PermissionException, CPropKeyNotFoundException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        boss.setCPropValue(this.auth.getAuthToken(), id, key, value);
    }

    public List getCPropKeys(int appdefType, int appdefTypeId)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException
    {
        AppdefBoss boss;

        boss = this.bossManager.getAppdefBoss();
        return boss.getCPropKeys(this.auth.getAuthToken(), appdefType,
                                 appdefTypeId);
    }

    public List getAllSubjects()
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException, CreateException, FinderException,
               PermissionException
    {
        AuthzBoss boss;

        boss = this.bossManager.getAuthzBoss();
        return boss.getAllSubjects( new Integer( this.auth.getAuthToken() ),
                                    null, PageControl.PAGE_ALL );
    }

    public void compactMeasurementData()
        throws ClientShellAuthenticationException, NamingException,
               SessionTimeoutException, SessionNotFoundException,
               DataNotAvailableException, RemoteException,
               ConfigPropertyException {
        MeasurementBoss boss;
    
        boss = this.bossManager.getMeasurementBoss();
        boss.invokeDataCompact(auth.getAuthToken());
    }

    public boolean ensureNamesAreIds(ConfigResponse response)
        throws SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, RemoteException, 
               NamingException, CreateException, FinderException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException
    {
        int type = new Integer( response.getValue
                                (EmailActionConfig.CFG_TYPE) ).intValue();
        String names = (String)response.getValue
            (EmailActionConfig.CFG_NAMES);
        switch (type) {
        case EmailActionConfig.TYPE_USERS:
            // build a map of subject name to subject ID
            List allSubjects = this.getAllSubjects();
            HashMap subjectMap = new HashMap();
            for (Iterator it=allSubjects.iterator(); it.hasNext();) {
                AuthzSubjectValue subject = (AuthzSubjectValue)it.next();
                subjectMap.put( subject.getName(), subject.getId() );
            }
            return updateResponseWithIds(subjectMap, names, response);
    
        default:
            // do nothing
            return true;
        }
    }
    
    protected boolean updateResponseWithIds(Map nameIdMap, String idsOrNamesCsv,
            ConfigResponse response)
    throws InvalidOptionException, InvalidOptionValueException
    {
        List ids = new ArrayList();
        List idsOrNames = StringUtil.explode(idsOrNamesCsv, ",");
        int numInvalid = 0;
        for (Iterator it=idsOrNames.iterator(); it.hasNext();) {
            String idOrName = (String)it.next();
            try {
                Integer id = new Integer(idOrName);
                ids.add(id);
            } catch (NumberFormatException e) {
                // look up user
                Integer id = (Integer)nameIdMap.get(idOrName);
                if (null == id) {
                    numInvalid++;
                } else {
                    ids.add(id);
                }
            }
        }
        String idsCsv = StringUtil.implode(ids, ",");
        response.setValue(EmailActionConfig.CFG_NAMES, idsCsv);
        
        return (0 == numInvalid);
    }

    public LiveDataResult getLiveData(LiveDataCommand command)
        throws NamingException, ClientShellAuthenticationException,
               PermissionException, RemoteException, AgentNotFoundException,
               LiveDataException, AppdefEntityNotFoundException,
               SessionTimeoutException, SessionNotFoundException
    {
        LiveDataBoss boss;

        boss = this.bossManager.getLiveDataBoss();

        return boss.getLiveData(auth.getAuthToken(), command);
    }

    public LiveDataResult[] getLiveData(LiveDataCommand[] commands) 
        throws NamingException, ClientShellAuthenticationException,
               PermissionException, RemoteException, AgentNotFoundException,
               LiveDataException, AppdefEntityNotFoundException,
               SessionTimeoutException, SessionNotFoundException
    {
        LiveDataBoss boss;

        boss = this.bossManager.getLiveDataBoss();

        return boss.getLiveData(auth.getAuthToken(), commands);
    }

    public String[] getLiveDataCommands(AppdefEntityID id)
        throws RemoteException, NamingException,
        ClientShellAuthenticationException, PluginException,
        PermissionException,
        SessionTimeoutException, SessionNotFoundException
    {
        LiveDataBoss boss;

        boss = this.bossManager.getLiveDataBoss();

        return boss.getLiveDataCommands(auth.getAuthToken(), id);
    }

    public ConfigSchema getLiveDataConfigSchema(AppdefEntityID id,
                                                String command)
        throws RemoteException, NamingException,
        ClientShellAuthenticationException, PluginException,
        PermissionException,
        SessionTimeoutException, SessionNotFoundException
    {
        LiveDataBoss boss;

        boss = this.bossManager.getLiveDataBoss();

        return boss.getConfigSchema(auth.getAuthToken(), id, command);
    }

    public void clearCaches() 
        throws RemoteException, NamingException,
               ClientShellAuthenticationException, PluginException,
               PermissionException, SessionTimeoutException, 
               SessionNotFoundException
    {
        ProductBoss boss;

        boss = this.bossManager.getProductBoss();

        boss.clearCaches(auth.getAuthToken());
    }

    public FindControlJobFetcher findControlJobFetcher(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException
    {
        return new FindControlJobFetcher(bossManager.getControlBoss(),
                                         auth.getAuthToken(),
                                         id);
    }

    public FindControlScheduleFetcher
        findControlScheduleFetcher(AppdefEntityID id)
        throws NamingException, ClientShellAuthenticationException
    {
        return new
            FindControlScheduleFetcher(bossManager.getControlBoss(),
                                       auth.getAuthToken(),
                                       id);
    }

    public void deleteControlJob(Integer[] ids)
        throws PluginException, ApplicationException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, ClientShellAuthenticationException
    {
        try {
            ControlBoss boss = bossManager.getControlBoss();
            boss.deleteControlJob(auth.getAuthToken(), ids);
        } catch (RemoteException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    public void doAction(AppdefEntityID id, String action, int[] orderSpec)
        throws PluginException, ApplicationException,
               SessionNotFoundException, SessionTimeoutException,
               ClientShellAuthenticationException, AppdefEntityNotFoundException,
               PermissionException, RemoteException, GroupNotCompatibleException
    {
        ControlBoss boss;

        try {
            boss = bossManager.getControlBoss();
            if (id.getType()==AppdefEntityConstants.APPDEF_TYPE_GROUP)
                boss.doGroupAction(auth.getAuthToken(),id,
                                   action, (String)null, orderSpec);
            else
                boss.doAction(auth.getAuthToken(), id, action, (String)null);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (RemoteException e) {
            throw new SystemException(e);
        }
    }

    public void doAction(AppdefEntityID id, String action,
                         ScheduleValue schedule, int[] orderSpec)
        throws PluginException, ApplicationException, SessionNotFoundException,
               SessionTimeoutException, PermissionException,
               ClientShellAuthenticationException, GroupNotCompatibleException,
               AppdefEntityNotFoundException, SchedulerException
    {
        ControlBoss boss;

        try {
            boss = bossManager.getControlBoss();

            if (id.getType()==AppdefEntityConstants.APPDEF_TYPE_GROUP)
                boss.doGroupAction(auth.getAuthToken(),id,action,
                                   orderSpec,schedule);
            else
                boss.doAction(auth.getAuthToken(), id, action, schedule);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (RemoteException e) {
            throw new SystemException(e);
        }
    }

    public List getActions(AppdefEntityID id)
        throws ApplicationException,
               PluginNotFoundException, ClientShellAuthenticationException,
               SessionNotFoundException, SessionTimeoutException,
               NamingException, RemoteException, AppdefEntityNotFoundException
    {
        ControlBoss boss;

        try {
            boss = bossManager.getControlBoss();
            return boss.getActions(auth.getAuthToken(), id);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (RemoteException e) {
            throw new SystemException(e);
        }
    }
}


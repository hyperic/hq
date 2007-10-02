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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.agent.AgentCommandsAPI;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationManagerUtil;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.bizapp.shared.ProductBossLocal;
import org.hyperic.hq.bizapp.shared.ProductBossUtil;
import org.hyperic.hq.bizapp.shared.resourceImport.BatchImportData;
import org.hyperic.hq.bizapp.shared.resourceImport.BatchImportException;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlAgentConnValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlApplicationServiceValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlApplicationValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlConfigInfo;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlCustomPropsValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlResourceValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlGroupMemberValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlGroupValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlIpValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlPlatformValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlServerValue;
import org.hyperic.hq.bizapp.shared.resourceImport.XmlServiceValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupCreationException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupModificationException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.grouping.shared.GroupVisitorException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.TextIndenter;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

class ImportHelper 
    extends BizappSessionEJB
{
    private AgentManagerLocal       _agentMan;
    private PlatformManagerLocal    _platMan;
    private ServerManagerLocal      _servMan;
    private ServiceManagerLocal     _serviceMan;
    private ApplicationManagerLocal _appMan;
    private ProductBossLocal        _prodBoss;
    private AppdefGroupManagerLocal _groupMan;

    private AuthzSubjectValue       _subject;
    private BatchImportData         _data;
    
    ImportHelper(AuthzSubjectValue subject, BatchImportData data){
        _subject      = subject;
        _data         = data;

        try {
            _agentMan    = this.getAgentManager();
            _platMan     = PlatformManagerUtil.getLocalHome().create();
            _servMan     = ServerManagerUtil.getLocalHome().create();
            _serviceMan  = ServiceManagerUtil.getLocalHome().create();
            _appMan      = ApplicationManagerUtil.getLocalHome().create();
            _prodBoss    = ProductBossUtil.getLocalHome().create();
            _groupMan    = this.getAppdefGroupManager();
        } catch(NamingException exc){
            throw new SystemException("Internal error setting up managers");
        } catch(CreateException exc){
            throw new SystemException("Internal error setting up managers");
        }
    }

    private void setCProps(AppdefEntityID id, XmlCustomPropsValue props,
                           String name, String typeName)
        throws BatchImportException
    {
        // This feature is disabled until further notice.        
//        CPropManagerLocal cpMan = this.getCPropManager();
//
//        if(props == null)
//            return;
//
//        for(Iterator i=props.getValues().entrySet().iterator(); i.hasNext(); ){
//            Map.Entry ent = (Map.Entry)i.next();
//
//            try {
//                cpMan.setValue(new AppdefEntityValue(id, this.subject), 
//                               (String)ent.getKey(), (String)ent.getValue());
//            } catch(AppdefEntityNotFoundException exc){
//                // Should not occur
//                throw new SystemException("Internal error finding " + id);
//            } catch(PermissionException exc){
//                throw new BatchImportException("Error setting custom " +
//                                               "properties for " + name + 
//                                               ": Permission denied");
//            } catch(CPropKeyNotFoundException exc){
//                throw new BatchImportException("Error setting custom " +
//                                 "properties for " + name + ": The key, '" + 
//                                 ent.getKey() + "', was not valid for " +
//                                 id.getTypeName() + " resource type '" +
//                                 typeName + "'");
//            }
//        }
    }

    private PlatformValue createPlatform(XmlPlatformValue platform,
                                         TextIndenter buf)
        throws BatchImportException, NamingException
    {
        XmlAgentConnValue agentInfo;
        PlatformValue aPlatform;
        PlatformTypeValue pType;
        String typeName, name, fqdn;
        Integer agentPk;
        int agtPort;

        name     = platform.getName();
        fqdn     = platform.getFqdn();
        typeName = platform.getType();

        try {
            pType = findPlatformTypeByName(typeName);
        } catch(PlatformNotFoundException exc){
            throw new BatchImportException("Platform '" + name + "' " +
                                           "depends on platform type '" + 
                                           typeName + "' which could not" +
                                           " be found");
        }

        aPlatform = new PlatformValue();
        aPlatform.setName(name);
        aPlatform.setFqdn(platform.getFqdn());
        aPlatform.setCertdn(platform.getCertdn());
        aPlatform.setPlatformType(pType);
        aPlatform.setCommentText(platform.getComment());
        aPlatform.setCpuCount(platform.getCpuCount());
        aPlatform.setDescription(platform.getDescription());
        aPlatform.setLocation(platform.getLocation());

        for(Iterator i=platform.getIpValues().iterator(); i.hasNext(); ){
            XmlIpValue ip = (XmlIpValue)i.next();
            IpValue aIp;

            aIp = new IpValue();
            aIp.setAddress(ip.getAddress());
            aIp.setNetmask(ip.getNetmask());
            aIp.setMACAddress(ip.getMacAddress());
            aPlatform.addIpValue(aIp);

            buf.append("- Assigning IP address " + ip.getAddress());
            buf.append("\n");
        }

        agentInfo = platform.getAgentConn();
        agtPort = agentInfo.getPort() == null ? AgentCommandsAPI.DEFAULT_PORT 
                                              : agentInfo.getPort().intValue();

        try {
            AgentValue agentVal;

            agentVal = _agentMan.getAgent(agentInfo.getAddress(), agtPort);
            agentPk  = agentVal.getId();
        } catch(AgentNotFoundException exc){
            throw new BatchImportException("Error creating platform '" + 
                                           name + ": No agent was found for " +
                                           agentInfo.getAddress() + ":" +
                                           agtPort);
        }


        try {
            try {
                Platform plat  = _platMan.createPlatform(_subject,
                                                         pType.getId(),
                                                         aPlatform, agentPk);
                aPlatform =
                    _platMan.getPlatformValueById(_subject, plat.getId());
            } catch (PlatformNotFoundException e) {
                throw new BatchImportException("Unable to find the platform " +
                                               " we just created");
            }
        } catch (AppdefDuplicateNameException e) {
            throw new BatchImportException("Error creating platform '" +
                                           name + "': Platform with that " +
                                           " name already exists");
        } catch (AppdefDuplicateFQDNException e) {
            throw new BatchImportException("Error creating platform '" +
                                           name + "': Platform with that " +
                                           " domain name (" + fqdn +
                                           ") already exists");
        } catch(CreateException exc){
            throw new BatchImportException("Error creating platform '" +
                                           name + "': " +exc.getMessage());
        } catch(ValidationException exc){
            throw new BatchImportException("Error creating platform '" +
                                           name + "': " +exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied, creating "+
                                           "platform '" + name + "': " +
                                           exc.getMessage());
        } catch(ApplicationException exc){
            throw new BatchImportException("Error creating platform '" +
                                           name + "': " +exc.getMessage());
        }

        this.setCProps(aPlatform.getEntityId(), platform.getCustomProps(),
                       name, typeName);
        return aPlatform;
    }

    private void processPlatform(XmlPlatformValue platform, 
                                 TextIndenter buf)
        throws BatchImportException, NamingException
    {
        PlatformValue aPlatform = null;
        boolean create;
        String name;

        name = platform.getName();
        create = false;
        try {
            aPlatform = findPlatformByName(name);
        } catch(PlatformNotFoundException exc){
            // New platform -- create it!
            create = true;
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied looking up "+
                                           "platform '" + name + "': " + 
                                           exc.getMessage());
        }

        if(create){
            buf.append("Creating new platform: " + name + "\n");
            buf.pushIndent();
            aPlatform = this.createPlatform(platform, buf);
            buf.popIndent();
        } else {
            buf.append("Processing platform: " + name + "\n");
        }

        buf.pushIndent();
        this.handleAllConfigs(aPlatform.getEntityId(), name, platform, buf);
        buf.popIndent();

        for(Iterator i=platform.getServers().iterator(); i.hasNext(); ){
            XmlServerValue server = (XmlServerValue)i.next();

            buf.pushIndent();
            this.processServer(server, aPlatform, buf);
            buf.popIndent();
        }
        
        buf.append("Running auto-scan for platform: " + name + "...");
        try {
            this.getAutoInventoryManager().startScan(_subject,
                                                     aPlatform.getEntityId(),
                                                     new ScanConfigurationCore(),
                                                     null, null, null);
            buf.append(" Done\n");
        } catch (Exception e) {
            buf.append(" Error: " + e + "\n");
        }
    }

    private ServerValue createServer(XmlServerValue server, 
                                     PlatformValue plat, TextIndenter buf)
        throws BatchImportException
    {
        ServerValue aServer;
        ServerTypeValue sType;
        String name, typeName, iPath, aiid;

        name     = server.getName();
        typeName = server.getType();
        iPath    = server.getInstallPath();
        aiid     = server.getAutoinventoryIdentifier();

        try {
            sType = findServerTypeByName(typeName);
        } catch(ServerNotFoundException exc){
            throw new BatchImportException("Server '" + name + "' " +
                                           "depends on server type '" + 
                                           typeName + "' which could not " +
                                           "be found");
        }

        aServer = new ServerValue();
        aServer.setName(name);
        aServer.setServerType(sType);
        aServer.setInstallPath(iPath);
        
        // Set the AI identifier if it is specified in the import file.  The import
        // script should only have this set if the import file was generated by the
        // 'resource export' command in the shell.

        aServer.setAutoinventoryIdentifier(aiid);

        aServer.setDescription(server.getDescription());
        aServer.setLocation(server.getLocation());

        try {
            _servMan.createServer(_subject, plat.getId(), sType.getId(),
                                  aServer);
        } catch(PlatformNotFoundException exc){
            throw new BatchImportException("Error creating server '" + name +
                                           "': It depends on platform " +
                                           plat.getName() + " which could " +
                                           "not be found");
        } catch(AppdefDuplicateNameException exc){
            throw new BatchImportException("Error creating server '" + name +
                                           "': Server with that name already "+
                                           "exists");
        } catch(CreateException exc){
            throw new BatchImportException("Error creating server '" + name +
                                           "': " + exc.getMessage());
        } catch(ValidationException exc){
            throw new BatchImportException("Error creating server '" + name +
                                           "': " + exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied, creating " +
                                           "server '" + name + "': " + 
                                           exc.getMessage());
        }
        
        this.setCProps(aServer.getEntityId(), server.getCustomProps(), 
                       name, typeName);
        return aServer;
    }

    private void processServer(XmlServerValue server, PlatformValue plat,
                               TextIndenter buf)
        throws BatchImportException, NamingException
    {
        ServerValue[] aServers;
        ServerValue aServer;
        String name;

        name = server.getName();

        try {
            aServers = findServersByName(name);
            if (aServers.length > 1) {
                throw new BatchImportException("Multiple matches for " + name);
            }
            aServer = aServers[0];
            buf.append("Processing server: " + name + "\n");
        } catch(ServerNotFoundException exc){
            buf.append("Creating new server: " + name + "\n");
            buf.pushIndent();
            aServer = this.createServer(server, plat, buf);
            buf.popIndent();
        }

        buf.pushIndent();
        this.handleAllConfigs(aServer.getEntityId(), name, server, buf);
        buf.popIndent();
        
        for(Iterator i=server.getServices().iterator(); i.hasNext(); ){
            XmlServiceValue service = (XmlServiceValue)i.next();
            
            buf.pushIndent();
            this.processService(service, aServer, buf);
            buf.popIndent();
        }
    }

    private ServiceValue createService(XmlServiceValue service, 
                                       ServerValue server,
                                       TextIndenter buf)
        throws BatchImportException
    {
        ServiceValue aService;
        ServiceTypeValue sType;
        String name, typeName, parentServiceName;

        name     = service.getName();
        typeName = service.getType();

        try {
            sType = findServiceTypeByName(typeName);
        } catch(ServiceNotFoundException exc){
            throw new BatchImportException("Service '" + name + "' " +
                                           "depends on service type '" + 
                                           typeName + "' which could not " +
                                           "be found");
        }

        aService = new ServiceValue();
        aService.setName(name);
        aService.setServiceType(sType);
        aService.setDescription(service.getDescription());
        aService.setLocation(service.getLocation());

        if((parentServiceName = service.getParentService()) != null){
            ServiceValue[] parentServices;

            try {
                parentServices = findServicesByName(parentServiceName);
            } catch(ServiceNotFoundException exc){
                throw new BatchImportException("Error creating service" +
                                      " '" + name + "':  It depends on the " +
                                      "parent service '" + parentServiceName +
                                      "' which could not be found");
            } catch(PermissionException exc){
                throw new BatchImportException("Error creating service '"+
                                      name + "': It depends on the parent " +
                                      "service '" + parentServiceName + 
                                      "' which could not be looked up due " +
                                      "to invalid permission");
            }

            //XXX: Should change parentService attribute to use id
            if (parentServices.length != 0) {
                throw new BatchImportException("Error creating service" + 
                                               "'" + name + "': The parent" +
                                               "service it depends on " +
                                               "returned multiple matches");
            }
            aService.setParentId(parentServices[0].getId());
        }

        try {
            Integer pk = _serviceMan.createService(_subject,
                                                   server.getId(),
                                                   sType.getId(),
                                                   aService);
            try {
                aService = _serviceMan.getServiceById(_subject, pk);
            } catch (ServiceNotFoundException e) {  
                throw new BatchImportException("Could not find service we " +
                                               "just created");
            }
        } catch(ServerNotFoundException exc){
            throw new BatchImportException("Error creating service '" + name +
                                           "': It depends on server '" + 
                                           server.getName() + " which could "+
                                           "not be found");
        } catch(AppdefDuplicateNameException exc){
            throw new BatchImportException("Error creating service '" + name +
                                           "': A service with that name " +
                                           "already exists");
        } catch(CreateException exc){
            throw new BatchImportException("Error creating service '" + name +
                                           "': " + exc.getMessage());
        } catch(ValidationException exc){
            throw new BatchImportException("Error creating service '" + name +
                                           "': " + exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied, creating " +
                                           "service '" + name + "': " + 
                                           exc.getMessage());
        }

        this.setCProps(aService.getEntityId(), service.getCustomProps(),
                       name, typeName);
        return aService;
    }

    private void processService(XmlServiceValue service, ServerValue server,
                                TextIndenter buf)
        throws BatchImportException, NamingException
    {
        ServiceValue[] aServices;
        ServiceValue aService;
        String name = service.getName();

        try {
            aServices = _serviceMan.findServicesByName(_subject, name);
            if (aServices.length > 1) {
                throw new BatchImportException("Multiple matches for " + name);
            }
            aService = aServices[0];
            buf.append("Processing service: " + name + "\n");
        } catch(AppdefEntityNotFoundException exc){
            buf.append("Creating new service: " + name + "\n");
            buf.pushIndent();
            aService = this.createService(service, server, buf);
            buf.popIndent();
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied looking up "+
                                           "service '" + name + "': " + 
                                           exc.getMessage());
        }
        buf.pushIndent();
        this.handleAllConfigs(aService.getEntityId(), name, service, buf);
        buf.popIndent();
    }

    private AppServiceValue findAppServiceForAdd(ApplicationValue aApp, 
                                                 String svc)
        throws BatchImportException
    {
        AppServiceValue[] svcs;

        svcs = aApp.getAppServiceValues();

        for(int i=0; i<svcs.length; i++){
            if(svcs[i].getService().getName().equalsIgnoreCase(svc)){
                return svcs[i];
            }
        }

        throw new BatchImportException("Error adding services to " +
                                       "Application '" + aApp.getName() +
                                       "' - failed to find service '" + 
                                       svc + "' within the service list");
    }

    private void setupAppServices(XmlApplicationValue app,
                                  ApplicationValue aApp)
        throws BatchImportException
    {
        DependencyTree dt;

        dt = new DependencyTree(aApp);

        for(Iterator i=app.getServices().iterator(); i.hasNext(); ){
            XmlApplicationServiceValue svc;
            AppServiceValue addSvc;

            svc = (XmlApplicationServiceValue)i.next();
            addSvc = findAppServiceForAdd(aApp, svc.getName());

            for(Iterator j=svc.getDependencies().iterator(); j.hasNext(); ){
                String depName = (String)j.next();
                AppServiceValue childSvc;

                childSvc = findAppServiceForAdd(aApp, depName);
                
                dt.addNode(addSvc, childSvc);
            }
        }

        try {
            _appMan.setServiceDepsForApp(_subject, dt);
        } catch(ApplicationNotFoundException exc){
            throw new SystemException("Unable to find application that " +
                                         "was just created, '" + 
                                         app.getName() + "'");
        } catch(RemoveException exc){
            throw new SystemException("Unable to set new service deps " +
                                         "for application '" + 
                                         app.getName() + "': " +
                                         exc.getMessage());
        } catch(CreateException exc){
            throw new BatchImportException("Error creating application '" +
                                           app.getName() + "': " +
                                           "failed to setup services - " +
                                           exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException("Error creating application '" +
                                           app.getName() + "': " +
                                           "failed to setup services - " +
                                           "Permission denied");
        }
    }

    private ApplicationValue createApp(XmlApplicationValue app, 
                                       TextIndenter buf)
        throws BatchImportException
    {
        ApplicationValue aApp;
        ArrayList serviceList;
        String name;

        name = app.getName();

        aApp = new ApplicationValue();
        aApp.setName(name);
        aApp.setDescription(app.getDescription());
        aApp.setLocation(app.getLocation());
        aApp.setBusinessContact(app.getBusinessContact());
        aApp.setEngContact(app.getEngContact());
        aApp.setOpsContact(app.getOpsContact());

        serviceList = new ArrayList();
        for(Iterator i=app.getServices().iterator(); i.hasNext(); ){
            XmlApplicationServiceValue appSvc;
            ServiceValue[] services;
            String serviceName;

            appSvc      = (XmlApplicationServiceValue)i.next();
            serviceName = appSvc.getName().toLowerCase();
            try {
                services = findServicesByName(serviceName);
            } catch(ServiceNotFoundException exc){
                throw new BatchImportException("Failed to create application "+
                                               "'" + name + "'.  It relies " +
                                               "on service '" + serviceName +
                                               "', which could not be " +
                                               "located");
            } catch(PermissionException exc){
                throw new BatchImportException("Failed to create application "+
                                               "'" + name + "'.  It relies " +
                                               "on service '" + serviceName +
                                               "', which could not be " +
                                               "located because of " +
                                               "permission problems: " +
                                               exc.getMessage());
            }
          
            for (int j=0; j<services.length; j++) {
                buf.append("- Adding service: " + services[j].getName() + "\n");
                serviceList.add(services[j]);
            }
        }

        try {
            Application pk =
                _appMan.createApplication(_subject, aApp, serviceList);
            aApp = pk.getApplicationValue();
        } catch(CreateException exc){
            throw new BatchImportException("Failed to create application '" +
                                           name + "': " + exc.getMessage());
        } catch(AppdefDuplicateNameException exc){
            throw new BatchImportException("Failed to create application "+
                                           "'" + name + "'.  An " +
                                           "application with that name " +
                                           "already exists");
        } catch(ValidationException exc){
            throw new BatchImportException("Failed to create application '" +
                                           name + "': " + exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException("Failed to create application '" +
                                           name + "': " + exc.getMessage());
        }

        this.setupAppServices(app, aApp);
        return aApp;
    }

    private void processApp(XmlApplicationValue app, TextIndenter buf)
        throws BatchImportException
    {
        ApplicationValue aApp = null;
        String name;
        boolean create;

        name = app.getName();
        create = false;
        try {
            aApp = findApplicationByName(name);
        } catch(ApplicationNotFoundException exc){
            create = true;
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied looking up " +
                                           "application '" + name + "': " +
                                           exc.getMessage());
        }

        if(create){
            buf.append("Creating new application: " + name + "\n");
            buf.pushIndent();
            aApp = this.createApp(app, buf);
            buf.popIndent();
        } else {
            buf.append("Processing application: " + name + "\n");
        }
    }

    private String getConfigFailMsg(ConfigFetchException exc){
        return exc.getEntity().toString() + " must have a " +
            exc.getProductType() + " configuration ";
    }

    private void handleAllConfigs(AppdefEntityID id, String name,
                                  XmlResourceValue entityVal,
                                  TextIndenter buf)
        throws BatchImportException, NamingException
    {
        this.handleConfig(id, name, ProductPlugin.TYPE_PRODUCT,
                          entityVal.getConfig(ProductPlugin.TYPE_PRODUCT),
                          buf);
        this.handleConfig(id, name, ProductPlugin.TYPE_CONTROL,
                          entityVal.getConfig(ProductPlugin.TYPE_CONTROL),
                          buf);
        this.handleConfig(id, name, ProductPlugin.TYPE_MEASUREMENT,
                          entityVal.getConfig(ProductPlugin.TYPE_MEASUREMENT),
                          buf);
    }

    private void handleConfig(AppdefEntityID id, String name, 
                              String configType,
                              XmlConfigInfo ghettoConfig,
                              TextIndenter buf)
        throws BatchImportException, NamingException
    {
        ConfigResponse response;
        ConfigSchema schema;
        String fullName;
        Map config;

        if(ghettoConfig == null)
            return;

        buf.append("- Using '" + ghettoConfig.getType() + "' configuration\n");
        config = ghettoConfig.getValues();
        fullName = id.getTypeName() + " '" + name + "'";
        try {
            schema = _prodBoss.getConfigSchema(_subject, id,
                                               ghettoConfig.getType(),
                                               true);
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied, " +
                                           "configuring " + fullName + 
                                           ": " + exc.getMessage());
        } catch(PluginNotFoundException exc){
            throw new BatchImportException("Could not find plugin to " +
                                           "handle " + fullName + 
                                           ": " + exc.getMessage());
        } catch(PluginException exc){
            throw new BatchImportException("Plugin for " + fullName + 
                                           " returned an error: " + 
                                           exc.getMessage());
        } catch(ConfigFetchException exc){
            throw new BatchImportException("Failed to configure " + 
                                           fullName + ": " + 
                                           this.getConfigFailMsg(exc));
        } catch(EncodingException exc){
            throw new BatchImportException("Failed to configure " + 
                                           fullName + ": " + exc.getMessage());
        } catch(FinderException exc){
            throw new BatchImportException("Failed to configure " + 
                                           fullName + ": Resource does not " +
                                           "support " + ghettoConfig.getType()+
                                           " configuration");
        } catch(AppdefEntityNotFoundException exc){
            throw new BatchImportException("Failed to configure " + 
                                           fullName + ": Resource does not " +
                                           "support " + ghettoConfig.getType()+
                                           " configuration");
        }

        // XXX: Disable validation for now
        //response = new Configresponse(schema);
        response = new ConfigResponse();

        try {
            for(Iterator i=config.entrySet().iterator(); i.hasNext(); ){
                Map.Entry ent = (Map.Entry)i.next();

                response.setValue((String)ent.getKey(), 
                                  (String)ent.getValue());
            }
        } catch(InvalidOptionException exc){
            String validOptions;

            validOptions = "";
            for(Iterator i=schema.getOptions().iterator(); i.hasNext(); ){
                ConfigOption opt = (ConfigOption)i.next();
                
                validOptions += opt.getName();
                if(i.hasNext()){
                    validOptions += ", ";
                }
            }

            if(validOptions.length() != 0){
                validOptions = ".  Valid options are: " + validOptions;
            }

            throw new BatchImportException(fullName + " " + configType + 
                                           " configuration contains an " + 
                                           "invalid key, '" + exc.getMessage()+
                                           "'" + validOptions);
        } catch(InvalidOptionValueException exc){
            throw new BatchImportException(fullName + " " + configType +
                                           " configuration is invalid: " +
                                           exc.getMessage());
        }

        try {
            _prodBoss.setConfigResponse(_subject, id, response,
                                        ghettoConfig.getType());
        } catch(ConfigFetchException exc){
            throw new BatchImportException("Failed to set configuration for " +
                                           fullName + ": " + 
                                           this.getConfigFailMsg(exc));
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied when setting " +
                                           "the configuration for " +
                                           fullName + ": " + exc.getMessage());
        } catch(ApplicationException exc){
            throw new BatchImportException("Error setting config for " +
                                           fullName + ": " +
                                           exc.getMessage());
        } catch(EncodingException exc){
            throw new BatchImportException("Internal server error");
        } catch(FinderException exc){
            throw new BatchImportException("Internal server error");
        }
    }

    private AppdefGroupValue createGroup(XmlGroupValue group, 
                                         TextIndenter buf)
        throws BatchImportException
    {
        final String ERR_BEGIN = "Error creating group '" + 
            group.getCapName() + "': ";
        AppdefGroupValue aGroup;
        String groupType, memberType, name, description, location;

        groupType   = group.getType();
        memberType  = group.getMemberType();
        name        = group.getCapName();
        description = group.getDescription();
        location    = group.getLocation();

        // First create the group -- add members later
        try {
            if(groupType.equals(XmlGroupValue.T_COMPAT)){
                AppdefResourceTypeValue resVal;
                String mTypeName = group.getMemberTypeName();

                if(memberType.equals(XmlGroupValue.N_PLATFORM)){
                    resVal = findPlatformTypeByName(mTypeName);
                } else if(memberType.equals(XmlGroupValue.N_SERVER)){
                    resVal = findServerTypeByName(mTypeName);
                } else if(memberType.equals(XmlGroupValue.N_SERVICE)){
                    resVal = findServiceTypeByName(mTypeName);
                } else {
                    throw new IllegalStateException("Unhandled member type, '"+
                                                    memberType + "'");
                }
                
                aGroup = _groupMan.createGroup(_subject,
                                               resVal.getAppdefType(),
                                               resVal.getId().intValue(),
                                               name, description,
                                               location);
            } else if(groupType.equals(XmlGroupValue.T_ADHOC)){
                if(memberType.equals(XmlGroupValue.N_MIXED)){
                    aGroup = _groupMan.createGroup(_subject, name,
                                                   description, location);
                } else if(memberType.equals(XmlGroupValue.N_GROUP)){
                    aGroup = _groupMan.createGroup(_subject,
                                      AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                      name, description, location);
                } else if(memberType.equals(XmlGroupValue.N_APP)){
                    aGroup = _groupMan.createGroup(_subject,
                               AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                               name, description, location);
                } else {
                    throw new IllegalStateException("Unhandled member type, '"+
                                                    memberType + "'");
                }
            } else {
                throw new IllegalStateException("Unhandled group type, '" +
                                                group.getType());
            }
        } catch(GroupCreationException exc){
            throw new BatchImportException(ERR_BEGIN + exc.getMessage());
        } catch(GroupDuplicateNameException exc){
            throw new BatchImportException(ERR_BEGIN + "A group with that " +
                                           "name already exists");
        } catch(AppdefEntityNotFoundException exc){
            throw new BatchImportException(ERR_BEGIN + "The compatable group "+
                                           "relies on " + memberType +
                                           "s of type '" + 
                                           group.getMemberTypeName() + 
                                           "', which could not be found");
        }
            
        // Add members to the group
        for(Iterator i=group.getMembers().iterator(); i.hasNext(); ){
            XmlGroupMemberValue member = (XmlGroupMemberValue)i.next();
            AppdefResourceValue resource, resources[];
            String type, entName;

            entName = member.getName();
            if((type = member.getType()) == null){
                type = memberType;
            }

            try {
                if(type.equals(XmlGroupValue.N_PLATFORM)) {
                    resource = findPlatformByName(entName);
                    aGroup.addAppdefEntity(resource.getEntityId());
                } else if(type.equals(XmlGroupValue.N_SERVER)) {
                    resources = findServersByName(entName);
                    for (int j=0; j<resources.length; j++) {
                        aGroup.addAppdefEntity(resources[j].getEntityId());
                    }
                } else if(type.equals(XmlGroupValue.N_SERVICE)) {
                    resources = findServicesByName(entName);
                    for (int j=0; j<resources.length; j++) {
                        aGroup.addAppdefEntity(resources[j].getEntityId());
                    }
                } else if(type.equals(XmlGroupValue.N_APP)) {
                    resource = findApplicationByName(entName);
                    aGroup.addAppdefEntity(resource.getEntityId());
                } else if(type.equals(XmlGroupValue.N_GROUP)) {
                    resource = findGroupByName(entName);
                    aGroup.addAppdefEntity(resource.getEntityId());
                } else {
                    throw new IllegalStateException("Unhandled member type, '"+
                                                    type + "'");
                }

            } catch(PermissionException exc){
                throw new BatchImportException(ERR_BEGIN + 
                                               "Permission denied while " +
                                               "trying to retrieve " +
                                               "member, '" + entName + "'");
            } catch(AppdefEntityNotFoundException exc){
                throw new BatchImportException(ERR_BEGIN + "It relies on " +
                                               type + " '" + entName +
                                               "' which could not be found");
            } catch(GroupVisitorException exc){
                throw new BatchImportException(ERR_BEGIN + "Unable to add " +
                                               type + " '" + entName + 
                                               "' to the group: " + 
                                               exc.getMessage());
            }
        }

        try {
            _groupMan.saveGroup(_subject, aGroup);
        } catch(VetoException exc) {
            throw new BatchImportException(ERR_BEGIN + exc.getMessage());
        } catch(GroupNotCompatibleException exc){
            throw new BatchImportException(ERR_BEGIN + exc.getMessage());
        } catch(GroupModificationException exc){
            throw new BatchImportException(ERR_BEGIN + exc.getMessage());
        } catch(GroupDuplicateNameException exc){
            throw new BatchImportException(ERR_BEGIN + 
                                           "A group with this name already " +
                                           "exists");
        } catch(GroupVisitorException exc){
            throw new BatchImportException(ERR_BEGIN + exc.getMessage());
        } catch(PermissionException exc){
            throw new BatchImportException(ERR_BEGIN + "Permission denied");
        } catch(AppSvcClustDuplicateAssignException exc){
            throw new BatchImportException(ERR_BEGIN + "One or more services "+
                                           "has already been assigned to a "+
                                           "service cluster");
        }
        return aGroup;
    }

    private void processGroup(XmlGroupValue group, TextIndenter buf)
        throws BatchImportException
    {
        AppdefGroupValue aGroup = null;
        boolean create;
        String name;

        name   = group.getCapName();
        create = false;
        try {
            aGroup = findGroupByName(name);
        } catch(AppdefGroupNotFoundException exc){
            create = true;
        } catch(PermissionException exc){
            throw new BatchImportException("Permission denied lookup " +
                                           "group '" + name + "': " + 
                                           exc.getMessage());
        }

        if(create){
            buf.append("Creating new group: " + name + "\n");
            buf.pushIndent();
            aGroup = this.createGroup(group, buf);
            buf.popIndent();
        } else {
            buf.append("Processing group: " + name + "\n");
        }
    }

    private ServiceValue[] findServicesByName(String name)
        throws ServiceNotFoundException, PermissionException
    {
        return _serviceMan.findServicesByName(_subject, name);
    }

    private ServerValue[] findServersByName(String name)
        throws ServerNotFoundException
    {
        return _servMan.findServersByName(_subject, name);
    }

    private PlatformValue findPlatformByName(String name)
        throws PlatformNotFoundException, PermissionException
    {
        return _platMan.getPlatformByName(_subject, name);
    }

    private ApplicationValue findApplicationByName(String name)
        throws ApplicationNotFoundException, PermissionException
    {
        return _appMan.findApplicationByName(_subject, name);
    }

    private AppdefGroupValue findGroupByName(String name)
        throws AppdefGroupNotFoundException, PermissionException
    {
        return _groupMan.findGroupByName(_subject, name);
    }

    private PlatformTypeValue findPlatformTypeByName(String typeName)
        throws PlatformNotFoundException
    {
        return _platMan.findPlatformTypeByName(typeName);
    }

    private ServerTypeValue findServerTypeByName(String typeName)
        throws ServerNotFoundException
    {
        try {
            return _servMan.findServerTypeByName(typeName);
        } catch (FinderException e) {
            throw new ServerNotFoundException(e.getMessage());
        }
    }

    private ServiceTypeValue findServiceTypeByName(String typeName)
        throws ServiceNotFoundException
    {
        try {
            return _serviceMan.findServiceTypeByName(typeName);
        } catch (FinderException exc) {
            throw new ServiceNotFoundException(exc.getMessage());
        }
    }

    String process()
        throws PermissionException, BatchImportException
    {
        TextIndenter res = new TextIndenter(2);

        try {
            List apps;

            for(Iterator i=_data.getPlatforms().iterator(); i.hasNext(); ){
                this.processPlatform((XmlPlatformValue)i.next(), res);
            }

            apps = _data.getApplications();
            if(apps.size() > 0)
                res.append("\nProcessing Applications:\n");
                
            for(Iterator i=apps.iterator();i.hasNext();){
                this.processApp((XmlApplicationValue)i.next(), res);
            }

            for(Iterator i=_data.getGroups().iterator(); i.hasNext(); ){
                this.processGroup((XmlGroupValue)i.next(), res);
            }
        } catch(NamingException exc){
            throw new SystemException(exc);
        }
        
        return res.toString();
    }
}

/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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
package org.hyperic.hq.test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.hyperic.hq.measurement.shared.TemplateManager;

public class TestHelper {

	@Autowired
    protected PlatformManager platformManager;

    @Autowired
    protected AuthzSubjectManager authzSubjectManager;

    @Autowired
    protected ServerManager serverManager;

    @Autowired
    protected ServiceManager serviceManager;

    @Autowired
    protected AgentManager agentManager;

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected ResourceManager resourceManager;

    @Autowired
    protected ResourceGroupManager resourceGroupManager;

    @Autowired
    protected ApplicationManager applicationManager;
    
    @Autowired
    protected PlatformTransactionManager txManager ; 
    
    @Autowired
    private ConfigManager configManager ;

    @Autowired
    protected MeasurementManager msmtManager;
    
    @Autowired
    protected DataManager dataManager;

    @Autowired
    protected TemplateManager tmpMgr;
    
    
    protected TransactionTemplate newTxTemplate(final int propagationType) { 
    	final TransactionTemplate txTemplate = new TransactionTemplate(this.txManager) ;
    	txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    	return txTemplate ; 
    }//EOM 
    
    protected Platform createPlatform(String agentToken, String platformType,
			String fqdn, String name) throws ApplicationException {
    	return this.createPlatform(agentToken, platformType, fqdn, name, authzSubjectManager.getOverlordPojo()) ; 
    }//EOM 
    
	protected Platform createPlatform(String agentToken, String platformType,
			String fqdn, String name, final AuthzSubject subject) throws ApplicationException {
		AIPlatformValue aiPlatform = new AIPlatformValue();
		aiPlatform.setCpuCount(2);
		aiPlatform.setPlatformTypeName(platformType);
		aiPlatform.setAgentToken(agentToken);
		aiPlatform.setFqdn(fqdn);
		aiPlatform.setName(name);
		return platformManager.createPlatform(subject, aiPlatform);
	}

	protected Server createServer(Platform platform, ServerType serverType,
			String name) throws PlatformNotFoundException,
			AppdefDuplicateNameException, ValidationException,
			PermissionException, NotFoundException {
		return this.createServer(platform, serverType, name, this.authzSubjectManager.getOverlordPojo()) ; 
	}//EOM 
	
	protected Server createServer(Platform platform, ServerType serverType,
			String name, final AuthzSubject subject) throws PlatformNotFoundException,
			AppdefDuplicateNameException, ValidationException,
			PermissionException, NotFoundException {
		ServerValue server = new ServerValue();
		server.setName(name);
		return serverManager.createServer(
				subject, platform.getId(),
				serverType.getId(), server);
	}

	protected ServerType createServerType(String serverTypeName,
			String serverVersion, String[] validPlatformTypes, String plugin,
			boolean virtual) throws NotFoundException {
		ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
		serverTypeInfo.setDescription(serverTypeName);
		serverTypeInfo.setName(serverTypeName);
		serverTypeInfo.setVersion(serverVersion);
		serverTypeInfo.setVirtual(virtual);
		serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
		return serverManager.createServerType(serverTypeInfo, plugin);
	}

	protected ServiceType createServiceType(String serviceTypeName,
			String plugin, ServerType serverType) throws NotFoundException {
		ServiceTypeInfo sinfo = new ServiceTypeInfo();
		sinfo.setDescription(serviceTypeName);
		sinfo.setInternal(false);
		sinfo.setName(serviceTypeName);
		return serviceManager.createServiceType(sinfo, plugin, serverType);
	}

	protected Service createService(Server server, ServiceType serviceType,
			String serviceName, String description, String location)
			throws ServerNotFoundException, AppdefDuplicateNameException,
			ValidationException, PermissionException {
		return this.createService(server, serviceType, serviceName, description, location, this.authzSubjectManager.getOverlordPojo()) ; 
	}//EOM
	
	protected Service createService(Server server, ServiceType serviceType,
			String serviceName, String description, String location, final AuthzSubject subject)
			throws ServerNotFoundException, AppdefDuplicateNameException,
			ValidationException, PermissionException {
		return serviceManager.createService(
				subject, server.getId(),
				serviceType.getId(), serviceName, "Spring JDBC Template",
				"my computer");
	}

	protected Agent createAgent(String address, Integer port, String authToken,
			String agentToken, String version) throws AgentCreateException {
		return agentManager.createLegacyAgent(address, port, authToken,
				agentToken, version);
	}

	protected PlatformType createPlatformType(String typeName, String plugin)
			throws NotFoundException {
		return platformManager.createPlatformType(typeName, plugin);
	}

	protected ResourceGroup createPlatformResourceGroup(
			Set<Platform> platforms, String groupName)
			throws ApplicationException, PermissionException {
		return createPlatformResourceGroup(platforms, groupName,
				new ArrayList<Role>(0));
	}

	protected ResourceGroup createPlatformResourceGroup(
			Set<Platform> platforms, String groupName, List<Role> roles)
			throws ApplicationException, PermissionException {
		List<Resource> resources = new ArrayList<Resource>();
		for (Platform platform : platforms) {
			Resource platformRes = platform.getResource();
			resources.add(platformRes);
		}
		AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
				AppdefEntityConstants.APPDEF_TYPE_PLATFORM, platforms
						.iterator().next().getPlatformType().getId());

		return createResourceGroup(groupName, appDefEntTypeId, roles, resources);
	}

	protected ResourceGroup createServerResourceGroup(Set<Server> servers,
			String groupName) throws ApplicationException, PermissionException {
		return createServerResourceGroup(servers, groupName,
				new ArrayList<Role>(0));
	}

	protected ResourceGroup createServerResourceGroup(Set<Server> servers,
			String groupName, List<Role> roles) throws ApplicationException,
			PermissionException {
		List<Resource> resources = new ArrayList<Resource>();
		for (Server server : servers) {
			Resource serverRes = server.getResource();
			resources.add(serverRes);
		}
		AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
				AppdefEntityConstants.APPDEF_TYPE_SERVER, servers.iterator()
						.next().getServerType().getId());

		return createResourceGroup(groupName, appDefEntTypeId, roles, resources);
	}

	protected ResourceGroup createServiceResourceGroup(Set<Service> services,
			String groupName) throws ApplicationException, PermissionException {
		return createServiceResourceGroup(services, groupName,
				new ArrayList<Role>(0));
	}

	protected ResourceGroup createServiceResourceGroup(Set<Service> services,
			String groupName, List<Role> roles) throws ApplicationException,
			PermissionException {
		List<Resource> resources = new ArrayList<Resource>();
		for (Service service : services) {
			Resource serviceRes = service.getResource();
			resources.add(serviceRes);
		}
		AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
				AppdefEntityConstants.APPDEF_TYPE_SERVICE, services.iterator()
						.next().getServiceType().getId());

		return createResourceGroup(groupName, appDefEntTypeId, roles, resources);
	}

	private ResourceGroup createResourceGroup(String groupName,
			AppdefEntityTypeID appDefEntTypeId, List<Role> roles,
			List<Resource> resources) throws GroupDuplicateNameException,
			GroupCreationException {
		ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo(groupName,
				"", AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS,
				resourceManager.findResourcePrototype(appDefEntTypeId), "", 0,
				false, false);
		ResourceGroup resGrp = resourceGroupManager
				.createResourceGroup(authzSubjectManager.getOverlordPojo(),
						gCInfo, roles, resources);
		return resGrp;
	}

	protected Application createApplication(String name, String desc,
			int applicationType, List<AppdefEntityID> services)
			throws AppdefDuplicateNameException, ValidationException,
			PermissionException, NotFoundException,
			ApplicationNotFoundException, AppdefGroupNotFoundException {
		ApplicationValue app = new ApplicationValue();
		app.setName(name);
		app.setDescription(desc);
		app.setEngContact("admin");
		app.setBusinessContact("admin");
		app.setOpsContact("admin");
		app.setLocation("dataCenter");
		app.setApplicationType(applicationManager
				.findApplicationType(applicationType));
		Application application = applicationManager.createApplication(
				authzSubjectManager.getOverlordPojo(), app);
		applicationManager.setApplicationServices(
				authzSubjectManager.getOverlordPojo(), application.getId(),
				services);
		return application;
	}

	protected List<Measurement> createMeasurements(AppdefResource rsc, List<MeasurementTemplate> tmps, long interval, ConfigResponse cr)  
            throws AppdefEntityNotFoundException, ConfigFetchException, PermissionException, EncodingException,
            MeasurementCreateException, TemplateNotFoundException {
	    
	    long[] intervals = new long[tmps.size()];
	    for (int i = 0; i < intervals.length; i++) {
            intervals[i]=interval;
        }
	    return createMeasurements(rsc, tmps, intervals, cr);
	}

	protected List<Measurement> createMeasurements(AppdefResource rsc, List<MeasurementTemplate> tmps, long[] intervals, ConfigResponse cr) 
	        throws AppdefEntityNotFoundException, ConfigFetchException, PermissionException, EncodingException,
	        MeasurementCreateException, TemplateNotFoundException {
	    
	    AuthzSubject subject = authzSubjectManager.getOverlordPojo();
        Integer[] tids = new Integer[tmps.size()];
	    int i = 0;
        for (MeasurementTemplate tmp : tmps) {
	        tids[i++] = tmp.getId();
        }
	    return this.msmtManager.createMeasurements(subject, rsc.getEntityId(), tids, intervals, cr);
	}
	
    public void addData(List<DataPoint> data, String aggTable) throws Exception {
        this.dataManager.addData(data, aggTable, this.sessionFactory.getCurrentSession().connection());
    }
    
    public void addData(List<DataPoint> data) {
        this.dataManager.addData(data, this.sessionFactory.getCurrentSession().connection());
    }
}// EOC

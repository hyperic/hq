package org.hyperic.hq.api.services.impl;

import static org.hyperic.hq.api.model.config.ServerConfigType.LDAP;
import static org.hyperic.hq.api.model.config.ServerConfigType.SERVER_GUID;
import static org.hyperic.hq.api.model.config.ServerConfigType.VCENTER;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hyperic.hq.api.model.config.ServerConfig;
import org.hyperic.hq.api.model.config.ServerConfigStatus;
import org.hyperic.hq.api.services.ServerConfigService;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.vm.VCConnection;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerConfigImpl extends RestApiService  implements ServerConfigService {

    private final VCManager vmMgr;
    private final ServerConfigManager serverConfigManager;

    @Autowired
    public ServerConfigImpl(VCManager vmMgr, ServerConfigManager serverConfigManager) {
        this.vmMgr=vmMgr;
        this.serverConfigManager = serverConfigManager;
    }

    @GET
    @Path("/")
    public List<ServerConfig> getServerConfigs(@QueryParam("type") String type) {
        List<ServerConfig> configurations = new ArrayList<ServerConfig>();
        if (StringUtil.isNullOrEmpty(type) || type.equalsIgnoreCase(VCENTER.getType())) {
            addVcConfiguration(configurations);
        }
        if (StringUtil.isNullOrEmpty(type) || type.equalsIgnoreCase(LDAP.getType())) {
        }
        if (StringUtil.isNullOrEmpty(type) || type.equalsIgnoreCase(SERVER_GUID.getType())) {
            addServerGuidConfiguration(configurations);
        }
        return configurations;
    }

    @POST
    @Path("/")
    public void addServerConfig(final ServerConfig config) {
        switch (config.getType()) {
        case VCENTER: 
            if (vmMgr.connectionExists(config.getProperty(HQConstants.vCenterURL), 
                    config.getProperty(HQConstants.vCenterUser), config.getProperty(HQConstants.vCenterPassword))) {
                throw errorHandler.newWebApplicationException(
                        Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.VC_CONNECTION_ALREADY_EXISTS,
                        config.getProperty(HQConstants.vCenterURL)); 
            }
            addVcConfig(config);
            break;
        case SERVER_GUID:
            throw errorHandler.newWebApplicationException(
                    Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.CANNOT_UPDATE_SERVER_GUID); 
        default:
            break;
        }
    }


    @POST
    @Path("/")
    public void addServerConfig(final List<ServerConfig> configs) {
        for (ServerConfig config : configs) {
            addServerConfig(config);
        }
    }

    private void addServerGuidConfiguration(List<ServerConfig> configurations) {
        ServerConfig conf = new ServerConfig(SERVER_GUID);
        conf.setStatus(ServerConfigStatus.CONFIGURED);
        conf.addProperty(HQConstants.HQ_GUID, serverConfigManager.getGUID());
        configurations.add(conf);
    }


    private void addVcConfiguration(List<ServerConfig> configurations) {
        for (VCConnection cred : vmMgr.getActiveVCConnections()) {
            ServerConfig conf = new ServerConfig(VCENTER);
            conf.setStatus(cred.lastSyncSucceeded() ? ServerConfigStatus.ACTIVE : ServerConfigStatus.CONNECTION_PROBLEM);
            conf.addProperty(HQConstants.vCenterURL, cred.getUrl());
            conf.addProperty(HQConstants.vCenterUser, cred.getUser());
            configurations.add(conf);
        }
    }

    private void addVcConfig(final ServerConfig config) {
        try {
            if (!vmMgr.validateVCSettings(config.getProperty(HQConstants.vCenterURL), 
                    config.getProperty(HQConstants.vCenterUser), config.getProperty(HQConstants.vCenterPassword))) {
                throw new Exception();
            }
            vmMgr.registerOrUpdateVC(config.getProperty(HQConstants.vCenterURL), 
                    config.getProperty(HQConstants.vCenterUser), config.getProperty(HQConstants.vCenterPassword));          
        }catch(Throwable e) {
            throw errorHandler.newWebApplicationException(
                    e, Response.Status.NOT_ACCEPTABLE, ExceptionToErrorCodeMapper.ErrorCode.CANNOT_VERIFY_VC_SETTINGS, e.getMessage());
        }
    }

    @PUT
    @Path("/")
    public void updateServerConfig(ServerConfig config) {
        switch (config.getType()) {
        case VCENTER: 
            if (!vmMgr.connectionExists(config.getProperty(HQConstants.vCenterURL), 
                    config.getProperty(HQConstants.vCenterUser), config.getProperty(HQConstants.vCenterPassword))) {
                throw errorHandler.newWebApplicationException(
                        Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.NO_VC_CONNECTION_EXISTS, 
                        config.getProperty(HQConstants.vCenterURL)); 
            }
            addVcConfig(config);
            break;
        case SERVER_GUID:
            throw errorHandler.newWebApplicationException(
                    Response.Status.BAD_REQUEST, ExceptionToErrorCodeMapper.ErrorCode.CANNOT_UPDATE_SERVER_GUID); 
        default:
            break;
        }
    }

    @PUT
    @Path("/")
    public void updateServerConfig(List<ServerConfig> configs) {
        for (ServerConfig config : configs) {
            updateServerConfig(config);
        }
    }

    @GET
    @Path("/time")
    public long getServerTime() {
        return System.currentTimeMillis();
    }

}

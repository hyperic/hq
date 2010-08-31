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

import java.util.ArrayList;

/**
 * Simple helper class to help keep plugin entity names
 * consistent.
 * 
 * This class supports adding one server, and its child services.
 * This class also supports adding multiple versions of the
 * same server.
 *   
 * Typically, the plugin developer uses the default 
 * <code>PLATFORM_NAMES</code> for the list
 * of platforms supported by a given server.  The plugin
 * developer can specify her own list of supported platforms.  
 * 
 * @since 1.0
 * @version 1.0
 */
public class TypeBuilder {

    /**
     * Version will be excluded from composed server name.
     */
    public static final String NO_VERSION = "x";

    public static final String[] ALL_PLATFORM_NAMES =
        PlatformDetector.PLATFORM_NAMES;
    
    public static final String[] WIN32_PLATFORM_NAMES =
        PlatformDetector.WIN32_PLATFORM_NAMES;
    
    public static final String[] UNIX_PLATFORM_NAMES =
        PlatformDetector.UNIX_PLATFORM_NAMES;
    
    private String serverName;
    private String serverDesc;
    private ArrayList types = new ArrayList();
    private String[] platforms = ALL_PLATFORM_NAMES; //default

    public TypeBuilder() {
    }

    /**
     * Create a server type for use on all supported platforms
     *
     * @param serverName name of the server
     * @param serverDesc server description
     */
    public TypeBuilder(String serverName, String serverDesc) {
        this(serverName, serverDesc, PlatformDetector.PLATFORM_NAMES);
    }

    /**
     * use this constructor for specifying a list of supported
     * platforms.
     * 
     * @param serverName name of the server
     * @param serverDesc server description
     */
    public TypeBuilder(String serverName, String serverDesc,
                       String[] platforms) {
        this.serverName = serverName;
        this.serverDesc = serverDesc;
        this.platforms = platforms;
    }

    public PlatformTypeInfo addPlatform(String name) {
        PlatformTypeInfo platform =
            new PlatformTypeInfo(name);
        types.add(platform);
        return platform;
    }

    /**
     * This method creates a ServerTypeInfo and adds this object
     * to the entity list using a version string.
     * 
     * @param version version of the server
     * @return ServerTypeInfo 
     */
    public ServerTypeInfo addServer(String version) {
        return addServer(this.serverName, version);
    }

    /**
     * This method creates a ServerTypeInfo and adds this object
     * to the entity list using a version string.
     * 
     * @param name name of the server
     * @param version version of the server
     * @return ServerTypeInfo 
     */
    public ServerTypeInfo addServer(String name, String version) {
        return addServer(name, version, this.platforms);
    }

    public ServerTypeInfo addServer(String name, String version,
                                    String[] platforms) {
        ServerTypeInfo server = 
            new ServerTypeInfo(composeServerTypeName(name, version),
                               name + " " + this.serverDesc,
                               version);

        server.setValidPlatformTypes(platforms);

        add(server);
        return server;
    }
    /**
     * Add a ServerTypeInfo clone of the given server,
     * changing valid platforms to the given platforms.
     * @param server Server type to clone.
     * @param platforms Platforms supported by the server clone.
     * @return The cloned server with valid platforms updated.
     */
    public ServerTypeInfo addServer(ServerTypeInfo server,
                                    String[] platforms) {
        ServerTypeInfo pServer =
            (ServerTypeInfo)server.clone();

        pServer.setValidPlatformTypes(platforms);

        add(pServer);

        return pServer;
    }

    /**
     * This method adds a list of services to the this.entity
     * list. All the services added through this method will be marked
     * with the internal flag set to false
     * 
     * @param server the server the services belong to
     * @param services a list of services names to add
     */
    public void addServices(ServerTypeInfo server, String[] services) {
        for (int i=0; i<services.length; i++) {
            addService(server, services[i]);
        }
    }
    
    /**
     * This method adds a list of internal and deployed services to the 
     * this.entity. 
     * @param server
     * @param deployedServices
     * @param internalServices
     */
    public void addServices(ServerTypeInfo server,
                            String[] deployedServices,
                            String[] internalServices) {
        this.addServices(server, deployedServices);
        for (int i=0; i<internalServices.length; i++) {
            ServiceTypeInfo service = addService(server, internalServices[i]);
            service.setInternal(true);
        }
    }
    /**
     * Clone all services from an existing server.
     * @param fromServer Server from which to clone services.
     * @param toServer Server to which the clones services are tied.
     */
    public void addServices(ServerTypeInfo fromServer,
                            ServerTypeInfo toServer) {
        ArrayList services = new ArrayList();

        for (int i=0; i<this.types.size(); i++) {
            TypeInfo info = (TypeInfo)this.types.get(i);

            if (!(info.getType() == TypeInfo.TYPE_SERVICE)) {
                continue;
            }

            ServiceTypeInfo service = (ServiceTypeInfo)info;
            ServerTypeInfo server = service.getServerTypeInfo();

            if (server != fromServer) {
                continue;
            }

            service = (ServiceTypeInfo)service.clone();
            service.setServerTypeInfo(toServer);

            services.add(service);
        }

        this.types.addAll(services);
    }

    /**
     * This method creates a deployed (non-internal)
     * ServiceTypeInfo object and adds it services to the entity list
     * 
     * @param server server which the server belongs to
     * @param name name of the service
     * 
     * @return ServiceTypeInfo 
     */
    public ServiceTypeInfo addService(ServerTypeInfo server, String name) {
        ServiceTypeInfo service =
            new ServiceTypeInfo(composeServiceTypeName(server.getName(), name),
                                server.getDescription() +
                                " " + name,
                                server);

        add(service);
        return service;
    }
    
    /**
     * Given a serverName and a serviceName, return the name of 
     * the service type.  This procedure is abstracted out into 
     * this method because it needs to be reused in runtime 
     * discovery.
     */
    public static String composeServiceTypeName ( String serverName,
                                                  String serviceName ) {
        return serverName + " " + serviceName;
    }

    public static String composeServerTypeName ( String name,
                                                 String version ) {
        if (version.equals(NO_VERSION)) {
            return name;
        }
        return name + " " + version;
    }

    public static String composePlatformTypeName ( String name,
                                                   String platformName ) {
        return name + " " + platformName;
    }

    /**
     * Add a ServerTypeInfo clone of the given server,
     * changing valid platforms to the given platforms.
     * Clone all services from the given server.
     * @param server Server to clone
     * @param platforms Supported platforms of the server clone
     * @see #addServer(ServerTypeInfo server,String[] platforms)
     * @see #addServices(ServerTypeInfo fromServer,ServerTypeInfo toServer)
     */
    public ServerTypeInfo addServerAndServices(ServerTypeInfo server,
                                               String[] platforms) {
        ServerTypeInfo pServer = addServer(server, platforms);
        addServices(server, pServer);
        return pServer;
    }

    /**
     * This method returns a list of server/service added
     * to this object.  
     * 
     * @return a list of TypeInfo objects. 
     */
    public TypeInfo[] getTypes() {
        return (TypeInfo[])this.types.toArray(new TypeInfo[0]);
    }

    public boolean add(TypeInfo type) {
        return this.types.add(type);
    }
}

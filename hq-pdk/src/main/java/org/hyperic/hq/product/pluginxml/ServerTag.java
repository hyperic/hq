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

package org.hyperic.hq.product.pluginxml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagInfo;

class ServerTag
    extends ResourceTag implements XmlEndAttrHandler {

    static final String ATTR_PLATFORMS = "platforms";
    static final String ATTR_VIRTUAL   = "virtual";
    
    private static final String[] OPTIONAL_ATTRS = new String[] {
        ATTR_NAME,
        ATTR_VERSION,
        ATTR_PLATFORMS,
        ATTR_DESCRIPTION,
        ATTR_INCLUDE,
        ATTR_VIRTUAL,
    };
    
    ProductTag product;
    PlatformTag platform = null;
    boolean isIncluded;
    private HashMap serverMap;
    private TypeBuilder builder;
    private String[][] platformTypes;
    private ServerTypeInfo server;
    HashMap includedServices = new HashMap();
    
    ServerTag(BaseTag parent) {
        super(parent);
        if (parent instanceof PlatformTag) {
            this.platform = (PlatformTag)parent;
            this.product = (ProductTag)this.platform.parent;
        }
        else {
            this.product = (ProductTag)parent;
        }
    }

    private ServiceTypeInfo addBuilderService(ServerTypeInfo server, String name) {
        String serviceName = server.getName() + " " + name; //the default
        //special case for platform services
        if (this.platform != null) {
            if (this.platform.type.isDevice() &&
                this.server.isVirtual())
            {
                serviceName =
                    this.platform.typeName + " " + name;
            }
        }
        else if (server.isVirtual()) {
            serviceName = name;
        }

        ServiceTypeInfo service =
            new ServiceTypeInfo(serviceName,
                                server.getDescription() +
                                " " + name,
                                server);

        this.builder.add(service);
        return service;
    }

    ServiceTypeInfo addService(String name, boolean isInternal) {
        ServiceTypeInfo service = null;

        if (isIncluded) {
            //services are included along with the server
            //dont want to re-add the service type
            //just allow for metrics to be appended to, etc.
            String typeName = this.server.getName() + " " + name;
            service = (ServiceTypeInfo)includedServices.get(typeName);
            if (service != null) {
                return service;
            }
            
            for (Iterator it=this.serverMap.values().iterator();
                 it.hasNext();)
            {
                ServerTypeInfo server = (ServerTypeInfo)it.next();
                service = addBuilderService(server, name);
                service.setInternal(isInternal);
            }
        }
        else {
            service = addBuilderService(this.server, name);
            service.setInternal(isInternal);
        }
        
        return service;
    }
    
    int getResourceType() {
        return TypeInfo.TYPE_SERVER;
    }
    
    public String getName() {
        return "server";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public XmlTagInfo[] getSubTags() {
        XmlTagInfo[] tags = new XmlTagInfo[] {
            new XmlTagInfo(new ServiceTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
            new XmlTagInfo(new ScanTag(this),
                           XmlTagInfo.ZERO_OR_MORE)                
        };

        return getMergedSubTags(super.getSubTags(), tags);
    }

    public void startTag() {
        this.isIncluded = false;
        this.builder = new TypeBuilder();
    }

    private TypeInfo[] getSavedTypes(String key) {
        return (TypeInfo[])getScratch(key);
    }
    
    private void putSavedTypes(TypeInfo[] types) {
        putScratch(this.typeName, types);
    }

    private void includePlugin(String type, String from, String to) {
        String plugin =
            this.data.getPlugin(type, from);
        if (plugin != null) {
            this.data.addPlugin(type, to, plugin);
        }        
    }
    
    private void includePlugins(String from, String to) {
        for (int j=0; j<ProductPlugin.TYPES.length; j++) {
            includePlugin(ProductPlugin.TYPES[j], from, to);
        }
        //XXX should just copy everything
        includePlugin(MeasurementPlugin.TYPE_COLLECTOR, from, to);
    }
    
    private void includeConfigSchema(String from, String to) {
        for (int i=0; i<ProductPlugin.CONFIGURABLE_TYPES.length; i++) {
            ConfigSchema schema = this.data.getConfigSchema(from, i);
            if (schema != null) {
                this.data.addConfigSchema(to, i, schema);
            }
        }
    }
    
    private void includeCustomPropertiesSchema(String from, String to) {
        ConfigSchema schema = this.data.getCustomPropertiesSchema(from);
        if (schema != null) {
            ConfigSchema cpropSchema = new ConfigSchema();
            cpropSchema.addOptions(schema.getOptions());
            this.data.addCustomPropertiesSchema(to, cpropSchema);
        }
    }
    
    private void includeHelp(String from, String to) {
        if (!this.collectHelp) {
            return;
        }
        if (this.data.help.get(to) != null) {
            return; //already defined, e.g. the apache plugin
        }
        String help = (String)this.data.help.get(from);
        if (help != null) {
            this.data.help.put(to, help);
        }
    }

    private void includeActions(String from, String to) {
        if (this.data.getControlActions(to) != null) {
            return; //already included
        }
        List controlActions = this.data.getControlActions(from);
        if (controlActions != null) {
            this.data.addControlActions(to, controlActions);
        }
    }

    private void includeComponents(TypeInfo fromType, TypeInfo toType) {
        String[] platforms = fromType.getPlatformTypes();
        String from = fromType.getName();
        String to = toType.getName();

        for (int i=0; i<platforms.length; i++) {
            String fromTypeName = from + " " + platforms[i];
            String toTypeName = to + " " + platforms[i];

            includeHelp(fromTypeName, toTypeName);
            includePlugins(fromTypeName, toTypeName);
            includeConfigSchema(fromTypeName, toTypeName);
            includeCustomPropertiesSchema(fromTypeName, toTypeName);
            includeActions(fromTypeName, toTypeName);
        }

        includeHelp(from, to);
        includePlugins(from, to);
        includeConfigSchema(from, to);
        includeCustomPropertiesSchema(from, to);
        includeActions(from, to);
        this.data.includeGlobalProperties(fromType.getName(),
                                          toType.getName());
    }

    private void includeMetrics(String name, String include) {
        if (data.getMetrics(name, false) != null) {
            //already included
            return;
        }
        MetricsTag.includeMetrics(this.data, name, include);
    }

    private void includeServer(String name,
                               String version,
                               String description,
                               String include)
        throws XmlAttrException {

        int i;
        
        //e.g. include == ".NET 1.2"
        TypeInfo[] types = getSavedTypes(include);

        if (types == null) {
            //e.g include == "1.2"
            String key = name + " " + include;
            types = getSavedTypes(key);
            if (types != null) {
                include = key;
            }
        }

        if (types == null) {
            throw new XmlAttrException("server include='"
                                            + include + "', name="
                                            + name + " not found");
        }
        
        this.isIncluded = true;
        String serverType = name + " " + version;
        
        if (this.collectMetrics) {
            includeMetrics(serverType, include);
        }
        
        List sigs = this.data.getFileScanIncludes(include);
        if (sigs != null) {
            this.data.addFileScanIncludes(serverType, sigs);
        }

        sigs = this.data.getRegistryScanIncludes(include);
        if (sigs != null) {
            this.data.addRegistryScanIncludes(serverType, sigs);
        }
        
        List keys = this.data.getRegistryScanKeys(include);
        if (keys != null) {
            for (i=0; i<keys.size(); i++) {
                this.data.addRegistryScanKey(serverType,
                                             (String)keys.get(i));
            }
        }

        //first pass, clone the server type(s)
        //and change name/version/description
        //will be more than 1 server type w/ same name
        //if platforms attribute was specified.
        this.serverMap = new HashMap();
        for (i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            if (type.getType() != TypeInfo.TYPE_SERVER) {
                continue;
            }
            
            ServerTypeInfo info = (ServerTypeInfo)type;
            //copy the name="..." attribute
            String nameValue = getNameProperty(info);
            info = (ServerTypeInfo)info.clone();
            serverMap.put(type, info);
            //XXX include metrics, scan, plugins
            info.setName(serverType);
            info.setVersion(version);
            if (description != null) {
                info.setDescription(description);
            }

            setNameProperty(info.getName(), nameValue);
            this.builder.add(info);
            includeComponents(type, info);
            //allows additional services
            this.server = info;
            this.typeName = info.getName();
        }
        
        //second pass clone the services
        for (i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            if (type.getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }

            ServiceTypeInfo info = (ServiceTypeInfo)type;
            //copy the name="..." attribute
            String nameValue = getNameProperty(info);
            info = (ServiceTypeInfo)info.clone();
            ServerTypeInfo server =
                (ServerTypeInfo)serverMap.get(info.getServerTypeInfo());
            info.setServerTypeInfo(server);
            info.setName(StringUtil.replace(info.getName(),
                                            include, serverType));
            this.includedServices.put(info.getName(), info);
            setNameProperty(info.getName(), nameValue);
            this.builder.add(info);
            includeComponents(type, info);
            if (this.collectMetrics) {
                includeMetrics(info.getName(), type.getName());
            }
        }

        Map serviceInventoryPlugins =
            this.data.getServiceInventoryPlugins(include);
        if (serviceInventoryPlugins != null) {
            for (Iterator plugins = serviceInventoryPlugins.entrySet().iterator();
                 plugins.hasNext();)
            {
                Map.Entry entry = (Map.Entry)plugins.next();
                String serviceName = (String)entry.getKey();
                String impl = (String)entry.getValue();
                if (PluginData.getServiceExtension(serviceName) != null) {
                    continue; //service extensions are not included
                }
                serviceName = StringUtil.replace(serviceName, include, serverType);
                this.data.addServiceInventoryPlugin(this.typeName, serviceName, impl);
            }
        }
    }
    
    public void endAttributes() throws XmlAttrException {
        String include     = getAttribute(ATTR_INCLUDE);
        String name        = getAttribute(ATTR_NAME);
        String version     = getAttribute(ATTR_VERSION);
        String platforms   = getAttribute(ATTR_PLATFORMS);
        String description = getAttribute(ATTR_DESCRIPTION);
        String virtual     = getAttribute(ATTR_VIRTUAL);

        if (this.platform != null) {
            if (name == null) {
                //likely a device such as Airport, assume server is virtual
                //default server name to platform name and
                //tack on " Server" so the name is different for plugin managers
                name = this.platform.typeName + " Server";
                if (virtual == null) {
                    virtual = "true";
                }
            }
        }
        else {
            if (name == null) {
                throw new XmlAttrException("missing server 'name' attribute");
            }
        }

        if (version == null) {
            version = TypeBuilder.NO_VERSION;
        }

        if (include != null) {
            includeServer(name, version, description, include);
            return;
        }
        
        List platformList;
        
        if (platforms == null) {
            if ((this.platform != null) &&
                 this.platform.type.isDevice())
            {
                this.platformTypes = new String[][] {
                    { this.platform.typeName }
                };
            }
            else {
                this.platformTypes = new String[][] {
                    TypeBuilder.ALL_PLATFORM_NAMES
                };
            }
        }
        else {
            //deal with the Win32 and Unix specific instances
            platformList = StringUtil.explode(platforms, ",");
            int len = platformList.size();
            int offset = 0;
            platformTypes = new String[len][];
            if (platformList.remove("Unix")) {
                offset = 1;
                this.platformTypes[0] = TypeBuilder.UNIX_PLATFORM_NAMES;
            }
	    else if (platformList.remove("Win32")) {
		offset = 1;
                this.platformTypes[0] = TypeBuilder.WIN32_PLATFORM_NAMES;
	    }
            for (int i=0; i<platformList.size(); i++) {
                this.platformTypes[i+offset] =
                    new String[] { (String)platformList.get(i) };
            }
        }

        this.server = this.builder.addServer(name, version, platformTypes[0]);
        if (description == null) {
            description = name + " Server";
        }
        this.server.setDescription(description);
        this.typeName = this.server.getName();
        if ("true".equals(virtual)) {
            this.server.setVirtual(true);
        }

        setNameProperty(name);
    }

    void endTag() throws XmlTagException {
        if (!this.isIncluded &&
            (this.platformTypes.length != 1))
        {
            for (int j=1; j<platformTypes.length; j++) {
                this.builder.addServerAndServices(this.server,
                                                  this.platformTypes[j]);
            }
        }

        this.includedServices.clear();
        TypeInfo[] types = this.builder.getTypes();
        this.server = null;
        this.platformTypes = null;
        this.serverMap = null;
        putSavedTypes(types);
        this.data.addTypes(types);

        super.endTag();
    }
}

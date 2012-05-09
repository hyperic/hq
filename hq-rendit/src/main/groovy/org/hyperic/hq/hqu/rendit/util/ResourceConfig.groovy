/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.util

import org.hyperic.hq.appdef.Agent

import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.bizapp.shared.AllConfigResponses
import org.hyperic.hq.product.ProductPlugin
import org.hyperic.hq.product.PluginNotFoundException
import org.hyperic.util.config.ConfigResponse

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * This class provides a layer of abstraction needed to get properties
 * from various subsystems (such as custom props, product configs, etc.)
 *
 * It provides a common interface for users of {@link Resource} to
 * interact with configuring a resource.
 *
 * In the future, we'll want to expand this to work for most resource
 * types (users?, roles?)
 */
class ResourceConfig {
    private static authzMan  = Bootstrap.getBean(AuthzSubjectManager.class)
    private static cpropMan  = Bootstrap.getBean(CPropManager.class)
    private static appBoss   = Bootstrap.getBean(AppdefBoss.class)
    private static prodBoss  = Bootstrap.getBean(ProductBoss.class)
    private static configMan = Bootstrap.getBean(ConfigManager.class)
    private static platMan   = Bootstrap.getBean(PlatformManager.class)
    private static svrMan    = Bootstrap.getBean(ServerManager.class)
    private static svcMan    = Bootstrap.getBean(ServiceManager.class)
    private static agentMan = Bootstrap.getBean(AgentManager.class)

    Log log = LogFactory.getLog(this.getClass())

    /**
     * *_FIELD_KEYS provides a mapping of string names onto closures which
     * allow us to set/get them easily.
     */
    
    private static PLATFORM_FIELD_KEYS = [
        'name':        [get: { plat -> plat.name },
                        set: { platVal, name -> platVal.name = name }], 
        'fqdn':        [get: { plat -> plat.fqdn },
                        set: { platVal, fqdn -> platVal.fqdn = fqdn }], 
        'comment':     [get: { plat -> plat.commentText },
                        set: { platVal, comment -> plat.commentText = comment}],
        'description': [get: { plat -> plat.description },
                        set: { platVal, desc -> platVal.description = desc}],
        'location':    [get: { plat -> plat.location },
                        set: { platVal, loc -> platVal.location = loc}],
         'agentId':     [get: { plat -> plat.agent.id },
                        set: { platVal, agentId -> platVal.agent = agentMan.getAgent(agentId)}],
    ]
    
    private static SERVER_FIELD_KEYS = [
        'name':        [get: { svr -> svr.name },
                        set: { svrVal, name -> svrVal.name = name} ],
        'description': [get: { svr -> svr.description },
                        set: { svrVal, desc -> svrVal.description = desc} ],
        'installPath': [get: { svr -> svr.installPath },
                        set: { svrVal, installPath -> svrVal.installPath = installPath} ],
        'autoIdentifier': [get: {svr -> svr.autoinventoryIdentifier },
                           set: {svrVal, autoIdentifier -> svrVal.autoinventoryIdentifier = autoIdentifier } ],
        'location':    [get: { svr -> svr.location },
                        set: { svrVal, loc -> svrVal.location = loc} ],
        'runtimeDiscovery':  [get: { svr -> svr.runtimeAutodiscovery.toString() },
                              set: { svrVal, enabled -> svrVal.runtimeAutodiscovery = enabled.toBoolean() } ], 
    ]
     
    private static SERVICE_FIELD_KEYS = [
        'name':        [get: { svc -> svc.name },
                        set: { svcVal, name -> svcVal.name = name }],
        'description': [get: { svc -> svc.description },
                        set: { svcVal, desc -> svcVal.description = desc }],
        'location':    [get: { svc -> svc.location },
                        set: { svcVal, loc -> svcVal.location = loc }],
        'autoIdentifier': [get: { svc -> svc.autoinventoryIdentifier },
                           set: { svcVal, loc -> svcVal.autoinventoryIdentifier = loc }],
    ]
    
    private static APPDEF_FIELD_HANDLERS = [
        'platform': [
            fields: PLATFORM_FIELD_KEYS,
            targetForGet:  { r -> r.toPlatform() },
            targetForSet:  { r -> r.toPlatform().platformValue },
            saveSetTarget: { subject, platVal ->
                platMan.updatePlatformImpl(subject, platVal)
            },
            populateAllCfg: { allCfg, platform ->
            },
        ],
        'server': [
            fields: SERVER_FIELD_KEYS,
            targetForGet: { r -> r.toServer() },
            targetForSet: { r -> r.toServer().serverValue },
            saveSetTarget: { subject, serverVal ->
               svrMan.updateServer(subject, serverVal)
            },
            populateAllCfg: { allCfg, server ->
                allCfg.enableRuntimeAIScan = server.isRuntimeAutodiscovery() 
            },
        ],
        'service': [
            fields: SERVICE_FIELD_KEYS,
            targetForGet: { r -> r.toService()} ,
            targetForSet: { r -> r.toService().serviceValue },
            saveSetTarget: { subject, serviceVal ->
                svcMan.updateService(subject, serviceVal)
            },
            populateAllCfg: { allCfg, service ->
            },
        ],
    ]
    
    private Resource resource
    private configResponses = [:]
    private cprops          = [:]
    private fieldProps      = [:]
    
    ResourceConfig(Resource r) {
        resource = r
    }

    ResourceConfig() {
        resource = null
    }
    
    private clear() {
        configResponses.clear()
        cprops.clear()
        fieldProps.clear()
    }
    
    void populate() {
        clear()
        def entityID = resource.entityId

        // Fill out config responses
        ProductPlugin.CONFIGURABLE_TYPES.each { type ->
            def cfgSchema
            def cfgResponse
            try {
                def schemaAndResponse = 
                    prodBoss.getConfigSchemaAndBaseResponse(null, entityID,
                                                            type, true)
                cfgSchema   = schemaAndResponse.schema
                cfgResponse = schemaAndResponse.response
            } catch(PluginNotFoundException e) {
                // This specified entity type does not support the TYPE_* plugin
                return
            }
            
            configResponses[type] = [response: cfgResponse,
                                     bytes: cfgResponse.encode(),
                                     schema: cfgSchema]
        }
        
        def proto  = resource.prototype
        def typeId = proto.appdefType
        
        // Fill out cprops
        for (cpropKey in cpropMan.getKeys(typeId, proto.instanceId)) {
            cprops.put(cpropKey.key, [key: cpropKey])
        }
        
        cpropMan.getEntries(entityID).each { key, val ->
            cprops[key].value = val
        }

        // Fill out pojo.field props
        def appdefHandler = getAppdefHandler(resource)
        def targ = appdefHandler.targetForGet(resource)
        appdefHandler.fields.each { keyName, keyInfo ->
            fieldProps[keyName] = keyInfo['get'](targ)
        }
    }

    /**
     * Gets a map of keys onto a map, describing the configuration.
     *
     * @return a map like:
            [ 'myKey' : [ value: '123', description: 'My Secret Key' ],
              'mtu'   : [ value: '1500', description: 'Interface MTU' ]]
     *
     */
    Map getEntries() {
        def res = [:]
        
        configResponses.each { type, cfgResponse ->
            cfgResponse.schema.options.each { opt ->
                res[opt.name] = [value: null, type: 'configResponse',
                                 description: opt.description]
            }
            
            def props = cfgResponse.response.toProperties()
            props.each { key, val ->
                if (res[key]) { 
                    res[key].value = val
                }
            }
        }

        cprops.each { key, val -> 
            res.put(key, [value: val.value, description:val.key.description,
                          type: 'cprop'])
        }
        
        fieldProps.each { key, val ->
            res.put(key, [value: val, type: 'field'])
        }
        res
    }

    /**
     * Set properties of the resource backing this configuration.  Only  
     * existing properties can be set -- new properties will be discarded.
     */
    void setProperties(Map props, AuthzSubject subject) {
    	setProperties(props, subject, Boolean.TRUE)
    }
    
    /**
     * Set properties of the resource backing this configuration.  Only  
     * existing properties can be set -- new properties will be discarded.
     */
    void setProperties(Map props, AuthzSubject subject, Boolean isUserManaged) {
        populate()

        def entityID   = resource.entityId

        // CProp changes
        def proto    = resource.prototype
        def typeId   = proto.appdefType
        cprops.each { key, val ->
            if (props.containsKey(key) && val != props[key]) {
                cpropMan.setValue(entityID, proto.instanceId, key, props[key])
            }
        }
        
        // POJO (field) level changes
        def appdefHandler = getAppdefHandler(resource)
        def targetForGet  = appdefHandler.targetForGet(resource)
        def changedFields = [:]
        appdefHandler.fields.each { key, keyInfo ->
            if (props.containsKey(key) && 
                keyInfo['get'](targetForGet) != props[key])
            {
                changedFields[key] = props[key]
            }
        }
        
        if (changedFields.size() > 0) {
            def targetForSet = appdefHandler.targetForSet(resource)
            changedFields.each { key, newVal ->
                appdefHandler.fields[key]['set'](targetForSet, newVal)
            }
            log.info("Saving changed fields for ${targetForSet} fields = ${changedFields}")
            appdefHandler.saveSetTarget(subject, targetForSet)
        }
        
        // Config Response changes
        def allConfigs      = new AllConfigResponses()
        allConfigs.resource = entityID
        def allConfigsRoll  = new AllConfigResponses()
        allConfigsRoll.resource = entityID
        
        for (i in 0..<ProductPlugin.CONFIGURABLE_TYPES.length) {
            def type        = ProductPlugin.CONFIGURABLE_TYPES[i]
            def responseMap = configResponses[type]
            if (responseMap == null) {
                allConfigsRoll.setSupports(i, false)
            } else {
                def curResponse = responseMap.response
                allConfigsRoll.setSupports(i, true)
                allConfigsRoll.setShouldConfig(i, true)
                allConfigsRoll.setConfig(i, curResponse)
                allConfigs.setSupports(i, true)
                allConfigs.setShouldConfig(i, true)
                
                def newResponse = new ConfigResponse()
                responseMap.schema.options.each { opt ->
                    def key = opt.name
                    def val = curResponse.getValue(key)
                    if (props.containsKey(key) && props[key] != val) {
                        newResponse.setValue(key, props[key])
                    } else {
                        newResponse.setValue(key, (String)val) 
                    }
                }
                allConfigs.setConfig(i, newResponse)
            }
        }
        
        appdefHandler.populateAllCfg(allConfigs, targetForGet)
        // XXX:  Undo this rollback crap when Appdef gets itself figured out.
        appBoss.setAllConfigResponses(subject, allConfigs, allConfigsRoll, isUserManaged)

    }
    
    private getAppdefHandler(Resource r) {
        if (r.isPlatform()) {
            return APPDEF_FIELD_HANDLERS['platform']
        } else if (r.isServer()) {
            return APPDEF_FIELD_HANDLERS['server']
        } else if (r.isService()) {
            return APPDEF_FIELD_HANDLERS['service']
        }
    }

    /**
     * Find an agent given some arbitrary string.  This is used when
     * configuring platforms via createInstance(), or when re-adjusting the
     * agent servicing a platform.  
     *
     * Currently, we return an agent if it is monitoring a platform with 
     * fqdn specified by the parameter
     */
    public static Agent findSuitableAgentFor(String s) {
        def overlord = authzMan.overlordPojo
        try {
            return platMan.findPlatformByFqdn(overlord, s)?.agent
        } catch(PlatformNotFoundException e) {
            return null
        }
    }
}

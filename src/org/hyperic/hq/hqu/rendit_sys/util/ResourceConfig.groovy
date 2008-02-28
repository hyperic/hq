package org.hyperic.hq.hqu.rendit.util

import org.hyperic.hq.appdef.Agent
import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl as CPropMan
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl as ConfigMan
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.appdef.shared.AppdefEntityValue
import org.hyperic.hq.appdef.shared.PlatformNotFoundException
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as AuthzMan
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl as AppdefBoss
import org.hyperic.hq.bizapp.server.session.ProductBossEJBImpl as ProductBoss
import org.hyperic.hq.bizapp.shared.AllConfigResponses
import org.hyperic.hq.product.ProductPlugin
import org.hyperic.hq.product.PluginNotFoundException
import org.hyperic.util.config.ConfigResponse

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
    private static authzMan  = AuthzMan.one
    private static cpropMan  = CPropMan.one
    private static appBoss   = AppdefBoss.one
    private static prodBoss  = ProductBoss.one
    private static configMan = ConfigMan.one
    private static platMan   = PlatMan.one
    private static svrMan    = ServerMan.one
    private static svcMan    = ServiceMan.one
     
    /**
     * *_FIELD_KEYS provides a mapping of string names onto closures which
     * allow us to set/get them easily.
     */
    
    private static PLATFORM_FIELD_KEYS = [
        'fqdn':        [get: { plat -> plat.fqdn },
                        set: { platVal, fqdn -> platVal.fqdn = fqdn }], 
        'comment':     [get: { plat -> plat.commentText },
                        set: { platVal, comment -> plat.commentText = comment}],
        'description': [get: { plat -> plat.description },
                        set: { platVal, desc -> platVal.description = desc}],
        'location':    [get: { plat -> plat.location },
                        set: { platVal, loc -> platVal.location = loc}],
    ]
    
    private static SERVER_FIELD_KEYS = [
        'installPath': [get: { svr -> svr.installPath },
                        set: { svrVal -> svrVal.installPath = installPath} ],
        'description': [get: { svr -> svr.description },
                        set: { svrVal, desc -> svrVal.description = desc} ],
        'location':    [get: { svr -> svr.location },
                        set: { svrVal, loc -> svr.location = loc} ],
    ]
     
    private static SERVICE_FIELD_KEYS = [
        'description': [get: { svc -> svc.description },
                        set: { svcVal, desc -> svcVal.description = desc }],
        'location':    [get: { svc -> svc.location },
                        set: { svcVal, loc -> svcVal.location = loc }],
    ]
    
    private static APPDEF_FIELD_HANDLERS = [
        'platform': [
            fields: PLATFORM_FIELD_KEYS,
            targetForGet:  { r -> r.toPlatform() },
            targetForSet:  { r -> r.toPlatform().platformValue },
            saveSetTarget: { subjectVal, platVal ->
                platMan.updatePlatformImpl(subjectVal, platVal)
            }
        ],
        'server': [
            fields: SERVER_FIELD_KEYS,
            targetForGet: { r -> r.toServer() },
            targetForSet: { r -> r.toServer().serverValue },
            saveSetTarget: { subjectVal, serverVal ->
               svrMan.updateServer(subjectVal, serverVal)
            }
        ],
        'service': [
            fields: SERVICE_FIELD_KEYS,
            targetForGet: { r -> r.toService()} ,
            targetForSet: { r -> r.toService().serviceValue },
            saveSetTarget: { subjectVal, serviceVal ->
                svcMan.updateService(subjectVal, serviceVal)
            }
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
        def entityID = resource.entityID

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
                } else {
                    // Not contained in the config schema?
                    res[key] = [value: val, type: 'configResponse'] 
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
        populate()

        def subjectVal = subject.valueObject
        def entityID   = resource.entityID

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
        
        if (!changedFields.empty) {
            def targetForSet = appdefHandler.targetForSet(resource)
            changedFields.each { key, newVal ->
                appdefHandler.fields[key]['set'](targetForSet, newVal)
            }
            
            appdefHandler.saveSetTarget(subjectVal, targetForSet)
        }
        
        // Config Response changes
        def entVal     = new AppdefEntityValue(entityID, subject)
        def appdefVal  = entVal.resourceValue
        def allConfigs     = new AllConfigResponses()
        allConfigs.resource = appdefVal
        def allConfigsRoll = new AllConfigResponses()
        allConfigsRoll.resource = appdefVal
        def newResponses = [:]
        
        for (i in 0..<ProductPlugin.CONFIGURABLE_TYPES.length) {
            def type        = ProductPlugin.CONFIGURABLE_TYPES[i]
            def responseMap = configResponses[type]
            if (responseMap == null) {
                allConfigsRoll.setSupports(i, false)
            } else {
                def curResponse = responseMap.response
                allConfigsRoll.setSupports(i, true)
                allConfigsRoll.setConfig(i, curResponse)
                allConfigs.setSupports(i, true)
                
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
        // XXX:  Undo this rollback crap when Appdef gets itself figured out.
        appBoss.setAllConfigResponses(subject, allConfigs, allConfigsRoll)
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
            return platMan.findPlatformByFqdn(overlord.valueObject, s)?.agent
        } catch(PlatformNotFoundException e) {
            return null
        }
    }
}

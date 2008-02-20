package org.hyperic.hq.hqu.rendit.util

import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl as CPropMan
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl as ConfigMan

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
    private static cpropMan  = CPropMan.one
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
            saveSetTarget: { subject, platVal ->
                platMan.updatePlatformImpl(subject.valueObject, platVal)
            }
        ],
        'server': [
            fields: SERVER_FIELD_KEYS,
            targetForGet: { r -> r.toServer() },
            targetForSet: { r -> r.toServer().serverValue },
            saveSetTarget: { subject, serverVal ->
               svrMan.updateServer(subject.valueObject, serverVal)
            }
        ],
        'service': [
            fields: SERVICE_FIELD_KEYS,
            targetForGet: { r -> r.toService()} ,
            targetForSet: { r -> r.toService().serviceValue },
            saveSetTarget: { subject, serviceVal ->
                svcMan.updateService(subject.valueObject, serviceVal)
            }
        ],
    ]
    

    private Resource resource
    private configResponses = [:]
    private cprops          = [:]
    private fieldProps      = [:]
    
    ResourceConfig(Resource r) {
        resource = r
        populate()
    }

    ResourceConfig() {
        resource = null
    }
    
    private clear() {
        configResponses.clear()
        cprops.clear()
        fieldProps.clear()
    }
    
    private populate() {
        def response = configMan.getConfigResponse(resource.entityID)
        
        // configResponses will contain a map of these keys onto their
        // decoded responses
        ['product', 'control', 'autoInventory', 'measurement',
         'responseTime'].each { type ->
            def cfgResponse = response."${type}Response"
            if (cfgResponse != null) {
                configResponses[type] = [response: ConfigResponse.decode(cfgResponse),
                                         bytes: cfgResponse]
            }
        }
        
        def proto  = resource.prototype
        def typeId = proto.appdefType
        
        for (cpropKey in cpropMan.getKeys(typeId, proto.instanceId)) {
            cprops.put(cpropKey.key, [key: cpropKey])
        }
        
        cpropMan.getEntries(resource.entityID).each { key, val ->
            cprops[key].value = val
        }

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
            def props = cfgResponse.response.toProperties()
            props.each { key, val ->
                res.put(key, [value: val, type: 'configResponse'])
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
    def setProperties(Map props, AuthzSubject subject) {
        clear()
        populate()

        def entityID = resource.entityID

        // Config Response changes
        def newResponses = [:]
        configResponses.each { type, responseMap ->
            def newResponse = new ConfigResponse()
            responseMap.response.toProperties().each { key, val ->
                if (props.containsKey(key) && props[key] != val) { 
                    newResponse.setValue(key, props[key])
                } else {
                    newResponse.setValue(key, val)
                }
            }
            newResponses[type] = newResponse.encode()
        }
        configMan.configureResource(subject.valueObject, 
                                    entityID,
                                    newResponses['product'],
                                    newResponses['measurement'],
                                    newResponses['control'],
                                    newResponses['responseTime'],
                                    true,  // User Managed
                                    true,  // send reconfig zevent
                                    false)  // force
        
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
            
            appdefHandler.saveSetTarget(subject, targetForSet)
        }
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
}

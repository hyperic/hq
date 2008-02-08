package org.hyperic.hq.hqu.rendit.util

import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl as CPropMan
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
    private cpropMan = CPropMan.one
    
    private Resource resource
    private configResponses = [:]
    private cprops          = [:]
    private fieldProps      = [:]
    
    ResourceConfig(Resource r) {
        resource = r
        clear()
        populate()
    }
    
    private clear() {
        configResponses.clear()
        cprops.clear()
    }
    
    private populate() {
        def response = ConfigMan.one.getConfigResponse(resource.entityID)
        
        // configResponses will contain a map of these keys onto their
        // decoded responses
        ['product', 'control', 'autoInventory', 'measurement',
         'responseTime'].each { type ->
            def cfgResponse = response."${type}Response"
            if (cfgResponse != null) {
                configResponses[type] = ConfigResponse.decode(cfgResponse)
            }
        }
        
        def proto  = resource.prototype
        def typeId = proto.appdefType
        
        for (cpropKey in cpropMan.getKeys(typeId, proto.instanceId)) {
            cprops.put(cpropKey.key, [key : cpropKey])
        }
        
        cpropMan.getEntries(resource.entityID).each { key, val ->
            cprops[key].value = val
        }

        if (resource.isPlatform()) {
            fieldProps.fqdn = resource.toPlatform().fqdn
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
    def getProperties() {
        def res = [:]
        
        configResponses.values()*.toProperties().each { props ->
            props.each { key, val ->
                res.put(key, [value: val, type: 'configResponse'])
            }
        }

        cprops.each { key, val -> 
            res.put(key, [value: val.value, description:val.key.description,
                          type: 'cprop'])
        }
        
        fieldProps.each { key, val ->
            res.put(key, [value: val, type: 'system'])
        }
        res
    }
}

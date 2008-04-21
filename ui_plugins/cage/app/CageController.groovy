import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.grouping.CritterRegistry
import org.hyperic.hq.grouping.CritterTranslator
import org.hyperic.hq.grouping.CritterTranslationContext
import org.hyperic.hq.grouping.CritterList
import org.hyperic.hq.grouping.CritterType
import org.hyperic.hq.grouping.prop.CritterPropDescription
import org.hyperic.hq.grouping.prop.StringCritterProp
import org.hyperic.dao.DAOFactory
import org.hyperic.hibernate.Util

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup

class CageController 
	extends BaseController
{
    private CritterRegistry _registry = CritterRegistry.getRegistry()
    
    protected void init() {
        onlyAllowSuperUsers()
        setXMLMethods(['peek', 'sync'])
    }
    
    private String getUpload(argName) {
        def res
        eachUpload() { upload ->
            if (upload.fieldName == argName)
                res = upload.openStream().getText()
        }
        res
    }

    private CritterType findCritterType(String name) {
        _registry.critterTypes.find { t ->
            t.class.name == name
        }
    }
    
    def index(params) {
        Resource r = getViewedResource()
        ResourceGroup g = resourceHelper.findGroup(r.instanceId) 
            
        def sess      = DAOFactory.getDAOFactory().currentSession
        def ctx       = new CritterTranslationContext(sess, Util.getHQDialect())
        def trans     = new CritterTranslator()
        def clist     = g.critterList
        log.info "Critters: ${clist.critters.config}"
        def proposedResources = trans.translate(ctx, clist).list()
        
        render(locals:[group:g, critterList:clist,
                       proposedResources:proposedResources])
    }
    
    def list(params) {
        def res = new StringBuilder()
        
        for (c in _registry.critterTypes) {
            res << "${c.class.name}:\n"
            res << "    Name:        ${c.name}\n"
            res << "    Description: ${c.description}\n"
            res << "    System:      ${c.isSystem()}\n\n"
        }
        render inline:res.toString()
    }
    
    def explain(params) {
        def typeName = params.getOne('class')
        def type = findCritterType(typeName)

        if (!type) {
            render inline: "Critter type [${typeName}] not found\n"
            return
        }
        
        def res = new StringBuilder()
        res << "Critter class: ${typeName}\n"
        res << "Name:          ${type.name}\n"
        res << "Description:   ${type.description}\n"
        
        type.propDescriptions.eachWithIndex { desc, i ->
            res << "Arg[${i}]:\n"
            res << "    name:    ${desc.name}\n"
            res << "    type:    ${desc.type.description}\n"
            res << "    purpose: ${desc.purpose}\n"
        }
        
        render inline: res.toString()
    }
    
    def sync(xmlOut, params) {
        def xmlIn = new XmlParser().parseText(getUpload('args'))
        def group = resourceHelper.findGroupByName(xmlIn.'@name')
        def isAny = xmlIn.'@isAny'?.toBoolean()
        
        def critters = parseCritters(xmlIn.critters)

        log.info "Critters ${critters.config}"
        GroupMan.one.setCriteria(user, group, new CritterList(critters, 
                                                              isAny == true))
        xmlOut.success()
    }
    
    private List parseCritters(xmlIn) {
        xmlIn.critter.collect { critterDef ->
            def critterType = findCritterType(critterDef.'@class')
            
            if (critterType == null) {
                xmlOut.error("Unable to find critter class [${critterDef.'@class'}]")
                return xmlOut
            }
            
            def props = []
            for (propDef in critterDef.children()) {
                if (propDef.name() == 'string') {
                    props << new StringCritterProp(propDef.text())
                } else {
                    xmlOut.error("Unhandled prop type: ${propDef.'@type'}")
                }
            }
            critterType.newInstance(props)
        }
    }
    
    def peek(xmlOut, params) {
        def xmlIn    = new XmlParser().parseText(getUpload('args'))
        def critters = parseCritters(xmlIn)

        def isAny     = xmlIn.'@isAny'?.toBoolean()
        def clist     = new CritterList(critters, isAny == true)
        def trans     = new CritterTranslator()
        def sess      = DAOFactory.getDAOFactory().currentSession
        def resources = trans.translate(sess, clist).list()
        
        xmlOut.resources { 
            for (r in resources) {
                resource(id:r.id, name: r.name, type: r.prototype.name)
            }
        }
        xmlOut
    }
}

import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.grouping.CritterRegistry
import org.hyperic.hq.grouping.CritterTranslator
import org.hyperic.hq.grouping.CritterTranslationContext
import org.hyperic.hq.grouping.CritterList
import org.hyperic.hq.grouping.CritterType
import org.hyperic.hq.grouping.prop.CritterPropDescription
import org.hyperic.hq.grouping.prop.EnumCritterPropDescription
import org.hyperic.hq.grouping.prop.StringCritterProp
import org.hyperic.hq.grouping.prop.EnumCritterProp
import org.hyperic.hq.grouping.prop.CritterPropType
import org.hyperic.hq.grouping.prop.GroupCritterProp
import org.hyperic.hq.grouping.prop.ProtoCritterProp
import org.hyperic.hq.grouping.prop.ResourceCritterProp
import org.hyperic.dao.DAOFactory
import org.hyperic.hibernate.Util
import org.hyperic.util.HypericEnum

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

        def sess        = DAOFactory.getDAOFactory().currentSession
        def ctx         = new CritterTranslationContext()
        def trans       = new CritterTranslator()
        def clist       = g.critterList
        def isAny       = clist.isAny()
        def systemCrits = clist.critters.findAll { it.critterType.isSystem() }
        def critters    = clist.critters.findAll { !it.critterType.isSystem() }
        
        if (!clist.critters) {
            render(locals:[group:g, isAny: isAny,
                           critters: null,
                           systemCritters: systemCrits,
                           proposedResources:null])
            return
        }
        log.info "Critters: ${clist.critters.config}"
        def proposedResources = trans.translate(ctx, clist).list()
        
        render(locals:[group:g, isAny: isAny,
                       critters: critters, 
                       systemCritters: systemCrits,
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
        CritterType type = findCritterType(typeName)

        if (!type) {
            render inline: "Critter type [${typeName}] not found\n"
            return
        }
        
        def res = new StringBuilder()
        res << "Critter class: ${typeName}\n"
        res << "Name:          ${type.name}\n"
        res << "Description:   ${type.description}\n"
        
        type.propDescriptions.eachWithIndex { CritterPropDescription desc, i -> 
            res << "Arg[${desc.id}]:\n"
            res << "    name:     ${desc.name}\n"
            res << "    type:     ${desc.type.description}\n"
            res << "    purpose:  ${desc.purpose}\n"
            res << "    required: ${desc.required}\n"
            if (desc.type == CritterPropType.ENUM) {
                EnumCritterPropDescription eDesc = desc
                res << "    possible values: " 
                res << eDesc.possibleValues.description.join(', ') 
                res << "\n"
            }
        }
        
        render inline: res.toString()
    }
    
    def sync(xmlOut, params) {
        def xmlIn = new XmlParser().parseText(getUpload('args'))
        def group = resourceHelper.findGroupByName(xmlIn.'@name')
        def isAny = xmlIn.'@isAny'?.toBoolean()
        def critters
        
        try {
            critters = parseCritters(xmlIn.critters)
        } catch(Exception e) {
            xmlOut.error(e.getMessage())
            log.error("Unable to parse critters", e)
            return xmlOut
        }

        CritterList clist = new CritterList(critters, isAny == true)
        GroupMan.one.setCriteria(user, group, clist)
        
        clist = group.critterList
        log.info "Critters ${clist.critters.config}"
        List resources = getResources(clist)

        group.setResources(user, resources)
        
        xmlOut.group(name:group.name, isAny:clist.isAny()) {
            xmlOut.resources {
                for (r in group.resources) {
                    xmlOut.resource(id:r.id, name:r.name)
                }
            }
            
            xmlOut.proposedResources {
                for (r in resources) {
                    xmlOut.resource(id:r.id, name:r.name)
                }
            }
        }
        xmlOut
    }
    
    private List parseCritters(xmlIn) {
        xmlIn.critter.collect { critterDef ->
            CritterType critterType = findCritterType(critterDef.'@class')
            
            if (critterType == null) {
                throw new Exception("Unable to find critter class [${critterDef.'@class'}]")
            }
            
            def props = [:]
            for (propDef in critterDef.children()) {
                String propId   = propDef.'@id'
                String propType = propDef.name() 
                if (propType == 'string') {
                    props[propId] = new StringCritterProp(propId, propDef.text())
                } else if (propType == 'resource') {
                    def rsrcId   = propDef.text().toInteger()
                    def resource = resourceHelper.findResource(rsrcId)
                    props[propId] = new ResourceCritterProp(propId, resource)
                } else if (propType == 'group') {
                    def group = resourceHelper.findGroupByName(propDef.text())
                    props[propId] = new GroupCritterProp(propId, group)
                } else if (propType == 'proto') { 
                    def proto  = resourceHelper.findResourcePrototype(propDef.text())
                    props[propId] = new ProtoCritterProp(propId, proto)
                } else if (propType == 'enum') {
                    def desc = critterType.propDescriptions.find { it.id == propId }
                    if (!desc) {
                        throw new Exception("Unknown prop [${propId}] for " + 
                                            "critter [${critterDef.class}]")
                    }
                    if (desc.type != CritterPropType.ENUM) {
                        throw new Exception("Property [${propId}] of critter ["+ 
                                            "[${critterDef.class}] should be " + 
                                            "of type <enum>")
                    }
                    
                    EnumCritterPropDescription eDesc = desc
                    def propDesc = propDef.text()
                    HypericEnum match = eDesc.possibleValues.find { it.description == propDesc }
                    if (match == null) {
                        throw new Exception("[${propDesc}] is not a valid " + 
                                            "value for prop [${propId}] for " +
                                            "critter [${critterDef.@class}]")
                    }
                    props[propId] = new EnumCritterProp(propId, match)
                } else {
                    throw new Exception("Unknown prop type [${propDef.name()}] for " + 
                                        "critter [${critterDef.@class}]")
                }
            }
            critterType.newInstance(props)
        }
    }
    
    private List getResources(CritterList clist) {
        def trans     = new CritterTranslator()
        def sess      = DAOFactory.getDAOFactory().currentSession
        def ctx       = new CritterTranslationContext(sess, Util.getHQDialect())
        
        trans.translate(ctx, clist).list()
    }
    
    def peek(xmlOut, params) {
        def xmlIn    = new XmlParser().parseText(getUpload('args'))

        def critters
        try {
            critters = parseCritters(xmlIn)
        } catch(Exception e) {
            log.error("Error parsing critters", e)
            xmlOut.error(e.getMessage())
            return xmlOut
        }

        def isAny     = xmlIn.'@isAny'?.toBoolean()
        def clist     = new CritterList(critters, isAny == true)
        def resources = getResources(clist)
        
        xmlOut.resources { 
            for (r in resources) {
                resource(id:r.id, name: r.name, type: r.prototype.name)
            }
        }
        xmlOut
    }
}

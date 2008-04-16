import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.grouping.CritterRegistry
import org.hyperic.hq.grouping.CritterTranslator
import org.hyperic.hq.grouping.CritterList
import org.hyperic.hq.grouping.CritterType
import org.hyperic.hq.grouping.prop.CritterPropDescription
import org.hyperic.hq.grouping.prop.StringCritterProp
import org.hyperic.dao.DAOFactory

class CageController 
	extends BaseController
{
    private CritterRegistry _registry = CritterRegistry.getRegistry()
    
    protected void init() {
        onlyAllowSuperUsers()
        setXMLMethods(['peek'])
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
    
    def list(params) {
        def res = new StringBuilder()
        
        for (c in _registry.critterTypes) {
            res << "${c.class.name}:\n"
            res << "    Name:        ${c.name}\n"
            res << "    Description: ${c.description}\n\n"
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
    
    def peek(xmlOut, params) {
        def xmlIn = new XmlParser().parseText(getUpload('args'))

        def critters = []
        for (critterDef in xmlIn.critter) {
            def critterType = findCritterType(critterDef.'@class')
            
            if (critterType == null) {
                xmlOut.error("Unable to find critter class [${critterDef.'@class'}]")
                return xmlOut
            }
            
            def props = []
            for (propDef in critterDef.children()) {
                log.info "${propDef.name()}"
                if (propDef.name() == 'string') {
                    props << new StringCritterProp(propDef.text())
                } else {
                    xmlOut.error("Unhandled prop type: ${propDef.'@type'}")
                }
            }
            critters << critterType.newInstance(props)
        }

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

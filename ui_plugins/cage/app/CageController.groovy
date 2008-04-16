import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.grouping.CritterRegistry
import org.hyperic.hq.grouping.prop.StringCritterProp
import org.hyperic.hq.grouping.CritterTranslator
import org.hyperic.hq.grouping.CritterList
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
    
    def peek(xmlOut, params) {
        def xmlIn = new XmlParser().parseText(getUpload('args'))

        def critters = []
        for (critterDef in xmlIn.critter) {
            def critterType = _registry.getCritterTypes().find { t -> 
                t.class.name == critterDef.'@class'
            }
            
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

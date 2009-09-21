import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig
import org.hyperic.hq.events.NoOpAction
import org.hyperic.hq.hqapi1.ErrorCode
import org.hyperic.util.config.ConfigResponse

class EscalationController extends ApiController {

    private EMAIL_NOTIFY_TYPE = [1:"email", 2:"users", 3:"roles"]

    private String getNotification(type, id) {
        if (type == 1) {
            return id
        } else if (type == 2) {
            return getUser(id.toInteger(), null)?.name
        } else if (type == 3) {
            return getRole(id.toInteger(), null)?.name
        }
        return null
    }

    private Closure getEscalationXML(e) {
        { doc -> 
            Escalation(id :           e.id,
                       name :         e.name,
                       description :  e.description,
                       pauseAllowed : e.pauseAllowed,
                       maxPauseTime : e.maxPauseTime,
                       notifyAll :    e.notifyAll,
                       repeat :       e.repeat) {
                for (ea in e.actions) {
                    def a = ea.action
                    def actionType = (a.className  =~ /.+\.([A-Za-z]+)/) [0][1]
                    switch (actionType) {
                    case 'EmailAction' :
                        EmailActionConfig cfg = new EmailActionConfig()
                        cfg.init(ConfigResponse.decode(a.config))
                        Action(id :         a.id,
                               wait :       ea.waitTime,
                               actionType : actionType,
                               sms :        cfg.sms,
                               notifyType : EMAIL_NOTIFY_TYPE[cfg.type]) {
                            for (n in cfg.names.split(',')) {
                                if (n.length() > 0)
                                    Notify(name : getNotification(cfg.type, n))
                            }
                        }
                        break;
                    case 'SyslogAction':
                        SyslogActionConfig cfg = new SyslogActionConfig()
                        cfg.init(ConfigResponse.decode(a.config))
                        Action(id : a.id,
                               wait : ea.waitTime,
                               actionType: actionType,
                               syslogMeta: cfg.meta,
                               syslogProduct: cfg.product,
                               syslogVersion: cfg.version)
                        break;
                    default :
                        Action(id :         a.id,
                               wait :       ea.waitTime,
                               actionType : actionType
                        )
                        break;
                    }
                }
            }
        }
    }

    private getEscalation(Integer id, String name) {
        // TODO: Work around deficiency in EscalationHelper
        def esc = escalationHelper.getEscalation(id, name)
        try {
            esc.name
        } catch (Throwable t) {
            return null
        }
        return esc
    }

    def get(params) {
        def id = params.getOne("id")?.toInteger()
        def name = params.getOne("name")
        
        def esc = getEscalation(id, name)
        renderXml() {
            out << EscalationResponse() {
                if (!esc) {
                    out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                         "Escalation with name='" + name +
                                         "' id=" + id + " not found")
                }
                else {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }

    def list(params) {
        renderXml() {
            out << EscalationsResponse() {
                out << getSuccessXML()
                for (e in escalationHelper.allEscalations.sort {a, b -> a.name <=> b.name}) {
                    out << getEscalationXML(e)
                }
            }
        }
    }

    def delete(params) {
        def id = params.getOne("id")?.toInteger()
        def failureXml

        if (id == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Required id parameter not given.")
        }

        def esc = getEscalation(id, null)
        if (!esc) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Escalation with id " + id + " not found")
        } else {
            escalationHelper.deleteEscalation(id)
        }

        renderXml() {
            out << StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }
    
    def create(params) {

        def failureXml
        def syncRequest = new XmlParser().parseText(getPostData())
        def esc

        if (syncRequest['Escalation'].size() != 1) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Single escalation not passed to create");
        } else {

            def xmlEsc       = syncRequest['Escalation'][0]
            def id           = xmlEsc.'@id'?.toInteger()
            def name         = xmlEsc.'@name'
            def desc         = xmlEsc.'@description'
            def pauseAllowed = xmlEsc.'@pauseAllowed'.toBoolean()
            def maxWaitTime  = xmlEsc.'@maxPauseTime'.toLong()
            def notifyAll    = xmlEsc.'@notifyAll'.toBoolean()
            def repeat       = xmlEsc.'@repeat'.toBoolean()

            esc = getEscalation(id, name)

            if (!name || name.length() == 0) {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "Invalid name " + name + " for " +
                                           "escalation")
            } else if (esc) {
                failureXml = getFailureXML(ErrorCode.OBJECT_EXISTS,
                                           "Escalation " + esc.name + " already " +
                                           "exists with id = " + esc.id)
            } else {
                esc = escalationHelper.createEscalation(name, desc,
                                                        pauseAllowed,
                                                        maxWaitTime, notifyAll,
                                                        repeat)

                failureXml = syncActions(esc, xmlEsc['Action'])
            }
        }

        renderXml() {
            out << EscalationResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }
    
    def update(params) {
        def failureXml
        def syncRequest = new XmlParser().parseText(getPostData())
        def esc

        if (syncRequest['Escalation'].size() != 1) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Single escalation not passed to update");
        } else {

            def xmlEsc       = syncRequest['Escalation'][0]
            def id           = xmlEsc.'@id'?.toInteger()
            def name         = xmlEsc.'@name'
            def desc         = xmlEsc.'@description'
            def pauseAllowed = xmlEsc.'@pauseAllowed'.toBoolean()
            def maxWaitTime  = xmlEsc.'@maxPauseTime'.toLong()
            def notifyAll    = xmlEsc.'@notifyAll'.toBoolean()
            def repeat       = xmlEsc.'@repeat'.toBoolean()
        
            esc = getEscalation(id, name)

            if (!esc) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find escalation with id=" +
                                           id + " name=" + name)
            } else {
                escalationHelper.updateEscalation(esc, name, desc, pauseAllowed,
                                                  maxWaitTime, notifyAll, repeat)
            
                failureXml = syncActions(esc, xmlEsc['Action'])
            }
        }

        renderXml() {
            out << EscalationResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }
    
    def sync(params) {
        def failureXml
        def syncRequest = new XmlParser().parseText(getPostData())
        
        for (xmlEsc in syncRequest['Escalation']) {
            def id           = xmlEsc.'@id'?.toInteger()
            def name         = xmlEsc.'@name'
            def desc         = xmlEsc.'@description'
            def pauseAllowed = xmlEsc.'@pauseAllowed'.toBoolean()
            def maxWaitTime  = xmlEsc.'@maxPauseTime'.toLong()
            def notifyAll    = xmlEsc.'@notifyAll'.toBoolean()
            def repeat       = xmlEsc.'@repeat'.toBoolean()
        
            def esc = getEscalation(id, name)

            if (!name || name.length() == 0) {
                failureXml = getFailureXml(ErrorCode.INVALID_PARAMETERS,
                                           "Name not given")
            } else if (esc) {
                escalationHelper.updateEscalation(esc, name, desc, pauseAllowed,
                                                  maxWaitTime, notifyAll,
                                                  repeat)
            } else {
                esc = escalationHelper.createEscalation(name, desc,
                                                        pauseAllowed,
                                                        maxWaitTime, notifyAll,
                                                        repeat)
            }

            failureXml = syncActions(esc, xmlEsc['Action'])
        }

        renderXml() {
            out << StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }
    
    private syncActions(esc, actions) {

        def failureXml = null

        // Remove all actions
        while (esc.actions.size() > 0) {
            escalationHelper.deleteAction(esc, esc.actions.get(0).action.id)
        }

        for (xmlAct in actions) {

            def action = null
            switch (xmlAct.'@actionType') {
            case 'EmailAction' :
                // Create Email action
                action = Class.forName(EmailActionConfig.implementor).newInstance()
                action.setSms(xmlAct.'@sms'.toBoolean())

                def type = xmlAct.'@notifyType'
                if (type == EMAIL_NOTIFY_TYPE[3]) {
                    action.setType(EmailActionConfig.TYPE_ROLES)
                } else if (type == EMAIL_NOTIFY_TYPE[2]) {
                    action.setType(EmailActionConfig.TYPE_USERS)
                } else if (type == EMAIL_NOTIFY_TYPE[1]) {
                    action.setType(EmailActionConfig.TYPE_EMAILS)
                }

                // Get all of the names to notify
                def names = xmlAct['Notify'].collect { notifyDef ->
                    def name = notifyDef.'@name'
                    switch (action.getType()) {
                        case EmailActionConfig.TYPE_ROLES:
                            def role = getRole(null, name)
                            if (!role) {
                                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                           "Cannot find role " + name)
                            } else {
                                name = role.id
                            }
                            break;
                        case EmailActionConfig.TYPE_USERS:
                            def u = getUser(null, name)
                            if (!u) {
                                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                                           "Cannot find user " + name)
                            } else {
                                name = u.id
                            }
                            break;
                        case EmailActionConfig.TYPE_EMAILS:
                        default:
                            break;
                    }
                    name
                }

                action.setNames(names.join(","))
                break
            case 'SyslogAction' :
                action = Class.forName(SyslogActionConfig._implementor).newInstance()
                action.setMeta(xmlAct.'@syslogMeta')
                action.setProduct(xmlAct.'@syslogProduct')
                action.setVersion(xmlAct.'@syslogVersion')
                break
            case 'NoOpAction' :
                action = new NoOpAction()
                break
            }

            if (failureXml != null) {
                return failureXml
            }

            if (action != null) {
                escalationHelper.addAction(esc, action, xmlAct.'@wait'.toLong())
            }
        }

        return null
    }
}

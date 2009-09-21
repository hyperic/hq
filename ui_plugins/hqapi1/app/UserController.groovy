import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.hqapi1.ErrorCode;

class UserController extends ApiController {

    private Closure getUserXML(u) {
        { doc -> 
            User(id          : u.id,
                 name        : u.name,
                 firstName   : u.firstName,
                 lastName    : u.lastName,
                 department  : (u.department ? u.department : ''),
                 emailAddress: u.emailAddress,
                 SMSAddress  : (u.SMSAddress ? u.SMSAddress : ''),
                 phoneNumber : (u.phoneNumber ? u.phoneNumber : ''),
                 active      : u.active,
                 htmlEmail   : u.htmlEmail,
                 passwordHash: u.password)
        }
    }

    def list(params) {
        renderXml() {
            out << UsersResponse() {
                out << getSuccessXML()
                for (u in userHelper.allUsers.sort {a, b -> a.name <=> b.name}) {
                    out << getUserXML(u)
                }
            }
        }
    }

    def get(params) {
        def id   = params.getOne("id")?.toInteger()
        def name = params.getOne("name")
        def u = getUser(id, name)
        
        renderXml() {
            UserResponse() {
                if (!u) {
                    out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                         "User with id=" + id + " name='" +
                                         name + "' not found")
                } else {
                    out << getSuccessXML()
                    out << getUserXML(u)
                }
            }
        }
    }

    def create(params) {
        // Required attributes
        def name     = params.getOne("name")
        def password = params.getOne("password")
        def first    = params.getOne("firstName")
        def last     = params.getOne("lastName")
        def email    = params.getOne("emailAddress")

        // Optional attributes
        def htmlEmail = params.getOne("htmlEmail", "false").toBoolean()
        def active    = params.getOne("active", "false").toBoolean()
        def dept      = params.getOne("department")
        def phone     = params.getOne("phoneNumber")
        def sms       = params.getOne("SMSAddress")

        // We require the user to authenticate via built in JDBC
        def dsn = "CAM"

        def failureXml
        def newUser
        if (!name || !password || !first || !last || !email) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        } else {
            try {
                def existing = getUser(null, name)
                if (existing) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_EXISTS,
                                               "User '" + name + "' already exists")
                } else {
                    newUser = userHelper.createUser(name, password, active,
                                                    dsn, dept, email, first,
                                                    last, phone, sms, htmlEmail)
                }
            } catch (PermissionException e) {
                log.debug("Permission denied [${user.name}]", e)
                failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e);
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }
        
        renderXml() {
            UserResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getUserXML(newUser)
                }
            }
        }
    }

    def delete(params) {
        def id = params.getOne("id")?.toInteger()
        def name = params.getOne("name")
        def existing = getUser(id, name)
        def failureXml
        
        if (!existing) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Unable to find user id=" + id +
                                       " name='" + name + "'")
        } else {
            try {
                existing.remove(user)
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e)
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }
        
        renderXml() {
            StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }
    
    def sync(params) {
        def failureXml
        try {
            def syncRequest = new XmlParser().parseText(getPostData())
            for (xmlUser in syncRequest['User']) {
                def existing = getUser(xmlUser.'@id'?.toInteger(),
                                       xmlUser.'@name')
                if (existing) {
                    userHelper.updateUser(existing,
                                          xmlUser.'@active'?.toBoolean(),
                                          "CAM", // Dsn
                                          xmlUser.'@department',
                                          xmlUser.'@emailAddress',
                                          xmlUser.'@firstName',
                                          xmlUser.'@lastName',
                                          xmlUser.'@phoneNumber',
                                          xmlUser.'@SMSAddress',
                                          xmlUser.'@htmlEmail'?.toBoolean())
                    def hash = xmlUser.'@passwordHash'
                    if (hash) {
                    	existing.updatePassword(user, hash)
                    }     	
                } else {
                    if (!xmlUser.'@name' || !xmlUser.'@firstName' ||
                        !xmlUser.'@lastName' || !xmlUser.'@emailAddress') {
                        failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
                    } else {
                        def newUser = 
                            userHelper.createUser(xmlUser.'@name',
                                                  xmlUser.'@active'?.toBoolean(),
                                                  "CAM", // Dsn
                                                  xmlUser.'@department',
                                                  xmlUser.'@emailAddress',
                                                  xmlUser.'@firstName',
                                                  xmlUser.'@lastName',
                                                  xmlUser.'@phoneNumber',
                                                  xmlUser.'@SMSAddress',
                                                  xmlUser.'@htmlEmail'?.toBoolean())
                        def hash = xmlUser.'@passwordHash'
                        if (hash) {
                            newUser.updatePassword(user, hash)
                        }     	
                    }
                }
            }
        } catch (PermissionException e) {
            log.debug("Permission denied [${user.name}]", e)
            failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e)
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }

        renderXml() {
            StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def changePassword(params) {
        def id = params.getOne("id")?.toInteger()
        def name = params.getOne("name")
        def password = params.getOne("password")

        def failureXml

        if (!password || password.length() == 0) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        } else {

            def existing = getUser(id, name)
            if (!existing) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Unable to find user id=" + id +
                                           " name='" + name + "'")
            } else {
                try {
                    existing.changePassword(user, password)
                } catch (PermissionException e) {
                    log.debug("Permission denied [${user.name}]");
                    failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                } catch (Exception e) {
                    log.error("UnexpectedError: " + e.getMessage(), e)
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
                }
            }
        }

        renderXml() {
            StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }
}

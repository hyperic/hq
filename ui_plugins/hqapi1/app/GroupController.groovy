import org.hyperic.hq.hqapi1.ErrorCode
import org.hyperic.hq.authz.shared.PermissionException

class GroupController extends ApiController {

    private Closure getGroupXML(g) {
        { doc ->
            Group(id          : g.id,
                  name        : g.name,
                  description : g.description,
                  location    : g.location) {
                if (g.resourcePrototype) {
                    ResourcePrototype(id   : g.resourcePrototype.id,
                                      name : g.resourcePrototype.name)
                }
                for (r in g.resources) {
                    Resource(id : r.id,
                             name : r.name)
                }
                for (r in g.roles) {
                    Role(id : r.id,
                         name : r.name)
                }
            }
        }
    }

    def get(params) {
        def id = params.getOne('id')?.toInteger()
        def name = params.getOne('name')

        def group
        def failureXml = null
        if (id == null && name == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        } else {
            group = getGroup(id, name)
            if (!group) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Group with id=" + id + " name='" +
                                           name + "' not found")
            }
        }

        renderXml() {
            GroupResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getGroupXML(group)
                }
            }
        }
    }

    def delete(params) {
        def id = params.getOne('id')?.toInteger()

        if (id == null) {
            renderXml() {
                out << StatusResponse() {
                    out << getFailureXML(ErrorCode.INVALID_PARAMETERS)
                }
            }
            return
        }

        def group = getGroup(id, null)
        def failureXml = null
        if (!group) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Group with id " + id + " not found")
        } else {

            if (group.system) {
                failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED,
                                           "Cannot delete system group " +
                                           group.name)
            } else {
                try {
                    group.remove(user)
                } catch (PermissionException e) {
                    failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                } catch (Exception e) {
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
                }
            }
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

    def sync(params) {
        def syncRequest = new XmlParser().parseText(getPostData())

        def groups = []

        for (xmlGroup in syncRequest['Group']) {
            // Check for existance
            def existing = getGroup(xmlGroup.'@id'?.toInteger(),
                                    xmlGroup.'@name')

            def failureXml = null
            def roles = [] as Set
            def resources = []
            def prototype = null

            if (!xmlGroup.'@name') {
                failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                           "Group name required.")
            }

            if (existing?.isSystem()) {
                failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED,
                                           "Cannot update system group " +
                                           "with id=" + existing.id)
            }

            // Look up prototype
            def xmlPrototype = xmlGroup.'ResourcePrototype'
            if (!xmlPrototype) {
                log.debug("No prototype found for " + xmlGroup.'@name')
            } else {
                prototype = resourceHelper.find(prototype:xmlPrototype.'@name')
                if (!prototype) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Unable to find prototype with " +
                                               "name " + xmlPrototype.'@name')
                } else {
                    if (existing) {
                        if (existing.resourcePrototype) {
                            // If already compatible - ensure the same type.
                            if (!existing.resourcePrototype.name.equals(prototype.name)) {
                                failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED,
                                                           "Cannot change group type from " +
                                                           existing.resourcePrototype.name +
                                                           " to " + prototype.name)
                            }
                        }
                    }
                }
            }

            // Look up roles
            for (xmlRole in xmlGroup['Role']) {
                log.debug("Found role "+ xmlRole.'@name')

                def role = getRole(xmlRole.'@id'?.toInteger(),
                                   xmlRole.'@name')
                if (!role) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Unable to find role with id " +
                                               xmlRole.'@id' + " and name " +
                                               xmlRole.'@name')
                } else {
                    roles.add(role)
                }
            }

            // Look up resources
            for (xmlResource in xmlGroup['Resource']) {
                log.debug("Found resource " + xmlResource.'@name')

                def resource = getResource(xmlResource.'@id'?.toInteger());

                if (!resource) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Unable to find resource with id " +
                                               xmlResource.'@id')
                } else {
                    if (prototype) {
                        if (!resource.prototype.name.equals(prototype.name)) {
                            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                                       "Resource " + resource.name +
                                                       " is not of type " +
                                                       prototype.name)
                        }
                    }

                    // Avoid duplicate resources which causes constraint violations
                    // TODO: Backend should handle this case.
                    if (!resources.contains(resource)) {
                        resources.add(resource)
                    }
                }
            }

            if (!failureXml) {
                if (existing) {
                    // TODO: This needs to be moved out to the Manager.
                    existing.setRoles(roles)
                    existing.setResources(user, resources)
                    existing.updateGroup(user,
                                         xmlGroup.'@name',
                                         xmlGroup.'@description',
                                         xmlGroup.'@location')
                    groups << existing
                } else {
                    // TODO: private groups
                    def group = resourceHelper.createGroup(xmlGroup.'@name',
                                                           xmlGroup.'@description',
                                                           xmlGroup.'@location',
                                                           prototype, roles,
                                                           resources,
                                                           false)
                    groups << group
                }
            }

            // If any group is unable to be synced exit with an error.
            if (failureXml) {
                renderXml() {
                    out << GroupsResponse() {
                        out << failureXml
                    }
                }
                return
            }
        }

        renderXml() {
            out << GroupsResponse() {
                out << getSuccessXML()
                for (g in  groups) {
                    out << getGroupXML(g)
                }
            }
        }
    }

    def list(params) {
        def compatible = params.getOne('compatible')?.toBoolean()

        def groups = resourceHelper.findViewableGroups()

        if (compatible != null) {
            if (compatible)
                groups = groups.grep { it.resourcePrototype != null }
            else
                groups = groups.grep { it.resourcePrototype == null }
        }

        renderXml() {
            out << GroupsResponse() {
                out << getSuccessXML()
                for (g in  groups.sort {a, b -> a.name <=> b.name}) {
                    out << getGroupXML(g)
                }
            }
        }
    }
}
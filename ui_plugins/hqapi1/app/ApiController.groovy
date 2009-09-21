import org.hyperic.hq.hqu.rendit.BaseController

import groovy.xml.StreamingMarkupBuilder
import org.hyperic.hq.hqapi1.ErrorCode

class ApiController extends BaseController {

    // Statistics object
    private static _methodStats = [:]
    private static final STATS_LOCK = new Object()
    
    /**
     * Get the ResponseStatus Success XML.
     */
    protected Closure getSuccessXML() {
        { doc -> 
            Status("Success")
        }
    }

    /**
     * Get the ResponseStatus Failure XML.
     */
    protected Closure getFailureXML(ErrorCode code) {
        return getFailureXML(code, code.getReasonText())
    }

    /**
     * Get the ResponseStatus Failure XML with the specified reason text.
     */
    protected Closure getFailureXML(ErrorCode code, String reason) {
        { doc ->
            Status("Failure")
            Error() {
                ErrorCode(code.getErrorCode())
                ReasonText(reason)
            }
        }
    }

    /**
     * Get an Agent by id, address or port.
     */
    protected getAgent(id, address, port) {
        if (id) {
            return agentHelper.getAgent(id)
        } else if (address && port) {
            return agentHelper.getAgent(address, port)
        }
        return null
    }

    /**
     * Get the resource based on the given id.  If the resource is not found,
     * null is returned.
     */
    protected getResource(id) {

        if (id == null) {
            return null
        }

        def resource = resourceHelper.findById(id)

        if (!resource) {
            return null
        } else {
            //XXX: ResourceHelper needs some work here..
            try {
                resource.name // Check the object really exists
                resource.entityId // Check the object is an appdef object
                return resource
            } catch (Throwable t) {
                return null
            }
        }
    }

    /**
     * Get a User by id or name
     * @return The user by the given id.  If the passed in id is null then
     * the user by the given name is returned.  If no user could be found
     * for either the id or name, null is returned.
     */
    protected getUser(Integer id, String name) {
        if (id != null) {
            return userHelper.getUser(id)
        } else {
            return userHelper.findUser(name)
        }
    }

    /**
     * Get a Role by id or name
     *
     * @return The role by the given id.  If the passed in id is null then
     * the Role by the given name is returned.  If no role could be found
     * for either the id or name, null is returned.
     */
    protected getRole(Integer id, String name) {
        if (id != null) {
            return roleHelper.getRoleById(id)
        } else {
            return roleHelper.findRoleByName(name)
        }
    }

    /**
     * Get a group by id or name
     *
     * @return The group with the given id or name.  If the passed in id is null,
     * then the group with the given name is returned.  If no group could be found
     * for either the id or name, null is returned.
     */
    protected getGroup(Integer id, String name) {
        if (id != null) {
            return resourceHelper.findGroup(id)
        } else {
            return resourceHelper.findGroupByName(name)
        }
    }

    /**
     * Get POST data from the client.
     */
    protected getPostData() {
        // Check for multipart/form-data
        if (invokeArgs.request.contentType.contains("multipart")) {
            return getUpload('postdata')
        } else {
            return invokeArgs.request.inputStream.getText()
        }
    }

    def dispatchRequest() {

        long start = System.currentTimeMillis()
        super.dispatchRequest()
        long total = System.currentTimeMillis() - start;

        synchronized(STATS_LOCK) {
            def method = controllerName + "." + action
            def stats = _methodStats[method]
            if (!stats) {
                stats = [calls: 0, maxTime: 0, minTime: Integer.MAX_VALUE,
                         totalTime: 0]
            }

            stats.calls++
            stats.totalTime += total
            if (stats.maxTime < total) {
                stats.maxTime = total
            }
            if (stats.minTime > total) {
                stats.minTime = total
            }

            _methodStats[method] = stats
        }
    }
        
    def index(params) {
        render(locals:[plugin: getPlugin(),
               stats: _methodStats])
    }
}

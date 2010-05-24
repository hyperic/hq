import org.hyperic.hq.hqu.rendit.BaseController

class SummaryController
	extends BaseController {

    /**
     * Get the custom properties for a resource
     * key = Cprop description
     * value = Cprop value
     */
    private getCustomProperties(resource) {
        def config = resource.config
        def props = [:]

        config.each { k, v ->
            if (v.type.equals("cprop")) {
                props[v.description] = v.value
            }
        }
        props
    }

    def index(params) {
        def resourceId = params.getOne('id')?.toInteger()

        if (!resourceId) {
            render(inline: "No resource selected")
            return
        }

        def r = resourceHelper.findById(resourceId)
        def host, hostProps
        def vm = null, vmProps = null
        def resource = null, resourceProps = null

        if (r.prototype.name == "VMware vSphere VM") {
            vm = r
            vmProps = getCustomProperties(r)
            host = VsphereDAO.getHostResourceByVMResource(user, vm)
            hostProps = getCustomProperties(host)
        } else if (r.prototype.name == "VMware vSphere Host") {
            host = r
            hostProps = getCustomProperties(r)
        } else {
            // HQ resource (currently an HQ Server type)
            resource = r
            resourceProps = getCustomProperties(r)

            def platform = r.toServer().platform
            vm = VsphereDAO.getVMByPlatform(platform)
            vmProps = getCustomProperties(vm)
            host = VsphereDAO.getHostResourceByVMResource(user, vm)
            hostProps = getCustomProperties(host)
        }

        render(locals:[ host : host,
                        hostProps : hostProps,
                        vm : vm,
                        vmProps : vmProps,
                        resource : resource,
                        resourceProps : resourceProps])
    }
}
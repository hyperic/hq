import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.measurement.MeasurementConstants

class VsphereController 
    extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
    }

    /**
     * Generate a 'VMware vSphere Host' node.
     */
    private getHostNode(host) {
        getNode(host.id, host.name, "icon icon-host", true, new JSONArray())
    }

    private getHQNode(resource) {
        getNode(resource.id, resource.name, "icon icon-hq", null, null)
    }
    
    private getNode(id, name, classes, collapsed, children) {
        JSONObject node = new JSONObject();
        
        node.put("id", id)
        node.put("text", name)
        
        if (classes) {
            node.put("classes", classes)
        }
        
        if (children) {
            node.put("children", children)
            if (collapsed) {
                node.put("collapsed", true)
            }
        }
        
        node
    }
    
    private getAvailability(resource) {
        def availMetric = resource.getAvailabilityMeasurement()
        def mv = availMetric.getLastDataPoint()

        mv.value
    }

    private getAssociatedPlatform(vm) {
        def config = vm.config
        def mac = config['macAddress']
        def platform = null
        
        if (mac && mac['value']) {
            platform = VsphereDAO.getPlatformByMAC(mac['value'])
        }
        
        platform
    }
    
    /**
     * Generate a 'VMware vSphere VM' node.
     */
    private getVMNode(vm) {
        def avail = getAvailability(vm)
        def icon
        
        if (avail == MeasurementConstants.AVAIL_UP) {
            icon = "icon icon-vmOn"
        } else if (avail == MeasurementConstants.AVAIL_DOWN) {
            icon = "icon icon-vmOff"
        } else if (avail == MeasurementConstants.AVAIL_PAUSED) {
            icon = "icon icon-vmSuspended"
        } else {
            icon = "icon icon-vm"
        }

        // Check if this VM has a corresponding platform in HQ and include it
        // directly.  May want to make this async in the future.
        def platform = getAssociatedPlatform(vm)
        def children
            
        if (platform && platform.servers && platform.servers.size() > 0) {
            children = new JSONArray()
        }

        getNode(vm.id, vm.name, icon, true, children)
    }
    
    private getChildNodes(id) {
        def nodes = new JSONArray()
        
        if (id) {
            def node = resourceHelper.findById(id)
    
            if (node && node.prototype) {
                if (node.prototype.name == "VMware vSphere Host") {
                    def vms = VsphereDAO.getVMsByHost(user, node.name)
        
                    for (vm in vms.sort{ a, b -> a.name.toLowerCase() <=> b.name.toLowerCase()}) {
                        nodes.put(getVMNode(vm))
                    }
                } else if (node.prototype.name == "VMware vSphere VM") {
                    def platform = getAssociatedPlatform(node)
                    
                    if (platform && platform.servers) {
                        for (server in platform.servers.sort{ a, b -> a.resource.name.toLowerCase() <=> b.resource.name.toLowerCase()}) {
                            if (!server.serverType.virtual) {
                                def res = server.resource
                                
                                nodes.put(getHQNode(res))
                            }
                        }
                    }
                }
            }
        } else {
            def hosts = resourceHelper.find(byPrototype: "VMware vSphere Host")
            
            if (hosts) {
                for (host in hosts.sort { a, b -> a.name <=> b.name}) {
                    nodes.put(getHostNode(host))
                }
            }
        }
        
        nodes
    }
    
    private getAncestors(id) {
        def resource = resourceHelper.findById(id)
        def openNodes = []
                         
        if (resource && resource.prototype) {
            if (resource.prototype.name == "VMware vSphere VM") {
                def host = VsphereDAO.getHostResourceByVMResource(user, resource)
                
                openNodes << host.id
            } else if (resource.prototype.name != "VMware vSphere Host") {
                def platform = resource.toServer().platform
                def vm = VsphereDAO.getVMByPlatform(platform)
               
                openNodes << vm.id
                
                def host = VsphereDAO.getHostResourceByVMResource(user, vm)
                
                openNodes << host.id
            }
        }
        
        return openNodes
    }
    
    private generateBranch(nodes, openNodes) {
        for (int x = 0; x < nodes.length(); x++) {
            def node = nodes.getJSONObject(x)
            def id = node.get("id")
            
            if (openNodes.find { it == id }) {
                def children = getChildNodes(id)
                
                if (children.length() > 0) {
                    generateBranch(children, openNodes)
                    
                    node.put("collapsed", false)
                }
                
                node.put("children", children)
            }
        }
    }
    
    def findByName(params) {
        def resourceName = params.getOne('name')
        def result = new JSONArray()
        
        if (resourceName) {
            def platforms = VsphereDAO.getVirtualPlatformsWithNameLike(resourceName)

            for (platform in platforms.sort{ a, b -> a.resource.name.toLowerCase() <=> b.resource.name.toLowerCase()}) {
                def listItem = new JSONObject()
                
                listItem.put("id", platform.resource.id)
                listItem.put("value", platform.resource.name)
                
                result.put(listItem)
            }
        }
        
        render(inline:"${result}", contentType:'text/json-comment-filtered')
    }
    
    def inventory(params) {
        def nodeId = params.getOne('nodeId')?.toInteger()
        def nodes = new JSONArray()
        def result = new JSONObject()
        
        if (nodeId) {
            nodes = getChildNodes(nodeId)
        } else {
            def openNodes = []  
            def selectedId = params.getOne('sn')?.toInteger()
            
            for (String id in params.get('on[]')) {
                openNodes << Integer.valueOf(id)
            }
            
            if (selectedId) {
                openNodes = (openNodes + getAncestors(selectedId)).unique()
            }

            nodes = getChildNodes(null)
            
            if (nodes.length() > 0 && openNodes.size() > 0) {
                generateBranch(nodes, openNodes)
            } 
        }

        result.put("payload", nodes)

        render(inline:"${result}", contentType:'text/json-comment-filtered')
    }

    def index(params) {
        def openNodes = []
        def selectedId = params.getOne('sn')?.toInteger()
        
        if (selectedId) {
            openNodes = getAncestors(selectedId)
        }
        
        def nodes = getChildNodes(null)
                    
        if (nodes.length() > 0 && openNodes.size() > 0) {
            generateBranch(nodes, openNodes)
        } 

        render(locals:[payload: nodes])
    }
}

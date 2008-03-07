import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as RGroupMan
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as ResourceMan
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.grouping.server.session.GroupManagerEJBImpl as GroupMan
import org.hyperic.hq.appdef.shared.AppdefGroupValue
import org.hyperic.hq.appdef.shared.AppdefEntityConstants


class GroupController extends BaseController {
    private rgroupMan    = RGroupMan.one
    private groupMan     = GroupMan.one
    private resourceMan  = ResourceMan.one
    
    private def groupTypeToName = [
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP):  'applications',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP):  'groups',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS):  'resources',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS):  'compat',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC): 'compat'
    ]

    private def typeToMethods = [
        compat:  [create: { groupDef -> createCompatGroup(groupDef) },
                  getPrototypes: { groupDef -> getCompatPrototypes(groupDef) },
                  updateGroupType: { ResourceGroup g, groupDef ->
                      updateCompatGroupType(g, groupDef)
                  }],
        resources:  [create: { groupDef -> createMixedGroup(groupDef) },
                     getPrototypes: { groupDef -> getAllAppdefPrototypes() },
                     updateGroupType: { ResourceGroup g, groupDef ->
                         updateMixedGroupType(g, groupDef)
                     }],
        applications:  [create: { groupDef -> createAppGroup(groupDef) },
                        getPrototypes: { [] /* Special cased */ },
                        updateGroupType: { ResourceGroup g, groupDef ->
                            updateAppGroupType(g, groupDef)
                        }],
        groups: [create: { groupDef -> createGroupOfGroups(groupDef) },
                           getPrototypes: { [] /* Special cased */ }, 
                           updateGroupType: { ResourceGroup g, groupDef ->
                               updateGroupOfGroupsType(g, groupDef)
                           }],
    ]
    
    private def groupNameToType = [
       applications: AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
       groups:       AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP, 
       resources:    AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
    ]
    
    private _allPrototypes
    private _allResourcesByProto = [:]
    
    def GroupController() {
        onlyAllowSuperUsers()
        setXMLMethods(['list', 'sync'])
    }

    private getAllAppdefPrototypes() {
        if (_allPrototypes == null)
            _allPrototypes = resourceHelper.findAllAppdefPrototypes()
        _allPrototypes
    }
    
    private List getAllResourcesFor(Resource proto) {
        def exist = _allResourcesByProto[proto]
        if (exist == null) {
            _allResourcesByProto[proto] = 
                resourceHelper.findResourcesOfType(proto) 
        }
        _allResourcesByProto[proto]
    }
    
    def sync(xmlOut, params) {
        def xmlDef = new XmlParser().parse(new StringReader(params.getOne('args')))
        def deleteMissing = xmlDef.'@deleteMissing'?.toBoolean()
        

        def found     = []
        def allGroups = resourceHelper.findAllGroups()
        for (groupDef in xmlDef.group) {
            def groupName = groupDef.'@name'
            def group = allGroups.find { it.name == groupName }
                
            if (group)
                found << group
            syncGroup(group, groupDef)
        }
        
        if (deleteMissing) {
            def toDelete  = allGroups - found

            for (group in toDelete) {
                if (group.isSystem()) {
                    log.warn "Not deleting system group [${group.name}]"
                    continue
                }
                rgroupMan.removeResourceGroup(user.valueObject,
                                              group.valueObject)
            }
        }
        
        xmlOut.success()
        xmlOut
    }
    
    /**
     * Get a list of Resource's which are prototypes available to the 
     * be associated with the contents of the group.
     */
    private List getCompatPrototypes(def groupDef) {
        def protoName = groupDef.'@resourceType'
        Resource proto = getAllAppdefPrototypes().find { Resource it -> 
            it.name == protoName
        }
        
        if (!proto)
            throw new Exception("Resource type [$protoName] not found")

        // Compatable groups only have the type itself as available
        [proto]
    }
    
    private List getAppPrototypes(def groupDef) {
        def pInfo = PageInfo.getAll(ResourceSortField.NAME, true)
        resourceMan.findResourcesOfType(AuthzConstants.authzApplicationProto,
                                        pInfo)
    }
    
    private void updateAppGroupType(ResourceGroup group, def groupDef) {
        if (group.groupType != AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP ||
            group.groupEntType != -1 ||
            group.groupEntResType != -1)
        {
            groupMan.updateGroupType(user, group, 
                                     AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
                                     -1, -1)
        }
    }

    private void updateGroupOfGroupsType(ResourceGroup group, def groupDef) {
        if (group.groupType != AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP ||
            group.groupEntType != AppdefEntityConstants.APPDEF_TYPE_GROUP ||
            group.groupEntResType != -1)
        {
            groupMan.updateGroupType(user, group, 
                                     AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP,
                                     AppdefEntityConstants.APPDEF_TYPE_GROUP,
                                     -1)
        }
    }
    
    private void updateMixedGroupType(ResourceGroup group, def groupDef) {
        if (group.groupType != AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS ||
            group.groupEntType != -1 ||
            group.groupEntResType != -1)
        {
            rgroupMan.updateGroupType(user, group, 
                                      AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
                                      -1, -1)
        }
    }
    
    private void updateCompatGroupType(ResourceGroup group, def groupDef) {
        def compatProto = getCompatPrototypes(groupDef)
        
        assert compatProto.size() == 1, 'Should have prototype here'
        compatProto = compatProto[0]

        def defGroupType
        if (compatProto.isServicePrototype()) {
            defGroupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
        } else if(compatProto.isPlatformPrototype() ||
                  compatProto.isServerPrototype()) 
        {
            defGroupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS
        } else {
            assert "Unhandled prototype -> appdef type conversion"
        }

        if (group.groupType != defGroupType ||
            group.groupEntType != compatProto.appdefType ||
            group.groupEntResType != compatProto.instanceId)
        {
            log.info "Changing group ${group.name} type to ${compatProto.name} " + 
                     "type=${group.groupType}"
            rgroupMan.updateGroupType(user, group, defGroupType,
                                      compatProto.appdefType,
                                      compatProto.instanceId)
        }
    }
    
    private void fillGroupProperties(AppdefGroupValue groupVal, def groupDef) {
        groupVal.name            = groupDef.'@name'
        groupVal.description     = groupDef.'@description'
        groupVal.location        = groupDef.'@location'
        groupVal.clusterId       = -1
        groupVal.groupEntType    = -1
        groupVal.groupEntResType = -1
        groupVal.clusterId       = -1
    }
    
    private ResourceGroup createAppGroup(def groupDef) {
        AppdefGroupValue groupVal = new AppdefGroupValue()

        fillGroupProperties(groupVal, groupDef)
        groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
        def resVal = groupMan.createGroup(user.valueObject, groupVal)
        rgroupMan.findResourceGroupById(user.valueObject, resVal.id)
    }
    
    private ResourceGroup createGroupOfGroups(def groupDef) {
        AppdefGroupValue groupVal = new AppdefGroupValue()

        fillGroupProperties(groupVal, groupDef)
        groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
        groupVal.groupEntType  = AppdefEntityConstants.APPDEF_TYPE_GROUP
        def resVal = groupMan.createGroup(user.valueObject, groupVal)
        rgroupMan.findResourceGroupById(user.valueObject, resVal.id)
    }

    private ResourceGroup createMixedGroup(def groupDef) {
        log.info "Creating mixed group [${groupDef.'@name'}]"
        AppdefGroupValue groupVal = new AppdefGroupValue()
        fillGroupProperties(groupVal, groupDef)
        groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS
        def resVal = groupMan.createGroup(user.valueObject, groupVal)
        rgroupMan.findResourceGroupById(user.valueObject, resVal.id)
    }
    
    private ResourceGroup createCompatGroup(def groupDef) {
        def protoName = groupDef.'@resourceType'
        if (!protoName) {
            throw new Exception("Group [${groupDef.'@name'}] is compat, and " +
                                "must specify resourceType=")
        }
        log.info "Creating group from ${protoName}"
        Resource proto = resourceHelper.findResourcePrototype(protoName)
        if (!proto) {
            throw new Exception("Resource type [$protoName] not found ")
        }
        
        AppdefGroupValue groupVal = new AppdefGroupValue()
        fillGroupProperties(groupVal, groupDef)
        if (proto.isPlatformPrototype()) {
            groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS
        } else if (proto.isServerPrototype()) {
            groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS
        } else if (proto.isServicePrototype()) {
            groupVal.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
        } else {
            throw new IllegalArgumentException("Resource type [$protoName] " +
                                               "unsupported as compat group")
        }
        groupVal.groupEntType    = proto.appdefType
        groupVal.groupEntResType = proto.instanceId
        def resVal = groupMan.createGroup(user.valueObject, groupVal)
        rgroupMan.findResourceGroupById(user.valueObject, resVal.id)
    }
    
    private boolean isGroupOfGroups(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
    }

    private boolean isGroupOfApps(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
    }
    
    private ResourceGroup syncGroup(ResourceGroup group, def groupDef) {
        def typeMethods
        if (!group) {
            // Create!
            def type = groupDef.'@type'
            typeMethods = typeToMethods[type]
            if (!typeMethods) {
                throw new IllegalArgumentException("Invalid group type [${type}]")
            }
            group = typeMethods.create(groupDef)
        } else { 
            typeMethods = typeToMethods[groupTypeToName[group.groupType]]
            typeMethods.updateGroupType(group, groupDef)
        }
        
        def shouldContain = []
        for (typeDef in groupDef.resourceType) {
            def typeName = typeDef.'@name' 
            if (isGroupOfGroups(group)) {
                if (typeName != 'Group') {
                    throw new Exception("Group ${group.name} is a group of " + 
                                        "groups, but resource type [${typeName}] " +
                                        "is not [Groups]")
                }
                List allResources = resourceHelper.findAllGroups() - [group]
                // Convert ResourceGroups to Resource
                allResources = allResources.resource
                shouldContain.addAll(syncResources(group, typeDef, allResources))
            } else if (isGroupOfApps(group)) {
                if (typeName != 'Application') {
                    throw new Exception("Group ${group.name} is a group of " +
                                        "applications, but resource type [$typeName] " +
                                        "is not [Application]")
                }

                List prototypes = getAppPrototypes(groupDef)
                for (proto in prototypes) {
                    List allResources = getAllResourcesFor(proto)
                    shouldContain.addAll(syncResources(group, typeDef, 
                                                       allResources))
                }
            } else {
                def validPrototypes = typeMethods.getPrototypes(groupDef)
                def typeMatcher = makeTypeMatcher(typeDef.'@name')
                def matchingTypes = validPrototypes.grep { Resource proto ->
                    typeMatcher(proto)
                }

                for (proto in matchingTypes) {
                    if (!proto.isVirtual()) {
                        List allResources = getAllResourcesFor(proto)
                        shouldContain.addAll(syncResources(group, typeDef, 
                                                           allResources))
                    }
                }
            }
        }
        
        group.setResources(user, shouldContain.unique() as List)
        group
    }
    
    /**
     * Make a closure which will return true when a prototype matches 
     * the given 'name'
     *
     * @param s:  A string that must equal the prototype name.  Wil aso 
     *            return true if it starts with regex: and the remainder
     *            matches the prototype name.
     */
    private Closure makeTypeMatcher(String s) {
        if (s.startsWith('regex')) {
            def regex = ~s[6..-1]
            return { Resource proto -> proto.name ==~ regex }
        } else {
            return { Resource proto -> proto.name == s }
        }
    }
    
    private Map makeResourceMatcher(def resourceDef) {
        def name = resourceDef.'@name'
        if (resourceDef.'@id' != null) {
            def id   = resourceDef.'@id'.toInteger()
            return [matches: { resource -> resource.id == id },  
                    wildcard: false]
        } else if (name.startsWith('regex')) {
            def regex = ~name[6..-1]
            return [matches: { resource -> resource.name ==~ regex },
                    wildcard: true]
        } else { 
            return [matches: { resource -> resource.name == name },
                    wildcard: false]
        }
    }
    
    private syncResources(ResourceGroup group, def typeDef, List allResources) {
        List shouldContain = []
        for (resourceDef in typeDef.resource) {
            def resourceMatcher = makeResourceMatcher(resourceDef)
            def matchingResources = allResources.grep { resource ->
                resourceMatcher.matches(resource)
            }

            if (!matchingResources && !resourceMatcher.wildcard) {
                // We couldn't find a resource which matched the tag, and
                // it wasn't a wildcard.  That's a problem.
                log.info "Unable to find resource [${resourceDef.'@name'}] " + 
                         "to add to group [${group.name}] typeDef=${typeDef}"
            }
                            
            shouldContain.addAll(matchingResources)
        }
        shouldContain
    }
    
    def list(xmlOut, params) {
        boolean includeSystem = params.getOne('includeSystem')?.toBoolean()
        boolean verbose = params.getOne('verbose')?.toBoolean()
        xmlOut.groups() {
            for (g in resourceHelper.findAllGroups()) {
                if (g.isSystem() && !includeSystem)
                    continue
        
                def groupAttrs = [name: g.name, 
                                  type:groupTypeToName[g.groupType]]
                if (verbose)
                    groupAttrs.id = g.id
                                  
                xmlOut.group(groupAttrs) {
                    bucketizeResourcesByType(g.resources).each { proto, resources ->
                        def typeAttrs = [name:proto.name]
                        
                        if (verbose)
                            typeAttrs.id = proto.id

                        // Groups don't have a prototype
                        if (isGroupOfGroups(g))
                            typeAttrs.name = 'Group'
                        else if (isGroupOfApps(g))
                            typeAttrs.name = 'Application'
                            
                        xmlOut.resourceType(typeAttrs) {
                            for (r in resources) {
                                xmlOut.resource(name:r.name, id:r.id)
                            }
                        }
                    }
                }
            }
        }
        xmlOut
    }
    
    
    /**
     * Return a map of Resource (prototypes) onto Resources of that type
     */
    private Map bucketizeResourcesByType(def resources) {
        def res = [:]
        resources.each { 
            res.get(it.prototype, []) << it
        }
        res
    }
}

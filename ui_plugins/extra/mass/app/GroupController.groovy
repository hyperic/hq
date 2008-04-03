import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as RGroupMan
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as ResourceMan
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl as AGroupMan
import org.hyperic.hq.appdef.shared.AppdefGroupValue
import org.hyperic.hq.appdef.shared.AppdefEntityConstants


class GroupController extends BaseController {
    private rgroupMan    = RGroupMan.one
    private agroupMan    = AGroupMan.one
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
                  updateGroupType: { AppdefGroupValue g, groupDef ->
                      updateCompatGroupType(g, groupDef)
                  }],
        resources:  [create: { groupDef -> createMixedGroup(groupDef) },
                     getPrototypes: { groupDef -> getAllAppdefPrototypes() },
                     updateGroupType: { AppdefGroupValue g, groupDef ->
                         updateMixedGroupType(g, groupDef)
                     }],
        applications:  [create: { groupDef -> createAppGroup(groupDef) },
                        getPrototypes: { [] /* Special cased */ },
                        updateGroupType: { AppdefGroupValue g, groupDef ->
                            updateAppGroupType(g, groupDef)
                        }],
        groups: [create: { groupDef -> createGroupOfGroups(groupDef) },
                           getPrototypes: { [] /* Special cased */ }, 
                           updateGroupType: { AppdefGroupValue g, groupDef ->
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
        def processed = []
        for (groupDef in xmlDef.group) {
            def groupName = groupDef.'@name'
            def group = allGroups.find { it.name == groupName }
                
            if (group)
                found << group
            log.info "Syncing group ${groupDef.'@name'}"
            processed << syncGroup(group, groupDef)
        }
        
        if (deleteMissing) {
            def toDelete  = allGroups - found

            for (group in toDelete) {
                if (group.isSystem()) {
                    log.warn "Not deleting system group [${group.name}]"
                    continue
                }
                log.info "Deleting group ${group.name}"
                agroupMan.deleteGroup(user.valueObject, group.id)
            }
        }
    
        _list(xmlOut, params, processed)
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
    
    private void updateAppGroupType(AppdefGroupValue group, def groupDef) {
        group.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
        group.groupEntType = -1
        group.groupEntResType = -1
    }

    private void updateGroupOfGroupsType(AppdefGroupValue group, def groupDef) {
        group.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
        group.groupEntType = AppdefEntityConstants.APPDEF_TYPE_GROUP
        group.groupEntResType = -1
    }
    
    private void updateMixedGroupType(AppdefGroupValue group, def groupDef) {
        group.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS
        group.groupEntType = -1
        group.groupEntResType = -1
    }
    
    private void updateCompatGroupType(AppdefGroupValue group, def groupDef) {
        def compatProto = getCompatPrototypes(groupDef)
        
        assert compatProto.size() == 1, 'Should have prototype here'
        compatProto = compatProto[0]

        def defGroupType
        if (compatProto.isServicePrototype()) {
            group.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
        } else if(compatProto.isPlatformPrototype() ||
                  compatProto.isServerPrototype()) 
        {
            group.groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS
        } else {
            assert "Unhandled prototype -> appdef type conversion"
        }

        group.groupEntType    = compatProto.appdefType
        group.groupEntResType = compatProto.instanceId
    }
    
    private void fillGroupProperties(AppdefGroupValue groupVal, def groupDef) {
        groupVal.name            = groupDef.'@name'
        groupVal.description     = groupDef.'@description'
        groupVal.location        = groupDef.'@location'
        groupVal.clusterId       = -1
        groupVal.groupEntType    = -1
        groupVal.groupEntResType = -1
    }
    
    private AppdefGroupValue createAppGroup(def groupDef) {
        agroupMan.createGroup(user.valueObject,
                              AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                              groupDef.'@name',
                              groupDef.'@description',
                              groupDef.'@location')
    }
    
    private AppdefGroupValue createGroupOfGroups(def groupDef) {
        agroupMan.createGroup(user.valueObject,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP,
                              groupDef.'@name',
                              groupDef.'@description',
                              groupDef.'@location')
    }

    private AppdefGroupValue createMixedGroup(def groupDef) {
        log.info "Creating mixed group [${groupDef.'@name'}]"
        agroupMan.createGroup(user.valueObject, groupDef.'@name',
                              groupDef.'@description', 
                              groupDef.'@location')
    }
    
    private AppdefGroupValue createCompatGroup(def groupDef) {
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
        
        agroupMan.createGroup(user.valueObject, proto.appdefType, 
                              proto.instanceId, 
                              groupDef.'@name',
                              groupDef.'@description',
                              groupDef.'@location')
    }
    
    private boolean isGroupOfGroups(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
    }

    private boolean isGroupOfApps(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
    }
    
    private ResourceGroup syncGroup(ResourceGroup group, def groupDef) {
        AppdefGroupValue groupVal
        def typeMethods

        if (!group) {
            // Create!
            def type = groupDef.'@type'
            typeMethods = typeToMethods[type]
            if (!typeMethods) {
                throw new IllegalArgumentException("Invalid group type [${type}]")
            }
            groupVal = typeMethods.create(groupDef)
            group    = rgroupMan.findResourceGroupByName(user.valueObject, 
                                                         groupDef.'@name')
        } else { 
            groupVal = agroupMan.findGroupByName(user.valueObject,
                                                 group.name)
            typeMethods = typeToMethods[groupTypeToName[group.groupType]]
            typeMethods.updateGroupType(groupVal, groupDef)
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
                def typeMatcher     = makeNameMatcher(typeDef.'@name')
                def matchingTypes   = validPrototypes.grep { Resource proto ->
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
        
        for (r in shouldContain) {
            groupVal.addAppdefEntity(r.entityID)
        }
        
        if (groupDef.'@description')
            groupVal.description = groupDef.'@description'
        if (groupDef.'@location')
            groupVal.location = groupDef.'@location'
            
        agroupMan.saveGroup(user.valueObject, groupVal)
        return rgroupMan.findResourceGroupByName(user.valueObject, 
                                                 groupDef.'@name')
    }
    
    private Closure makeNameMatcher(String s) {
        if (!s) {
            return { true }
        } else if (s.startsWith('regex')) {
            def regex = ~s[6..-1]
            return { r -> r.name ==~ regex }
        } else {
            return { r -> r.name == s }
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
        _list(xmlOut, params, resourceHelper.findAllGroups())
    }
    
    private _list(xmlOut, params, groups) {
        boolean includeSystem = params.getOne('includeSystem')?.toBoolean()
        boolean verbose = params.getOne('verbose')?.toBoolean()
        Closure matcher = makeNameMatcher(params.getOne('name'))
        xmlOut.groups() {
            for (g in groups) {
                if (!matcher(g))
                    continue
                    
                if (g.isSystem() && !includeSystem)
                    continue
        
                def groupAttrs = [name: g.name, description:g.description,
                                  location:g.location,
                                  type:groupTypeToName[g.groupType]]
                if (verbose)
                    groupAttrs.id = g.id

                if (g.isCompatible()) {
                    groupAttrs.resourceType = g.compatibleType.name
                }
                    
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

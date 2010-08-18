/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.hqu.rendit.BaseController

class GroupController extends BaseController {

    private def groupTypeToName = [
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP):  'applications',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP):  'groups',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS):  'resources',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS):  'compat',
       (AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC): 'compat'
    ]

    private def typeToMethods = [
        compat:  [create: { groupDef -> createCompatGroup(groupDef) },
                  getPrototypes: { groupDef -> [getCompatPrototype(groupDef)] },
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
        println(_allResourcesByProto[proto])
        _allResourcesByProto[proto]
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

    def sync(xmlOut, params) {
        String s = getUpload('args') 
        def xmlDef = new XmlParser().parse(new StringReader(s))

        if (xmlDef.group.size() != 1) {
            xmlOut.error("Only 1 group supported for sync")
            return xmlOut
        }
                        
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
        
        _list(xmlOut, params, processed)
        xmlOut
    }

    private ResourceGroup createAppGroup(def groupDef) {
        log.info "Creating group of apps [${groupDef.'@name'}]"
        Resource r = resourceHelper
            .findResourceTypeResourceById(AuthzConstants.authzApplicationProto)
        r.createGroup(user, groupDef.'@name', groupDef.'@description',
                      groupDef.'@location')
    }
     private ResourceGroup createGroupOfGroups(def groupDef) {
        log.info "Creating group of groups [${groupDef.'@name'}]"
        Resource r = resourceHelper.findById(5)     // covalentAuthzResourceGroup
        r.createGroup(user, groupDef.'@name', groupDef.'@description',
                      groupDef.'@location')
    }

    private ResourceGroup createMixedGroup(def groupDef) {
        log.info "Creating mixed group [${groupDef.'@name'}]"
        Resource r = resourceHelper.findById(7)     // ROOT_RESOURCE_GROUP
        r.createGroup(user, groupDef.'@name', groupDef.'@description',
                      groupDef.'@location')
    }

    private ResourceGroup createCompatGroup(def groupDef) {
        Resource proto = getCompatPrototype(groupDef)
        log.info "Creating group from ${proto.name}"
        if (!proto) {
            throw new Exception("Resource type [$protoName] not found ")
        }
        
        proto.createGroup(user, groupDef.'@name', groupDef.'@description',
                          groupDef.'@location')
    }
    
    private boolean isGroupOfGroups(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
    }

    private boolean isGroupOfApps(ResourceGroup group) {
        group.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
    }

    private ResourceGroup syncGroup(ResourceGroup group, def groupDef) {
        def typeMethods

        if (group) {
            typeMethods = typeToMethods[groupTypeToName[group.groupType]]
            typeMethods.updateGroupType(group, groupDef)
        }
        else {
            // Create
            def type = groupDef.'@type'
            typeMethods = typeToMethods[type]
            if (!typeMethods) {
                throw new IllegalArgumentException("Invalid group type [${type}]")
            }
            group = typeMethods.create(groupDef)
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

                List allResources = resourceHelper.findAllApplications()
                shouldContain.addAll(syncResources(group, typeDef, allResources))
            } else {
                def validPrototypes = typeMethods.getPrototypes(groupDef)
                def typeMatcher     = makeNameMatcher(typeName)
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

            def deleteMissing = groupDef.'@deleteMissing'?.toBoolean()
            if (!deleteMissing) {
                shouldContain = (shouldContain + group.resources) as Set
            }
            
            println("Groups: " + shouldContain)
            // Set the resources
            group.setResources(user, shouldContain)

            // Update other attributes
            def name = group.name
            def desc = group.description
            def loc  = group.location

            if (groupDef.'@name')
                name = groupDef.'@name'
            if (groupDef.'@description')
                desc = groupDef.'@description'
            if (groupDef.'@location')
                loc = groupDef.'@location'

            group.updateGroup(user, name, desc, loc)
        }

        resourceHelper.findGroupByName(groupDef.'@name')
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

    /**
     * Get a list of Resource's which are prototypes available to the 
     * be associated with the contents of the group.
     */
    private Resource getCompatPrototype(def groupDef) {
        def protoName = groupDef.'@resourceType'
        Resource proto = resourceHelper.findPrototype([prototype: protoName])
        
        if (!proto)
            throw new Exception("Resource type [$protoName] not found")

        // Compatable groups only have the type itself as available
        proto
    }

    private void updateAppGroupType(ResourceGroup group, def groupDef) {
        group.updateGroupType(user,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP,
                              -1)
    }

    private void updateGroupOfGroupsType(ResourceGroup group, def groupDef) {
        group.updateGroupType(user,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP,
                              -1)
    }

    private void updateMixedGroupType(ResourceGroup group, def groupDef) {
        group.updateGroupType(user,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
                              AppdefEntityConstants.APPDEF_TYPE_GROUP,
                              -1)
    }

    private void updateCompatGroupType(ResourceGroup group, def groupDef) {
        def compatProto = getCompatPrototype(groupDef)
        
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

        group.updateGroupType(user, defGroupType, compatProto.appdefType,
                              compatProto.instanceId)
    }
}

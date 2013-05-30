/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.shared;

import java.util.Arrays;
import java.util.List;

public final class AuthzConstants {

    // Root Resource Type
    public static final String rootResType = "covalentAuthzRootResourceType";

    // This assumes that the root resource is always initialized
    // with the first id available in a sequence that starts at 0
    public static final Integer rootResourceId = new Integer(0);
    public static final Integer rootSubjectId = new Integer(1);
    public static final String groupResourceTypeName =
        "covalentAuthzResourceGroup";
    public static final String rootResourceGroupName = "ROOT_RESOURCE_GROUP";
    public static final Integer rootResourceGroupId = new Integer(1);
    public static final Integer authzResourceGroupId = new Integer(0);

    // Appdef Resource Types
    public static final String platformResType = "covalentEAMPlatform";
    public static final String serverResType = "covalentEAMServer";
    public static final String serviceResType = "covalentEAMService";
    public static final String applicationResType = "covalentEAMApplication";
    public static final String groupResType = "covalentAuthzResourceGroup";
    public static final String policyResType = "groupPolicyResourceType";

    // Appdef Operations

    public static final String VIEW_PREFIX = "view";
    // Platform Operations
    public static final String platformOpCreatePlatform = "createPlatform";
    public static final String platformOpViewPlatform = VIEW_PREFIX + "Platform";
    public static final String platformOpModifyPlatform = "modifyPlatform";
    public static final String platformOpRemovePlatform = "removePlatform";
    public static final String platformOpAddServer = "addServer";
    public static final String platformOpMonitorPlatform = "monitorPlatform";
    public static final String platformOpControlPlatform = "controlPlatform";
    public static final String platformOpManageAlerts = "managePlatformAlerts";

    // Server Operations
    public static final String serverOpCreateServer = "createServer";
    public static final String serverOpViewServer = VIEW_PREFIX + "Server";
    public static final String serverOpModifyServer = "modifyServer";
    public static final String serverOpRemoveServer = "removeServer";
    public static final String serverOpAddService = "addService";
    public static final String serverOpMonitorServer = "monitorServer";
    public static final String serverOpControlServer = "controlServer";
    public static final String serverOpManageAlerts = "manageServerAlerts";

    // Service Operations
    public static final String serviceOpCreateService = "createService";
    public static final String serviceOpViewService = VIEW_PREFIX + "Service";
    public static final String serviceOpModifyService = "modifyService";
    public static final String serviceOpRemoveService = "removeService";
    public static final String serviceOpMonitorService = "monitorService";
    public static final String serviceOpControlService = "controlService";
    public static final String serviceOpManageAlerts = "manageServiceAlerts";

    // Application Operations
    public static final String appOpCreateApplication = "createApplication";
    public static final String appOpViewApplication = VIEW_PREFIX + "Application";
    public static final String appOpModifyApplication = "modifyApplication";
    public static final String appOpRemoveApplication = "removeApplication";
    public static final String appOpMonitorApplication = "monitorApplication";
    public static final String appOpControlApplication = "controlApplication";
    public static final String appOpManageAlerts = "manageApplicationAlerts";

    // Group Operations
    public static final String groupOpCreateResourceGroup = "createResourceGroup";
    public static final String groupOpViewResourceGroup = VIEW_PREFIX + "ResourceGroup";
    public static final String groupOpModifyResourceGroup = "modifyResourceGroup";
    public static final String groupOpRemoveResourceGroup = "removeResourceGroup";
    public static final String groupOpAddRole = "addRole";
    public static final String groupOpControlResourceGroup = "controlResourceGroup";
    public static final String groupOpMonitorResourceGroup = "monitorResourceGroup";
    public static final String groupOpManageAlerts = "manageGroupAlerts";

    // View permission constants - defined in authz-data.xml
    public static final Integer perm_viewSubject       = new Integer(8);
    public static final Integer perm_viewRole          = new Integer(16);
    public static final Integer perm_viewResourceGroup = new Integer(28);
    public static final Integer perm_viewPlatform      = new Integer(305);
    public static final Integer perm_viewServer        = new Integer(311);
    public static final Integer perm_viewService       = new Integer(315);
    public static final Integer perm_viewApplication   = new Integer(319);
    // Modify permission constants - defined in authz-data.xml
    public static final Integer perm_modifySubject       = new Integer(6);
    public static final Integer perm_modifyRole          = new Integer(11);
    public static final Integer perm_modifyResourceGroup = new Integer(24);
    public static final Integer perm_modifyPlatform      = new Integer(301);
    public static final Integer perm_modifyServer        = new Integer(307);
    public static final Integer perm_modifyService       = new Integer(313);
    public static final Integer perm_modifyApplication   = new Integer(317);
    // remove permission constants - defined in authz-data.xml
    public static final Integer perm_removeSubject       = new Integer(7);
    public static final Integer perm_removeRole          = new Integer(30);
    public static final Integer perm_removeResourceGroup = new Integer(31);
    public static final Integer perm_removePlatform      = new Integer(302);
    public static final Integer perm_removeServer        = new Integer(308);
    public static final Integer perm_removeService       = new Integer(314);
    public static final Integer perm_removeApplication   = new Integer(318);

    // Authz Stuff...
    public static final String rootRoleName = "Super User Role";
    public static final Integer rootRoleId = new Integer(0);
    public static final String creatorRoleName = "RESOURCE_CREATOR_ROLE";
    public static final String subjectResourceTypeName = "covalentAuthzSubject";
    public static final String typeResourceTypeName =
        "covalentAuthzRootResourceType";
    public static final String roleResourceTypeName = "covalentAuthzRole";
    public static final Integer overlordId = new Integer(0);
    public static final String overlordName = "admin";
    public static final String overlordDsn = "covalentAuthzInternalDsn";
    public static final Integer guestId = new Integer(2);
    public static final String authzResourceGroupName =
        "covalentAuthzResourceGroup";
    public static final String escalationResourceTypeName = "EscalationScheme";
    public static final String hqSystemResourceTypeName = "HQSystem";
    public static final String platformPrototypeTypeName = "PlatformPrototype";
    public static final String serverPrototypeTypeName = "ServerPrototype";
    public static final String servicePrototypeTypeName = "ServicePrototype";
    public static final String appPrototypeTypeName = "ApplicationPrototype";
    
    public static final String platformPrototypeVmwareVsphereVm = "VMware vSphere VM";
    public static final String platformPrototypeVmwareVsphereHost = "VMware vSphere Host";
    public static final String serverPrototypeVmwareVcenter = "VMware vCenter";
    
    public static final List<String> VMWARE_PROTOTYPES =
    	Arrays.asList(new String[] {
    			serverPrototypeVmwareVcenter,
    			platformPrototypeVmwareVsphereHost,
    			platformPrototypeVmwareVsphereVm
    });
        
    public static final String rootOpCAMAdmin = "administerCAM";

    public static final String typeOpCreateResource = "createResource";
    public static final String typeOpModifyResourceType = "modifyResourceType";
    public static final String typeOpAddOperation = "addOperation";
    public static final String typeOpRemoveOperation = "removeOperation";

    public static final String subjectOpViewSubject = VIEW_PREFIX + "Subject";
    public static final String subjectOpModifySubject = "modifySubject";
    public static final String subjectOpRemoveSubject = "removeSubject";
    public static final String subjectOpCreateSubject = "createSubject";

    public static final String roleOpCreateRole = "createRole";
    public static final String roleOpModifyRole = "modifyRole";
    public static final String roleOpRemoveRole = "removeRole";
    public static final String roleOpViewRole = VIEW_PREFIX + "Role";

    public static final String escOpCreateEscalation = "createEscalation";
    public static final String escOpViewEscalation = VIEW_PREFIX + "Escalation";
    public static final String escOpModifyEscalation = "modifyEscalation";
    public static final String escOpRemoveEscalation = "removeEscalation";

    public static final String policyOpCreatePolicy = "createPolicy";
    public static final String policyOpViewPolicy = VIEW_PREFIX + "Policy";
    public static final String policyOpModifyPolicy = "modifyPolicy";
    public static final String policyOpRemovePolicy = "removePolicy";

    
    public static final Integer perm_createPolicy   = new Integer(430);
    public static final Integer perm_modifyPolicy   = new Integer(431);
    public static final Integer perm_removePolicy   = new Integer(432);
    public static final Integer perm_viewPolicy     = new Integer(433);
    
    public static final Integer perm_createGroup    = new Integer(29);//used for dynamicGroup

    public static final String privateRoleGroupName = "camPrivateRoleGroup:";
    public static final int authzDefaultResourceGroupType = 13;

    public static final Integer     authzSubject        =  new Integer(1);
    public static final Integer     authzRole           =  new Integer(2);
    public static final Integer     authzGroup          =  new Integer(3);
    public static final Integer     authzPlatform       =  new Integer(301);
    public static final Integer     authzServer         =  new Integer(303);
    public static final Integer     authzService        =  new Integer(305);
    public static final Integer     authzApplication    =  new Integer(308);
    public static final Integer     authzEscalation     =  new Integer(401);
    public static final Integer     authzHQSystem       =  new Integer(501);
    public static final Integer     authzLocation       =  new Integer(309);
    public static final Integer     authzPlatformProto  =  new Integer(601);
    public static final Integer     authzServerProto    =  new Integer(602);
    public static final Integer     authzServiceProto   =  new Integer(603);
    public static final Integer     authzApplicationProto  =  new Integer(604);
    public static final Integer     authzPolicy         =  new Integer(701);
 
    // Resource Relation constants
    public static final Integer RELATION_CONTAINMENT_ID = new Integer(1);
    public static final Integer RELATION_NETWORK_ID = new Integer(2);
    public static final Integer RELATION_VIRTUAL_ID = new Integer(3);
    public static final String ResourceEdgeContainmentRelation = "containment";
    public static final String ResourceEdgeNetworkRelation = "network";
    public static final String ResourceEdgeVirtualRelation = "virtual";
    
    // List of alert operations
    public static final String[] VIEW_ALERTS_OPS =
        new String[] {  platformOpViewPlatform,
                        serverOpViewServer,
                        serviceOpViewService,
                        groupOpViewResourceGroup
    };

    public static boolean isOverlord(Integer subject) {
        return subject.equals(AuthzConstants.overlordId);
    }
}

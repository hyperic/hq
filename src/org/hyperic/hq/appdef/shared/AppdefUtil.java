/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceValue;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

/** AppdefUtil - utility methods for appdef entities and
* brethren.
*
* <a href="mailto:desmond@covalent.net">desmond</a>
* */

public class AppdefUtil {


    /** Translate appdefTypeId to authz resource type string.
     * @param appdef type id
     * @return authz resource type string
     * @throws InvalidAppdefTypeException
     */
    public static String appdefTypeIdToAuthzTypeStr (int adTypeId)
        throws InvalidAppdefTypeException {
        
        if (adTypeId == AppdefEntityConstants.APPDEF_TYPE_PLATFORM)
            return AuthzConstants.platformResType;
        if (adTypeId == AppdefEntityConstants.APPDEF_TYPE_SERVER)
            return AuthzConstants.serverResType;
        if (adTypeId == AppdefEntityConstants.APPDEF_TYPE_SERVICE)
            return AuthzConstants.serviceResType;
        if (adTypeId == AppdefEntityConstants.APPDEF_TYPE_APPLICATION)
            return AuthzConstants.applicationResType;
        if (adTypeId == AppdefEntityConstants.APPDEF_TYPE_GROUP)
            return AuthzConstants.groupResourceTypeName;
        throw new InvalidAppdefTypeException("No authz resource type "+
            "String provisioned for appdef type argument.");
    }

    /** Transform an authz resource type name into an appdef type id.
     * @param authz resource name
     * @return AppdefEntity type id
     * @throws InvalidAppdefTypeException
     */
    public static int resNameToAppdefTypeId (String resName)
        throws InvalidAppdefTypeException {

        if ( resName.equals(AuthzConstants.platformResType))
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        if ( resName.equals(AuthzConstants.serverResType))
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        if ( resName.equals(AuthzConstants.serviceResType))
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        if ( resName.equals(AuthzConstants.applicationResType))
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        if ( resName.equals(AuthzConstants.groupResourceTypeName))
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;

        throw new InvalidAppdefTypeException("No appdef entity type "+
            "provisioned for authz resource name argument: " + resName);
    }


    /** Transform a ResourceValue into an appdef entity id.
     * @param ResourceValue object.
     * @return AppdefEntityID object.
     * @throws InvalidAppdefTypeException
     */
    public static AppdefEntityID resValToAppdefEntityId (ResourceValue rv)
        throws InvalidAppdefTypeException {

        if ( rv.getResourceTypeValue().getName().equals(
            AuthzConstants.platformResType))
            return AppdefEntityID.newPlatformID(rv.getInstanceId());
        if ( rv.getResourceTypeValue().getName().equals(
             AuthzConstants.serverResType))
            return AppdefEntityID.newServerID(rv.getInstanceId());
        if ( rv.getResourceTypeValue().getName().equals(
             AuthzConstants.serviceResType))
            return AppdefEntityID.newServiceID(rv.getInstanceId());
        if ( rv.getResourceTypeValue().getName().equals(
             AuthzConstants.applicationResType))
            return AppdefEntityID.newAppID(rv.getInstanceId());
        if ( rv.getResourceTypeValue().getName().equals(
            AuthzConstants.groupResourceTypeName))
            return AppdefEntityID.newGroupID(rv.getInstanceId());

        throw new InvalidAppdefTypeException("No appdef entity type "+
            "provisioned for authz resource argument:"+rv.toString());
    }

    /**
     * Runtime scans are supported servers that were not autodiscovered
     * themselves, as well as servers that were autodiscovered but
     * whose services are not automanaged (if they were, it would mean
     * that the server that autodiscovered them is managing its services,
     * so runtime scans should not be run for this server).
     * @return true if runtime scans could possibly be enabled for the
     * server, false otherwise.
     */
    public static boolean areRuntimeScansEnabled ( Server server ) {
        return ( server.getRuntimeAutodiscovery() &&
                 ( !server.getWasAutodiscovered() ||
                   ( server.getWasAutodiscovered() &&
                     !server.getServicesAutomanaged()) ));
    }

    public static Map groupByAppdefType(AppdefEntityID[] ids) {
        HashMap m = new HashMap();
        for (int i = 0; i < ids.length; i++) {
            Integer type = new Integer(ids[i].getType());
            ArrayList idList = (ArrayList) m.get(type);
            if (idList == null) {
                idList = new ArrayList();
                m.put(type, idList);
            }
            idList.add(ids[i].getId());
        }
        return m;
    }

    private AppdefUtil () {}
}

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

import org.hyperic.hq.product.TypeInfo;

public final class AppdefEntityConstants {

    public static final int APPDEF_TYPE_PLATFORM    = 1;
    public static final int APPDEF_TYPE_SERVER      = 2;
    public static final int APPDEF_TYPE_SERVICE     = 3;
    public static final int APPDEF_TYPE_APPLICATION = 4;
    public static final int APPDEF_TYPE_GROUP       = 5;
    public static final int APPDEF_TYPE_AIPLATFORM  = 6;
    public static final int APPDEF_TYPE_AISERVER    = 7;
    public static final int APPDEF_TYPE_AIIP        = 8;
    public static final int APPDEF_TYPE_AUTOGROUP   = 9;

    private static final String APPDEF_NAME_PLATFORM    = "platform";
    private static final String APPDEF_NAME_SERVER      = "server";
    private static final String APPDEF_NAME_SERVICE     = "service";
    private static final String APPDEF_NAME_APPLICATION = "application";
    private static final String APPDEF_NAME_GROUP       = "group";
    private static final String APPDEF_NAME_AIPLATFORM  = "aiplatform";
    private static final String APPDEF_NAME_AISERVER    = "aiserver";
    private static final String APPDEF_NAME_AIIP        = "aiip";
    private static final String APPDEF_NAME_AUTOGROUP   = "autogroup";

    public static final String GENERIC_APPLICATION_TYPE = 
        "Generic Application";

    // ResourceTreeGenerator traversal constants
    public static final int RESTREE_TRAVERSE_NORMAL = 1;
    public static final int RESTREE_TRAVERSE_UP     = 2;

    /* The group types defined below break down as follows:
     *          ADHOC                             COMPAT
     *          / | \                               /\
     *         /  |  \                             /  \
     *        /   |   \                           /    \
     *       /    |    \                         /      \
     *     (Group)| or  \                    (Service) or\
     *          (App) or \                            (Platform||Server)
     *             (Platform&Server&Service)
     *
     */
    /* Group of applications */
    public static final int    APPDEF_TYPE_GROUP_ADHOC_APP     = 11;
    /* Group of group */
    public static final int    APPDEF_TYPE_GROUP_ADHOC_GRP     = 12;
    /* Group of platform, server, service */
    public static final int    APPDEF_TYPE_GROUP_ADHOC_PSS     = 13;
    /* Compatible group of Platform or Servers */
    public static final int    APPDEF_TYPE_GROUP_COMPAT_PS     = 14;
    /* Compatible group of Services (cluster) */
    public static final int    APPDEF_TYPE_GROUP_COMPAT_SVC    = 15;

    private static final String APPDEF_TYPE_GROUP_ADHOC_APP_LABEL   = 
        "Mixed Group - Applications";
    private static final String APPDEF_TYPE_GROUP_ADHOC_GRP_LABEL   = 
        "Mixed Group - Groups";
    private static final String APPDEF_TYPE_GROUP_ADHOC_PSS_LABEL   = 
        "Mixed Group - Platforms,Servers & Services";
    private static final String APPDEF_TYPE_GROUP_COMPAT_PS_LABEL   = 
        "Compatible / Cluster Group";
    private static final String APPDEF_TYPE_GROUP_COMPAT_SVC_LABEL  = 
        "Compatible Group - Service Cluster";

    /** get primary appdef types */
    public static int[] getAppdefTypes () {
        return new int[] { APPDEF_TYPE_PLATFORM,
                           APPDEF_TYPE_SERVER,
                           APPDEF_TYPE_SERVICE,
                           APPDEF_TYPE_APPLICATION,
                           APPDEF_TYPE_GROUP };
    }
    public static int[] getAppdefGroupTypes () {
        return new int[] { APPDEF_TYPE_GROUP_ADHOC_APP,
                           APPDEF_TYPE_GROUP_ADHOC_GRP,
                           APPDEF_TYPE_GROUP_ADHOC_PSS,
                           APPDEF_TYPE_GROUP_COMPAT_PS,
                           APPDEF_TYPE_GROUP_COMPAT_SVC };
    }

    // Most group contexts collapse or normalize compatible
    // PSS and SVC together. 
    public static int[] getAppdefGroupTypesNormalized () {
        return new int[] { APPDEF_TYPE_GROUP_ADHOC_APP,
                           APPDEF_TYPE_GROUP_ADHOC_GRP,
                           APPDEF_TYPE_GROUP_ADHOC_PSS,
                           APPDEF_TYPE_GROUP_COMPAT_SVC };
    }

    public static String typeToString(int type){
        switch(type){
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AppdefEntityConstants.APPDEF_NAME_PLATFORM;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return AppdefEntityConstants.APPDEF_NAME_SERVER;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return AppdefEntityConstants.APPDEF_NAME_SERVICE;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return AppdefEntityConstants.APPDEF_NAME_APPLICATION;
        case AppdefEntityConstants.APPDEF_TYPE_GROUP:
            return AppdefEntityConstants.APPDEF_NAME_GROUP;
        case AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM:
            return AppdefEntityConstants.APPDEF_NAME_AIPLATFORM;
        case AppdefEntityConstants.APPDEF_TYPE_AISERVER:
            return AppdefEntityConstants.APPDEF_NAME_AISERVER;
        case AppdefEntityConstants.APPDEF_TYPE_AIIP:
            return AppdefEntityConstants.APPDEF_NAME_AIIP;
        case AppdefEntityConstants.APPDEF_TYPE_AUTOGROUP:
            return AppdefEntityConstants.APPDEF_NAME_AUTOGROUP;
        default:
            throw new IllegalArgumentException("Unknown appdef type: " + type);
        }
    }

    public static int stringToType(String name){
        if (name.equals(AppdefEntityConstants.APPDEF_NAME_PLATFORM)) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_SERVER)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_SERVICE)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else if (name.
                   equals(AppdefEntityConstants.APPDEF_NAME_APPLICATION)) {
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_GROUP)) {
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_AIPLATFORM)) {
            return AppdefEntityConstants.APPDEF_TYPE_AIPLATFORM;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_AISERVER)) {
            return AppdefEntityConstants.APPDEF_TYPE_AISERVER;
        } else if (name.equals(AppdefEntityConstants.APPDEF_NAME_AIIP)) {
            return AppdefEntityConstants.APPDEF_TYPE_AIIP;
        } else {
            throw new IllegalArgumentException("Unknown appdef type: " + name);
        }
    }

    public static int entityInfoTypeToAppdefType(int entityInfoType){
        switch(entityInfoType){
        case TypeInfo.TYPE_PLATFORM:
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        case TypeInfo.TYPE_SERVER:
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        case TypeInfo.TYPE_SERVICE:
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        default:
            throw new IllegalArgumentException("Unknown TypeInfo type");
        }
    }

    /**
     * Convert a TypeInfo PDK entity type into an Appdef entity type.
     */
    public static int appdefTypeToEntityInfoType(int appdefType){
        switch(appdefType){
        case APPDEF_TYPE_PLATFORM:
            return TypeInfo.TYPE_PLATFORM;
        case APPDEF_TYPE_SERVER:
            return TypeInfo.TYPE_SERVER;
        case APPDEF_TYPE_SERVICE:
            return TypeInfo.TYPE_SERVICE;
        }
        throw new IllegalArgumentException("Appdef type " + appdefType + 
                                           " is an invalid entity type");
    }

    public static boolean typeIsValid(int type){
        return type >= AppdefEntityConstants.APPDEF_TYPE_PLATFORM &&
            type <= AppdefEntityConstants.APPDEF_TYPE_AIIP;
    }

    public static boolean groupTypeIsValid (int type) {
        return type >= AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP &&
            type <= AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
    }

    public static String getAppdefGroupTypeName(int grpType){
        switch (grpType) {
        case APPDEF_TYPE_GROUP_ADHOC_APP:
            return APPDEF_TYPE_GROUP_ADHOC_APP_LABEL;
        case APPDEF_TYPE_GROUP_ADHOC_GRP:
            return APPDEF_TYPE_GROUP_ADHOC_GRP_LABEL;
        case APPDEF_TYPE_GROUP_ADHOC_PSS:
            return APPDEF_TYPE_GROUP_ADHOC_PSS_LABEL;
        case APPDEF_TYPE_GROUP_COMPAT_PS:
            return APPDEF_TYPE_GROUP_COMPAT_PS_LABEL;
        case APPDEF_TYPE_GROUP_COMPAT_SVC:
            return APPDEF_TYPE_GROUP_COMPAT_SVC_LABEL;
        }
        throw new IllegalArgumentException("Unknown appdef group type: " +
                                           grpType);
    }

    public static boolean isGroupAdhoc (int grpType) {
        return (grpType ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP ||
                grpType ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP ||
                grpType ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS );
    }

    public static boolean isGroupCompat(int grpType) {
        return (grpType ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS  ||
                grpType ==
                AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC );
    }

}

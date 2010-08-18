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

package org.hyperic.hq.authz.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.util.HypericEnum;

public class MixedGroupType extends HypericEnum {
    
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.grouping.Resources");
    
    public static final MixedGroupType ALL_MIXED_GROUPS =
        new MixedGroupType(1000, "allMixedGroups",
            "authz.grouptype.typeServiceCluster");
    public static final MixedGroupType APPLICATION_GROUP =
        new MixedGroupType(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP,
            "groupOfApps", "authz.grouptype.typeApplicationGroup");
    public static final MixedGroupType GROUP_OF_GROUPS =
        new MixedGroupType(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP,
            "groupOfGroups", "authz.grouptype.typeGroupOfGroups");
    public static final MixedGroupType PLATFORM_SERVER_SERVICE_GROUP =
        new MixedGroupType(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
            "groupOfPlatsServersServices",
            "authz.grouptype.typePlatformServerServiceGroup");

    protected MixedGroupType(int code, String desc, String localeProp) {
        super(MixedGroupType.class, code, desc, localeProp, BUNDLE);
    }

    public static MixedGroupType findByCode(int code) {
        return (MixedGroupType)findByCode(MixedGroupType.class, code);  
    }

    /*
     * @return List of Integer of AppdefEntityConstants
     */
    public List getAppdefEntityTypes() {
        List rtn = new ArrayList();
        if (this.equals(ALL_MIXED_GROUPS)) {
            rtn.add(new Integer(GROUP_OF_GROUPS.getCode()));
            rtn.add(new Integer(APPLICATION_GROUP.getCode()));
            rtn.add(new Integer(PLATFORM_SERVER_SERVICE_GROUP.getCode()));
        } else {
            rtn.add(new Integer(this.getCode()));
        }
        return rtn;
    }

    /*
     * @param codes array of AppdefEntityConstants representing either
     * a mixed or compatible group.
     */
    public static MixedGroupType findByCode(int[] codes) {
        Integer[] rtn = new Integer[codes.length];
        for (int i=0; i<codes.length; i++) {
            rtn[i] = new Integer(codes[i]);
        }
        return findByCode(rtn);
    }

    /*
     * @param codes array of AppdefEntityConstants representing either
     * a mixed or compatible group.
     */
    public static MixedGroupType findByCode(Integer[] codes) {
        List list = Arrays.asList(codes);
        if (list.contains(new Integer(APPLICATION_GROUP.getCode())) &&
            list.contains(new Integer(GROUP_OF_GROUPS.getCode())) &&
            list.contains(new Integer(PLATFORM_SERVER_SERVICE_GROUP.getCode()))) {
            return ALL_MIXED_GROUPS;
        }
        return findByCode(codes[0].intValue());
    }
}

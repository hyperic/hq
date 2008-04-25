package org.hyperic.hq.authz.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.util.HypericEnum;

public class GroupType extends HypericEnum {
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.grouping.Resources");
    
    public static final GroupType COMPATIBLE_GROUP =
        new GroupType(100, "Compatible group",
            "authz.grouptype.typeServiceCluster");
    public static final GroupType MIXED_GROUP =
        new GroupType(101, "Mixed group",
            "authz.grouptype.typeServiceCluster");
    private static final Integer APPLICATION_GROUP =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP);
    private static final Integer GROUP_OF_GROUPS =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP);
    private static final Integer PLATFORM_SERVER_SERVICE_GROUP =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS);
    private static final Integer PLATFORM_SERVER_GROUP =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
    private static final Integer SERVICE_CLUSTER =
        new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC);

    protected GroupType(int code, String desc, String localeProp) {
        super(GroupType.class, code, desc, localeProp, BUNDLE);
    }
    
    /*
     * @return List of Integer of AppdefEntityConstants
     */
    public List getAppdefEntityTypes() {
        List rtn = new ArrayList();
        if (this.equals(MIXED_GROUP)) {
            rtn.add(GROUP_OF_GROUPS);
            rtn.add(APPLICATION_GROUP);
            rtn.add(PLATFORM_SERVER_SERVICE_GROUP);
            return rtn;
        } else if (this.equals(COMPATIBLE_GROUP)) {
            rtn.add(PLATFORM_SERVER_GROUP);
            rtn.add(SERVICE_CLUSTER);
            return rtn;
        }
        // blow up if this does not equal MIXED or COMPATIBLE GROUP
        assert false;
        return null;
    }

    /*
     * @param codes array of AppdefEntityConstants representing either
     * a mixed or compatible group.
     */
    public static GroupType findByCode(int[] codes) {
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
    public static GroupType findByCode(Integer[] codes) {
        if (codes.length == 1) {
            return findByCode(codes[0].intValue());
        }
        List list = Arrays.asList(codes);
        if (list.contains(APPLICATION_GROUP) ||
            list.contains(GROUP_OF_GROUPS) ||
            list.contains(PLATFORM_SERVER_SERVICE_GROUP)) {
            return MIXED_GROUP;
        }
        else if (list.contains(PLATFORM_SERVER_GROUP) ||
                 list.contains(SERVICE_CLUSTER)) {
            return COMPATIBLE_GROUP;
        }
        else {
            return findByCode(codes[0].intValue());
        }
    }

    public static GroupType findByCode(int code) {
        return (GroupType)findByCode(GroupType.class, code);  
    }
}
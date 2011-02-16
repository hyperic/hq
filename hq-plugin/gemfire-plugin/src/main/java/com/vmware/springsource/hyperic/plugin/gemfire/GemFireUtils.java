/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vmware.springsource.hyperic.plugin.gemfire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author laullon
 */
public class GemFireUtils {

    static Log log = LogFactory.getLog(GemFireUtils.class);
    private static final Map<String, String> membersIDCache = new HashMap();

    public static List<String> getMembers(MBeanServerConnection mServer) throws PluginException {
        String[] members;
        Object[] args = new Object[0];
        String[] def = new String[0];
        try {
            members = (String[]) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMembers", args, def);
        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
        }
        return Arrays.asList(members);
    }

    public static Map<String, Object> getMemberDetails(String memberID, MBeanServerConnection mServer) throws PluginException {
        Map<String, Object> memberDetails = null;
        try {
            Object[] args2 = {memberID};
            String[] def2 = {String.class.getName()};
            memberDetails = (Map) mServer.invoke(new ObjectName("GemFire:type=MemberInfoWithStatsMBean"), "getMemberDetails", args2, def2);
        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
        }

        if ((memberDetails == null) || memberDetails.isEmpty()) {
            String msg = "Member '" + memberID + "' not found!!!";
            if (log.isDebugEnabled()) {
                log.debug("[getMemberDetails] " + msg);
            }
            throw new PluginException(msg);
        }
        return memberDetails;
    }

    public static void clearNameCache() {
        membersIDCache.clear();
    }

    public static String memberNameToMemberID(String memberName, MBeanServerConnection mServer) throws PluginException {
        if (membersIDCache.isEmpty()) {
            synchronized (membersIDCache) {
                membersIDCache.clear();
                List<String> members = GemFireUtils.getMembers(mServer);
                for (String member : members) {
                    Map<String, Object> memberDetails = getMemberDetails(member, mServer);
                    membersIDCache.put((String) memberDetails.get("gemfire.member.name.string"), member);
                }
            }
            log.debug("[memberNameToMemberID] membersIDCache.size() => " + membersIDCache.size());
        }

        String memberID = membersIDCache.get(memberName);
        if (memberID == null) {
            String msg = "Member named '" + memberName + "' not found!!!";
            if (log.isDebugEnabled()) {
                log.debug("[memberNameToMemberID] " + msg);
            }
            throw new PluginException(msg);
        }
        log.debug("[memberNameToMemberID] " + memberName + "=" + memberID);

        return memberID;
    }
}

/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.gfee.cache;

public class MemberInfo {
    
    public final static int MEMBER_TYPE_NOTSET = 0;
    public final static int MEMBER_TYPE_CACHEVM = 1;
    public final static int MEMBER_TYPE_APPLICATION = 2;

    private String gfid;
    private String name;
    private String host;
    private String workingDirectory;
    private boolean statisticsDirty;
    private int memberType;

    public MemberInfo(String gfid, String name, String host,
            String workingDirectory, int memberType) {
        super();
        this.gfid = gfid;
        this.name = name;
        this.host = host;
        this.workingDirectory = workingDirectory;
        this.statisticsDirty = true;
        this.memberType = memberType;
    }

    public MemberInfo(String gfid, String name, String host,
            String workingDirectory) {
        this(gfid, name, host, workingDirectory, MEMBER_TYPE_NOTSET);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MemberInfo) {
            MemberInfo m = (MemberInfo)obj;
            String mwd = m.getWorkingDirectory();
            if(mwd == null || mwd.length() == 0) {
                boolean is = (name.equals(m.getName()) && host.equals(m.getHost()));
                return (name.equals(m.getName()) && host.equals(m.getHost()));
            } else {
                boolean is = (workingDirectory.equals(m.getWorkingDirectory()) && host.equals(m.getHost()));
                return (workingDirectory.equals(m.getWorkingDirectory()) && host.equals(m.getHost()));				
            }
        }
        return false;
    }

    public boolean isStatisticsDirty() {
        return statisticsDirty;
    }

    public void setStatisticsDirty(boolean statisticsDirty) {
        this.statisticsDirty = statisticsDirty;
    }

    public int getMemberType() {
        return memberType;
    }

    public void setMemberType(int memberType) {
        this.memberType = memberType;
    }

    @Override
    public int hashCode() {
        if(workingDirectory == null || workingDirectory.length() == 0) {
            return new String(host+name).hashCode();
        } else {
            return new String(host+workingDirectory).hashCode();
        }
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getGfid() {
        return gfid;
    }

}

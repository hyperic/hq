package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.appdef.shared.AIIpValue;

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
/**
 *
 */
public class AIIpDAO extends HibernateDAO
{
    public AIIpDAO(Session session)
    {
        super(AIIp.class, session);
    }

    public AIIp findById(Integer id)
    {
        return (AIIp)super.findById(id);
    }

    public void evict(AIIp entity)
    {
        super.evict(entity);
    }

    public AIIp merge(AIIp entity)
    {
        return (AIIp)super.merge(entity);
    }

    public void save(AIIp entity)
    {
        super.save(entity);
    }

    public void remove(AIIp entity)
    {
        super.remove(entity);
    }

    public AIIp create(AIIpValue ipv)
    {
        AIIp ip = new AIIp();
        ip.setAddress(ipv.getAddress());
        ip.setNetmask(ipv.getNetmask());
        ip.setMACAddress(ipv.getMACAddress());
        ip.setQueueStatus(ipv.getQueueStatus());
        ip.setDiff(ipv.getDiff());
        ip.setIgnored(ipv.getIgnored());
        save(ip);
        return ip;
    }

    public AIIp findByAddress(String addr)
    {
        String sql="from AIIp where address=?";
        return (AIIp)getSession().createQuery(sql)
            .setString(0, addr)
            .uniqueResult();
    }

}

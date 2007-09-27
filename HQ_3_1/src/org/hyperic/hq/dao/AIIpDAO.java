package org.hyperic.hq.dao;

import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.autoinventory.AIIp;

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

public class AIIpDAO extends HibernateDAO
{
    public AIIpDAO(DAOFactory f) {
        super(AIIp.class, f);
    }

    public AIIp findById(Integer id)
    {
        return (AIIp)super.findById(id);
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
        ip.setMacAddress(ipv.getMACAddress());
        ip.setQueueStatus(ipv.getQueueStatus());
        ip.setDiff(ipv.getDiff());
        ip.setIgnored(ipv.getIgnored());
        save(ip);
        return ip;
    }

    public List findByAddress(String addr)
    {
        String sql="from AIIp where address=?";
        return getSession().createQuery(sql)
            .setString(0, addr)
            .list();
    }


    public List findByMACAddress(String addr)
    {
        String sql="from AIIp where macAddress=?";
        return getSession().createQuery(sql)
            .setString(0, addr)
            .list();
    }
}

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.appdef.shared.AIServerValue;

import java.util.Collection;

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
 * CRUD, finders, etc for AIServer
 */
public class AIServerDAO extends HibernateDAO
{
    public AIServerDAO(Session session)
    {
        super(AIServer.class, session);
    }

    public void remove(AIServer entity)
    {
        super.remove(entity);
    }

    public void evict(AIServer entity)
    {
        super.evict(entity);
    }

    public AIServer merge(AIServer entity)
    {
        return (AIServer)super.merge(entity);
    }

    public void save(AIServer entity)
    {
        super.save(entity);
    }

    public AIServer findById(Integer id)
    {
        return (AIServer)super.findById(id);
    }

    public AIServer create(AIServerValue server)
    {
        AIServer as = new AIServer();

        as.setInstallPath      (server.getInstallPath());
        as.setAutoinventoryIdentifier
                               (server.getAutoinventoryIdentifier());
        as.setServicesAutomanaged
                               (server.getServicesAutomanaged());
        as.setName             (server.getName());
        as.setQueueStatus      (server.getQueueStatus());
        as.setDescription      (server.getDescription());
        as.setDiff             (server.getDiff());
        as.setIgnored          (server.getIgnored());
        as.setServerTypeName   (server.getServerTypeName());
        as.setProductConfig    (server.getProductConfig());
        as.setMeasurementConfig(server.getMeasurementConfig());
        as.setControlConfig    (server.getControlConfig());
        as.setCustomProperties (server.getCustomProperties());
        save(as);
        return as;
    }

    public AIServer findByName(String name)
    {
        String sql="from AIServer where name=?";
        return (AIServer)getSession().createQuery(sql)
            .setString(0, name)
            .uniqueResult();
    }

    public Collection findByPlatformId(Integer platformid)
    {
        String sql="from AIServer where aIPlatform.id=?";
        return getSession().createQuery(sql)
            .setInteger(0, platformid.intValue())
            .list();
    }

}

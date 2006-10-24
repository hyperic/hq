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

package org.hyperic.hibernate.dao;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import org.hyperic.hq.measurement.Category;
import org.hyperic.hq.measurement.MeasurementTemplate;
import org.hyperic.hq.measurement.MonitorableType;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLiteValue;

/**
 * CRUD methods, finders, etc. for MeasurementTemplate
 */
public class MeasurementTemplateDAO extends HibernateDAO
{
    public MeasurementTemplateDAO(Session session) {
        super(MeasurementTemplate.class, session);
    }

    public MeasurementTemplate findById(Integer id) {
        return (MeasurementTemplate)super.findById(id);
    }

    public void evict(MeasurementTemplate entity) {
        super.evict(entity);
    }

    public MeasurementTemplate merge(MeasurementTemplate entity) {
        return (MeasurementTemplate)super.merge(entity);
    }

    public void save(MeasurementTemplate entity) {
        super.save(entity);
    }

    public void remove(MeasurementTemplate entity) {
        super.remove(entity);
    }

    public MeasurementTemplate create(MeasurementTemplateLiteValue lite,
                                      MonitorableType monitorableType,
                                      Category cat,
                                      Collection lineItems) {
        MeasurementTemplate mt = new MeasurementTemplate();

        mt.setName(lite.getName()); 
        mt.setAlias(lite.getAlias());
        mt.setUnits(lite.getUnits());
        mt.setCollectionType(new Integer(lite.getCollectionType()));
        mt.setDefaultOn(lite.getDefaultOn());
        mt.setDefaultInterval(lite.getDefaultInterval());
        mt.setDesignate(lite.getDesignate());
        mt.setPlugin(lite.getPlugin());
        mt.setTemplate(lite.getTemplate());
        mt.setMonitorableType(monitorableType);
        mt.setCategory(cat);
        mt.setMeasurementArgs(lineItems);

        save(mt);
        return mt;
    }

    public List findTemplates(Integer ids[]) {
        StringBuffer buf = 
            new StringBuffer("from MeasurementTemplate where id IN (");
        int len = ids.length;
        for (int i = 0; i < len - 1; i++) {
            buf.append(ids[i] + ", ");
        }
        buf.append(ids[len - 1] + ")");

        return getSession().createQuery(buf.toString()).list();
    }

    public List findTemplatesByMonitorableType(String type) {
        String sql = 
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=?";
        return getSession().createQuery(sql).
            setString(0, type).list();
    }

    public List findTemplatesByMonitorableTypeAndCategory(String type,
                                                          String cat) {
        String sql = 
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "join fetch t.category cat " +
            "where mt.name=? and cat.name=?";
        return getSession().createQuery(sql)
            .setString(0, type)
            .setString(1, cat).list();
    }        
}

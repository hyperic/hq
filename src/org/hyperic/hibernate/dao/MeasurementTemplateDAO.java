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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.measurement.Category;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementTemplate;
import org.hyperic.hq.measurement.MonitorableType;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLiteValue;
import org.hyperic.hq.product.MeasurementInfo;

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
        mt.setCollectionType(lite.getCollectionType());
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

    public void update(MeasurementTemplate mt, String pluginName,
                       MeasurementInfo info) {
        // Load category
        CategoryDAO catDAO = DAOFactory.getDAOFactory().getCategoryDAO();
        Category cat = catDAO.findByName(info.getCategory());
        if (cat == null) {
            cat = catDAO.create(info.getCategory());
        }

        // Update raw template
        mt.setTemplate(info.getTemplate());
        mt.setCollectionType(info.getCollectionType());
        mt.setPlugin(pluginName);
        mt.setCategory(cat);
        save(mt);

        // Update the derived template
        MeasurementTemplate dmt = 
            findByArgAndTemplate(mt.getId(),
                                 MeasurementConstants.TEMPL_IDENTITY);
        
        dmt.setAlias(info.getAlias());
        dmt.setDesignate(info.isIndicator());
        dmt.setUnits(info.getUnits());
        dmt.setCollectionType(info.getCollectionType());
        dmt.setDefaultOn(info.isDefaultOn());
        dmt.setDefaultInterval(info.getInterval());
        dmt.setCategory(cat);
        save(dmt);
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
            "where mt.name=? and t.defaultInterval > 0";
        return getSession().createQuery(sql).
            setString(0, type).list();
    }

    public List findTemplatesByMonitorableTypeAndCategory(String type,
                                                          String cat) {
        String sql = 
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "join fetch t.category cat " +
            "where mt.name=? and t.defaultInterval > 0 and cat.name=?";
        return getSession().createQuery(sql)
            .setString(0, type)
            .setString(1, cat).list();
    }

    public List findDefaultsByMonitorableType(String mt, int appdefType) {
        String sql =
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.defaultInterval > 0 and t.defaultOn = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    public List findDesignatedByMonitorableType(String mt, int appdefType) {
        String sql =
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.defaultInterval > 0 and t.designate = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    public List findRawByMonitorableType(Integer mtId) {
        String sql =
            "from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.id=? and t.defaultInterval=0";

        return getSession().createQuery(sql)
            .setInteger(0, mtId.intValue()).list();
    }

    public List findByMeasurementArg(Integer tId) {
        String sql =
            "from MeasurementTemplate t " +
            "join fetch t.measurementArgs args " +
            "where args.templateArg.id=?";

        return getSession().createQuery(sql)
            .setInteger(0, tId.intValue()).list();
    }

    public MeasurementTemplate findByArgAndTemplate(Integer tId,
                                                    String template) {
        String sql =
            "from MeasurementTemplate t " +
            "join fetch t.measurementArgs args " +
            "where args.templateArg.id=? and t.template=?";
        
        return (MeasurementTemplate)getSession().createQuery(sql)
            .setInteger(0, tId.intValue())
            .setString(1, template).uniqueResult();
    }
}

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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.product.MeasurementInfo;

public class MeasurementTemplateDAO extends HibernateDAO {
    public MeasurementTemplateDAO(DAOFactory f) {
        super(MeasurementTemplate.class, f);
    }

    public MeasurementTemplate get(Integer id) {
        return (MeasurementTemplate)super.get(id);
    }

    public MeasurementTemplate findById(Integer id) {
        return (MeasurementTemplate)super.findById(id);
    }

    /**
     * Remove a MeasurementTemplate and it's associated Measurements.
     */
    void remove(MeasurementTemplate mt) {
        super.remove(mt);
    }

    MeasurementTemplate create(String name, String alias, String units,
                               int collectionType, boolean defaultOn,
                               long defaultInterval, boolean designate,
                               String template, MonitorableType monitorableType,
                               Category cat, String plugin) {
        MeasurementTemplate mt = new MeasurementTemplate(name, alias,
                                                         units, collectionType,
                                                         defaultOn,
                                                         defaultInterval,
                                                         designate,
                                                         template,
                                                         monitorableType,
                                                         cat, plugin);
        save(mt);
        return mt;
    }

    void update(MeasurementTemplate mt, String pluginName,
                MeasurementInfo info) {
        // Load category
        Category cat;
        if (info.getCategory() != null) {
            if (!mt.getCategory().getName().equals(info.getCategory())) {
                CategoryDAO catDAO = DAOFactory.getDAOFactory().getCategoryDAO();
                cat = catDAO.findByName(info.getCategory());
                if (cat == null) {
                    cat = catDAO.create(info.getCategory());
                }
            } else {
                cat = mt.getCategory();
            }
        } else {
            throw new IllegalArgumentException("Category is null");
        }

        // Update the MeasurementTemplate
        mt.setTemplate(info.getTemplate());
        mt.setCollectionType(info.getCollectionType());
        mt.setPlugin(pluginName);
        mt.setCategory(cat);
        save(mt);
    }

    List findAllTemplates(PageInfo pInfo, Boolean defaultOn) {
        String sql = "select t from MeasurementTemplate t";
        
        if (defaultOn != null) {
            sql += " where t.defaultOn = :defaultOn ";
        }
        
        sql += 
            "order by " + 
            ((MeasurementTemplateSortField)pInfo.getSort()).getSortString("t");
        
        Query q = getSession().createQuery(sql);
        if (defaultOn != null) {
            q.setParameter("defaultOn", defaultOn);
        }
        return pInfo.pageResults(q).list();
    }
    
    List findTemplates(Integer[] ids) {
        if (ids.length == 1) {
            Object res = get(ids[0]);
            
            if (res == null)
                return new ArrayList();
            
            return Collections.singletonList(res);
        }
        
        return createCriteria()
            .add(Restrictions.in("id", ids))
            .setCacheable(true)
            .setCacheRegion("MeasurementTemplate.findTemplates")
            .list();
    }

    List findTemplatesByMonitorableType(String type) {
        PageInfo pInfo = 
            PageInfo.getAll(MeasurementTemplateSortField.TEMPLATE_NAME, true);
        return findTemplatesByMonitorableType(pInfo, type, null);
    }

    List findTemplatesByMonitorableType(PageInfo pInfo, String type,
                                        Boolean defaultOn) 
    {
        String sql = 
            "select t from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=:typeName";

        if (defaultOn != null) {
            sql += " and t.defaultOn = :defaultOn";
        }
        
        sql += " order by " +
            ((MeasurementTemplateSortField)pInfo.getSort()).getSortString("t");
            
        Query q = getSession().createQuery(sql)
            .setString("typeName", type);
        
        if (defaultOn != null)
            q.setParameter("defaultOn", defaultOn);
        
        return pInfo.pageResults(q).list();
    }
    
    List findTemplatesByMonitorableTypeAndCategory(String type,
                                                   String cat) {
        String sql = 
            "select t from MeasurementTemplate t " +
            "where t.monitorableType.name=? " +
            "and t.category.name=? " +
            "order by t.name";
        
        return getSession().createQuery(sql)
            .setString(0, type)
            .setString(1, cat).list();
    }

    List findDefaultsByMonitorableType(String mt, int appdefType) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.defaultOn = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    List findDesignatedByMonitorableType(String mt, int appdefType) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join fetch t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.designate = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    List findRawByMonitorableType(MonitorableType mt) {
        String sql =
            "select t from MeasurementTemplate t " +
            "where t.monitorableType=?";

        return getSession().createQuery(sql)
            .setParameter(0, mt).list();
    }
    
    List findDerivedByMonitorableType(String name) {
        // Oracle doesn't like 'distinct' qualifier on select when
        // there are BLOB attributes.  The Oracle exception is
        // (ORA-00932: inconsistent datatypes: expected - got BLOB)
        // I am removing the 'distinct' qualifier so that
        // Oracle does not blow up on the select query.
        // I think the distinct qualifier is unnecessary in this
        // query as the results I believe are already distinct.
        //
        // Some other options which may work with the distinct qualifier:
        // 1. Use HQL projection to selectively return non-binary attributes
        //
        // 2. More exotic solution may be lazy property loading for
        // binary or BLOB attributes(?), but requires hibernate proxy
        // byte code instrumentation (RISKY!).
        //
        String sql =
            "select m from MeasurementTemplate m " +
            "join fetch m.monitorableType mt " +
            "where mt.name = ? " +
            "order by m.name asc ";

        return getSession().createQuery(sql)
            .setString(0, name).list();
    }
}

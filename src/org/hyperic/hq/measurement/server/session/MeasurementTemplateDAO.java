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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLiteValue;
import org.hyperic.hq.product.MeasurementInfo;

public class MeasurementTemplateDAO extends HibernateDAO {
    private final static Log log =
        LogFactory.getLog(MeasurementTemplateDAO.class);

    public MeasurementTemplateDAO(DAOFactory f) {
        super(MeasurementTemplate.class, f);
    }

    public MeasurementTemplate findById(Integer id) {
        return (MeasurementTemplate)super.findById(id);
    }

    /**
     * Remove a raw template and it's associated derived measurement
     */
    void remove(MeasurementTemplate mt) {
        // Update the derived template
        HashSet dm = new HashSet();
        for (Iterator i = mt.getRawMeasurementArgs().iterator(); i.hasNext();) {
            MeasurementArg raw = (MeasurementArg)i.next();
            MeasurementTemplate derived = raw.getTemplate();
            // clear measurement arg collection for cascade delete
            derived.getMeasurementArgs().clear();
            dm.add(derived);
        }
        // clear collection to avoid ObjectDeletedException
        //
        // must clear the raw measurement collection as
        // the derived measurement template also references the same
        // measurement arg instance. If we don't clear rawMeasurement
        // collection, then Hibernate will throw a ObjectDeletedException
        // complaining the RawMeasurement arg will be resaved..
        mt.getRawMeasurementArgs().clear();
        
        // remove all dependent derived measurements and
        // its measurements
        for (Iterator i=dm.iterator(); i.hasNext();) {
            MeasurementTemplate dmt = (MeasurementTemplate)i.next();
            removeMeasurements(dmt);
            super.remove(dmt);
        }
        removeMeasurements(mt);
        super.remove(mt);
    }

    private void removeMeasurements(MeasurementTemplate mt) {
        String sql = "delete Measurement where template.id=?";
        int rows = getSession().createQuery(sql)
            .setInteger(0, mt.getId().intValue())
            .executeUpdate();
        if (log.isDebugEnabled()) {
            log.debug("removed " + rows + " measurements for template id "+
                      mt.getId());
        }
    }

    MeasurementTemplate create(MeasurementTemplateLiteValue lite,
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
            throw new IllegalArgumentException("category has null value");
        }

        // Update raw template
        mt.setTemplate(info.getTemplate());
        mt.setCollectionType(info.getCollectionType());
        mt.setPlugin(pluginName);
        mt.setCategory(cat);
        save(mt);

        // Update the derived template
        for (Iterator i = mt.getRawMeasurementArgs().iterator(); i.hasNext();) {
            MeasurementArg raw = (MeasurementArg)i.next();
            MeasurementTemplate derived = raw.getTemplate();
            if (MeasurementConstants.TEMPL_IDENTITY
                .equals(derived.getTemplate())) {
                derived.setAlias(info.getAlias());
                derived.setDesignate(info.isIndicator());
                derived.setUnits(info.getUnits());
                derived.setCollectionType(info.getCollectionType());
                derived.setDefaultOn(info.isDefaultOn());
                derived.setDefaultInterval(info.getInterval());
                derived.setCategory(cat);
                save(mt);
                return;
            }
        }
    }

    List findTemplates(Integer ids[]) {
        StringBuffer buf = 
            new StringBuffer("from MeasurementTemplate where id IN (");
        int len = ids.length;
        for (int i = 0; i < len - 1; i++) {
            buf.append(ids[i] + ", ");
        }
        buf.append(ids[len - 1] + ")");

        return getSession().createQuery(buf.toString()).list();
    }

    List findTemplatesByMonitorableType(String type) {
        String sql = 
            "select t from MeasurementTemplate t " +
            "join t.monitorableType mt " +
            "where mt.name=? and t.defaultInterval > 0 order by t.name";
        return getSession().createQuery(sql).
            setString(0, type).list();
    }

    List findTemplatesByMonitorableTypeAndCategory(String type,
                                                   String cat) {
        String sql = 
            "select t from MeasurementTemplate t " +
            "join t.monitorableType mt " +
            "join t.category cat " +
            "where mt.name=? and t.defaultInterval > 0 and cat.name=? " +
            "order by t.name";
        
        return getSession().createQuery(sql)
            .setString(0, type)
            .setString(1, cat).list();
    }

    List findDefaultsByMonitorableType(String mt, int appdefType) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.defaultInterval > 0 and t.defaultOn = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    List findDesignatedByMonitorableType(String mt, int appdefType) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join t.monitorableType mt " +
            "where mt.name=? and mt.appdefType=? " +
            "and t.defaultInterval > 0 and t.designate = true " +
            "order by mt.name";
 
        return getSession().createQuery(sql)
            .setString(0, mt)
            .setInteger(1, appdefType).list();
    }

    List findRawByMonitorableType(Integer mtId) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join fetch t.rawMeasurementArgs ra " +
            "join fetch ra.template dt " +
            "join fetch t.category c " +
            "join t.monitorableType mt " +
            "where mt.id=? and t.defaultInterval=0";

        return getSession().createQuery(sql)
            .setInteger(0, mtId.intValue()).list();
    }

    List findByMeasurementArg(Integer tId) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join t.measurementArgs args " +
            "where args.templateArg.id=?";

        return getSession().createQuery(sql)
            .setInteger(0, tId.intValue()).list();
    }

    MeasurementTemplate findByArgAndTemplate(Integer tId,
                                             String template) {
        String sql =
            "select t from MeasurementTemplate t " +
            "join t.measurementArgs args " +
            "where args.templateArg.id=? and t.template=?";
        
        return (MeasurementTemplate)getSession().createQuery(sql)
            .setInteger(0, tId.intValue())
            .setString(1, template).uniqueResult();
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
            "join m.monitorableType mt " +
            "where mt.name = ? and " +
            "m.defaultInterval > 0 " +
            "order by m.name asc ";

        return getSession().createQuery(sql)
            .setString(0, name).list();
    }
}

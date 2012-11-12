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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.product.MeasurementInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MeasurementTemplateDAO
    extends HibernateDAO<MeasurementTemplate> {

    private CategoryDAO catDAO;

    private final Log log = LogFactory.getLog(MeasurementTemplateDAO.class);

    private static final int ALIAS_LIMIT = 100;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public MeasurementTemplateDAO(SessionFactory f, CategoryDAO categoryDAO,
                                  JdbcTemplate jdbcTemplate) {
        super(MeasurementTemplate.class, f);
        this.catDAO = categoryDAO;
        this.jdbcTemplate = jdbcTemplate;
    }

    MeasurementTemplate create(String name, String alias, String units, int collectionType,
                               boolean defaultOn, long defaultInterval, boolean designate,
                               String template, MonitorableType monitorableType, Category cat,
                               String plugin) {
        MeasurementTemplate mt = new MeasurementTemplate(name, alias, units, collectionType,
            defaultOn, defaultInterval, designate, template, monitorableType, cat, plugin);
        save(mt);
        return mt;
    }

    void update(MeasurementTemplate mt, String pluginName, MeasurementInfo info) {
        // Load category
        Category cat;
        if (info.getCategory() != null) {
            if (!mt.getCategory().getName().equals(info.getCategory())) {

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
        mt.setUnits(info.getUnits());

        // Don't reset indicator, defaultOn or interval if it's been
        // changed
        if (mt.getMtime() == mt.getCtime()) {
            mt.setDesignate(info.isIndicator());
            mt.setDefaultOn(info.isDefaultOn());
            mt.setDefaultInterval(info.getInterval());
        }

        save(mt);
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findAllTemplates(PageInfo pInfo, Boolean defaultOn) {
        String sql = "select t from MeasurementTemplate t";

        if (defaultOn != null) {
            sql += " where t.defaultOn = :defaultOn ";
        }

        sql += " order by " + ((MeasurementTemplateSortField) pInfo.getSort()).getSortString("t");

        Query q = getSession().createQuery(sql);
        if (defaultOn != null) {
            q.setParameter("defaultOn", defaultOn);
        }
        return pInfo.pageResults(q).list();
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findTemplates(Integer[] ids) {
        if (ids.length == 1) {
            MeasurementTemplate res = get(ids[0]);

            if (res == null) {
                return new ArrayList<MeasurementTemplate>();
            }

            return Collections.singletonList(res);
        }

        return createCriteria().add(Restrictions.in("id", ids)).setCacheable(true).setCacheRegion(
            "MeasurementTemplate.findTemplates").list();
    }

    List<MeasurementTemplate> findTemplatesByMonitorableType(String type) {
        PageInfo pInfo = PageInfo.getAll(MeasurementTemplateSortField.TEMPLATE_NAME, true);
        return findTemplatesByMonitorableType(pInfo, type, null);
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findTemplatesByMonitorableType(PageInfo pInfo, String type,
                                                             Boolean defaultOn) {
        String sql = "select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
                     + "where mt.name=:typeName";

        if (defaultOn != null) {
            sql += " and t.defaultOn = :defaultOn";
        }

        sql += " order by " + ((MeasurementTemplateSortField) pInfo.getSort()).getSortString("t");

        Query q = getSession().createQuery(sql).setString("typeName", type);

        if (defaultOn != null)
            q.setParameter("defaultOn", defaultOn);

        return pInfo.pageResults(q).list();
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findTemplatesByMonitorableTypeAndCategory(String type, String cat) {
        String sql = "select t from MeasurementTemplate t " + "where t.monitorableType.name=? "
                     + "and t.category.name=? " + "order by t.name";

        return getSession().createQuery(sql).setString(0, type).setString(1, cat).list();
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findTemplatesByName(List<String> tmpNames) {
    	if (tmpNames==null || tmpNames.size()==0) {
    	    throw new IllegalArgumentException("no template names passed to MeasurementTemplateImpl.findTemplatesByName()");
    	}
        StringBuilder sql = new StringBuilder().append("select t from MeasurementTemplate t where");
    	for (int i=0 ; i<tmpNames.size()-1 ; i++) {
    		sql.append(" alias=? or");
		}
        sql.append(" alias=? order by t.alias");
        Query getTmpQuery = getSession().createQuery(sql.toString());
        if (getTmpQuery==null) {
            throw new HibernateException("failed creating template retrieval by name query");
        }
        int i=0;
        for (String tmpName : tmpNames) {
        	getTmpQuery.setString(i++, tmpName);
		}
        List<MeasurementTemplate> tmpsRes = getTmpQuery.list();
        
        return tmpsRes;
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findDefaultsByMonitorableType(String mt, int appdefType) {
        String sql = "select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
                     + "where mt.name=? and mt.appdefType=? " + "and t.defaultOn = true "
                     + "order by mt.name";

        return getSession().createQuery(sql).setString(0, mt).setInteger(1, appdefType).list();
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findDesignatedByMonitorableType(String mt, int appdefType) {
        String sql = "select t from MeasurementTemplate t " + "join fetch t.monitorableType mt "
                     + "where mt.name=? and mt.appdefType=? " + "and t.designate = true "
                     + "order by mt.name";

        return getSession().createQuery(sql).setString(0, mt).setInteger(1, appdefType).list();
    }

    @SuppressWarnings("unchecked")
    List<MeasurementTemplate> findRawByMonitorableType(MonitorableType mt) {
        String sql = "select t.id from MeasurementTemplate t where t.monitorableType=?";
        List<Integer> tids = getSession().createQuery(sql).setParameter(0, mt).list();
        List<MeasurementTemplate> rtn = new ArrayList<MeasurementTemplate>(tids.size());
        for (Integer tid : tids) {
            MeasurementTemplate templ = get(tid);
            if (templ == null) {
                continue;
            }
            rtn.add(templ);
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public List<MeasurementTemplate> findDerivedByMonitorableType(String name) {
        // Oracle doesn't like 'distinct' qualifier on select when
        // there are BLOB attributes. The Oracle exception is
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
        String sql = "select m from MeasurementTemplate m " + "join fetch m.monitorableType mt "
                     + "where mt.name = ? " + "order by m.name asc ";

        return getSession().createQuery(sql).setString(0, name).list();
    }

    public void createTemplates(final String pluginName,
                                final Map<MonitorableType,List<MonitorableMeasurementInfo>> toAdd) {
        final IdentifierGenerator tmplIdGenerator = ((SessionFactoryImpl) sessionFactory)
            .getEntityPersister(MeasurementTemplate.class.getName()).getIdentifierGenerator();

        final String templatesql = "INSERT INTO EAM_MEASUREMENT_TEMPL "
                                   + "(id, name, alias, units, collection_type, default_on, "
                                   + "default_interval, designate, monitorable_type_id, "
                                   + "category_id, template, plugin, ctime, mtime) "
                                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final List<MonitorableMeasurementInfo> combinedInfos = new ArrayList<MonitorableMeasurementInfo>();
        for(List<MonitorableMeasurementInfo> info : toAdd.values()) {
            combinedInfos.addAll(info);
        }
        final long current = System.currentTimeMillis();
        //We need JdbcTemplate to throw runtime Exception to roll back tx if batch update fails, else we'll get partial write
        jdbcTemplate.batchUpdate(templatesql, new BatchPreparedStatementSetter() {
            HashMap<String, Category> cats = new HashMap<String, Category>();
            public void setValues(PreparedStatement stmt, int i) throws SQLException {
                
                MeasurementInfo info = combinedInfos.get(i).getMeasurementInfo();
                Category cat = (Category) cats.get(info.getCategory());
                if (cat == null) {
                    cat = catDAO.findByName(info.getCategory());
                    if (cat == null) {
                        cat = catDAO.create(info.getCategory());
                    }
                    cats.put(info.getCategory(), cat);
                }
                Integer rawid = (Integer) tmplIdGenerator.generate((SessionImpl) getSession(),
                    new MeasurementTemplate());
                stmt.setInt(1, rawid.intValue());
                stmt.setString(2, info.getName());
                String alias = info.getAlias();
                if (alias.length() > ALIAS_LIMIT) {
                    alias = alias.substring(0, ALIAS_LIMIT);
                    log
                        .warn("ALIAS field of EAM_MEASUREMENT_TEMPLATE truncated: original value was " +
                              info.getAlias() + ", truncated value is " + alias);
                }
                stmt.setString(3, alias);
                stmt.setString(4, info.getUnits());
                stmt.setInt(5, info.getCollectionType());
                stmt.setBoolean(6, info.isDefaultOn());
                stmt.setLong(7, info.getInterval());
                stmt.setBoolean(8, info.isIndicator());
                stmt.setInt(9, combinedInfos.get(i).getMonitorableType().getId().intValue());
                stmt.setInt(10, cat.getId().intValue());
                stmt.setString(11, info.getTemplate());
                stmt.setString(12, pluginName);
                stmt.setLong(13, current);
                stmt.setLong(14, current);
            }

            public int getBatchSize() {
                return combinedInfos.size();
            }
        });
    }
}

package org.hyperic.hq.measurement.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.impl.SessionImpl;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.product.MeasurementInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

public class MeasurementTemplateRepositoryImpl implements MeasurementTemplateCustom {

    private JdbcTemplate jdbcTemplate;

    private final Log log = LogFactory.getLog(MeasurementTemplateRepositoryImpl.class);

    private static final int ALIAS_LIMIT = 100;

    private CategoryRepository categoryRepository;

    private EntityManagerFactory entityManagerFactory;

    @Autowired
    public MeasurementTemplateRepositoryImpl(JdbcTemplate jdbcTemplate,
                                             CategoryRepository categoryRepository,
                                             EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryRepository = categoryRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Transactional
    public void createTemplates(final String pluginName,
                                final Map<MonitorableType, List<MonitorableMeasurementInfo>> toAdd) {
        final IdentifierGenerator tmplIdGenerator = getIdentifierGenerator(MeasurementTemplate.class
            .getName());

        final String templatesql = "INSERT INTO EAM_MEASUREMENT_TEMPL "
                                   + "(id, name, alias, units, collection_type, default_on, "
                                   + "default_interval, designate, monitorable_type_id, "
                                   + "category_id, template, plugin, ctime, mtime, version_col) "
                                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final List<MonitorableMeasurementInfo> combinedInfos = new ArrayList<MonitorableMeasurementInfo>();
        for (List<MonitorableMeasurementInfo> info : toAdd.values()) {
            combinedInfos.addAll(info);
        }
        final long current = System.currentTimeMillis();
        // We need JdbcTemplate to throw runtime Exception to roll back tx if
        // batch update fails, else we'll get partial write
        jdbcTemplate.batchUpdate(templatesql, new BatchPreparedStatementSetter() {
            HashMap<String, Category> cats = new HashMap<String, Category>();

            public void setValues(PreparedStatement stmt, int i) throws SQLException {
                MeasurementInfo info = combinedInfos.get(i).getMeasurementInfo();
                Category cat = (Category) cats.get(info.getCategory());
                if (cat == null) {
                    cat = categoryRepository.findByName(info.getCategory());
                    if (cat == null) {
                        cat = new Category(info.getCategory());
                        categoryRepository.save(cat);
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
                    log.warn("ALIAS field of EAM_MEASUREMENT_TEMPLATE truncated: original value was " +
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
                stmt.setLong(15, 0);
            }

            public int getBatchSize() {
                return combinedInfos.size();
            }
        });
    }

    private IdentifierGenerator getIdentifierGenerator(String className) {
        return ((SessionFactoryImplementor) getSession().getSessionFactory()).getEntityPersister(
            className).getIdentifierGenerator();
    }

    private Session getSession() {
        return (Session) EntityManagerFactoryUtils.getTransactionalEntityManager(
            entityManagerFactory).getDelegate();
    }

}

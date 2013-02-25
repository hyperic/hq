package org.hyperic.hq.notifications.filtering;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.jdom.filter.ContentFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationsFilterDAO extends HibernateDAO<Filter>{
    protected final Log log = LogFactory.getLog(NotificationsFilterDAO.class.getName());
    @Autowired
    protected MetricFilterDAO metricFilterDAO;
    @Autowired
    protected MetricFilterByResourceDAO metricFilterByResourceDAO;
    @Autowired
    protected ResourceContentFilterDAO resourceContentFilterDAO;

    @Autowired
    protected NotificationsFilterDAO(SessionFactory f) {
        super(Filter.class, f);
    }

    public void save(List<? extends Filter<? extends BaseNotification, ? extends FilteringCondition<?>>> filters) {
        for(Filter<? extends BaseNotification, ? extends FilteringCondition<?>> filter:filters) {
            super.save(filter);
//            if (filter instanceof MetricFilter) {
//                this.metricFilterDAO.save((MetricFilter) filter);
//            } else if (filter instanceof MetricFilterByResource) {
//                this.metricFilterByResourceDAO.save((MetricFilterByResource) filter);
//            } else if (filter instanceof ResourceContentFilter) {
//                this.resourceContentFilterDAO.save((ResourceContentFilter) filter);
//            }
        }
        getSession().flush();
    }
    public Integer create(Class<BaseNotification> entityType,
            List<? extends Filter<? extends BaseNotification, ? extends FilteringCondition<?>>> filters) {
        this.save(filters);
        return null;
    }

}
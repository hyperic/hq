package org.hyperic.hq.notifications;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RegistrationDAO extends HibernateDAO<xxxxxx>{
    protected final Log log = LogFactory.getLog(RegistrationDAO.class.getName());

    @Autowired
    protected RegistrationDAO(SessionFactory f) {
        super(xxxx.class, f);
    }

    public Integer create(Class<BaseNotification> entityType,
            List<Filter<BaseNotification, ? extends FilteringCondition<?>>> userFilters) {
        return null;
    }

}

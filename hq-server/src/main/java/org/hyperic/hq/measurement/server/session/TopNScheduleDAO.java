package org.hyperic.hq.measurement.server.session;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TopNScheduleDAO extends HibernateDAO<TopNSchedule> {

    @Autowired
    protected TopNScheduleDAO(SessionFactory f) {
        super(TopNSchedule.class, f);
    }

    @Override
    public void save(TopNSchedule entity) {
        super.save(entity);
        getSession().flush();
    }

}

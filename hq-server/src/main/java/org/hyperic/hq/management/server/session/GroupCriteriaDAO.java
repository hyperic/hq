package org.hyperic.hq.management.server.session;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.management.shared.GroupCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class GroupCriteriaDAO extends HibernateDAO<GroupCriteria> {

    @Autowired
    public GroupCriteriaDAO(SessionFactory f) {
        super(GroupCriteria.class, f);
    }
    
}

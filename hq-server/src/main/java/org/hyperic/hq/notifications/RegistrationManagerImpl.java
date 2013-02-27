package org.hyperic.hq.notifications;

import java.util.List;

import org.hibernate.Session;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.NotificationsFilterDAO;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

@Component
public class RegistrationManagerImpl implements RegistrationManager {
    @Autowired
    protected DestinationEvaluator evaluator;
    @Autowired
    protected NotificationsFilterDAO filterDAO;

    @Transactional(readOnly=false)
    public Integer register(Class<? extends BaseNotification> entityType,
            List<? extends Filter<? extends BaseNotification, ? extends FilteringCondition<?>>> userFilters) {
//        Integer regID = registrationDAO.create(entityType);
//        for(Filter<? extends BaseNotification, ? extends FilteringCondition<?>> filter:userFilters) {
            this.filterDAO.create(null, userFilters);
//        }
//        this.evaluator.register(regID,entityType,userFilters);
//        return regID;
        return new Integer(5);
    }
    
    @Transactional(readOnly=false)
    public void unregister(Integer regID) {
        //TODO~ remove from DB
        this.evaluator.unregister(regID);
    }
    
    @PostConstruct
    @Transactional(readOnly=true)
    public boolean loadRegistrations() {
        //TODO~ load user files and entityType from DB
        //TODO~ bind thread to hibernate session as per OpenSessionInViewFilter.doFilterInternal()
        Session session = getSession(sessionFactory);
        TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
//        List filters = this.filterDAO.findAll();
//        this.evaluator.register(regID,entityType,userFilters);
        return true;
    }
}

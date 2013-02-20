package org.hyperic.hq.notifications;

import java.util.List;

import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InventoryNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class RegistrationManagerImpl implements RegistrationManager {
    @Autowired
    protected DestinationEvaluator evaluator;

    @Transactional(readOnly=false)
    public Integer register(Class<? extends BaseNotification> entityType,
            List<? extends Filter<? extends BaseNotification, ? extends FilteringCondition<?>>> userFilters) {
        Integer regID = registrationDAO.create(entityType);
        for(Filter<? extends BaseNotification, ? extends FilteringCondition<?>> filter:userFilters) {
            
        }
        this.evaluator.register(regID,entityType,userFilters);
        return regID;
    }
    
    @Transactional(readOnly=false)
    public void unregister(Integer regID) {
        //TODO~ remove from DB
        this.evaluator.unregister(regID);
    }
    
    public boolean loadRegistrations() {
        //TODO~ load user files and entityType from DB
        this.evaluator.register(regID,entityType,userFilters);
        return true;
    }
}

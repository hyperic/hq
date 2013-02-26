package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.notifications.model.BaseNotification;
/**
 * 
 * @author yakarn
 *
 * @param <N> defines the type of entities this filter handles
 */
public abstract class Filter<N extends BaseNotification, C extends FilteringCondition<?>> extends PersistedObject {
    //TODO~  change filterType to ENUM:
//    protected String filterType;
    //TODO~ remove regID and make it a one-to-many relation to new reg table
    protected Long regID = new Long(5);
    protected C cond;
    protected abstract Class<? extends N> getHandledNotificationClass();
//    protected abstract String initFilterType();
   
    public Filter(C cond) {
        this.cond=cond;
//        this.filterType = initFilterType();
    }

    public List<N> filter(List<N> notifications) {
        List<N> notificationsLeftIn = new ArrayList<N>();
        for(N notification:notifications) {
            if (!getHandledNotificationClass().isAssignableFrom(notification.getClass())) {
                notificationsLeftIn.add(notification);
                continue;
            }
            N notificationLeftIn = this.filter((N)notification);
            if (notificationLeftIn!=null) {
                notificationsLeftIn.add(notificationLeftIn);
            }
        }
        return notificationsLeftIn;
    }
    /**
     * 
     * @param notification
     * @return the notification in case it passed the filtering condition check, o/w - null
     */
    protected abstract N filter(N notification);
    
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null || !(obj instanceof Filter)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Filter<N, C> other = (Filter<N, C>) obj;
        if (this.cond==null) {
            return other.cond==null;
        }
        return this.cond.equals(other.cond);
    }
//    protected String getFilterType() {
//        return this.filterType;
//    }
//    protected void setFilterType(String filterType) {
//        this.filterType=filterType;
//    }
    public Long getRegID() {
        return regID;
    }
    public void setRegID(Long regID) {
        this.regID = regID;
    }

    public C getCond() {
        return cond;
    }

    public void setCond(C cond) {
        this.cond = cond;
    }
}
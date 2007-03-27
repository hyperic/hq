package org.hyperic.hq.ui.rendit.metaclass

/**
 * This class holds onto data that is used by the categories
 */
class CategoryInfo {
    private static final ThreadLocal USER = new ThreadLocal() 
    
    static void setUser(user) {
        USER.set(user)
    }

    static getUser() {
        USER.get()
    }
}

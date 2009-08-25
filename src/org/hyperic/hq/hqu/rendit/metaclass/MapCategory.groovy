package org.hyperic.hq.hqu.rendit.metaclass

class MapCategory {
    /**
     * Checks if the value of a key is non-null and an array of size 1 
     *
     * Useful for dealing with parameter checking in controllers
     */
    static boolean hasOne(Map m, String key) {
        m.get(key) != null && m.get(key).size() == 1
    }

    /**
     * Gets the first element in an array of the value of a key, if it exists.
     *
     * I.e.:   [foo:[1, 2, 3]].getOne('foo') -> 1
     */
    static Object getOne(Map m, String key) {
        def res = m.get(key)
        if (res != null && res.size() > 0)
            return res[0]
        return null
    }
     
    /**
     * Get the first element in an array of the value of a key, else use
     * the passed default
     */
    static Object getOne(Map m, String key, String defalt) {
        Object res = getOne(m, key)
        if (res == null)
            return defalt
        return res
    }
}

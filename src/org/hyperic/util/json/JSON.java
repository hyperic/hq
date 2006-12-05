package org.hyperic.util.json;

import org.json.JSONObject;

/**
 * Objects implementing this interface can be converted to JSON objects.
 */
public interface JSON {
    /**
     * Convert the object to a Json object
     */
    public JSONObject toJSON();

    public String getJsonName();
}

package org.hyperic.hq;

import org.json.JSONObject;
import org.json.JSONException;

/**
 */
public interface Json
{
    public JSONObject toJSON() throws JSONException;
    public String getJsonName();
}

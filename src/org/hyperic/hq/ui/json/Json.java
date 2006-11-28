package org.hyperic.hq.ui.json;

import org.json.JSONObject;
import org.json.JSONException;

/**
 */
public interface Json
{
    public JSONObject toJSON() throws JSONException;
}

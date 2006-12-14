package org.hyperic.hq.ui.json.action.escalation.crud;

import org.json.JSONObject;
import org.json.JSONException;

/**
 */
class ActionData
{
    int id;
    long _version_;
    long waitTime;

    ActionData(String[]idarr, String[]varr, String[] timearr)
    {
        if (idarr != null) {
            id = Integer.valueOf(idarr[0]).intValue();
            _version_ = Long.valueOf(varr[0]).longValue();
        }
        waitTime = Long.valueOf(timearr[0]).longValue();
    }

    JSONObject toJSON() throws JSONException
    {
        JSONObject json = new JSONObject();
        if (id > 0) {
            json.put("id", id)
                .put("_version_", _version_);
        }
        return json;
    }
}
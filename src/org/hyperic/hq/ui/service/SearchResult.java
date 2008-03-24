package org.hyperic.hq.ui.service;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Bean that describes a search result for the search service.
 * 
 */
public class SearchResult {

    private String _name;
    private String _resType;
    private String _adeId;

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getResType() {
        return _resType;
    }

    public void setResType(String resType) {
        _resType = resType;
    }

    public String getAdeId() {
        return _adeId;
    }

    public void setAdeId(String adeId) {
        _adeId = adeId;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("name", getName()).put("resType",
                getResType()).put("eId", getAdeId());
    }
}

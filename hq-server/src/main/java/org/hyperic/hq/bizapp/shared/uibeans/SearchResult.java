/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.bizapp.shared.uibeans;

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

    public SearchResult(String name, String type, String id) {
        super();
        _name = name;
        _resType = type;
        _adeId = id;
    }

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

    public String toString() {
        return _name + " (" + _adeId + ")";
    }
}

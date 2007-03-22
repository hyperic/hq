/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.livedata.shared;

import org.json.JSONObject;
import org.json.JSONArray;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Iterator;

public class LiveDataTranslator {

    public static JSONArray encode(Object o) throws Exception {

        JSONArray jarr = new JSONArray();

        if (o.getClass().isArray()) {
            int len = Array.getLength(o);
            for (int i = 0; i < len; i++) {
                Object element = Array.get(o, i);
                JSONObject jobj = translate(element);
                jarr.put(jobj);
            }
        } else {
            JSONObject jobj = translate(o);
            jarr.put(jobj);
        }

        return jarr;
    }

    // XXX: handle embedded arrays
    private static JSONObject translate(Object o)
        throws Exception
    {
        JSONObject json = new JSONObject();

        Map props = PropertyUtils.describe(o);
        for (Iterator i = props.keySet().iterator(); i.hasNext(); ) {
            String method = (String)i.next();
            Object val = PropertyUtils.getProperty(o, method);
            json.put(method, val);
        }

        return json;
    }
}

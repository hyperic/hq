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

package org.hyperic.hq.ui.json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.Writer;
import java.io.IOException;

public class JSONResult
{
    // mutually exclusive
    private JSONArray array;
    private JSONObject object;

    public JSONResult(JSONArray arr)
    {
        array = arr;
    }

    public JSONResult(JSONObject obj)
    {
        object = obj;
    }

    public void write(Writer w, boolean pretty)
            throws IOException, JSONException
    {
        if (array != null) {
            if (pretty) {
                w.write(array.toString(2));
            } else {
                array.write(w);
            }
        } else if (object != null) {
            if (pretty) {
                w.write(object.toString(2));
            } else {
                object.write(w);
            }
        }
    }
    
    public String writeToString(Writer w, boolean pretty)
            throws IOException, JSONException
    {
    	String outcome=null;
        if (array != null) {
            if (pretty) {
            	 w.write( array.toString(2) );
            	 
            } else {
                array.write(w);
            }
            outcome = array.toString(2);
        } else if (object != null) {
            if (pretty) {
                w.write(object.toString(2));
            } else {
                object.write(w);
            }
            outcome = object.toString(2);
        }
        
        return outcome;
    }
}

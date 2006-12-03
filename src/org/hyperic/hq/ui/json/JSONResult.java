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
}

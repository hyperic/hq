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
            w.write(pretty ? array.toString(2) : array.toString());
        } else if (object != null) {
            w.write(pretty ? object.toString(2) : object.toString());
        }
    }
}

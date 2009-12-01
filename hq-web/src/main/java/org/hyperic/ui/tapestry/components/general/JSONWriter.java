package org.hyperic.ui.tapestry.components.general;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.json.IJSONWriter;
import org.apache.tapestry.json.JSONObject;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class JSONWriter extends BaseComponent {
  
    @Parameter(name = "key")
    public abstract String getKeyName();

    @Parameter(name = "data")
    public abstract JSONObject getData();

    public void renderComponent(IJSONWriter writer, IRequestCycle cycle){
        if (!cycle.isRewinding()) {
            writer.object().put(getKeyName(), getData());
        }
    }

}

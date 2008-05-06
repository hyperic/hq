package org.hyperic.ui.tapestry.components.grid;

import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectScript;
import org.hyperic.ui.tapestry.components.BaseComponent;
import org.json.JSONObject;

public abstract class Grid extends BaseComponent {

    /**
     * 
     * @return the javascript for the grid
     */
    @InjectScript("Grid.script")
    public abstract IScript getScript();

    /**
     * process the template and javascript
     */
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        super.renderComponent(writer, cycle);
        Map<String, String> map = new HashMap<String, String>();
        PageRenderSupport pageRenderSupport = TapestryUtils
                .getPageRenderSupport(cycle, this);
        getScript().execute(this, cycle, pageRenderSupport, map);
    }

    /**
     * 
     * @return
     */
    public JSONObject getColumnSchema() {

        return new JSONObject();
    }
}

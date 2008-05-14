package org.hyperic.ui.tapestry.components.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class TabContainer extends BaseComponent{

    @InjectScript("TabContainer.script")
    public abstract IScript getScript();
    
    @Parameter(name="conaterStyle", defaultValue="literal:width:100%;height:100%;margin-right:5px")
    public abstract String getContainerStyle();

    @Parameter(name="tabs")
    public abstract List<Tab> getTabs();
    
    @Parameter(name="selectedId")
    public abstract String getSelectedId();
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        if (!cycle.isRewinding()) {
            Map map = new HashMap();
            PageRenderSupport pageRenderSupport = TapestryUtils
                    .getPageRenderSupport(cycle, this);
            getScript().execute(this, cycle, pageRenderSupport, map);
        }
        super.renderComponent(writer, cycle);
    }
    
}
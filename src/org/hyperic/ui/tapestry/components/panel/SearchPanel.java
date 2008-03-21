package org.hyperic.ui.tapestry.components.panel;

import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;

public abstract class SearchPanel extends BaseComponent{
    
    /**
     * Should the enter, escape and ctrl-s keystrokes be captured for 
     * this component
     * @return are hotkeys enabled
     */
    @Parameter(name = "enableHotkeys", defaultValue = "ognl:true")
    public abstract boolean getEnableHotkeys();
    public abstract void setEnableHotkeys(boolean enable);
    
    @InjectScript("SearchPanel.script")
    public abstract IScript getScript();
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
        super.renderComponent(writer, cycle);
        if (!cycle.isRewinding()) {
	    if (getEnableHotkeys()) {
		Map<String, Object> map = new HashMap<String, Object>();
		PageRenderSupport pageRenderSupport = TapestryUtils
			.getPageRenderSupport(cycle, this);
		getScript().execute(this, cycle, pageRenderSupport, map);
	    }
        }
    }
}

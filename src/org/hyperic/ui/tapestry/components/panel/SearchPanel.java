package org.hyperic.ui.tapestry.components.panel;

import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.engine.ILink;

public abstract class SearchPanel extends BaseComponent{
    
    /**
     * Should the enter, escape and ctrl-s keystrokes be captured for 
     * this component
     * @return are hotkeys enabled
     */
    @Parameter(name = "enableHotkeys", defaultValue = "ognl:true")
    public abstract boolean getEnableHotkeys();
    public abstract void setEnableHotkeys(boolean enable);

    /**
     * The JSON object that specifies 
     * @return the hotkey combination
     */
    @Parameter(name = "hotKey", defaultValue = "literal:{keyCode: 83, ctrl: true}")
    public abstract String getHotkey();
    public abstract void setHotkey(String hotKey);

    @InjectScript("SearchPanel.script")
    public abstract IScript getScript();
    
    @InjectObject("infrastructure:searchService")
    public abstract IEngineService getSearchService();
   
    /**
     * Get the URL for the search service
     * Used for SearchPanel.script for the search XHR
     * @return the URL.toString()
     */
    public String getSearchServiceURL(){
        ILink link = getSearchService().getLink(false, null);
        return link.getURL();
    }
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
        super.renderComponent(writer, cycle);
        if (!cycle.isRewinding()) {
            if (getEnableHotkeys()) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("enableHotkeys", getEnableHotkeys());
                map.put("keyCombination", getHotkey());
                map.put("searchURL", getSearchServiceURL());
                map.put("resourcePageURL", "Resource.do");
                PageRenderSupport pageRenderSupport = TapestryUtils
                .getPageRenderSupport(cycle, this);
                getScript().execute(this, cycle, pageRenderSupport, map);
            }
        }
    }
}

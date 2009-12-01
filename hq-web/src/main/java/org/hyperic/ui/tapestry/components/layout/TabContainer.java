package org.hyperic.ui.tapestry.components.layout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectComponent;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.components.Block;
import org.apache.tapestry.link.AbstractLinkComponent;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class TabContainer extends BaseComponent {

    @InjectScript("TabContainer.script")
    public abstract IScript getScript();

    @InjectComponent("link")
    public abstract AbstractLinkComponent getLinkComponent();

    @Parameter(name = "conaterStyle", defaultValue = "literal:width:100%;height:100%;margin-right:5px")
    public abstract String getContainerStyle();

    @Parameter(name = "conatinerId")
    public abstract String getContainerId();

    @Parameter(name = "tabs")
    public abstract List<Tab> getTabs();

    @Parameter(name = "selectedId")
    public abstract String getSelectedId();
    public abstract void setSelectedId(String selectedId);
    
    @Parameter(name = "tabClass", defaultValue="literal:tabButton")
    public abstract String getTabClass();

    public abstract Tab getTab();
    public abstract void setTab(Tab tab);

    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        super.renderComponent(writer, cycle);
    }
    
    /**
     * 
     * @param selectedId the currently selected tab id
     */
    public void tabClicked(String selectedId) {
        setSelectedId(selectedId);
    }

    /**
     * 
     * @return the currently selected block. If no block is selected, select the
     *         first
     */
    public Block getSelectedBlock() {
        Iterator<Tab> i = getTabs().iterator();
        while (i.hasNext()) {
            Tab t = i.next();
            if (t.getId().equals(getSelectedId())) {
                return t.getBlock();
            }
        }
        return getTabs().get(0).getBlock();
    }
}
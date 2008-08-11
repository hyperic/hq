package org.hyperic.ui.tapestry.components.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry.IActionListener;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.form.IPropertySelectionModel;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class CBGModifyDialog extends BaseComponent {

    
    @Parameter(name ="groupName", defaultValue="", required=true)
    public abstract String getGroupName();
    public abstract void setGroupName(String name);
    
    @Parameter(name = "modifyListener", required = true)
    public abstract IActionListener getModifyListener();
    public abstract void setModifyListener(IActionListener listener);
    
    @Parameter(name = "selectedAddItems", required = true)
    public abstract void setSelectedAddItems(List items);
    public abstract List getSelectedAddItems();
    
    @Parameter(name = "selectedRemoveItems", required = true)
    public abstract void setSelectedRemoveItems(List items);
    public abstract List getSelectedRemoveItems();
    
    @Parameter(name = "availableAddItems", required = true)
    public abstract void setAvailableAddItems(IPropertySelectionModel model);
    public abstract IPropertySelectionModel getAvailableAddItems();
    
    @Parameter(name = "availableRemoveItems", required = true)
    public abstract void setAvailableRemoveItems(IPropertySelectionModel model);
    public abstract IPropertySelectionModel getAvailableRemoveItems();
    
    @InjectScript("CBGModifyDialog.script")
    public abstract IScript getScript();

    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        super.renderComponent(writer, cycle);
        if (!cycle.isRewinding()) {
            Map<String, Object> map = new HashMap<String, Object>();
            PageRenderSupport pageRenderSupport = TapestryUtils.getPageRenderSupport(cycle, this);
            getScript().execute(this, cycle, pageRenderSupport, map);
        }
    }
}

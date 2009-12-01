package org.hyperic.ui.tapestry.components.dialog;

import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry.IActionListener;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IScript;
import org.apache.tapestry.PageRenderSupport;
import org.apache.tapestry.TapestryUtils;
import org.apache.tapestry.annotations.InjectScript;
import org.apache.tapestry.annotations.Parameter;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class CBGDeleteDialog extends BaseComponent {

    @Parameter(name = "deleteListener", required = true)
    public abstract IActionListener getDeleteListener();
    public abstract void setDeleteListener(IActionListener listener);
    
    @Parameter(name = "groupName")
    public abstract String getGroupName();
    public abstract void setGroupName(String name);
    
    @InjectScript("CBGDeleteDialog.script")
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

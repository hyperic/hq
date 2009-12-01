package org.hyperic.ui.tapestry.components.panel;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IActionListener;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.components.Block;
import org.apache.tapestry.listener.ListenerInvoker;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class ListPanel extends BaseComponent {
    private final Log _log = LogFactory.getLog(ListPanel.class);
    
    public interface ListItem {
        String getId();
        String getLabel();
    }

    @Persist
    public abstract void setCurrentSelection(String id);
    public abstract String getCurrentSelection();
    
    @Parameter(name = "listItems", required = true)
    public abstract void setListItems(List listItems);
    public abstract List getListItems();
    
    @Parameter(name = "selectListener", required = true)
    public abstract void setSelectListener(IActionListener listener);
    public abstract IActionListener getSelectListener();

    @InjectObject("infrastructure:listenerInvoker")
    public abstract ListenerInvoker getListenerInvoker();
    
    public Block getSelectedBlock() {
        return (Block)getContainer().getComponent("selectedBlock");
    }
    
    public Block getUnselectedBlock() {
        return (Block)getContainer().getComponent("unselectedBlock");
    }

    /**
     * Used for iterating over the collection.
     */
    public abstract void setItem(ListItem item);
    public abstract ListItem getItem();

    public boolean getLoopedItemIsSelected() {
        String curId = getCurrentSelection();
        
        if (curId == null)
            return false;
        
        return curId.equals(getItem().getId());
    }
    
    /**
     * Listener, called when an item is selected via DirectLink. 
     */
    public void selectItem(IRequestCycle cycle, String id) {
        _log.info("ListPanel selected id = " + id);
        setCurrentSelection(id);
        cycle.getResponseBuilder().updateComponent("listContents");
        getListenerInvoker().invokeListener(getSelectListener(), this, cycle);
    }
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
       super.renderComponent(writer, cycle);
    }
}

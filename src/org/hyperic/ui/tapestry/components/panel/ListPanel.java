package org.hyperic.ui.tapestry.components.panel;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.IActionListener;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.listener.ListenerInvoker;
import org.hyperic.ui.tapestry.components.BaseComponent;

public abstract class ListPanel extends BaseComponent {
    private final Log _log = LogFactory.getLog(ListPanel.class);
    
    public interface ListItem {
        String getId();
        String getLabel();
    }

    @Parameter(name = "listItems", required = true)
    public abstract void setListItems(List listItems);
    public abstract List getListItems();
    
    @Parameter(name = "selectListener", required = true)
    public abstract void setSelectListener(IActionListener listener);
    public abstract IActionListener getSelectListener();

    @InjectObject("infrastructure:listenerInvoker")
    public abstract ListenerInvoker getListenerInvoker();
    
    /**
     * Used for iterating over the collection.
     */
    public abstract void setItem(ListItem item);
    public abstract ListItem getItem();

    /**
     * Listener, called when an item is selected via DirectLink. 
     */
    public void selectItem(IRequestCycle cycle, String id) {
        _log.debug("ListPanel selected id = " + id);
        getListenerInvoker().invokeListener(getSelectListener(), this, cycle);
    }
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
       super.renderComponent(writer, cycle);
    }
}

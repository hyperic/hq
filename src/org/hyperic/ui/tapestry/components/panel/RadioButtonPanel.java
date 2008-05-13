package org.hyperic.ui.tapestry.components.panel;

import java.util.List;

import org.apache.tapestry.IActionListener;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.components.Block;
import org.apache.tapestry.listener.ListenerInvoker;
import org.hyperic.ui.tapestry.components.BaseComponent;

/**
 * The RadioButton panel provides a series of radio buttons and a
 * 'current selection' value.
 */
public abstract class RadioButtonPanel extends BaseComponent {
    public interface Button {
        String getId();
        String getText();
        String getStyleClass();
        boolean isDepressed();
    };
    
    /**
     * Takes a list of {@link Button}s
     */
    @Parameter(name = "buttons", required = true)
    public abstract void setButtons(List listItems);
    public abstract List getButtons();
    
    @Persist
    public abstract void setCurrentSelection(String id);
    public abstract String getCurrentSelection();

    @Parameter(name = "selectListener", required = true)
    public abstract void setSelectListener(IActionListener listener);
    public abstract IActionListener getSelectListener();
    
    @InjectObject("infrastructure:listenerInvoker")
    public abstract ListenerInvoker getListenerInvoker();
    
    /**
     * sets/gets the button that is currently being worked on, when rendering
     */
    public abstract void setButton(Button b);
    public abstract Button getButton();

    public Block getButtonBlock() {
        return (Block)getContainer().getComponent("buttonBlock");
    }
    
    public void selectButton(IRequestCycle cycle, String id) {
        setCurrentSelection(id);
        getListenerInvoker().invokeListener(getSelectListener(), this, cycle);
    }
    
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle){
       super.renderComponent(writer, cycle);
    }
}

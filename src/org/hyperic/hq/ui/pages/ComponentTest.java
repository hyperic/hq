package org.hyperic.hq.ui.pages;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.Persist;

public abstract class ComponentTest extends BasePage {

    @Persist
    public abstract String getValue();
    public abstract void setValue(String value);
    
    @Persist
    public abstract String getValue1();
    public abstract void setValue1(String value);
    
    @Persist
    public abstract String getValue2();
    public abstract void setValue2(String value);

    public void buttonListener(IRequestCycle cycle) {
    }
    
    /**
     * This placeholder test was added so that this unit test would not fail. 
     */
    public final void testPlaceHolder() {}

}

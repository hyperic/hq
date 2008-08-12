package org.hyperic.ui.tapestry.components.dialog;

import java.util.ArrayList;
import java.util.Collection;
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

public abstract class CBGAddDialog extends BaseComponent {
    
    public abstract Integer getCritterIndex();
    public abstract void setCritterIndex(Integer index);

    public abstract CritterCriteria getCritter();
    public abstract void setCritter(CritterCriteria type);
    
    /**
     * @return Is this an Any or All condition
     */
    @Parameter(name = "isAny", defaultValue = "ognl:true")
    public abstract String getIsAny();
    public abstract void setIsAny(String value);

    /**
     * @return The List of critters created
     */
    @Parameter(name = "criteriaList", required = true)
    public abstract List<CritterCriteria> getCritterList();
    public abstract void setCritterList(List<CritterCriteria> critters);

    /**
     * @return the selection model for the avail CritterPropTypes
     */
    @Parameter(name = "critterPropTypeModel", required = true)
    public abstract IPropertySelectionModel getCritterTypeList();
    public abstract void setCritterTypeList(IPropertySelectionModel list);

    /**
     * @return binding for the onCreate listener
     */
    @Parameter(name = "createListener", required = true)
    public abstract IActionListener getCreateListener();
    public abstract void setCreateListener(IActionListener listener);
    
    /**
     * @return the components to update after form submit
     */
    @Parameter(name = "updateComponents")
    public abstract Collection<String> getUpdateComponents();
    public abstract void setUpdateComponents(Collection<String> components);
    
    @Parameter(name = "groupName")
    public abstract String getGroupName();
    public abstract void setGroupName(String name);
    
    @InjectScript("CBGAddDialog.script")
    public abstract IScript getScript();

    /**
     * row has been deleted
     * @param index
     */
    public void onDeleteRow(Integer index) {
        getCritterList().remove(index);
    }

    /**
     * Add a row to the end of the List
     * @param cycle 
     */
    public void onAddRow(IRequestCycle cycle) {
        //add an item to the cbg list
        List<CritterCriteria> critterList = getCritterList();
        critterList.add(new CritterCriteriaImpl());
        setCritterList(critterList);
        
    }

    /**
     * Ok button has been clicked in the dialog - update the selected list
     * @param cycle
     */
    public void onCreateListener(IRequestCycle cycle) {
        //reset the list for the next group creates
        onCancelListener(cycle);
    }

    /**
     * Cancel button has been clicked, clear the selected criteria list
     * @param cycle
     */
    public void onCancelListener(IRequestCycle cycle) {
        //TODO clear out the current list
        List list = new ArrayList();
        list.add(new CritterCriteriaImpl());
        setCritterList(list);
        
    }
    
    public void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        super.renderComponent(writer, cycle);
        if (!cycle.isRewinding()) {
            Map<String, Object> map = new HashMap<String, Object>();
            PageRenderSupport pageRenderSupport = TapestryUtils.getPageRenderSupport(cycle, this);
            getScript().execute(this, cycle, pageRenderSupport, map);
        }
    }

}

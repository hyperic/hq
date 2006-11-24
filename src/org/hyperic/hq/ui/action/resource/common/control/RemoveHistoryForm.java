package org.hyperic.hq.ui.action.resource.common.control;

import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the
 * <em>RemoveControlAction</em> form.
 */
public class RemoveHistoryForm extends BaseValidatorForm {

    //-------------------------------------instance variables
    
    /** Holds value of property controlActions. */
    private Integer[] controlActions;
    
    //-------------------------------------constructors

    public RemoveHistoryForm() {
    }

    //-------------------------------------public methods

    public String toString() {
        if (controlActions == null)
            return "empty";
        else
            return controlActions.toString();    
    }
    
    /** Getter for property controlAction.
     * @return Value of property controlAction.
     *
     */
    public Integer[] getControlActions() {
        return this.controlActions;
    }
    
    /** Setter for property controlAction.
     * @param controlAction New value of property controlAction.
     *
     */
    public void setControlActions(Integer[] controlActions) {
        this.controlActions = controlActions;
    }
    
}

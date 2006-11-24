package org.hyperic.hq.ui.action.resource.common.control;

import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the
 * <em>RemoveControlJobsAction</em> form.
 *
 * This is a [] of ControlActionSchedule.triggerNames's.
 */
public class RemoveControlJobsForm extends BaseValidatorForm  {

    //-------------------------------------instance variables
    
    /** Holds value of property controlJobs. */
    private Integer[] controlJobs;
    
    //-------------------------------------constructors

    public RemoveControlJobsForm() {
    }

    //-------------------------------------public methods

    public String toString() {
        if (controlJobs == null)
            return "empty";
        else
            return controlJobs.toString();    
    }
    
    /** Getter for property controlJobs
     * @return Value of property controlJobs.
     *
     */
    public Integer[] getControlJobs() {
        return this.controlJobs;
    }
    
    /** Setter for property controlJobs
     * @param controlAction New value of property controlJobs.
     *
     */
    public void setControlJobs(Integer[] controlJobs) {
        this.controlJobs = controlJobs;
    }
    
}

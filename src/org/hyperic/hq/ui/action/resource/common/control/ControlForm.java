package org.hyperic.hq.ui.action.resource.common.control;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.ScheduleForm;

/**
 * A subclass of <code>ScheduleForm</code> representing the
 * <em>Control</em> form data. 
 *
 * @see org.hyperic.hq.ui.action.ScheduleForm
 */
public class ControlForm extends ScheduleForm  {

    private String controlAction;
    private String description;
    
    public String getControlAction() {
        return this.controlAction;
    }

    public void setControlAction(String a) {
        this.controlAction = a;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.description = null;
        this.controlAction = null;
        super.reset(mapping, request);
    }

    public ActionErrors validate(ActionMapping mapping, 
        HttpServletRequest request) {
        ActionErrors errs = null;
        
        if (!shouldValidate(mapping, request)) {
            return null;
        }
        
        errs = super.validate(mapping, request);        
        if (errs == null ) {
            errs = new ActionErrors();
        }
        
        return errs;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("controlAction= ").append(controlAction);
        buf.append(" description= ").append(description);
        return super.toString() + buf.toString();
    }
}

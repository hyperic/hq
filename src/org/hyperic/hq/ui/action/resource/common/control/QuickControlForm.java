package org.hyperic.hq.ui.action.resource.common.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> that contatins all of the properties
 * for scheduling an action.
 */
public class QuickControlForm extends BaseValidatorForm  {

    /** Holds value of property resourceId. */
    private Integer resourceId;
    
    /** Holds value of property resourceType. */
    private Integer resourceType;
    
    /** Holds value of property resourceAction
     * to be performed. 
     */
    private String resourceAction;
    
    /** Holds value of property controlActions. */
    private List controlActions;
    
    /** Holds value of property numControlActions. */
    private Integer numControlActions;

    /** Holds value of property arguments */
    private String arguments;
    
    //-------------------------------------instance variables


    //-------------------------------------constructors

    public QuickControlForm() {
        super();
    } 

    //-------------------------------------public methods

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        resourceAction = null;
        controlActions = null;
        numControlActions = null;
        arguments = null;
        resourceId = null;
        super.reset(mapping, request);
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors ae = new ActionErrors();
        // XXX make sure that is a valid resourceType
        return super.validate(mapping, request);
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("resourceId= ").append(resourceId);
        s.append("resourceAction= ").append(resourceAction);
        s.append("controlActions= ").append(controlActions);
        s.append("numControlActions= " ).append(numControlActions);
        s.append("arguments= ").append(arguments);
        return s.toString();
    }
    
    /** Getter for property resourceId.
     * @return Value of property resourceId.
     *
     */
    public Integer getResourceId() {
        return this.resourceId;
    }
    
    /** Setter for property resourceId.
     * @param resourceId New value of property resourceId.
     *
     */
    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }
    
    /** Getter for property resourceType.
     * @return Value of property resourceType.
     *
     */
    public Integer getResourceType() {
        return this.resourceType;
    }
    
    /** Setter for property resourceType.
     * @param resourceType New value of property resourceType.
     *
     */
    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }
    
    /** Getter for property resourceAction.
     * @return Value of property resourceAction.
     *
     */
    public String getResourceAction() {
        return this.resourceAction;
    }
    
    /** Setter for property resourceAction.
     * @param resourceAction New value of property resourceAction.
     *
     */
    public void setResourceAction(String resourceAction) {
        this.resourceAction = resourceAction;
    }
    
    /** Getter for property controlActions.
     * @return Value of property controlActions.
     *
     */
    public List getControlActions() {
        return this.controlActions;
    }
    
    /** Setter for property controlActions.
     * @param controlActions New value of property controlActions.
     *
     */
    public void setControlActions(List controlActions) {
        this.controlActions = controlActions;
    }
    
    /** Getter for property numControlActions.
     * @return Value of property numControlActions.
     *
     */
    public Integer getNumControlActions() {
        return this.numControlActions;
    }
    
    /** Setter for property numControlActions.
     * @param numControlActions New value of property numControlActions.
     *
     */
    public void setNumControlActions(Integer numControlActions) {
        this.numControlActions = numControlActions;
    }

    public String getArguments() {
        return this.arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}

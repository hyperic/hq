/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.validator.ValidatorForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for
 * dealing with image-based form buttons.
 */
public class BaseValidatorForm
    extends ValidatorForm {

    // -------------------------------------instance variables

    private ImageButtonBean add;
    private ImageButtonBean cancel;
    private ImageButtonBean create;
    private ImageButtonBean delete;
    private ImageButtonBean ok;
    private ImageButtonBean okassign;
    private Integer pageSize;
    private ImageButtonBean reset;
    private ImageButtonBean remove;
    private ImageButtonBean enable;
    private ImageButtonBean userset;

    /** Holds value of property pn. */
    private Integer pn;

    // -------------------------------------constructors

    private void setDefaults() {
        add = new ImageButtonBean();
        cancel = new ImageButtonBean();
        create = new ImageButtonBean();
        delete = new ImageButtonBean();
        ok = new ImageButtonBean();
        okassign = new ImageButtonBean();
        pageSize = null;
        reset = new ImageButtonBean();
        remove = new ImageButtonBean();
        enable = new ImageButtonBean();
        userset = new ImageButtonBean();
    }

    public BaseValidatorForm() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    public void setAdd(ImageButtonBean add) {
        this.add = add;
    }

    public ImageButtonBean getAdd() {
        return this.add;
    }

    public void setCancel(ImageButtonBean cancel) {
        this.cancel = cancel;
    }

    public ImageButtonBean getCancel() {
        return this.cancel;
    }

    public void setCreate(ImageButtonBean create) {
        this.create = create;
    }

    public ImageButtonBean getCreate() {
        return this.create;
    }

    public void setDelete(ImageButtonBean delete) {
        this.delete = delete;
    }

    public ImageButtonBean getDelete() {
        return this.delete;
    }

    public void setOk(ImageButtonBean ok) {
        this.ok = ok;
    }

    public ImageButtonBean getOk() {
        return this.ok;
    }

    public Integer getPs() {
        return this.pageSize;
    }

    public void setPs(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setRemove(ImageButtonBean remove) {
        this.remove = remove;
    }

    public ImageButtonBean getRemove() {
        return this.remove;
    }

    public void setReset(ImageButtonBean reset) {
        this.reset = reset;
    }

    public ImageButtonBean getReset() {
        return this.reset;
    }

    public void setEnable(ImageButtonBean enable) {
        this.enable = enable;
    }

    public ImageButtonBean getEnable() {
        return this.enable;
    }

    public void setUserset(ImageButtonBean userset) {
        this.userset = userset;
    }

    public ImageButtonBean getUserset() {
        return this.userset;
    }

    /**
     * Setter for property p.
     * @param p New value of property p.
     * 
     */
    public void setPn(Integer pn) {
        this.pn = pn;
    }

    /**
     * Getter for property p.
     * @return Value of property p.
     * 
     */
    public Integer getPn() {
        return this.pn;
    }

    /**
     * Sets the okAdd.
     * @param okAdd The okAdd to userset
     */
    public void setOkassign(ImageButtonBean okAdd) {
        this.okassign = okAdd;
    }

    /**
     * @return ImageButtonBean
     */
    public ImageButtonBean getOkassign() {
        return okassign;
    }

    public boolean isAddClicked() {
        return getAdd().isSelected();
    }

    public boolean isCancelClicked() {
        return getCancel().isSelected();
    }

    public boolean isCreateClicked() {
        return getCreate().isSelected();
    }

    public boolean isDeleteClicked() {
        return getDelete().isSelected();
    }

    public boolean isOkClicked() {
        return getOk().isSelected();
    }

    public boolean isOkAssignClicked() {
        return getOkassign().isSelected();
    }

    public boolean isRemoveClicked() {
        return getRemove().isSelected();
    }

    public boolean isResetClicked() {
        return getReset().isSelected();
    }

    public boolean isEnableClicked() {
        return getEnable().isSelected();
    }

    public boolean isUsersetClicked() {
        return getUserset().isSelected();
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    /**
     * Only validate if 1) the form's ok or okassign button was clicked and 2)
     * the mapping specifies an input form to return to.
     * 
     * condition #2 can be false when a form has failed validation and has
     * forwarded to the input page; the ok button request parameter will still
     * be userset, but the prepare action for the input page will not have
     * (another) input page specified.
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

        if (shouldValidate(mapping, request)) {
            ActionErrors errs = super.validate(mapping, request);
            return errs;
        } else {
            return null;
        }
    }

    /*
     * Only validate if 1) the form's ok or okassign button was clicked and 2)
     * the mapping specifies an input form to return to.
     * 
     * Child classes should call this to decide whether or not to perform custom
     * validation steps.
     */
    protected boolean shouldValidate(ActionMapping mapping, HttpServletRequest request) {
        return (isOkClicked() || isOkAssignClicked()) && mapping.getInput() != null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("add=");
        s.append(add);
        s.append(" cancel=");
        s.append(cancel);
        s.append(" create=");
        s.append(create);
        s.append(" delete=");
        s.append(delete);
        s.append(" ok=");
        s.append(ok);
        s.append(" remove=");
        s.append(remove);
        s.append(" reset=");
        s.append(reset);
        s.append(" enable=");
        s.append(enable);
        s.append(" userset=");
        s.append(userset);
        s.append(" pageSize=");
        s.append(pageSize);

        return s.toString();
    }

}

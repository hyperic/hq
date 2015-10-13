package org.hyperic.hq.ui.action;

import javax.servlet.http.HttpServletRequest;
import org.hyperic.hq.ui.util.ImageButtonBean;

public class BaseValidatorFormNG  {

	private String mode;
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

	public BaseValidatorFormNG() {
        super();
        setDefaults();
	}
    
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


    public void reset() {
        setDefaults();
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

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}



}

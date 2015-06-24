package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;

public class AddResourcesFormNG extends ResourceFormNG {
    // -------------------------------------instance variables

    private String[] availableResources;
    private String[] pendingResources;
    private Integer psa;
    private Integer psp;
    private List availResourceTypes;
    private String key;
    private String ft;
    private Integer ff;
    private HashMap<String,String> functions;
    private HashMap<String,String> types;
    private String nameFilter;
    private String token;

    // -------------------------------------constructors

    public AddResourcesFormNG() {
        super();
        setDefaults();
    }

    // -------------------------------------public methods

    public String[] getAvailableResource() {
        return this.availableResources;
    }

    public String[] getAvailableResources() {
        return getAvailableResource();
    }

    public void setAvailableResource(String[] availableResource) {
        this.availableResources = availableResource;
    }

    public void setAvailableResources(String[] availableResources) {
        setAvailableResource(availableResources);
    }

    public String[] getPendingResource() {
        return this.pendingResources;
    }

    public String[] getPendingResources() {
        return getPendingResource();
    }

    public void setPendingResource(String[] pendingResource) {
        this.pendingResources = pendingResource;
    }

    public void setPendingResources(String[] pendingResources) {
        setPendingResource(pendingResources);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.availableResources = new String[0];
        this.pendingResources = new String[0];
        this.psa = null;
        this.psp = null;
        super.reset(mapping, request);
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");

        s.append("availableResources={");
        listToString(s, availableResources);
        s.append("} ");

        s.append("pendingResources={");
        listToString(s, pendingResources);
        s.append("}");

        return s.toString();
    }

    private void listToString(StringBuffer s, String[] l) {
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                s.append(l[i]);
                if (i < l.length - 1) {
                    s.append(", ");
                }
            }
        }
    }

    /**
     * @return List
     */
    public List getAvailResourceTypes() {
        return availResourceTypes;
    }

    /**
     * Sets the availResourceTypes.
     * @param availResourceTypes The availResourceTypes to set
     */
    public void setAvailResourceTypes(List availResourceTypes) {
        this.availResourceTypes = availResourceTypes;
    }

    /**
     * Getter for property key.
     * @return Value of property key.
     * 
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Setter for property key.
     * @param key New value of property key.
     * 
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Getter for property ft.
     * @return Value of property ft.
     * 
     */
    public String getFt() {
        return this.ft;
    }

    /**
     * Setter for property ft.
     * @param ft New value of property ft.
     * 
     */
    public void setFt(String ft) {
        this.ft = ft;
    }

    /**
     * Getter for property ff.
     * @return Value of property ff.
     * 
     */
    public Integer getFf() {
        return this.ff;
    }

    /**
     * Setter for property ff.
     * @param ff New value of property ff.
     * 
     */
    public void setFf(Integer ff) {
        this.ff = ff;
    }

    /**
     * Getter for property functions.
     * @return Value of property functions.
     * 
     */
    public HashMap<String,String> getFunctions() {
        return this.functions;
    }

    /**
     * Setter for property functions.
     * @param functions New value of property functions.
     * 
     */
    public void setFunctions(HashMap<String,String> functions) {
        this.functions = functions;
    }

    public void addFunction(String key, String val) {
        if (this.functions != null) {
        	this.functions.put(key, val);
        }
    }

    /**
     * Getter for property types.
     * @return Value of property types.
     * 
     */
    public HashMap<String,String> getTypes() {
        return this.types;
    }

    /**
     * Setter for property types.
     * @param types New value of property types.
     * 
     */
    public void setTypes(HashMap<String,String> types) {
        this.types = types;
    }

    public void addType(String key, String val) {
        if (this.types != null) {
            this.types.put(key, val);
        }
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // ******************** support methods ***********************************
    private void setDefaults() {
        ff = null;
        ft = null;
        functions = new LinkedHashMap<String,String>();
        types = new LinkedHashMap<String,String>();
        nameFilter = null;
        token = null;
    }

}

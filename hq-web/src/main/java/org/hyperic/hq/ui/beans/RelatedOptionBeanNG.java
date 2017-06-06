package org.hyperic.hq.ui.beans;

import java.util.Map;

public class RelatedOptionBeanNG {

    private String label;
    private String value;
	private Map relatedOptions;
    
    public RelatedOptionBeanNG(String label, String value, Map relatedOptions) {
        this.label = label;
        this.value = value;
        this.relatedOptions = relatedOptions;
    }

    public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Map getRelatedOptions() {
        return relatedOptions;
    }
    
    public void setRelatedOptions(Map relatedOptions) {
        this.relatedOptions = relatedOptions;
    }
    
}

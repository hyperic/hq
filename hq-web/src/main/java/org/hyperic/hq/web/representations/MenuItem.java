package org.hyperic.hq.web.representations;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
	private String id;
	private String label;
	private String url;
	private List<MenuItem> subMenuItems;
	
	public String getId() {
		if (id == null) {
			id = label;
		}
		
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public List<MenuItem> getSubMenuItems() {
		if (subMenuItems == null) {
			subMenuItems = new ArrayList<MenuItem>();
		}
		
		return subMenuItems;
	}

	public void setSubMenuItems(List<MenuItem> subMenuItems) {
		this.subMenuItems = subMenuItems;
	}
}
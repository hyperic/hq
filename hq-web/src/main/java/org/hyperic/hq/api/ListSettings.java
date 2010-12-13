package org.hyperic.hq.api;

public class ListSettings {
	private Integer page = 1;
	private Integer size = 100;
	
	public Integer getPage() {
		return page;
	}
	public void setPage(Integer page) {
		this.page = page;
	}
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
}


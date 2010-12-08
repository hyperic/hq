package org.hyperic.hq.api;

public interface Entity {
	public Long getId();
	public void setId(Long id);
	public void create();
	public void update();
	public void delete();
}


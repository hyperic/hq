package org.hyperic.hq.inventory.domain;

public interface PersistenceAware<T> {
	public void persist();
	public T merge();
	public void remove();
}


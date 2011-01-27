package org.hyperic.hq.inventory.dao;

import java.util.List;

public interface GenericDao<T> {
	public T findById(Integer id);
	public List<T> findAll();
	public List<T> find(Integer firstResult, Integer maxResults);
	public Long count();
	public T findByName(String name);
}


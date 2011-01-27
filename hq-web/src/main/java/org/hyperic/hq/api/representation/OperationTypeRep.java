package org.hyperic.hq.api.representation;

import org.hyperic.hq.inventory.domain.OperationType;

public class OperationTypeRep {
	private Integer id;
	private String name;
	
	public OperationTypeRep() {}
	
	public OperationTypeRep(OperationType operationType) {
		id = operationType.getId();
		name = operationType.getName();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}


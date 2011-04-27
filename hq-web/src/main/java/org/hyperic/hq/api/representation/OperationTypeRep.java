package org.hyperic.hq.api.representation;

import org.hyperic.hq.inventory.domain.OperationType;

public class OperationTypeRep {
	
	private String name;
	
	public OperationTypeRep() {}
	
	public OperationTypeRep(OperationType operationType) {
		name = operationType.getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}


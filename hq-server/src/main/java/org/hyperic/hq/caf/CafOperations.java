package org.hyperic.hq.caf;

/**
 * An enum representing CAF operations
 */
public enum CafOperations {
	
	DEPLOY_AGENT("Hyperic:AgentDeployer:1.0", "install_agent");
	
	String fqc;
	String operation;
	
	private CafOperations(String fqc, String operation) {
		this.fqc = fqc;
		this.operation = operation;
	}
	
	public String getFqc() {
		return fqc;
	}
	
	public String getOperation() {
		return operation;
	}
}

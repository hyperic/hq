package org.hyperic.hq.caf;

import java.util.List;

import com.vmware.commonagent.contracts.exceptions.CafException;

/**
 * Local interface for CAF operations, all CAF related operations will
 * be executed and handled via this CafManager
 */
public interface CafManager {

	/**
	 * Deploys a new agent via CAF, sends an install_agent request to all the
	 * CAFs in the cafIds list. The provider that gets the request installs the agent
	 * and configures it according to parameters passed from the server. This provides 
	 * a fully automated deployment of agents.
	 * @param user - during the agent installation the installer needs to provide 
	 * the server a user name for the admin user.
	 * @param password - during the agent installation the installer needs to provide 
	 * the server a password for the admin user.
	 * @param cafIds - a list of caf ids
	 * @return - a message containing the execution results (it's a string because for
	 * now this will be called from the groovy console and we want the user to get
	 * a nice feedback about the execution results).
	 * @throws CafException
	 * 
	 * For now in order to execute this request use the groovy console with this script - 
	 * import org.hyperic.hq.caf.CafManagerImpl
	 * import org.hyperic.hq.context.Bootstrap
	 * def CAF = Bootstrap.getBean(CafManagerImpl)
	 * CAF.deployNewAgent("hqadmin","hqadmin",["first-id", "second-is", ..])
	 */
	public String deployNewAgent(String user, String password, List<String> cafIds)
			 throws CafException;
	 
	/**
	 * Deploys a new agent via CAF, sends an install_agent request to all the
	 * CAFs in the cafIds list. The provider that gets the request installs the agent
	 * and configures it according to parameters passed from the server. This provides 
	 * a fully automated deployment of agents.
	 * @param user - during the agent installation the installer needs to provide 
	 * the server a user name for the admin user.
	 * @param password - during the agent installation the installer needs to provide 
	 * the server a password for the admin user.
	 * @param serverIp - what is the ip of the server the agent should connect to
	 * @param cafIds - a list of caf ids
	 * @return - a message containing the execution results (it's a string because for
	 * now this will be called from the groovy console and we want the user to get
	 * a nice feedback about the execution results).
	 * @throws CafException
	 * 
	 * For now in order to execute this request use the groovy console with this script - 
	 * import org.hyperic.hq.caf.CafManagerImpl
	 * import org.hyperic.hq.context.Bootstrap
	 * def CAF = Bootstrap.getBean(CafManagerImpl)
	 * CAF.deployNewAgent("hqadmin","hqadmin", "ip", ["first-id", "second-is", ..])
	 */
	public String deployNewAgent(String user, String password, String serverIp, List<String> cafIds) 
			 throws CafException;
	

	/**
	 * Starts the agent on a CAF machine
	 * @param cafId - the id of the CAF 
	 */
	public CafResponse startAgent(String cafId);
	
	/**
	 * Stops the agent on a CAF machine
	 * @param cafId - the id of the CAF 
	 */
	public CafResponse stopAgent(String cafId);
	
	/**
	 * Restarts the agent on a CAF machine
	 * @param cafId - the id of the CAF 
	 */
	public CafResponse restartAgent(String cafId);
	 
}

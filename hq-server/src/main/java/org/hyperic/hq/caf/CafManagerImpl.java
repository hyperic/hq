package org.hyperic.hq.caf;

import static org.hyperic.hq.caf.CafOperations.DEPLOY_AGENT;
import static org.hyperic.hq.caf.CafOperations.RESTART_AGENT;
import static org.hyperic.hq.caf.CafOperations.START_AGENT;
import static org.hyperic.hq.caf.CafOperations.STOP_AGENT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.commonagent.contracts.exceptions.CafException;

//TODO: Guys 13/12/2012 - temporarily removed so as to disable until implementation is copmleted
//@Service
public class CafManagerImpl implements CafManager{

	private static final Logger log = LoggerFactory.getLogger(CafManagerImpl.class);
	private final CafRequestsExeutor cafRequestExecutor;
	private final AuthManager authManager;
	private final AuthzSubjectManager authzSubjectManager;
	private final PermissionManager permissionManager;
	private final AIQueueManager aiQueueManager;
	private final AgentManager agentManager;
	private String serverPort;
	private String serverSecurePort;

	/**
	 * @param cafRequestExecutor
	 * @param authManager
	 * @param authzSubjectManager
	 * @param permissionManager
	 */
	@Autowired
	public CafManagerImpl(CafRequestsExeutor cafRequestExecutor, AuthManager authManager, 
			AuthzSubjectManager authzSubjectManager, PermissionManager permissionManager,
			AIQueueManager aiQueueManager, AgentManager agentManager) {
		this.cafRequestExecutor = cafRequestExecutor;
		this.authManager = authManager;
		this.authzSubjectManager = authzSubjectManager;
		this.permissionManager = permissionManager;
		this.aiQueueManager = aiQueueManager;
		this.agentManager = agentManager;
	}
	
	/* (non-Javadoc)
	 * @see org.hyperic.hq.caf.CafManager#startAgent(java.lang.String)
	 */
	public CafResponse startAgent(String cafId) {
		return executeAgentStopStartRestartOperation(cafId, START_AGENT);
	}
	
	/* (non-Javadoc)
	 * @see org.hyperic.hq.caf.CafManager#stopAgent(java.lang.String)
	 */
	public CafResponse stopAgent(String cafId) {
		return executeAgentStopStartRestartOperation(cafId, STOP_AGENT);
	}
	
	/* (non-Javadoc)
	 * @see org.hyperic.hq.caf.CafManager#restartAgent(java.lang.String)
	 */
	public CafResponse restartAgent(String cafId) {
		return executeAgentStopStartRestartOperation(cafId, RESTART_AGENT);
	}
	
	/**
	 * Executes stop/start/restart agent operation via CAF
	 * @param cafId - the CAF id
	 * @param operation - the operation to execute
	 * @return CafResponse
	 */
	private CafResponse executeAgentStopStartRestartOperation(String cafId, CafOperations operation) {
		Map<UUID,CafResponse> responses;
		Set<UUID> requests;
		Map<String, Object> params;
		UUID requestId;
		Agent agent = agentManager.findAgentsByCafId(cafId);
		if (null == agent) {
			CafResponse response = new CafResponse();
			response.setErrorMessage("There is no agent installed on CAF '" + cafId + "'");
			return response;
		}
		requestId = UUID.randomUUID();
		params = new HashMap<String, Object>();
		params.put("agent_directory", agentManager.getAgentInstallationPath(agent.getAgentToken()));
		cafRequestExecutor.executeInvokeOperation(requestId, cafId, operation.getFqc(), 
				operation.getOperation(), params);
		requests = new HashSet<UUID>();
		requests.add(requestId);
		responses = waitAndGetResults(requests);
		if (null == responses.get(requestId)) {
			CafResponse response = new CafResponse();
			response.setErrorMessage("Did not recieve any response in time");
			return response;
		}
		return responses.get(requestId);
	}

	/* (non-Javadoc)
	 * @see org.hyperic.hq.caf.CafManager#deployNewAgent(java.lang.String, java.lang.String, java.lang.String)
	 */
	public String deployNewAgent(String user, String password, List<String> cafIds) 
			throws CafException {
		return deployNewAgent(user, password, getDefaultIpAddress(), cafIds);
	}

	/* (non-Javadoc)
	 * @see org.hyperic.hq.caf.CafManager#deployNewAgent(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public String deployNewAgent(final String user, String password, String serverIp, List<String> cafIds) 
			throws CafException {
		UUID requestId;
		Map<UUID, String> requestToPmeId;
		Map<UUID,CafResponse> responses;
		Map<String, Object> params;
		String message = "";
		try {
			log.info("Deploying new agents to CAFs '" + cafIds + "'");
			requestToPmeId = new HashMap<UUID, String>();
			checkDeployAgentPermissions(user, password);
			params = new HashMap<String, Object>();
			//The next 2 line are temporary until we will
			//be able to send the agent file as an attachment to the request
			params.put("path_to_agent_file", "/tmp");
			params.put("file_name", "hyperic-hqee-agent-5.0.BUILD-SNAPSHOT-noJRE.tar.gz");
			params.put("to_directory", "/opt/hyperic");
			params.put("ip", serverIp);
			params.put("port", serverPort);
			params.put("secure_port", serverSecurePort);
			params.put("username", user);
			params.put("password", password);

			for (String pmeId : cafIds) {
				Agent agent = agentManager.findAgentsByCafId(pmeId);
				//There is an agent installed on this CAF, don't reinstall
				if (null != agent) {
					message += "An agent is already installed on CAF '" + pmeId + "'\n\n";
					continue;
				}
				requestId = UUID.randomUUID();
				requestToPmeId.put(requestId, pmeId);
				cafRequestExecutor.executeInvokeOperation(requestId, pmeId, DEPLOY_AGENT.getFqc(), 
						DEPLOY_AGENT.getOperation(), params);
			}
		}
		catch (Exception e) {
			log.warn("Could not execute deploy agent operation - " + e.getMessage());
			throw new CafException(e.getMessage());
		}
		responses = waitAndGetResults(requestToPmeId.keySet());
		addNewDeployedAgentsToInventory(user, responses);
		return buildDeployAgentResultsMessage(requestToPmeId, responses) + message;
	}

	/**
	 * Tries to automatically add all the new deployed agents to the
	 * server's inventory by extracting all the agent tokens from the responses
	 * and calling the AIQueueManager to add them to the inventory
	 * @param user
	 * @param responses
	 */
	private void addNewDeployedAgentsToInventory(final String user,
			Map<UUID, CafResponse> responses) {
		
		final List<String> tokens = getDeployedAgentsTokens(responses);
		if (tokens.isEmpty()) {
			return;
		}
		
		new Thread(new Runnable(){		
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				try {
					PageList<AIPlatformValue> platforms = aiQueueManager.getQueue(
							authzSubjectManager.findSubjectByAuth(user, 
									HQConstants.ApplicationName), true, true, false, PageControl.PAGE_ALL);

					for (AIPlatformValue platform : platforms) {
						if (!tokens.contains(platform.getAgentToken())) {
							continue;
						}
						List<Integer> platformIds = new ArrayList<Integer>();
						List<Integer> sevrerIds = new ArrayList<Integer>();
						List<Integer> platformIps = new ArrayList<Integer>();

						if (platform.getQueueStatus() != AIQueueConstants.Q_STATUS_PLACEHOLDER) {
							platformIds.add(platform.getId());
						}
						for (AIServerValue server : platform.getAIServerValues()) {
							sevrerIds.add(server.getId());
						}
						for(AIIpValue ip : platform.getAIIpValues()) {
							platformIps.add(ip.getId());
						}

						aiQueueManager.processQueue(authzSubjectManager.findSubjectByAuth(user, 
								HQConstants.ApplicationName), platformIds, sevrerIds, platformIps,
								AIQueueConstants.Q_DECISION_APPROVE);
					}

				} catch (Exception e) {
				}
			}
		}).run();
	}
	
	
	/**
	 * Check if the user has permissions to deploy an agent,
	 * an PermissionException will be thrown if the user does not 
	 * have permissions
	 * @param user
	 * @param password
	 * @throws SubjectNotFoundException
	 * @throws PermissionException
	 */
	private void checkDeployAgentPermissions(String user, String password)
			throws SubjectNotFoundException, PermissionException {
		authManager.authenticate(user, password);
		AuthzSubject subject = authzSubjectManager.findSubjectByAuth(user, 
				HQConstants.ApplicationName);
		permissionManager.checkCreatePlatformPermission(subject);
	}
	
	/**
	 * Extract all the new deployed agents' tokens from the responses
	 * @param responses
	 */
	private List<String> getDeployedAgentsTokens(Map<UUID, CafResponse> responses) {
		List<String> tokens = new ArrayList<String>();
		String pattern = "(\\d)*-(\\d)*-(\\d)*";
		for (CafResponse response : responses.values()) {
				for(String line : response.getProviderStdout()) {
					String agentToken = line.trim();
					if (agentToken.matches(pattern)) {
						tokens.add(agentToken);
						try {
							agentManager.updateAgentCafId(agentToken, response.getPmeIdStr());
						} catch (AgentNotFoundException e) {				
						}
						break;
					}
				}
			
		}
		return tokens;
	}

	/**
	 * Builds a human friendly message about the deploy agents execution results,
	 * this is needed because for now the deploy agents operation will be invoked 
	 * from the groovy console and we want to give the user a nice feedback about
	 * what happened. 
	 * @param requestToPmeId
	 * @param responses
	 */
	private String buildDeployAgentResultsMessage(Map<UUID, String> requestToPmeId,
			Map<UUID, CafResponse> responses) {
		String message = "";
		for (UUID id : requestToPmeId.keySet()) {
			if (null != responses.get(id)) {
				if (!responses.get(id).getProviderStdout().isEmpty()) {
					if (responses.get(id).getProviderStdout().contains("- Successfully setup agent")) {
						message += "Successfully deployed new agent to CAF '" + 
								responses.get(id).getPmeIdStr() + "'\n";
					}
				}
				else {
					String errorMessage = "Problem deploying new agent to Caf '" +
							responses.get(id).getPmeIdStr() + "' - \n";
					for (String line : responses.get(id).getProviderStderr()) {
						errorMessage += line + "\n";
					}
					message += errorMessage;
				}
			}
			else {
				message += "Did not recieve any reply from CAF '" 
						+ requestToPmeId.get(id) + "'\n";
			}
			message += "\n";
		}
		return message;
	}

	/**
	 * Waits until all the responses have arrived or until a timeout of 200 seconds has passed,
	 * this is needed because for now the deploy agents operation will be invoked 
	 * from the groovy console and we want the user to wait until there are some results.
	 * @param requestsId
	 */
	private Map<UUID, CafResponse> waitAndGetResults(Set<UUID> requestsId) {
		Map<UUID, CafResponse> responses = new HashMap<UUID, CafResponse>();
		for (int i=0; i < 100 ; i++) {
			for (UUID id : requestsId) {
				if (null != CafResultsHolder.getResults(id.toString())) {
					responses.put(id, CafResultsHolder.getResults(id.toString()));
					CafResultsHolder.removeResult(id.toString());
				}
			}
			if (responses.keySet().size() == requestsId.size()) {
				return responses;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
		return responses;
	}


	/**
	 * Tries to find out what is this server's IP
	 */
	private String getDefaultIpAddress() {
		String address = null;
		try {
			address =
					InetAddress.getLocalHost().getHostAddress();
			if (!"127.0.0.1".equals(address) && !"127.0.1.1".equals(address)) {
				return address;
			}
		} catch(UnknownHostException e) {
			//hostname not in DNS or /etc/hosts
		}
		Sigar sigar = new Sigar();
		try {
			address =
					sigar.getNetInterfaceConfig().getAddress();
		} catch (SigarException e) {

		} finally {
			sigar.close();
		}
		if (null == address) {
			throw new CafException("Cannot find the server's IP address");
		}
		return address;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	public void setServerSecurePort(String serverSecurePort) {
		this.serverSecurePort = serverSecurePort;
	}
}

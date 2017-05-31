package org.hyperic.hq.web.admin.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.ui.KeyConstants;
import org.hyperic.hq.vm.VCDAO;
import org.hyperic.hq.vm.VmMapping;
import org.hyperic.hq.web.BaseController;
import org.hyperic.util.security.MD5;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/managers/migration")
public class MigrationManagerController extends BaseController implements
		ApplicationContextAware {
	private static final Log log = LogFactory
			.getLog(MigrationManagerController.class);

	private static final String HELP_PAGE_MAIN = "Migration Manager";

	private final PluginManager pluginManager;
	private final AgentManager agentManager;
	private final ResourceManager resourceManager;
	private ApplicationContext applicationContext;
	private final ServerConfigManager serverConfigManager;
	private final AgentCommandsClientFactory agentCommandsClientFactory;
	private final VCDAO vcDao;

	SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");

	@Autowired
	public MigrationManagerController(AppdefBoss appdefBoss,
			AuthzBoss authzBoss, PluginManager pluginManager,
			AgentManager agentManager, ResourceManager resourceManager,
			ServerConfigManager serverConfigManager, VCDAO vcDao, 
			AgentCommandsClientFactory agentCommandsClientFactory) {
		super(appdefBoss, authzBoss);
		this.pluginManager = pluginManager;
		this.agentManager = agentManager;
		this.resourceManager = resourceManager;
		this.serverConfigManager = serverConfigManager;
		this.vcDao = vcDao;
		this.agentCommandsClientFactory = agentCommandsClientFactory;
	}

	@RequestMapping(method = RequestMethod.GET)
	public String index(Model model) {
		model.addAttribute("info", getAgentInfo());
		model.addAttribute("mechanismOn","true");

		model.addAttribute(KeyConstants.PAGE_TITLE_KEY, HELP_PAGE_MAIN);
		return "admin/managers/migration";
	}

	@RequestMapping(method = RequestMethod.GET, value = "/list", headers = "Accept=application/json")
	public @ResponseBody
	List<Map<String, Object>> getPluginSummaries() {
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/info", headers = "Accept=application/json")
	public @ResponseBody
	Map<String, Object> getAgentInfo() {
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("agentErrorCount", 0);
		info.put("syncableAgentCount", 0);
		info.put("totalAgentCount", 0);
		String serverVersion = getServerVersion();
		info.put("serverVersion", serverVersion);
		return info;
	}

	private List<Map<String, String>> createAgentVersionList(
			Collection<Agent> agents) {
		List<Map<String, String>> res = new ArrayList<Map<String, String>>(
				agents.size());
		for (Agent agent : agents) {
			Map<String, String> agentInfoMap = new HashMap<String, String>(2);
			String version = agent.getVersion();
			agentInfoMap.put("version", version);
			String agentName = getAgentName(agent);
			agentInfoMap.put("agentName", agentName);
			res.add(agentInfoMap);
		}

		return res;
	}

	/**
	 * @param agent
	 * @return the address of Agent
	 */
	private String getAgentName(Agent agent) {
		Collection<Platform> platforms = agent.getPlatforms();
		if (platforms != null) {
			for (Platform platform : platforms) {
				if (PlatformDetector.isSupportedPlatform(platform
						.getPlatformType().getName())) {
					return platform.getFqdn();
				}
			}
		}
		return agent.getAddress();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/deployTelegrafPlugin")
	public @ResponseBody String deployTelegrafPlugin(HttpSession session) {
		AuthzSubject subject;
		try {
			subject = getAuthzSubject(session);
			Map<String, byte[]> pluginInfo = new HashMap<String, byte[]>();
			RandomAccessFile pluginFile = new RandomAccessFile(new File("conf/telegraf-plugin.xml"), "r");
			byte[] bytes = new byte[(int)pluginFile.length()];
			pluginFile.readFully(bytes);
			pluginInfo.put("telegraf-plugin.xml", bytes);
			pluginManager.deployPluginIfValid(subject, pluginInfo);
			return "success";
		} catch (SessionNotFoundException e) {
			log.error(e, e);
		} catch (SessionTimeoutException e) {
			log.error(e, e);
		} catch (PermissionException e) {
			log.error(e, e);
		} catch(IOException ioe) {
			log.error(ioe, ioe);
		} catch(PluginDeployException pde) {
			log.error(pde, pde);
		}
		return "error";
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/stopAgents")
	public @ResponseBody String stopAgents(HttpSession session) {
		AuthzSubject subject;
		try {
			subject = getAuthzSubject(session);
			List<Agent> agents = agentManager.getAgents();
			for (Agent agent : agents) {
				AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
				client.die();
			}
			return "success";
		} catch (SessionNotFoundException e) {
			log.error(e, e);
		} catch (SessionTimeoutException e) {
			log.error(e, e);
		} catch (PermissionException e) {
			log.error(e, e);
		} catch (AgentRemoteException e) {
			log.error(e, e);
		} catch (AgentConnectionException e) {
			log.error(e, e);
		}
		return "error";
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/migrate")
	public @ResponseBody
	String migrateAgents(HttpSession session) {
			AuthzSubject subject;
			try {
				subject = getAuthzSubject(session);
				migrateAgents(null,subject);
				return "success";
	
			} catch (SessionNotFoundException e) {
				log.error(e, e);
			} catch (SessionTimeoutException e) {
				log.error(e, e);
			} catch (PermissionException e) {
				log.error(e, e);
			}
		return "error";
	}

	public void migrateAgents(String[] platformIds,AuthzSubject subject) {
		
		if (platformIds != null) {

		}

		Properties migrationProp = new Properties();
		try {
			migrationProp
					.load(new FileInputStream("conf/migration.properties"));
		} catch (Exception ex) {
			log.error("Error reading migration properties file " + ex);
			return;
		}
		List<Agent> agents = agentManager.getAgents();
		for (Agent agent : agents) {
			try {
				boolean precheck = false;
				log.info("Pushing migration scripts to " + agent);
				File conffile = new File("conf/configfile.properties");
				String saltMaster = null;
				String mqttIp = null;
				String macAddress = null;
				List<Platform> platformList = new ArrayList<Platform>();
				platformList.addAll(agent.getPlatforms());
				Platform platform = platformList.get(0);
				for (Ip ip : platform.getIps()) {
					if (!"00:00:00:00:00:00".equals(ip.getMacAddress())) {
						macAddress = ip.getMacAddress();
						log.info("Got mac address for ip " + ip + " "
								+ ip.getMacAddress());
						break;
					}
				}
				if (macAddress != null) {
					VmMapping vmMapping = vcDao.findVMByMac(macAddress);
					if (vmMapping != null) {
						Properties prop = new Properties();
						FileOutputStream output = null;
						try {
							output = new FileOutputStream(conffile);
							// set the properties value
							prop.setProperty("vc_id", vmMapping.getVcUUID());
							prop.setProperty("vm_id", vmMapping.getMoId());
							prop.setProperty("salt_master",
									migrationProp.getProperty("UCP.IP"));
							// save properties to project root folder
							prop.store(output, null);
							output.close();
							output = null;
							precheck = true;

						} catch (IOException io) {
							log.error("Error while creating config file " + io);
						} finally {
							if (output != null) {
								try {
									output.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

					} else {
						log.warn("Failed pushing scripts as VM Mapping not found for agent " + agent); 
					}
				} else {
					log.warn("Failed pushing scripts as MAC address not found for agent " + agent);
				}
				if (precheck) {
					sendFile("bin/uaf-bootstrap.sh","../../../uaf-bootstrap.sh",agent);
                    sendFile("bin/uaf-bootstrap.bat","../../../uaf-bootstrap.bat",agent);
                    sendFile("bin/bootstrap-salt.sh","../../../bootstrap-salt.sh",agent);
                    sendFile(conffile.getAbsolutePath(),"../../../configfile.properties",agent);
                    agentManager.transferAgentPlugin(subject, platform.getEntityId(), "hqagent-plugin.jar");
				}
			} catch (Exception ex) {
				log.error("Error migrating agent " + agent + " : " + ex );
			}
		}
	}
	
	private void sendFile(String filePath, String destFilePath, Agent agent) throws FileNotFoundException, AgentRemoteException, AgentConnectionException {
		AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        File file = new File(filePath);
        FileData fileData = new FileData(destFilePath, file.length(), 2);
        String md5sum = MD5.getMD5Checksum(file);
        fileData.setMD5CheckSum(md5sum);
        FileInputStream is = new FileInputStream(file);
        List<FileData> data = new ArrayList<FileData>(1);
        data.add(fileData);
        List<InputStream> stream = new ArrayList<InputStream>(1);
        stream.add(is);
        FileDataResult[] result = client.agentSendFileData(
                data.toArray(new FileData[0]), stream.toArray(new InputStream[0]));
        log.info("Sent File " + filePath + " to agent " + " result:" + result);
    }

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	private String getServerVersion() {
		String serverVersion = serverConfigManager
				.getPropertyValue(HQConstants.ServerVersion);
		return serverVersion;
	}
}
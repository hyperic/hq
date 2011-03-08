package org.hyperic.hq.web.admin.managers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.web.BaseController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/managers/plugin")
public class PluginManagerController extends BaseController implements ApplicationContextAware {
	private ApplicationContext applicationContext;
	private ProductManager productManager;
	private PluginManager pluginManager;
	private AgentManager agentManager;
	private AuthzBoss authzBoss;
	

	
	@Autowired
	public PluginManagerController(AppdefBoss appdefBoss, AuthzBoss authzBoss, 
	        ProductManager productManager, PluginManager pluginManager,
	        AgentManager agentManager) {
		super(appdefBoss, authzBoss);
		this.productManager = productManager;
		this.pluginManager = pluginManager;
		this.agentManager = agentManager;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String index(Model model) {
		model.addAttribute("pluginSummaries", getPluginSummaries());
		
		return "admin/managers/plugin";
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/list", headers="Accept=application/json")
	public @ResponseBody List<Map<String, Object>> getPluginSummaries() {
		List<Map<String, Object>> pluginSummaries = new ArrayList<Map<String,Object>>();
		List<Plugin> plugins =  pluginManager.getAllPlugins();
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");
		
		Comparator<Plugin> sortByPluginName = new Comparator<Plugin>() {
			public int compare(Plugin o1, Plugin o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		
		Collections.sort(plugins, sortByPluginName);
		
		
		Map<Integer, Map<AgentPluginStatusEnum, Integer>> allPluginStatus = 
		    pluginManager.getPluginRollupStatus();
		
		for (Plugin plugin : plugins) {
		    int pluginId = plugin.getId();
			Map<String, Object> pluginSummary = new HashMap<String, Object>();
			Map<AgentPluginStatusEnum, Integer> pluginStatus = 
			    allPluginStatus.get(pluginId);
			
			pluginSummary.put("id", pluginId);
			pluginSummary.put("name", plugin.getName());
			pluginSummary.put("jarName", plugin.getPath());
			pluginSummary.put("initialDeployDate", formatter.format(plugin.getCreationTime()));
			int successAgentCount =0;
			int errorAgentCount =0;
			int inProgressAgentCount =0;
			
			if (pluginStatus!=null){
    			successAgentCount = pluginStatus.get(AgentPluginStatusEnum.SYNC_SUCCESS);
    			errorAgentCount = pluginStatus.get(AgentPluginStatusEnum.SYNC_FAILURE);
    			inProgressAgentCount = pluginStatus.get(AgentPluginStatusEnum.SYNC_IN_PROGRESS);
			}
			int allAgentCount = successAgentCount + errorAgentCount +inProgressAgentCount;
			boolean isInProgress = false;
			if (inProgressAgentCount>0){
			    isInProgress = true;
			}
			
			pluginSummary.put("allAgentCount",allAgentCount);
			pluginSummary.put("successAgentCount",successAgentCount);
			pluginSummary.put("errorAgentCount",errorAgentCount);
			pluginSummary.put("inProgress",isInProgress);
            pluginSummary.put("updatedDate", formatter.format(plugin.getModifiedTime()));
            
            //TODO - Annie: get the version!
            pluginSummary.put("version", "1.2");            
            
			
			List<Map<String,Object>> errorAgents = new ArrayList<Map<String,Object>>();
			Collection<AgentPluginStatus> errorAgentStatusList = pluginManager.getErrorStatusesByPluginId(pluginId);
			
			for(AgentPluginStatus errorAgentStatus: errorAgentStatusList){
			    Map<String,Object> errorAgent = new HashMap<String,Object>();
			    errorAgent.put("agentName", getAgentName(errorAgentStatus.getAgent())); 
			    errorAgent.put("syncDate", formatter.format(errorAgentStatus.getLastSyncAttempt()));
			    errorAgents.add(errorAgent);
			}
			pluginSummary.put("errorAgents", errorAgents);
			pluginSummaries.add(pluginSummary);
		}

            
		return pluginSummaries;
	}
	/**
	 * @param agent
	 * @return the address of Agent
	 */
	private String getAgentName(Agent agent){
	    //Our intent is to return the FQDN of the platform the agent is running on. 
	    // However, we haven't figure out a good way to distinguish "fake" platform now.
	    Collection <Platform> platforms = agent.getPlatforms();
	    for (Platform platform: platforms){
	        if(PlatformDetector.isSupportedPlatform(platform.getPlatformType().getName())){
	            return platform.getFqdn();
	        }
	    }
	    return agent.getAddress();
	}
	
	
	 @RequestMapping(method = RequestMethod.DELETE, value="/delete")
	 public @ResponseBody String deletePlugin(@RequestParam(RequestParameterKeys.DELETE_ID) String[] deleteIds, HttpSession session, Model model){
	    String response = "delete plugins:";
	    
	    for (int i = 0 ; i< deleteIds.length;i++){
	        //agentManager.agentRemovePlugins(subject, i, null);
	        
	        response+=deleteIds[i]+",";
	        
	    }
	    return response;
	}
	   
	@RequestMapping(method = RequestMethod.POST, value="/upload")
	public String uploadProductPlugin(@RequestParam MultipartFile plugin, HttpSession session, Model model) {
		boolean success = false;
		String messageKey = "";
		String filename = "";
		AuthzSubject subject;
		
		try {
			subject = getAuthzSubject(session);
			
			if (!plugin.isEmpty() && productManager.deployPluginIfValid(subject, plugin.getBytes())) {
				success = true;
				messageKey = "admin.managers.plugin.message.success";
			} else {
				messageKey = "admin.managers.plugin.message.io.failure";
			}
		} catch (SessionNotFoundException e) {
			e.printStackTrace();
			messageKey = "admin.managers.plugin.message.io.failure";
		} catch (SessionTimeoutException e) {
			e.printStackTrace();
			messageKey = "admin.managers.plugin.message.io.failure";
		} catch (PermissionException e) {
			e.printStackTrace();
			messageKey = "admin.managers.plugin.message.io.failure";
		} catch (IOException e) {
			e.printStackTrace();
			messageKey = "admin.managers.plugin.message.io.failure";
		}

		model.addAttribute("success", success);
		model.addAttribute("messageKey", messageKey);
		model.addAttribute("filename", filename);
		
		return "admin/managers/plugin/upload/status";
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
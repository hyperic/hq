package org.hyperic.hq.web.admin.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.product.Plugin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/managers/plugin")
public class PluginManagerController implements ApplicationContextAware {
	private ApplicationContext applicationContext;
	private AgentManager agentManager;
	
	@Autowired
	public PluginManagerController(AgentManager agentManager) {
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
		List<Plugin> plugins = agentManager.getAllPlugins();
		Comparator<Plugin> sortByPluginName = new Comparator<Plugin>() {
			public int compare(Plugin o1, Plugin o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		Map<Plugin, Collection<Agent>> pluginAgentMap = agentManager.getOutOfSyncAgentsByPlugin();
		long agentCount = agentManager.getNumAutoUpdatingAgents();
		
		Collections.sort(plugins, sortByPluginName);
		
		for (Plugin plugin : plugins) {
			Map<String, Object> pluginSummary = new HashMap<String, Object>();
			
			pluginSummary.put("id", plugin.getId());
			pluginSummary.put("name", plugin.getName());
			pluginSummary.put("jarName", plugin.getPath());
			pluginSummary.put("initialDeployDate", new Date(plugin.getCreationTime()));
			pluginSummary.put("lastSyncDate", "n/a");
			
			String status = "Successfully deployed";
			
			if (pluginAgentMap.containsKey(plugin)) {
				int undeployedCount = pluginAgentMap.get(plugin).size();
				
				status = "In Progress -- " + (100 * (agentCount/undeployedCount)) + "% deployed";
			}
			
			pluginSummary.put("status", status);
			
			pluginSummaries.add(pluginSummary);
		}
		
		return pluginSummaries;
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/upload")
	public String uploadProductPlugin(@RequestParam MultipartFile plugin, Model model) {
		boolean success = false;
		String messageKey = "";
		String filename = "";
		
		if (!plugin.isEmpty()) {
			filename = plugin.getOriginalFilename();
			
			try {
				File pluginDir = applicationContext.getResource("WEB-INF/hq-plugins").getFile();
					
				if (pluginDir.exists()) {
					String pathToUploadPlugin = pluginDir.getAbsolutePath() + File.separator + filename;
					File pluginFile = new File(pathToUploadPlugin);
	
					plugin.transferTo(pluginFile);
					
					success = true;
					messageKey = "admin.managers.plugin.message.success";
				}
			} catch(IOException e) {
				// TODO
				e.printStackTrace();
				
				messageKey = "admin.managers.plugin.message.io.failure";
			}
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
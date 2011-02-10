package org.hyperic.hq.web.admin.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/managers/plugin")
public class PluginManagerController implements ApplicationContextAware {
	private ApplicationContext applicationContext;
	
	@RequestMapping(method = RequestMethod.GET)
	public String index(Model model) {
		List<Map<String, Object>> pluginSummaries = new ArrayList<Map<String,Object>>();
		String[] plugins = new String[] { "mysql", "vsphere", "some custom plugin" };
		int index = 1;
		
		for (String plugin : plugins) {
			Map<String, Object> pluginSummary = new HashMap<String, Object>();
			
			pluginSummary.put("id", index++);
			pluginSummary.put("name", plugin);
			pluginSummary.put("lastSyncDate", new Date());
			pluginSummaries.add(pluginSummary);
		}
		
		model.addAttribute("pluginSummaries", pluginSummaries);
		
		return "admin/managers/plugin";
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
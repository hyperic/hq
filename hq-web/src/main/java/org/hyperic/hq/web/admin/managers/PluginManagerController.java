package org.hyperic.hq.web.admin.managers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.product.shared.PluginTypeEnum;
import org.hyperic.hq.ui.KeyConstants;
import org.hyperic.hq.web.BaseController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/managers/plugin")
public class PluginManagerController extends BaseController implements ApplicationContextAware {
    private static final Log log = LogFactory.getLog(PluginManagerController.class);
    
    private static final String HELP_PAGE_MAIN = "Administration.Plugin.Manager";

    private PluginManager pluginManager;
    private AgentManager agentManager;
    private ApplicationContext applicationContext;
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");

    
    @Autowired
    public PluginManagerController(AppdefBoss appdefBoss, AuthzBoss authzBoss, 
            PluginManager pluginManager, AgentManager agentManager) {
        super(appdefBoss, authzBoss);
        this.pluginManager = pluginManager;
        this.agentManager = agentManager;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("info",getAgentInfo());
        model.addAttribute("mechanismOn", pluginManager.isPluginSyncEnabled());
        if (pluginManager.isPluginSyncEnabled()){
            model.addAttribute("instruction", "admin.managers.plugin.instructions");
        }else{
            model.addAttribute("instruction", "admin.managers.plugin.mechanism.off");
        }
        model.addAttribute("customDir", pluginManager.getCustomPluginDir().getAbsolutePath());
        model.addAttribute(KeyConstants.PAGE_TITLE_KEY, HELP_PAGE_MAIN);
        return "admin/managers/plugin";
    }
    
    @RequestMapping(method = RequestMethod.GET, value="/list", headers="Accept=application/json")
    public @ResponseBody List<Map<String, Object>> getPluginSummaries() {
        List<Map<String, Object>> pluginSummaries = new ArrayList<Map<String,Object>>();
        List<Map<String, Object>> inProgressPluginSummaries = new ArrayList<Map<String,Object>>();
        List<Map<String, Object>> finalPluginSummaries = new ArrayList<Map<String,Object>>();
        List<Plugin> plugins =  pluginManager.getAllPlugins();
        
        if(plugins!=null){
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
                
                pluginSummary.put("inProgressAgentCount", inProgressAgentCount);
                pluginSummary.put("allAgentCount",allAgentCount);
                pluginSummary.put("successAgentCount",successAgentCount);
                pluginSummary.put("errorAgentCount",errorAgentCount);
                pluginSummary.put("inProgress",isInProgress);
                pluginSummary.put("updatedDate", formatter.format(plugin.getModifiedTime()));
                pluginSummary.put("version", plugin.getVersion());   
                pluginSummary.put("disabled", plugin.isDisabled());
                pluginSummary.put("deleted", plugin.isDeleted());
                
                boolean isDefault = false;
                boolean isCustom = false;
                boolean isServer = false;
                Collection<PluginTypeEnum> pluginType = pluginManager.getPluginType(plugin);
                if(pluginType!=null){
                    for(PluginTypeEnum type:pluginType){
                        switch(type){
                            case SERVER_PLUGIN:
                                isServer = true;
                                break;
                            case CUSTOM_PLUGIN:
                                isCustom = true;
                                break;
                            case DEFAULT_PLUGIN:
                                isDefault = true;
                                break;
                        }
                    }
                }
                pluginSummary.put("isdefaultPlugin", isDefault);
                pluginSummary.put("isCustomPlugin",isCustom);
                pluginSummary.put("isServerPlugin", isServer);
                if(errorAgentCount>0){
                    finalPluginSummaries.add(pluginSummary);
                }else if(inProgressAgentCount>0){
                    inProgressPluginSummaries.add(pluginSummary);
                }else{
                    pluginSummaries.add(pluginSummary);
                }
            }
            finalPluginSummaries.addAll(inProgressPluginSummaries);
            finalPluginSummaries.addAll(pluginSummaries);
        }

        return finalPluginSummaries;
    }
    
    @RequestMapping(method = RequestMethod.GET, value="/info", headers="Accept=application/json")
    public @ResponseBody Map<String, Object> getAgentInfo() {
        Map<String, Object> info = new HashMap<String,Object>();
        int agentErrorCount=0;
        Map<Integer, AgentPluginStatus> failureAgents = pluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE);
        
        if(failureAgents!=null && failureAgents.size()>0){
            agentErrorCount = failureAgents.size();
        }
       
        info.put("agentErrorCount", agentErrorCount);
        info.put("allAgentCount", agentManager.getNumAutoUpdatingAgents());
        return info;
    }

    @RequestMapping(method = RequestMethod.GET, value="/agent/summary", headers="Accept=application/json")
    public @ResponseBody List<String> getAgentStatusSummary() {
        List<String> agentNames = new ArrayList<String>();
        
        Map<Integer, AgentPluginStatus> failureAgents = pluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE);
        Iterator it = failureAgents.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer, AgentPluginStatus> pairs = (Map.Entry<Integer, AgentPluginStatus>) it.next();
            AgentPluginStatus status = pairs.getValue();
            agentNames.add(getAgentName(status.getAgent()));
        }
       
        Collections.sort(agentNames);
        
        return agentNames;
    }    
    
    @RequestMapping(method = RequestMethod.GET, value="/status/{pluginId}", headers="Accept=application/json")
    public @ResponseBody List<Map<String, Object>> getAgentStatus(@PathVariable int pluginId, 
        @RequestParam("searchWord") String searchWord, @RequestParam("status") String status) {
        List<Map<String,Object>> resultAgents = new ArrayList<Map<String,Object>>();
        Collection<AgentPluginStatus> agentStatusList ;
        
        if("error".equals(status)){
            agentStatusList = pluginManager.getStatusesByPluginId(pluginId, AgentPluginStatusEnum.SYNC_FAILURE);
        }else if("inprogress".equals(status)){
            agentStatusList = pluginManager.getStatusesByPluginId(pluginId, AgentPluginStatusEnum.SYNC_IN_PROGRESS);
        }else{
            return resultAgents;
        }
        
        for (AgentPluginStatus agentStatus : agentStatusList){
            String agentName = getAgentName(agentStatus.getAgent());
            if ("".equals(searchWord) || agentName.contains(searchWord)){
                Map<String,Object> errorAgent = new HashMap<String,Object>();
                errorAgent.put("agentName", agentName); 
                if(agentStatus.getLastSyncAttempt()!=0){
                    errorAgent.put("syncDate", formatter.format(agentStatus.getLastSyncAttempt())); 
                }else{
                    errorAgent.put("syncDate", "");
                }
                errorAgent.put("status", status);
                resultAgents.add(errorAgent);
            }
        }
        return resultAgents;
    }
    
    
    /**
     * @param agent
     * @return the address of Agent
     */
    private String getAgentName(Agent agent){
        Collection <Platform> platforms = agent.getPlatforms();
        if(platforms!=null){
            for (Platform platform: platforms){
                if(PlatformDetector.isSupportedPlatform(platform.getPlatformType().getName())){
                    return platform.getFqdn();
                }
            }
        }
        return agent.getAddress();
    }
    
    
     @RequestMapping(method = RequestMethod.DELETE, value="/delete")
     public @ResponseBody String deletePlugin(@RequestParam(RequestParameterKeys.DELETE_ID) 
                                              String[] deleteIds, HttpSession session, Model model){
        AuthzSubject subject;
        try {
            subject = getAuthzSubject(session);
            Collection<String> pluginFilenames = new ArrayList<String>();
            for (int i = 0 ; i< deleteIds.length;i++){
                Plugin plugin = pluginManager.getPluginById(Integer.parseInt(deleteIds[i]));
                pluginFilenames.add(plugin.getPath());
            }
            pluginManager.removePlugins(subject, pluginFilenames);
         
            return "success";

        } catch (SessionNotFoundException e) {
            log.error(e,e);
        } catch (SessionTimeoutException e) {
            log.error(e,e);
        } catch (PermissionException e) {
            log.error(e,e);
        } catch (PluginDeployException e) {
            log.error(e,e);
        }
        
        return "error";
    }
       
    @RequestMapping(method = RequestMethod.POST, value="/upload")
    public String uploadProductPlugin(@RequestParam MultipartFile[] plugins, HttpSession session, Model model) {
        boolean success = false;
        String messageKey = "";
        List<String> filename = new ArrayList<String>() ;
        AuthzSubject subject;
        Map<String, byte[]> pluginInfo = new HashMap<String, byte[]>();
        String[] messageParams = new String[3];
        
        try{
            for (int i= 0 ; i<plugins.length;i++){
                MultipartFile plugin = plugins[i];
                String name = "";
                name = plugin.getOriginalFilename();
                filename.add(name);
                pluginInfo.put(name, plugin.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
            messageKey = "admin.managers.plugin.message.io.failure";
        }
         
        try {
            subject = getAuthzSubject(session);
            
            if (plugins.length>0) {
                pluginManager.deployPluginIfValid(subject, pluginInfo);
                success = true;
                messageKey = "admin.managers.plugin.message.success";
            } else {
                messageKey = "admin.managers.plugin.message.io.failure";
            }
        } catch (SessionNotFoundException e) {
            log.error(e,e);
            messageKey = "admin.managers.plugin.message.io.failure";
        } catch (SessionTimeoutException e) {
            log.error(e,e);
            messageKey = "admin.managers.plugin.message.io.failure";
        } catch (PermissionException e) {
            log.error(e,e);
            messageKey = "admin.managers.plugin.message.io.failure";
        } catch (PluginDeployException e){
            messageKey = e.getMessage();
            Map<Integer, String> param = e.getParameters();
            
            if(param!=null){
                for(int i = 0; i<param.size();i++){
                    messageParams[i]=param.get(i);
                }
            }
            log.error(e,e);
        }
        model.addAttribute("params", messageParams);
        model.addAttribute("success", success);
        model.addAttribute("messageKey", messageKey);
        
        return "admin/managers/plugin/upload/status";
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
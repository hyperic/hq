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
import org.hibernate.Query;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformDAO;
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
@RequestMapping("/admin/managers/agentreport")
public class AgentReportManagerController extends BaseController implements ApplicationContextAware {
    private static final Log log = LogFactory.getLog(AgentReportManagerController.class);
    
    private static final String HELP_PAGE_MAIN = "Administration.Plugin.Manager";

    private final PluginManager pluginManager;
    private final AgentManager agentManager;
    private final ResourceManager resourceManager;
    private ApplicationContext applicationContext;
    private final ServerConfigManager serverConfigManager;
    private PlatformDAO platformDAO;
    
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");

    
    @Autowired
    public AgentReportManagerController(AppdefBoss appdefBoss, AuthzBoss authzBoss, 
            PluginManager pluginManager, AgentManager agentManager, ResourceManager resourceManager,
            ServerConfigManager serverConfigManager,PlatformDAO platformDAO) {
        super(appdefBoss, authzBoss);
        this.pluginManager = pluginManager;
        this.agentManager = agentManager;
        this.resourceManager = resourceManager;
        this.serverConfigManager = serverConfigManager;
        this.platformDAO = platformDAO;
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
        return "admin/managers/agentreport";
    }
    /**
     * Get the resource count for each plugin. (The count will be shown in delete confirmation dialog.)
     * @param deleteIds
     * @return 
     */
    @RequestMapping(method = RequestMethod.GET, value="/resource/count", headers="Accept=application/json")
    public @ResponseBody List<Map<String, String>> getResourceCount(@RequestParam("deleteIds") String deleteIds){
        
        Map<String,String> nameIdMapping = new HashMap<String,String>();
        String[] tempDeleteIds = deleteIds.split(",");
        List<Plugin> plugins = new ArrayList<Plugin>();
        
        for (int i= 0; i<tempDeleteIds.length;i++){
            Plugin plugin = pluginManager.getPluginById(Integer.parseInt(tempDeleteIds[i]));
            if(plugin!=null){
                plugins.add(plugin);
                nameIdMapping.put(plugin.getName(), String.valueOf(plugin.getId()));
            }
        }
        List<Map<String,String>> result = new ArrayList<Map<String,String>>();
        Map<String, Long> counts = resourceManager.getResourceCountByPlugin(plugins);
        Iterator it = counts.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String, Long> pair = (Map.Entry<String, Long>) it.next();
            Map<String,String> count = new HashMap<String, String>();
            count.put("pluginId", nameIdMapping.get(pair.getKey()));
            count.put("count", String.valueOf(pair.getValue()));
            result.add(count);
        }
        
        return result;
    }
    
    @RequestMapping(method = RequestMethod.GET, value="/list", headers="Accept=application/json")
    public @ResponseBody List<Map<String, Object>> getPluginSummaries() {
        log.info("agentreportManger: entering get list");
        
        List<Map<String, Object>> finalPluginSummaries = new ArrayList<Map<String,Object>>();
        List<ReportBean> listOfReport = getReportData();
        for (ReportBean reportBean : listOfReport){
        	Map<String, Object> map=new HashMap<String, Object>();
            map.put("id", reportBean.getId());
            map.put("fqdn", reportBean.getFqdn());
            map.put("ip", reportBean.getIp());
            map.put("os", reportBean.getOsType());
            List<String> plugins = reportBean.getListOfPlugins();
            String pluginStr="";
            int pluginSize = plugins.size();
            for (int i=0;i<pluginSize;i++){
            	if(i != (pluginSize-1)){
            		pluginStr=pluginStr+plugins.get(i)+",";
            	}
            	else{
            		pluginStr=pluginStr+plugins.get(i);
            	}
            }
            map.put("plugins", pluginStr);
            finalPluginSummaries.add(map);
            
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
        long numAgentsTotal = agentManager.getAgentCountUsed();
        long numAutoUpdatingAgents = agentManager.getNumAutoUpdatingAgents();
        info.put("syncableAgentCount", numAutoUpdatingAgents);
        info.put("totalAgentCount", numAgentsTotal);
        String serverVersion = getServerVersion();
        info.put("serverVersion", serverVersion);
        return info;
    }

    @RequestMapping(method = RequestMethod.GET, value="/agent/summary", headers="Accept=application/json")
    public @ResponseBody List<String> getAgentStatusSummary() {
        List<String> agentNames = new ArrayList<String>();
        
        Map<Integer, AgentPluginStatus> failureAgents = pluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE);
        for (Map.Entry<Integer, AgentPluginStatus> failedAgent : failureAgents.entrySet()){
            AgentPluginStatus status = failedAgent.getValue();
            agentNames.add(getAgentName(status.getAgent()));            
        }
       
        Collections.sort(agentNames);
        
        return agentNames;
    }                   
    
    private List<Map<String,String>> createAgentVersionList(Collection<Agent> agents)
    {
        List<Map<String,String>> res = new ArrayList<Map<String,String>>(agents.size());
        for (Agent agent : agents) {
            Map<String,String> agentInfoMap = new HashMap<String, String>(2);
            String version = agent.getVersion();        
            agentInfoMap.put("version", version);
            String agentName = getAgentName(agent);
            agentInfoMap.put("agentName", agentName);
            res.add(agentInfoMap);
        }
        
        return res;   	
    }

    @RequestMapping(method = RequestMethod.GET, value="/agent/old/summary", headers="Accept=application/json")
    public @ResponseBody  List<Map<String,String>> getOldAgentStatusSummary() {
        List<Agent> oldAgents = agentManager.getOldAgentsUsed(); 
        return (createAgentVersionList(oldAgents));
    }         
    
    
     
    @RequestMapping(method = RequestMethod.GET, value="/agent/unsynchable/cur/summary", headers="Accept=application/json")
    public @ResponseBody  List<Map<String,String>> getCurrentNonSyncAgentStatusSummary() {
        List<Agent> curUnsynchableAgents = agentManager.getCurrentNonSyncAgents();
        return (createAgentVersionList(curUnsynchableAgents));
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
                                              String[] deleteIds, HttpSession session){
        AuthzSubject subject;
        try {
            subject = getAuthzSubject(session);
            Collection<String> pluginFilenames = new ArrayList<String>();
            for (int i = 0 ; i< deleteIds.length;i++){
                Plugin plugin = pluginManager.getPluginById(Integer.parseInt(deleteIds[i]));
                pluginFilenames.add(plugin.getPath());
            }
            pluginManager.removePluginsInBackground(subject, pluginFilenames);
         
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
    
    private String getServerVersion() {
        String serverVersion = serverConfigManager.getPropertyValue(HQConstants.ServerVersion);               
        return serverVersion;
    }
    
    public List<ReportBean> getReportData(){
        List<ReportBean> reportList=new ArrayList<ReportBean>();
        try{
                log.info("entering getReportData");

                String sql1=" select a.id,a.fqdn,a.description,a.agent.address " +
                    " from Platform a ";
                Query query = this.platformDAO.getSession().createQuery(sql1);
                List<Object[]> listPlatform=query.list();
                sql1="select distinct a.platform.id,a.serverType.plugin "+
                " from Server a ";
                query = this.platformDAO.getSession().createQuery(sql1);
                List<Object[]> listServer=query.list();

                Map<Integer, List <String>> pluginMap =  new HashMap<Integer, List<String>>();
                for(Object[] obj1:listServer){
                        if(pluginMap.get((Integer)obj1[0]) == null){
                                List <String> list1=new ArrayList<String>();
                                pluginMap.put((Integer)obj1[0], list1);
                        }
                        List <String> list=pluginMap.get((Integer)obj1[0]);
                        list.add((String) obj1[1]);
                }



                for (Object[] obj:listPlatform){
                        ReportBean report=new ReportBean();
                        report.setId((Integer)obj[0]);
                        report.setFqdn((String) obj[1]);
                                report.setOsType((String) obj[2]);
                                report.setIp((String) obj[3]);
                        List <String> pluginList=new ArrayList<String>();
                        int id=report.getId();
                        
                        report.setListOfPlugins(pluginMap.get(id));
                        reportList.add(report);
                }
                for (ReportBean bean:reportList){
                        String plugin="";
                        for (String pl:bean.getListOfPlugins()){
                                plugin=plugin+" , "+pl;
                        }
                        
                }
                log.info("exiting getReportData");


        }

        catch (Exception e){
                log.error("error ",e);
        }
                return reportList;

    }
}

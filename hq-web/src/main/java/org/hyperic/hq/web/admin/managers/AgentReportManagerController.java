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
    
    private final ResourceManager resourceManager;
    private ApplicationContext applicationContext;
    private final ServerConfigManager serverConfigManager;
    private PlatformDAO platformDAO;
    
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");

    
    @Autowired
    public AgentReportManagerController(AppdefBoss appdefBoss, AuthzBoss authzBoss, 
            PluginManager pluginManager,  ResourceManager resourceManager,
            ServerConfigManager serverConfigManager,PlatformDAO platformDAO) {
        super(appdefBoss, authzBoss);
        this.pluginManager = pluginManager;
        
        this.resourceManager = resourceManager;
        this.serverConfigManager = serverConfigManager;
        this.platformDAO = platformDAO;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public String index(Model model) {
        //model.addAttribute("info",getAgentInfo());
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
            		pluginStr=pluginStr+plugins.get(i)+"<br/>";
            	}
            	else{
            		pluginStr=pluginStr+plugins.get(i);
            	}
            }
            map.put("plugins", pluginStr);
            
            plugins = reportBean.getListOfProcess();
            pluginStr="";
            pluginSize = plugins.size();
            for (int i=0;i<pluginSize;i++){
            	if(i != (pluginSize-1)){
            		pluginStr=pluginStr+plugins.get(i)+"<br/>";
            	}
            	else{
            		pluginStr=pluginStr+plugins.get(i);
            	}
            }
            map.put("process", pluginStr);
            
            finalPluginSummaries.add(map);
            
        }
    	        return finalPluginSummaries;
    }
         
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
                
                sql1="select a.server.platform.id,a.resource.name"
                		+ " from Service a";
                query = this.platformDAO.getSession().createQuery(sql1);
                List<Object[]> listProcess=query.list();
                Map<Integer, List <String>> processMap =  new HashMap<Integer, List<String>>();
                for(Object[] obj1:listProcess){
                        if(processMap.get((Integer)obj1[0]) == null){
                                List <String> list1=new ArrayList<String>();
                                processMap.put((Integer)obj1[0], list1);
                        }
                        List <String> list=processMap.get((Integer)obj1[0]);
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
                        report.setListOfProcess(processMap.get(id));
                        reportList.add(report);
                }
                /*for (ReportBean bean:reportList){
                        String plugin="";
                        for (String pl:bean.getListOfPlugins()){
                                plugin=plugin+" , "+pl;
                        }
                        
                }*/
                log.info("exiting getReportData");


        }

        catch (Exception e){
                log.error("error ",e);
        }
                return reportList;

    }
}

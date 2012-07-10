/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.web.admin.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentPluginStatus;
import org.hyperic.hq.appdef.server.session.AgentPluginStatusEnum;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.product.shared.PluginTypeEnum;
import org.hyperic.hq.web.BaseControllerTest;
import org.hyperic.util.ConfigPropertyException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

public class PluginManagerControllerTest extends BaseControllerTest {
    private PluginManagerController pluginManagerController;
    private PluginManager mockPluginManager;
    private AgentManager mockAgentManager;
    private ResourceManager mockResourceManager;
    private AppdefBoss mockAppdefBoss;
    private AuthzBoss mockAuthzBoss;
    private ServerConfigManager mockServerConfigManager;
    
    private static SimpleDateFormat format;
    private static Date date1 = new Date();
    private static Date date2 = new Date();
    
    @BeforeClass
    public static void beforeClass(){
        format = new SimpleDateFormat("MM/dd/yyyy hh:mm aa zzz");
        try {
            date1 = format.parse("01/15/2011 06:00 PM PST");
            date2 = format.parse("01/01/2010 06:30 PM PST");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    @Before
    public void setup(){
        super.setUp();
        mockPluginManager = createMock(PluginManager.class);
        mockAgentManager = createMock(AgentManager.class);
        mockResourceManager = createMock(ResourceManager.class);
        mockAppdefBoss = getMockAppdefBoss();
        mockAuthzBoss = getMockAuthzBoss();
        mockServerConfigManager = createMock(ServerConfigManager.class);
        pluginManagerController = new PluginManagerController(mockAppdefBoss, mockAuthzBoss, 
            mockPluginManager, mockAgentManager,mockResourceManager, mockServerConfigManager);

    }
    
    @Test
    public void testMechanismOff() throws ConfigPropertyException{
        Model model =new ExtendedModelMap();
        expect(mockPluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getStatusesByAgentId());
        expect(mockPluginManager.getAllPlugins()).andStubReturn(new ArrayList<Plugin>());
        expect(mockPluginManager.getPluginRollupStatus()).andStubReturn(new HashMap<Integer, Map<AgentPluginStatusEnum, Integer>>());
        expect(mockPluginManager.isPluginSyncEnabled()).andStubReturn(false);
        expect(mockPluginManager.getCustomPluginDir()).andStubReturn(new File("/root/hq/test"));
        expect(mockAgentManager.getNumAutoUpdatingAgents()).andReturn(Long.parseLong("0"));
        expect(mockAgentManager.getAgentCountUsed()).andStubReturn(Integer.parseInt("0"));
        expect(mockAgentManager.getAgentCountUsed()).andStubReturn(Integer.parseInt("0"));
        expect(mockAgentManager.getNumOldAgents()).andStubReturn(Long.parseLong("0"));

        String serverVersion = "4.5.6.BUILD-SNAPSHOT"; 
        expect(mockServerConfigManager.getPropertyValue(HQConstants.ServerVersion)).andStubReturn(serverVersion);
        
        replay(mockAgentManager);
        replay(mockPluginManager);
        replay(mockServerConfigManager);
        
        String toPage = pluginManagerController.index(model);
        Map<String,Object> map = model.asMap();
        Map<String, Object> info = (Map<String, Object>)map.get("info");
        
        assertEquals("should be direct to admin/managers/plugin page.", "admin/managers/plugin",toPage);
        assertEquals("total agent count should be 0",Long.valueOf(0),info.get("totalAgentCount"));
        assertEquals("syncable agent count should be 0",Long.valueOf(0),info.get("syncableAgentCount"));
        assertEquals("server version should be " + serverVersion,serverVersion,info.get("serverVersion"));        
        assertTrue("mechanismOn should be false", !(Boolean)map.get("mechanismOn"));
        assertEquals("instruction should be admin.managers.plugin.mechanism.off",
            "admin.managers.plugin.mechanism.off",
            map.get("instruction"));
    }
    
    @Test
    public void testMechanismOn() throws ConfigPropertyException{
        Model model =new ExtendedModelMap();
        expect(mockPluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getStatusesByAgentId());
        expect(mockPluginManager.getAllPlugins()).andStubReturn(getAllPlugins());
        expect(mockPluginManager.getPluginRollupStatus()).andStubReturn(getPluginRollupStatus());
        expect(mockPluginManager.isPluginSyncEnabled()).andStubReturn(true);
        expect(mockPluginManager.getCustomPluginDir()).andStubReturn(new File("/root/hq/test"));
        expect(mockAgentManager.getNumAutoUpdatingAgents()).andStubReturn(Long.parseLong("3"));
        expect(mockAgentManager.getAgentCountUsed()).andStubReturn(Integer.parseInt("3"));
        expect(mockAgentManager.getNumOldAgents()).andStubReturn(Long.parseLong("0"));
        
        String serverVersion = "4.5.6.BUILD-SNAPSHOT"; 
        expect(mockServerConfigManager.getPropertyValue(HQConstants.ServerVersion)).andStubReturn(serverVersion);
        
        replay(mockAgentManager);
        replay(mockPluginManager);
        replay(mockServerConfigManager);
        
        String toPage = pluginManagerController.index(model);
        Map<String,Object> map = model.asMap();
        Map<String, Object> info = (Map<String, Object>)map.get("info");
        
        assertEquals("should be direct to admin/managers/plugin page.", "admin/managers/plugin",toPage);
        assertTrue("mechanismOn should be true", (Boolean)map.get("mechanismOn"));
        assertEquals("total agent count should be 3",Long.valueOf(3),info.get("totalAgentCount"));
        assertEquals("syncable agent count should be 3",Long.valueOf(3),info.get("syncableAgentCount"));
        assertEquals("server version should be " + serverVersion,serverVersion,info.get("serverVersion"));
        assertEquals("instruction should be admin.managers.plugin.instructions",
            "admin.managers.plugin.instructions", map.get("instruction"));
        assertEquals("file path should be /root/hq/test","/root/hq/test",""+map.get("customDir"));
    }         
    
    @Test
    public void testAgentSummary(){
        expect(mockPluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getStatusesByAgentId());
        replay(mockPluginManager);
        
        List<String> summaries = pluginManagerController.getAgentStatusSummary();
        
        assertEquals("There should be only two agents in summary",2,summaries.size());
        assertEquals("agentX's name should be 3.3.3.3","3.3.3.3",summaries.get(0));
        assertEquals("agentZ's name should be 5.5.5.5","5.5.5.5",summaries.get(1));
    }
    
    @Test
    public void testInfoNull() throws ConfigPropertyException{
        expect(mockPluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(null);
        expect(mockAgentManager.getNumAutoUpdatingAgents()).andReturn(Long.parseLong("0"));
        expect(mockAgentManager.getAgentCountUsed()).andStubReturn(Integer.parseInt("0"));
        expect(mockAgentManager.getNumOldAgents()).andStubReturn(Long.parseLong("0"));        

        String serverVersion = "4.5.6.BUILD-SNAPSHOT"; 
        expect(mockServerConfigManager.getPropertyValue(HQConstants.ServerVersion)).andStubReturn(serverVersion);
        
        replay(mockPluginManager);
        replay(mockAgentManager);
        replay(mockServerConfigManager);
        
        Map<String, Object> info = pluginManagerController.getAgentInfo();
        
        assertEquals("agentErrorCount should be 0",0,info.get("agentErrorCount"));
        assertEquals("total agent count should be 0",Long.valueOf(0),info.get("totalAgentCount"));
        assertEquals("syncable agent count should be 0",Long.valueOf(0),info.get("syncableAgentCount"));
    }
    
    @Test
    public void testInfo() throws ConfigPropertyException{
        expect(mockPluginManager.getStatusesByAgentId(AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getStatusesByAgentId());
        expect(mockAgentManager.getNumAutoUpdatingAgents()).andReturn(Long.parseLong("3"));
        expect(mockAgentManager.getAgentCountUsed()).andStubReturn(Integer.parseInt("4"));
        expect(mockAgentManager.getNumOldAgents()).andStubReturn(Long.parseLong("1"));        

        String serverVersion = "4.5.6.BUILD-SNAPSHOT"; 
        expect(mockServerConfigManager.getPropertyValue(HQConstants.ServerVersion)).andStubReturn(serverVersion);
        
        replay(mockPluginManager);
        replay(mockAgentManager);
        replay(mockServerConfigManager);

        Map<String, Object> info = pluginManagerController.getAgentInfo();
        
        assertEquals("total agent count should be 4",Long.valueOf(4),info.get("totalAgentCount"));
        assertEquals("syncable agent count should be 3",Long.valueOf(3),info.get("syncableAgentCount"));
        assertEquals("agentErrorCount should be 2",2,info.get("agentErrorCount"));
    }
    
    @Test
    public void testPluginSummaries() throws ParseException{
        List<Plugin> plugins = getAllPlugins();
        expect(mockPluginManager.getAllPlugins()).andReturn(plugins);
        expect(mockPluginManager.getPluginRollupStatus()).andReturn(getPluginRollupStatus());
        expect(mockPluginManager.getPluginType(plugins.get(0))).andReturn(getPluginType(0));
        expect(mockPluginManager.getPluginType(plugins.get(1))).andReturn(getPluginType(2));//pluginType is set before plugin list sort by status
        expect(mockPluginManager.getPluginType(plugins.get(2))).andReturn(getPluginType(1));
        replay(mockPluginManager);
 
        List<Map<String, Object>> summaries = pluginManagerController.getPluginSummaries();
        
        assertEquals("plugin-a: inProgressAgentCount should be 0",0,summaries.get(0).get("inProgressAgentCount"));
        assertEquals("plugin-a: allAgentCount should be 101",101,summaries.get(0).get("allAgentCount"));
        assertEquals("plugin-a: successAgentCount should be 100",100,summaries.get(0).get("successAgentCount"));
        assertEquals("plugin-a: errorAgentCount should be 1",1,summaries.get(0).get("errorAgentCount"));
        assertEquals("plugin-a: inProgress should be false",false,summaries.get(0).get("inProgress"));
        assertEquals("plugin-a: updatedDate should be ...", date1.getTime(), format.parse(summaries.get(0).get("updatedDate").toString()).getTime());
        assertEquals("plugin-a: initialDeployDate should be ...",format.format(date2),""+summaries.get(0).get("initialDeployDate"));
        assertEquals("plugin-a: id should be 1",1,summaries.get(0).get("id"));
        assertEquals("plugin-a: name should be plugin-a","plugin-a",summaries.get(0).get("name"));
        assertEquals("plugin-a: is server plugin",true, summaries.get(0).get("isServerPlugin"));
        assertEquals("plugin-a: is not custom plugin",false, summaries.get(0).get("isCustomPlugin"));
        
        //make sure plugins are sorted by status
        assertEquals("plugin-c: name should be plugin-c","plugin-c",summaries.get(1).get("name"));
        assertEquals("plugin-c: inProgressAgentCount should be 4",4,summaries.get(1).get("inProgressAgentCount"));
        assertEquals("plugin-c: allAgentCount should be 7",7,summaries.get(1).get("allAgentCount"));
        assertEquals("plugin-c: successAgentCount should be 0",0,summaries.get(1).get("successAgentCount"));
        assertEquals("plugin-c: errorAgentCount should be 3",3,summaries.get(1).get("errorAgentCount"));
        assertEquals("plugin-c: inProgress should be true",true,summaries.get(1).get("inProgress"));
        assertEquals("plugin-c: id should be 3",3,summaries.get(1).get("id"));        
        assertEquals("plugin-c: is not server plugin",false, summaries.get(1).get("isServerPlugin"));
        assertEquals("plugin-c: is custom plugin",true, summaries.get(1).get("isCustomPlugin"));
        
        assertEquals("plugin-b: name should be plugin-b","plugin-b",summaries.get(2).get("name"));
        assertEquals("plugin-b: inProgressAgentCount should be 0",0,summaries.get(2).get("inProgressAgentCount"));
        assertEquals("plugin-b: allAgentCount should be 99",99,summaries.get(2).get("allAgentCount"));
        assertEquals("plugin-b: successAgentCount should be 99",99,summaries.get(2).get("successAgentCount"));
        assertEquals("plugin-b: errorAgentCount should be 0",0,summaries.get(2).get("errorAgentCount"));
        assertEquals("plugin-b: inProgress should be false",false,summaries.get(2).get("inProgress"));
        assertEquals("plugin-b: id should be 2",2,summaries.get(2).get("id"));        
        assertEquals("plugin-b: is not server plugin",false, summaries.get(2).get("isServerPlugin"));
        assertEquals("plugin-b: is not custom plugin",false, summaries.get(2).get("isCustomPlugin"));


    }
    private Collection<PluginTypeEnum> getPluginType(int index){
        Collection<PluginTypeEnum> result = new ArrayList<PluginTypeEnum>();
        switch (index){
            case 0:
                result.add(PluginTypeEnum.SERVER_PLUGIN);
                return result;
            case 1:
                result.add(PluginTypeEnum.CUSTOM_PLUGIN);
                result.add(PluginTypeEnum.DEFAULT_PLUGIN);
                return result;
            case 2:
                return result;
        }
        return null;
    }
    
    @Test
    public void testAgentStatus(){
        expect(mockPluginManager.getStatusesByPluginId(3, AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getErrorAgentStatusList());
        expect(mockPluginManager.getStatusesByPluginId(3, AgentPluginStatusEnum.SYNC_IN_PROGRESS)).andStubReturn(getInProgressAgentStatusList());
        replay(mockPluginManager);
        
        List<Map<String, Object>> result = pluginManagerController.getAgentStatus(3, "","inprogress");
        assertEquals("result should be 1",1,result.size());
    }
    
    @Test
    public void testAgentStatusWithKeyword(){
        expect(mockPluginManager.getStatusesByPluginId(3, AgentPluginStatusEnum.SYNC_FAILURE)).andStubReturn(getErrorAgentStatusList());
        expect(mockPluginManager.getStatusesByPluginId(3, AgentPluginStatusEnum.SYNC_IN_PROGRESS)).andStubReturn(getInProgressAgentStatusList());
        replay(mockPluginManager);
        List<Map<String, Object>> result =  pluginManagerController.getAgentStatus(3, "xx","inprogress");
        
        assertEquals("result should be empty",0,result.size());
    }
    @Test
    public void testDeletePluginResouceCount(){
        List<Plugin> plugins = getAllPlugins();
        
        expect(mockPluginManager.getPluginById(1)).andStubReturn(plugins.get(0));
        expect(mockPluginManager.getPluginById(2)).andStubReturn(plugins.get(1));
        expect(mockPluginManager.getPluginById(3)).andStubReturn(plugins.get(2));
        expect(mockResourceManager.getResourceCountByPlugin(plugins)).andStubReturn(getResourceCounts());
        replay(mockPluginManager);
        replay(mockResourceManager);
        
        List<Map<String, String>> result = pluginManagerController.getResourceCount("1,2,3");
        
        assertEquals("There should be only two entries",2,result.size());
        assertEquals("The first one should be plugin-a (id=1) ","1",result.get(0).get("pluginId"));
        assertEquals("plugin-a should have 200 count","200",result.get(0).get("count"));
        assertEquals("The second one should be plugin-c (id=3) ","3",result.get(1).get("pluginId"));
        assertEquals("plugin-a should have 1 count","1",result.get(1).get("count"));        
    }
    private Map<String, Long> getResourceCounts(){
        Map<String, Long> result = new HashMap<String,Long>();
        result.put("plugin-a", (long)200);
        result.put("plugin-c", (long)1);
        return result;
    }
    
    private Map<Integer, AgentPluginStatus> getStatusesByAgentId(){
        Map<Integer, AgentPluginStatus>  result = new HashMap<Integer, AgentPluginStatus>();
        AgentPluginStatus statusX = new AgentPluginStatus();
        Agent agentX = new Agent();
        Collection <Platform> platformsX = new ArrayList<Platform>();
        agentX.setPlatforms(platformsX);
        agentX.setAddress("3.3.3.3");
        statusX.setAgent(agentX);
        
        AgentPluginStatus statusZ = new AgentPluginStatus();
        Agent agentZ = new Agent();
        Collection <Platform> platformsZ = new ArrayList<Platform>();
        agentZ.setPlatforms(platformsZ);
        agentZ.setAddress("5.5.5.5");    
        statusZ.setAgent(agentZ);
        
        result.put(new Integer(0), statusX);
        result.put(new Integer(2), statusZ);
        return result;
    }
    
    private Collection<AgentPluginStatus> getErrorAgentStatusList(){
        Collection<AgentPluginStatus> result = new ArrayList<AgentPluginStatus>();
        
        AgentPluginStatus apCZ = new AgentPluginStatus();
        Agent agentZ = new Agent();
        Collection <Platform> platformsZ = new ArrayList<Platform>();
        agentZ.setPlatforms(platformsZ);
        agentZ.setAddress("5.5.5.5");
        apCZ.setAgent(agentZ);
        apCZ.setLastSyncAttempt(date1.getTime());
        result.add(apCZ);

        return result;
    }
    private Collection<AgentPluginStatus> getInProgressAgentStatusList(){
        Collection<AgentPluginStatus> result = new ArrayList<AgentPluginStatus>();

        AgentPluginStatus apCX = new AgentPluginStatus();
        Agent agentX = new Agent();
        Collection <Platform> platformsX = new ArrayList<Platform>();
        agentX.setPlatforms(platformsX);
        agentX.setAddress("3.3.3.3");
        apCX.setAgent(agentX);
        apCX.setLastSyncAttempt(date2.getTime());        
        
        result.add(apCX);
        return result;
    }    
    private HashMap<Integer, Map<AgentPluginStatusEnum, Integer>> getPluginRollupStatus(){
        HashMap<Integer, Map<AgentPluginStatusEnum, Integer>> rollupStatus = 
            new HashMap<Integer, Map<AgentPluginStatusEnum, Integer>>();
        Map<AgentPluginStatusEnum, Integer> pluginA = new HashMap<AgentPluginStatusEnum, Integer>();
        pluginA.put(AgentPluginStatusEnum.SYNC_SUCCESS, 100);
        pluginA.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 0);
        pluginA.put(AgentPluginStatusEnum.SYNC_FAILURE, 1);

        Map<AgentPluginStatusEnum, Integer> pluginB = new HashMap<AgentPluginStatusEnum, Integer>();
        pluginB.put(AgentPluginStatusEnum.SYNC_SUCCESS, 99);
        pluginB.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 0);
        pluginB.put(AgentPluginStatusEnum.SYNC_FAILURE, 0);
  
        Map<AgentPluginStatusEnum, Integer> pluginC = new HashMap<AgentPluginStatusEnum, Integer>();
        pluginC.put(AgentPluginStatusEnum.SYNC_SUCCESS, 0);
        pluginC.put(AgentPluginStatusEnum.SYNC_IN_PROGRESS, 4);
        pluginC.put(AgentPluginStatusEnum.SYNC_FAILURE, 3);
        
        rollupStatus.put(1, pluginA);
        rollupStatus.put(2, pluginB);
        rollupStatus.put(3, pluginC);
        
        return rollupStatus;
    }

    private List<Plugin> getAllPlugins(){
        List<Plugin> plugins = new ArrayList<Plugin>();
        Plugin pluginA = new Plugin();
        pluginA.setName("plugin-a");
        pluginA.setId(1);
        pluginA.setModifiedTime(date1.getTime());
        pluginA.setCreationTime(date2.getTime());
        plugins.add(pluginA);
        
        Plugin pluginB = new Plugin();
        pluginB.setName("plugin-b");
        pluginB.setId(2);
        plugins.add(pluginB);
        
        Plugin pluginC = new Plugin();
        pluginC.setName("plugin-c");
        pluginC.setId(3);
        plugins.add(pluginC);
        
        return plugins;
    }
}

/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.plugin.vsphere;

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.pdk.domain.Agent;
import org.hyperic.hq.pdk.domain.Resource;
import org.hyperic.hq.pdk.domain.ResourceType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test of the {@link VMAndHostVCenterPlatformDetector}
 * TODO Add tests for deleting removed hosts and VMs from inventory
 * @author jhickey
 * 
 */
public class VMAndHostVCenterPlatformDetectorTest {
	private static final String HQ_IP = "agent.setup.camIP";
	private static final String HQ_PORT = "agent.setup.camPort";
	// private static final String HQ_SPORT = "agent.setup.camSSLPort";
	private static final String HQ_SSL = "agent.setup.camSecure";
	private static final String HQ_USER = "agent.setup.camLogin";
	private static final String HQ_PASS = "agent.setup.camPword";
    private static final String VCENTER_URL = "url";
    private static final String VCENTER_UNAME = "uname";
    private static final String VCENTER_PW = "pass";
    private static final String AGENT_IP = "agent.setup.agentIP";
    private static final String AGENT_PORT = "agent.setup.agentPort";
    private static final String AGENT_UNIDIRECTIONAL = "agent.setup.unidirectional";
    private Properties properties;
    
    @Before
    public void setUp() throws Exception {
    	properties = new Properties();
    	
        // Setup rest api conn info
    	properties.put(HQ_IP, "127.0.0.1");
        properties.put(HQ_PORT, "8080");
        properties.put(HQ_SSL, "no");
        properties.put(HQ_USER, "hqadmin");
        properties.put(HQ_PASS, "hqadmin");
        
        // Setup vsphere api conn info
        properties.put(VSphereCollector.PROP_URL, "https://10.150.29.72/sdk");
        properties.put(VSphereCollector.PROP_USERNAME, "Administrator");
        properties.put(VSphereCollector.PROP_PASSWORD, "ca$hc0w");
        
        // Setup agent conn info
        properties.put(AGENT_IP, "127.0.0.1");
        properties.put(AGENT_PORT, 2144);
        properties.put(AGENT_UNIDIRECTIONAL, "NO");
    }

    @Test
    @Ignore("Not a unit test.  RestApi is trying to make a connection")
    public void testDiscoverPlatforms() throws Exception {
    	RestApi api = new RestApi(properties);
    	VMAndHostVCenterPlatformDetector detector = new VMAndHostVCenterPlatformDetector();
    	VSphereUtil vimApi = VSphereUtil.getInstance(properties);

    	createVCenterServer(api);
    	
    	detector.discoverPlatforms(api, properties, vimApi);
    }
    
    private void createVCenterServer(RestApi api) {
    	Agent agent = api.getAgent("127.0.0.1", 2144);
    	ResourceType type = api.getResourceType("VMware vCenter");
    	Resource resource = new Resource();
    	
    	resource.setAgent(agent);
    	resource.setConfigs(new HashMap<String, Object>());
    	resource.setDescription("vcenter server");
    	resource.setLocation("/some/location");
    	resource.setModifiedBy("hqadmin");
    	resource.setName("vCenter TEST");
    	resource.setProperties(new HashMap<String, Object>());
    	resource.setType(type);
    	
    	Resource vcenter = api.createResource(resource);
    	
    	vcenter.getId();
    }
}
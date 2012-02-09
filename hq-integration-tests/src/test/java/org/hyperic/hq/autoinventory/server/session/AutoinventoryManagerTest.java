package org.hyperic.hq.autoinventory.server.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.roo.support.util.Assert;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(loader    = IntegrationTestContextLoader.class,
                      locations = { "classpath*:META-INF/spring/*-context.xml",
                                    "AutoinventoryManagerTest-context.xml" })
@DirtiesContext
public class AutoinventoryManagerTest {
	@Autowired
	AutoinventoryManager autoinventoryManager;
	@Autowired
	AIQueueManager aiqManager;
	@Autowired
	private AuthzSubjectManager authzSubjectManager;
	
	@Test
	public void reportAIDataTest() throws ClassNotFoundException, IOException, AutoinventoryException{
			
		// Deserialize from a file
		//File file = new File();
		InputStream is = this.getClass().getResourceAsStream("/data/stateCore.serialized");
		ObjectInputStream in = new ObjectInputStream(is);
		// Deserialize the object
		ScanStateCore stateCore = (ScanStateCore) in.readObject();
   		in.close();

		autoinventoryManager.reportAIData("1320579062071-7289977906287378553-4952493223028022472", stateCore);
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
		PageList<AIPlatformValue> queue = aiqManager.getQueue(overlord, false, false, false, PageControl.PAGE_ALL);
		Assert.isTrue(queue.size() == 1);
		AIPlatformValue val = queue.iterator().next();
		Assert.isTrue(val.getQueueStatus() == AIQueueConstants.Q_STATUS_ADDED);
		
	}
}

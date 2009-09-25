package org.hyperic.hq.authz.server.session;



import java.util.ArrayList;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectDAO;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;



@ContextConfiguration(locations={"/test-context.xml"})
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)

public class ResourceGroupManagerTest
{

   

    @Autowired
    private ResourceGroupManager resourceGroupManager;

    @Autowired
    private AuthzSubjectDAO authzSubjectDAO;

    private AuthzSubject testUser;

    private ResourceGroup testGroup;
    
    private ResourceGroup.ResourceGroupCreateInfo info;

    @Before
    public void setUpTestDataWithinTransaction() throws GroupDuplicateNameException, GroupCreationException {
        testUser = authzSubjectDAO.findById(AuthzConstants.overlordId);
        info = new ResourceGroup.ResourceGroupCreateInfo("Test Group",
                                                                                               "Test Group Description",
                                                                                               AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS,
                                                                                               null,
                                                                                               "Test Group Location",
                                                                                               0,
                                                                                               false,
                                                                                               false);
       
    }

    @Test
    public void something() throws PermissionException, GroupDuplicateNameException, GroupCreationException {
        testGroup = resourceGroupManager.createResourceGroup(testUser, info, new ArrayList(0), new ArrayList(0));
    }
}


package org.hyperic.hq.hqu.rendit.helpers

import org.easymock.classextension.EasyMock
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.auth.domain.AuthzSubject
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap
import org.hyperic.hq.hqu.rendit.BaseRenditTest
import org.hyperic.hq.hqu.rendit.metaclass.AgentCategory
import org.hyperic.hq.hqu.rendit.metaclass.AppdefCategory
import org.hyperic.hq.hqu.rendit.metaclass.AuthzSubjectCategory
import org.hyperic.hq.hqu.rendit.metaclass.MapCategory
import org.hyperic.hq.hqu.rendit.metaclass.ResourceCategory
import org.hyperic.hq.hqu.rendit.metaclass.ResourceGroupCategory
import org.hyperic.hq.hqu.rendit.metaclass.RoleCategory
import org.hyperic.hq.hqu.rendit.metaclass.StringCategory


public class ResourceHelperTest extends BaseRenditTest {
    private AuthzSubject user;
   
    private ResourceHelper resourceHelper;
    
    protected void setUp() {
        super.setUp();
        this.user = new AuthzSubject(true, "joe", "dept", "email", true, "Joe", "User", "joe", "123-4567", "sms", false);
    }
    
    public void testFindByIdNotFound() {
        EasyMock.expect(authzSubjectManager.getOverlordPojo()).andReturn(overlord);
        EasyMock.expect(resourceManager.findResourceById(234)).andReturn(null);
        replay();
        def CATEGORIES = [AuthzSubjectCategory,AppdefCategory,
            MapCategory,
            ResourceCategory,
            ResourceGroupCategory, RoleCategory,
            StringCategory, AgentCategory]
       Resource actual;
       use (*CATEGORIES) {
            this.resourceHelper = new ResourceHelper(this.user);
            actual = resourceHelper.findById(234);
       }
       verify();
       assertNull(actual);
    }
    
  


}

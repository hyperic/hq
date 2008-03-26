package org.hyperic.hq.ui.pages;

import org.apache.tapestry.BaseComponentTestCase;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.PageRedirectException;
import org.apache.tapestry.engine.ILink;
import org.apache.tapestry.link.PageLink;
import org.hyperic.ui.tapestry.page.PageListing;
import org.junit.Test;

public class SignIn_test extends BaseComponentTestCase {

    /**
     * Note: this test requires the container
     */
    @Test
    public void test_SignInListener() {

	/*
	IMarkupWriter writer = newWriter();
	IRequestCycle cycle = newCycle(true, writer);
	// Create the signin page so the enhanced subclass can be created
	PageLink signInLink = (PageLink) newInstance(PageLink.class, "page",
		"SignIn");
	SignIn signInPage = (SignIn) newInstance(SignIn.class, "userName",
		"hqadmin", "password", "hqadmin", "message", null,
		"signinLink", signInLink);
	try {
	    ILink retVal = signInPage.signinButtonListener(cycle);
	} catch (PageRedirectException e) {
	    assertEquals(e.getTargetPageName(), PageListing.DASHBOARD_URL);
	}
	
	signInPage = (SignIn) newInstance(SignIn.class, "userName",
		"hqadmin", "password", "badpassword", "message", null,
		"signinLink", signInLink);
	ILink retVal = signInPage.signinButtonListener(cycle);
	
	assertEquals(retVal.getURL(), PageListing.SIGN_IN +".html");
	 */
    }
    
}

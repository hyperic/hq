package org.hyperic.hq.web.filter;

import junit.framework.TestCase;

import org.hyperic.hq.web.filter.CharacterEncodingFilter.URIMatcher;

/**
 * Unit test of the {@link CharacterEncodingFilter}
 *
 */
public class CharacterEncodingFilterTest extends TestCase {

    public void testEmptyExcludePath() {  
    	
    	String excludePaths = "";
    	URIMatcher uriMatcher = new URIMatcher();
    	uriMatcher.setPatterns(excludePaths);
    	
    	assertFalse(uriMatcher.matches("/ServerInvokerServlet"));
    }
    
    public void testSingleExcludePath() {  
    	
    	String excludePaths = "/ServerInvokerServlet";
    	URIMatcher uriMatcher = new URIMatcher();
    	uriMatcher.setPatterns(excludePaths);
    	
    	assertTrue(uriMatcher.matches("/ServerInvokerServlet"));
    	assertFalse(uriMatcher.matches("/FakePath"));
    }

    public void testSingleExcludePathWithWildcard() {  
    	
    	String excludePaths = "/ServerInvokerServlet*";
    	URIMatcher uriMatcher = new URIMatcher();
    	uriMatcher.setPatterns(excludePaths);
    	
    	assertTrue(uriMatcher.matches("/ServerInvokerServlet"));  	
    	assertFalse(uriMatcher.matches("/FakePath/fake"));
    }

    public void testSingleInvalidExcludePathWithWildcard() {  
    	
    	String excludePaths = "/ServerInvokerServlet/*";
    	URIMatcher uriMatcher = new URIMatcher();
    	uriMatcher.setPatterns(excludePaths);
    	
    	assertFalse(uriMatcher.matches("/ServerInvokerServlet"));  	
    	assertFalse(uriMatcher.matches("/FakePath/fake"));
    }
    
    public void testMultipleExcludePathsWithWildcards() {  
    	
    	String excludePaths = "/images/*, /ServerInvokerServlet*,/transport/*";
    	URIMatcher uriMatcher = new URIMatcher();
    	uriMatcher.setPatterns(excludePaths);
    	
    	assertTrue(uriMatcher.matches("/ServerInvokerServlet"));  
    	assertTrue(uriMatcher.matches("/transport/ServerInvokerServlet"));  
    	assertTrue(uriMatcher.matches("/images/fake.gif"));  	
    	assertFalse(uriMatcher.matches("/js/fake.js"));
    }
}
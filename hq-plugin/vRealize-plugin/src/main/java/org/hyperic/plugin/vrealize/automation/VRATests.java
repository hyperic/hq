package org.hyperic.plugin.vrealize.automation;

//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import clover.org.apache.commons.lang.StringUtils;

public class VRATests {

//	@Test
//	public void testGetFQDN(){
//		String trueFQDN = "example.com";
//		ArrayList<String> addresses = new ArrayList<String>(){{
//			add("example.com");
//			add("example.com:7444");
//			add("example.com\\:7444");
//			add("https://example.com/component-registry/");
//			add("https\\://example.com/component-registry/");
//			add("sqlserver://example.com:1433/vCO;domain=refarch.eng.vmware.com;useNTLMv2=true");
//			add("example.com:1433/vCO");
//		}};
//		for(String address : addresses){
//			String fqdn = VRAUtils.getFqdn(address);
//			Assert.assertEquals("tried the address: " + address, trueFQDN, fqdn);
//		}
//
//		String malformedFQDNExpected = StringUtils.EMPTY;
//		ArrayList<String> malfomedAddresses = new ArrayList<String>(){{
//			add(" ");
//			add("");
//			add(null);
//		}};
//		for(String address : malfomedAddresses){
//			String fqdn = VRAUtils.getFqdn(address);
//			Assert.assertEquals("tried the address: " + address, malformedFQDNExpected, fqdn);
//		}
//	}
//	
//	@Test
//	public void testGetFqdnWithIp(){
//	    String trueFQDN = "localhost";
//	    ArrayList<String> addresses = new ArrayList<String>(){{
//	        add("localhost");
//	        add("127.0.0.1");
//	        add("http://127.0.0.1");
//	        add("127.0.0.1:80");
//	        add("http://127.0.0.1:80");
//	        add("127.0.0.1:800");
//	        add("http://127.0.0.1:800");
//
////	          add("fe80::9eb:2212:c291:301d%6");
////	            add("fe80::9eb:2212:c291:301d%6:7080");
////	            add("http://10.23.197.215");
//	    }};
//	    for(String address : addresses){
//	        String fqdn = VRAUtils.getFqdn(address);
//	        Assert.assertEquals("tried the address: " + address, trueFQDN, fqdn);
//	    }
//
//	    String malformedFQDNExpected = StringUtils.EMPTY;
//	    ArrayList<String> malfomedAddresses = new ArrayList<String>(){{
//	        add(" ");
//	        add("");
//	        add(null);
//	    }};
//	    for(String address : malfomedAddresses){
//	        String fqdn = VRAUtils.getFqdn(address);
//	        Assert.assertEquals("tried the address: " + address, malformedFQDNExpected, fqdn);
//	    }
//	}

    /*
    @Test
    public void testDnsNamesExtractor(){
        Collection<String> dnsNames = VRAUtils.getDnsNames("https://ra-vco-a2-bg-00.refarch.eng.vmware.com:8281");
        Assert.assertNotNull("DNS names list is NULL", dnsNames);

        for(String dns : dnsNames){
            System.out.println(" --> " + dns);
        }
    }
    */
	
}

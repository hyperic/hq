package org.hyperic.hq.api;

import org.springframework.web.util.UriTemplate;

public class LinkHelper {
	private final static UriTemplate ROOT_URI = new UriTemplate("/api");
	private final static UriTemplate DOMAIN_URI = new UriTemplate(ROOT_URI.toString() + "/{domainName}");
	private final static UriTemplate INSTANCE_URI = new UriTemplate(DOMAIN_URI.toString() + "/{id}");
	private final static UriTemplate COLLECTION_URI = new UriTemplate(INSTANCE_URI.toString() + "/{collectionName}");
	
	public static String getRootUri() {
		return ROOT_URI.expand().toASCIIString();
	}
	
	public static String getDomainUri(String domainName) {
		return DOMAIN_URI.expand(domainName).toASCIIString();
	}
	
	public static String getInstanceByIdUri(String domainName, Integer id) {
		return INSTANCE_URI.expand(domainName, id).toASCIIString();
	}
	
	public static String getInstanceByNameUri(String domainName, String instanceName) {
		return INSTANCE_URI.expand(domainName, instanceName).toASCIIString();
	}
	
	public static String getCollectionUri(String domainName, Integer id, String collectionName) {
		return COLLECTION_URI.expand(domainName, id, collectionName).toASCIIString();
	}
}
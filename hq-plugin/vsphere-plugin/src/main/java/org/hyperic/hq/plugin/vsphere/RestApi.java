package org.hyperic.hq.plugin.vsphere;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.hyperic.hq.pdk.domain.Agent;
import org.hyperic.hq.pdk.domain.AgentResponse;
import org.hyperic.hq.pdk.domain.ListOfResourcesResponse;
import org.hyperic.hq.pdk.domain.Resource;
import org.hyperic.hq.pdk.domain.ResourceResponse;
import org.hyperic.hq.pdk.domain.ResourceType;
import org.hyperic.hq.pdk.domain.ResourceTypeResponse;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

public class RestApi {
	private static final String HQ_IP = "agent.setup.camIP";
	private static final String HQ_PORT = "agent.setup.camPort";
	// private static final String HQ_SPORT = "agent.setup.camSSLPort";
	private static final String HQ_SSL = "agent.setup.camSecure";
	private static final String HQ_USER = "agent.setup.camLogin";
	private static final String HQ_PASS = "agent.setup.camPword";
	
	private RestTemplate rest;
	private String baseUri;
	
	public RestApi(Properties props) {
		System.out.println("Configuring Http Client...");
		
		HttpClient client = new HttpClient();
		
		client.getParams().setAuthenticationPreemptive(true);
		
		Credentials credentials = new UsernamePasswordCredentials(props.getProperty(HQ_USER, "hqadmin"), 
				props.getProperty(HQ_PASS, "hqadmin"));
		
		String address = props.getProperty(HQ_IP, "localhost");
		Integer port;
		String scheme;
		
		// TODO support SSL
		if ("yes".equals(props.getProperty(HQ_SSL))) {
			port = Integer.valueOf(props.getProperty(HQ_PORT, "8443"));
			scheme = "https";
		} else {
			port = Integer.valueOf(props.getProperty(HQ_PORT, "8080"));
			scheme = "http";
		}
		
		client.getState().setCredentials(new AuthScope(address, port), credentials);
		
		CommonsClientHttpRequestFactory clientFactory = new CommonsClientHttpRequestFactory(client);
		RestTemplate rest = new RestTemplate(clientFactory);

		// TODO make configurable via DI?
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		
		messageConverters.add(new FormHttpMessageConverter());
		messageConverters.add(new MappingJacksonHttpMessageConverter());
		
		rest.setMessageConverters(messageConverters);
	
		this.baseUri = new UriTemplate("{scheme}://{address}:{port}/app/api").expand(scheme, address, port).toASCIIString();
		this.rest = rest;
		
		System.out.println("Base Uri is " + this.baseUri);
	}
	
	public Agent getAgent(String address, Integer port) {
		String url = baseUri + "/agents/{address}:{port}";

		System.out.print("GET " + url + ", address["+ address + "], port[" + port + "]...");

		// TODO Use parameterized type instead of what we're doing. https://jira.springsource.org/browse/SPR-7023
		AgentResponse response = rest.getForObject(url, AgentResponse.class, address, port);
		
		System.out.println("SUCCESS");
		
		return response.getData();
	}
	
	public ResourceType getResourceType(String name) {
		String url = baseUri + "/resource-types/name:{name}";

		System.out.print("GET " + url + ", name["+ name + "]...");

		// TODO Use parameterized type instead of what we're doing. https://jira.springsource.org/browse/SPR-7023
		ResourceTypeResponse response = rest.getForObject(url, ResourceTypeResponse.class, name);
		
		System.out.println("SUCCESS");
		
		return response.getData();
	}
	
	public void makeRelationship(Resource from, List<Resource> tos) {
		for (Resource to : tos) {
			makeRelationship(from, to);
		}
	}
	
	public void makeRelationship(Resource from, Resource to) {
		String url = baseUri + "/resources/{id}/relationships/{toId}";

		System.out.print("PUT " + url + ", id["+ from.getId() + "], toId[" + to.getId() + "]...");

		rest.put(url, null, from.getId(), to.getId());
		
		System.out.println("SUCCESS");
	}
	
	public List<Resource> getResourcesByTypeName(String typeName) {
		String url = baseUri + "/resources/type:{name}";
		
		System.out.print("GET " + url + ", name["+ typeName + "]...");

		ListOfResourcesResponse response = rest.getForObject(url, ListOfResourcesResponse.class, typeName);
		
		System.out.println("SUCCESS");

		return response.getData().getList();
	}
	
	public List<Resource> createResources(List<Resource> resources) {
		List<Resource> result = new ArrayList<Resource>();
		
		for (Resource resource : resources) {
			result.add(createResource(resource));
		}
		
		return result;
	}
	
	public Resource getResourceById(Integer id) {
		String url = baseUri + "/resources/{id}";
		
		System.out.print("GET " + url + ", id["+ id + "]...");

		ResourceResponse response = rest.getForObject(url, ResourceResponse.class, id);
		
		System.out.println("SUCCESS");
		
		return response.getData();
	}
	
	public Resource createResource(Resource resource) {
		String url = baseUri + "/resources";
		
		System.out.print("POST " + url + "...");

		ResourceResponse response = rest.postForObject(url, resource, ResourceResponse.class);
		
		System.out.println("SUCCESS");
		
		return response.getData();
	}
	
	public void deleteResource(Integer id) {
		String url = baseUri + "/resources/{id}";
		
		System.out.print("DELETE " + url + ", id["+ id + "]...");

		rest.delete(url, id);
		
		System.out.println("SUCCESS");
	}
	
	public ResourceType syncResourceType(ResourceType resourceType) {
		String url = baseUri + "/resource-types/name:{name}";
		ResourceTypeResponse response;
		
		try {
			System.out.print("GET " + url + ", name[" + resourceType.getName() + "]...");
			
			response = rest.getForObject(url, ResourceTypeResponse.class, resourceType.getName());
			
			System.out.println("SUCCESS");
		} catch(HttpClientErrorException e) {
			System.out.println("NOT FOUND");
			
			url = baseUri + "/resource-types";
			
			System.out.print("POST " + url + "...");
			
			response = rest.postForObject(url, resourceType, ResourceTypeResponse.class);
			
			System.out.println("SUCCESS");
		}
		
		return response.getData();
	}

	public void makeRelationship(ResourceType from, ResourceType to, String name) {
		String url = baseUri + "/resource-types/{id}/relationships/{name}/{toId}";
		
		System.out.print("PUT " + url + ", id["+ from.getId() + "], name[" + name + "], toId[" + to.getId() + "]...");
		
		rest.put(url, null, from.getId(), name, to.getId());
		
		System.out.println("SUCCESS");
	}
	
	public ResourceType getResourceTypeRoot() {
		String url = baseUri + "/resource-types/root";
		
		System.out.print("GET " + url + "...");
		
		ResourceTypeResponse response = rest.getForObject(url, ResourceTypeResponse.class);
		
		System.out.println("SUCCESS");
		
		return response.getData();
	}
}
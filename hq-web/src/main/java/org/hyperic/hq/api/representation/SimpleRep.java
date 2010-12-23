package org.hyperic.hq.api.representation;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleRep {
	private Integer id;
	private String name;
	private String uri;
	
	public SimpleRep() {}
	
	public SimpleRep(SimpleRepresentation entity) {
		this(entity.getId(), entity.getName(), entity.getLinks());
	}
	
	public SimpleRep(Integer id, String name, Map<String, String> links) {
		String uri = null;
		
		if (links.containsKey(LinkedRepresentation.SELF_LABEL)) {
			uri = links.get(LinkedRepresentation.SELF_LABEL).toString();
		}
		
		this.id = id;
		this.name = name;
		this.uri = uri;
	}
	
	public SimpleRep(Integer id, String name, String uri) {
		this.id = id;
		this.name = name;
		this.uri = uri;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}


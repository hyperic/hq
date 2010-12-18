package org.hyperic.hq.api.representation;

import java.util.Map;

public interface SimpleRepresentation {
	public Integer getId();
	public String getName();
	public Map<String, String> getLinks();
}


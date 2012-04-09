package org.hyperic.hq.caf;

import java.util.HashMap;
import java.util.Map;

/**
 * This component holds CAF responses so that those who execute invoke operation
 * could find the response to those operation here mapped by the request's id
 */
public class CafResultsHolder {
	
	private static Map<String, CafResponse> cafResults = new HashMap<String, CafResponse>();
	
	public static void addResult(String id, CafResponse response) {
		cafResults.put(id, response);
	}
	
	public static CafResponse getResults(String id) {
		return cafResults.get(id);
	}

	public static void removeResult(String id) {
		cafResults.remove(id);
	}

}

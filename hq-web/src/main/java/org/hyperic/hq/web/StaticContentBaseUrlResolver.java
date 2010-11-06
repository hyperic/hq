package org.hyperic.hq.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticContentBaseUrlResolver {
	private static final String BASE_PATH = "static";
	
	private String baseUrl;
	
	@Autowired
	public StaticContentBaseUrlResolver(@Value("#{ webProperties['static.content.base.url'] }") String baseUrl, 
			                            @Value("#{ webProperties['version'] }") String version) {
		StringBuilder tempBaseUrl = new StringBuilder();
		
		// ...first construct the base path...
		if (baseUrl != null ) {
			tempBaseUrl.append(baseUrl);
			
			// ...does the baseUrl have a trailing '/', if not, add one...
			if (!baseUrl.endsWith("/")) {
				tempBaseUrl.append("/");
			}
		}

		// ...if length is 0, we are dealing with a relative path, add a '/'...
		if (tempBaseUrl.length() == 0) {
			tempBaseUrl.append("/");
		}

		// ...continue with adding the static path info...
		tempBaseUrl.append(BASE_PATH).append("/");

		// ...and finally, add the version number, if we have one...
		if (version != null) {
			tempBaseUrl.append(version).append("/");
		}

		// ...we have the base url, set it...
		this.baseUrl = tempBaseUrl.toString();
	}

	public String getBaseUrl() {
		return baseUrl;
	}
}
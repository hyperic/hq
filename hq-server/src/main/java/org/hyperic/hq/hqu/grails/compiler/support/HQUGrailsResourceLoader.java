package org.hyperic.hq.hqu.grails.compiler.support;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.hyperic.hq.hqu.grails.commons.HQUGrailsResourceUtils;
import org.springframework.core.io.Resource;

public class HQUGrailsResourceLoader extends GrailsResourceLoader {
	
	private static Log log = LogFactory.getLog(HQUGrailsResourceLoader.class);

    private List loadedResources = new ArrayList();
    private Map classToResource = new HashMap();
    private Map pathToResource = new HashMap();

	public HQUGrailsResourceLoader(Resource[] resources) {
		super(resources);
		createPathToURLMappings(resources);
	}
	
	public List getLoadedResources() {
		return ListUtils.sum(super.getLoadedResources(), loadedResources);
	}
	
	public void setResources(Resource[] resources) {
		super.setResources(resources);		
		createPathToURLMappings(resources);
	}
		
	public URL loadGroovySource(String className) throws MalformedURLException {
		URL url = super.loadGroovySource(className);
		if(url == null) {
			String groovyFile = className.replace('.', '/') + ".groovy";
	        try {

	            Resource foundResource = (Resource)pathToResource.get(groovyFile);
	            if (foundResource != null) {
	                loadedResources.add(foundResource);
	                classToResource.put(className, foundResource);
	                return foundResource.getURL();
	            } else {
	                return null;
	            }
	        } catch (IOException e) {
	            throw new GrailsConfigurationException("I/O exception loaded resource:" + e.getMessage(),e);
	        }	
		} else {
			return url;			
		}
	}
	
	public Resource getResourceForClass(Class theClass) {
		Resource res = super.getResourceForClass(theClass);

		if(res == null) {
			res = (Resource)classToResource.get(theClass.getName());
		}
		
		return res;
	}
	
    private void createPathToURLMappings(Resource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            String resourceURL;
            try {
                resourceURL = resources[i].getURL().toString();
            } catch (IOException e) {
                throw new GrailsConfigurationException("Unable to load Grails resource: " + e.getMessage(), e);
            }
            String pathWithinRoot = HQUGrailsResourceUtils.getPathFromRoot(resourceURL);
            pathToResource.put(pathWithinRoot, resources[i]);
        }
    }

	
}

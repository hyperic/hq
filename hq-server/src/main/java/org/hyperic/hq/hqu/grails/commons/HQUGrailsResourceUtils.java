package org.hyperic.hq.hqu.grails.commons;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.springframework.core.io.Resource;

public class HQUGrailsResourceUtils {

	public static final Pattern HQU_GRAILS_RESOURCE_PATTERN_FIRST_MATCH;
	
	static {
        String fs = File.separator;
        if (fs.equals("\\")) fs = "\\\\"; // backslashes need escaping in regexes

        HQU_GRAILS_RESOURCE_PATTERN_FIRST_MATCH = Pattern.compile(createHQUGrailsResourcePattern(fs, "\\w+"));

	}
	
    private static String createHQUGrailsResourcePattern(String separator, String base) {
		return ".+"+separator +base+separator +"(.+)\\.groovy";
	}
    
    // TODO: make this more clever!!!
    public static Pattern RESOURCE_PATH_PATTERN = Pattern.compile(".+?/hqu-plugins/(.+?)/(.+?)/(.+?\\.groovy)");


    public static Pattern[] COMPILER_ROOT_PATTERNS = {
        RESOURCE_PATH_PATTERN
    };


    public static final Pattern[] patterns = new Pattern[]{
        HQU_GRAILS_RESOURCE_PATTERN_FIRST_MATCH
};

	public static String getClassName(Resource resource) {
        try {
        	return getClassName(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new GrailsConfigurationException("I/O error reading class name from resource ["+resource+"]: " + e.getMessage(),e );
        }        	
	}

	public static String getClassName(String path) {
		for (int i = 0; i < patterns.length; i++) {
			Matcher m = patterns[i].matcher(path);
	        if(m.find()) {
	            return m.group(1).replaceAll("[/\\\\]", ".");
	        }			
		}
        return null;
	}
	
    public static String getPathFromRoot(String path) {
        for (int i = 0; i < COMPILER_ROOT_PATTERNS.length; i++) {
            Matcher m = COMPILER_ROOT_PATTERNS[i].matcher(path);
            if(m.find()) {
                return m.group(m.groupCount());
            }
        }
        return null;
    }


}

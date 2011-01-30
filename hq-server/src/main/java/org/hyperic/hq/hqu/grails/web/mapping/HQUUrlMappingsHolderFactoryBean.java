package org.hyperic.hq.hqu.grails.web.mapping;

import groovy.lang.Script;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsUrlMappingsClass;
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingParser;
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.UrlMappingEvaluator;
import org.codehaus.groovy.grails.web.mapping.UrlMappingParser;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

/**
 * A FactoryBean for constructing the UrlMappingsHolder from the registered UrlMappings class within a
 * GrailsApplication
 *
 * @author Graeme Rocher
 */
public class HQUUrlMappingsHolderFactoryBean implements FactoryBean, InitializingBean, GrailsApplicationAware, ServletContextAware {
    private GrailsApplication grailsApplication;
    private UrlMappingsHolder urlMappingsHolder;
    private UrlMappingEvaluator mappingEvaluator;
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();
    private ServletContext servletContext;

    public Object getObject() throws Exception {
        return this.urlMappingsHolder;
    }

    public Class getObjectType() {
        return UrlMappingsHolder.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        if(grailsApplication == null) throw new IllegalStateException("Property [grailsApplication] must be set!");

        List urlMappings = new ArrayList();
        List excludePatterns = new ArrayList();
        
        GrailsClass[] mappings = grailsApplication.getArtefacts(UrlMappingsArtefactHandler.TYPE);

        this.mappingEvaluator = new DefaultHQUUrlMappingEvaluator(servletContext);

        for (GrailsClass mapping : mappings) {
            GrailsUrlMappingsClass mappingClass = (GrailsUrlMappingsClass) mapping;
            List grailsClassMappings;
            if (Script.class.isAssignableFrom(mappingClass.getClass())) {
                grailsClassMappings = mappingEvaluator.evaluateMappings(mappingClass.getClazz());
            }
            else {
                grailsClassMappings = mappingEvaluator.evaluateMappings(mappingClass.getMappingsClosure());
            }

            urlMappings.addAll(grailsClassMappings);
            if (mappingClass.getExcludePatterns() != null) excludePatterns.addAll(mappingClass.getExcludePatterns());
        }



        this.urlMappingsHolder = new DefaultUrlMappingsHolder(urlMappings, excludePatterns);

    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}

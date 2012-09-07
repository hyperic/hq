/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.tools.ant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DataType;
import org.hyperic.tools.ant.utils.PropertiesMerger;
import org.hyperic.tools.ant.utils.PropertiesMerger.PropertiesMergerFilter;

public class PropertiesMergerTask extends Task {

    private String baseFile ; 
    private String overrideFile ; 
    private String outputFile ; 
    private Filter filter ; 
    
    public final void setBaseFile(final String baseFile) { 
        this.baseFile = baseFile ; 
    }//EOM 
    
    public final void setOverrideFile(final String overrideFile) { 
        this.overrideFile = overrideFile ; 
    }//EOM 
    
    public final void setOutputFile(final String outputFile) { 
        this.outputFile = outputFile ; 
    }//EOM 
    
    public final void addConfiguredPropertiesMergeFilter(Filter filter) { 
        if(filter.isReference()) filter = (Filter) filter.getRefid().getReferencedObject() ; 
        this.filter = filter ; 
    }//EOM 
    
    @SuppressWarnings("unchecked")
    @Override
    public final void execute() throws BuildException {
        try{ 
            final PropertiesMerger merger = new PropertiesMerger(this.outputFile, this.filter) ;
            merger.setBaseFile(this.baseFile) ; 
            merger.setOverrideFile(this.overrideFile) ;
            if(this.filter.prunes != null) merger.setPrunePropertiesList(this.filter.prunes) ; 
            if(this.filter.environmentValues != null) merger.setOverridePropertyValues(this.filter.environmentValues) ; 
            merger.merge() ; 
        }catch(Throwable t) { 
            throw new BuildException(t) ; 
        }//EO catch block 
    }//EOM 
    
    public static final class Filter extends DataType implements PropertiesMergerFilter{ 
        private Set<String> includes ;
        private Set<String> excludes;
        private Set<String> prunes ; 
        private Map<String,String> environmentValues ; 
        private boolean shouldIncludeNewProperties = true ; 
        
        public void addConfiguredInclude(final IncludesFilter includeFilter) { 
            if(this.includes  == null) this.includes = new HashSet<String>() ;
            
            final String propertyName = includeFilter.name ; 
            if(!includeFilter.environment) this.includes.add(propertyName) ; 
            else { 
                final String envValue = this.getProject().getProperty(propertyName) ; 
                if(envValue != null) { 
                    if(this.environmentValues == null) this.environmentValues = new HashMap<String,String>() ; 
                    this.environmentValues.put(propertyName, envValue) ;
                }//EO if the property value was not null 
            }//EO else if environment property 
        }//EOM 
        
        public void addConfiguredExclude(final ExcludesFilter excludeFilter) {
            if(this.excludes == null) this.excludes = new HashSet<String>() ;
            this.excludes.add(excludeFilter.name) ; 
        }//EOM 
        
        public void addConfiguredPrune(final PruneFilter pruneFilter) {
            if(this.prunes == null) this.prunes = new HashSet<String>() ;
            this.prunes.add(pruneFilter.name) ; 
        }//EOM
        
        public void setIncludeNew(final boolean shouldIncludeNewProperties) { 
            this.shouldIncludeNewProperties = shouldIncludeNewProperties ; 
        }//EOM
         
        public boolean apply(String propertyName, String propertyValue, final Properties properties) {
            //return true IFF: 
            // - not in the excludes list &&  
            // - includes list is empty || 
            // - in the includes list || 
            // - new property &&  shouldIncludeNewProperties == true
            boolean propertyExists = false ; 
            return  ( 
                        (this.excludes == null || !this.excludes.contains(propertyName)) && 
                        (
                             (this.includes != null && this.includes.contains(propertyName)) || 
                             (this.shouldIncludeNewProperties && ! (propertyExists = properties.containsKey(propertyName))) ||
                             (propertyExists && this.includes == null)
                        )
                   )  ;
        }//EOM 
        
    }//EO inner class Filter 
    
    public static class IncludesFilter { 
        protected String name ; 
        protected boolean environment ; 
        
        public final void setName(final String name) { 
            this.name = name ; 
        }//EOM 
        
        public final void setEnvironment(final boolean environment) { 
            this.environment = environment; 
        }//EOM 
    }//EO inner class IncludesFilter
    
    public static final class ExcludesFilter extends IncludesFilter {}//EO inner class IncludesFilter
    
    public static final class PruneFilter extends IncludesFilter {}//EO inner class PruneFilter 

    public static void main(String[] args) throws Throwable {
        
        final String propertyName = "test1" ; 
        final HashSet<String> excludes = new HashSet<String>() ; 
        //excludes.add("test") ; 
        final HashSet<String> includes = null ; // new HashSet<String>() ;
        //includes.add("test");
        final Properties properties = new Properties() ;
        properties.setProperty("test", "Sdf") ; 
        final boolean shouldIncludeNewProperties= true ; 
       
        boolean propertyExists = false ; 
        System.out.println( ( 
                 (excludes == null || !excludes.contains(propertyName)) && 
                 (
                  (includes != null && includes.contains(propertyName)) || 
                  (shouldIncludeNewProperties && ! (propertyExists = properties.containsKey(propertyName))) ||
                  (propertyExists && includes == null)
                )
                )
        );
    }//EOM 
}//EOC 

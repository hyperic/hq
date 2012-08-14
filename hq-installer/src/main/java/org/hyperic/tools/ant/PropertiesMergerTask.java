package org.hyperic.tools.ant;

import java.util.HashSet;
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
    
    @Override
    public final void execute() throws BuildException {
        try{ 
            final PropertiesMerger merger = new PropertiesMerger(this.outputFile, this.filter) ;
            merger.setBaseFile(this.baseFile) ; 
            merger.setOverrideFile(this.overrideFile) ; 
            merger.merge() ; 
        }catch(Throwable t) { 
            throw new BuildException(t) ; 
        }//EO catch block 
    }//EOM 
    
    
    public static final class Filter extends DataType implements PropertiesMergerFilter{ 
        private Set<String> includes ;
        private Set<String> excludes;
        private boolean shouldIncludeNewProperties = true ; 
        
        
        public void addConfiguredInclude(final IncludesFilter includeFilter) { 
            if(this.includes  == null) this.includes = new HashSet<String>() ;
            this.includes.add(includeFilter.name) ;
        }//EOM 
        
        public void addConfiguredExclude(final ExcludesFilter excludeFilter) {
            if(this.excludes == null) this.excludes = new HashSet<String>() ;
            this.excludes.add(excludeFilter.name) ; 
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
        public final void setName(final String name) { 
            this.name = name ; 
        }//EOM 
    }//EO inner class IncludesFilter
    
    public static final class ExcludesFilter extends IncludesFilter {}//EO inner class IncludesFilter

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

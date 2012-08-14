package org.hyperic.tools.ant.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class LoadFileContentTask extends Task{
    
    private static final String MATCH_REGEX = "\\$\\{(.*?)\\}" ;
    private static final Pattern MATCH_PATTERN = Pattern.compile(MATCH_REGEX, Pattern.MULTILINE)  ;

    private String file ; 
    private String property ; 
    private boolean trim ; 
    
    public LoadFileContentTask() {
    }//EOM 
    
    public final void setTrim(final boolean trim) { 
        this.trim = trim ; 
    }//EOM 
    
    public final void setProperty(final String propertyName) { 
        this.property = propertyName ;  
    }//EOM 
    
    public final void setFile(final String file) { 
        this.file = file ;
    }//EOM
    
    @Override
    public void execute() throws BuildException {
       try{ 
           FileInputStream fis = null ; 
            try{ 
               fis = new FileInputStream(this.file) ;
               final byte[] arrContent = new byte[(int)fis.available()] ; 
               fis.read(arrContent) ; 
               String content = new String(arrContent) ;
               
               final Matcher matcher = MATCH_PATTERN.matcher(content) ;
               
               String propertyName = null, propertyValue = null ; 
               
               final List<Object[]> replacementRegions = new ArrayList<Object[]>() ;
               
               final Project project = this.getProject() ;
               
               while(matcher.find()) {
                   propertyName = matcher.group(1) ;
                   propertyValue = project.getProperty(propertyName) ;
                   replacementRegions.add(new Object[] { matcher.start(), matcher.end(), propertyValue }) ; 
               }//EO while there are more matches
                   
               Object[] replacementRegion = null ; 
               for(int i=replacementRegions.size()-1; i >= 0 ; i--) {
                   replacementRegion = replacementRegions.get(i); 
                   propertyValue = (String) replacementRegion[2] ; 
                 
                   content = content.substring(0, (Integer)replacementRegion[0]) + (propertyValue == null ? "" : propertyValue) 
                           + content.substring((Integer)replacementRegion[1]) ;
               }//EO while there are more replacement regions 
               
               if(this.trim) content = content.trim() ;
               
               if(this.property != null) this.getProject().setProperty(this.property, content) ; 
               else this.log(content, Project.MSG_WARN) ;
               
           }finally{ 
               if(fis != null) fis.close() ; 
           }//EO catch block
       }catch(IOException ioe)  {
           throw new BuildException(ioe) ; 
       }//EO catch block 
    }//EOM 
    
    public static void main(String[] args) throws Throwable {
        
        final Map<String,String> properties = new HashMap<String,String>() ; 
        properties.put("SOURCE_SERVER_VERSION", "1") ; 
        properties.put("SOURCE_DB_VERSION", "2") ; 
        properties.put("TARGET_SERVER_VERSION", "3") ; 
        final Project project = new Project() { 
            
            @Override
            public String getProperty(String propertyName) {
                return properties.get(propertyName) ; 
            }//EOM 
            
        }; //EO project
        
        final LoadFileContentTask task = new LoadFileContentTask() ; 
        task.setProject(project) ; 
        task.setFile("/work/workspaces/master-complete/hq/dist/installer/src/main/resources/data/reports/migration-summary.txt") ;
        task.execute() ;
        
    }//EOM 
}//EOC 

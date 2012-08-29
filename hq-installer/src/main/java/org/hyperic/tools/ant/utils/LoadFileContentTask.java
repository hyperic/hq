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

/**
 * Loads the complete content of a file.<br/>
 * If the property member was defined store the content is the property else 
 * logs the content using {@link Project#MSG_WARN} level.
 * @author guy
 *
 */
public class LoadFileContentTask extends Task{
    
    private static final String MATCH_REGEX = "\\$\\{(.*?)\\}" ;
    private static final Pattern MATCH_PATTERN = Pattern.compile(MATCH_REGEX, Pattern.MULTILINE)  ;

    private String file ; 
    private String property ; 
    private boolean trim ; 
    
    public LoadFileContentTask() {}//EOM 
    
    /**
     * @param trim if true shall trim all whitespaces from beginning and end of the content 
     */
    public final void setTrim(final boolean trim) { 
        this.trim = trim ; 
    }//EOM 
    
    /**
     * @param propertyName property name to store the content in
     */
    public final void setProperty(final String propertyName) { 
        this.property = propertyName ;  
    }//EOM 
    
    /**
     * @param file source file from which to load the content 
     */
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

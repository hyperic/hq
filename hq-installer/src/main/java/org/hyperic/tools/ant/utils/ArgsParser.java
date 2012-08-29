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

import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Commandline arguments parser supports the following formats:</b> 
 * <p>
 * -D<<key>>=<<value> ...<br/> 
 * -flag ...<br/>
 * --flag ...<br/>
 * </p> 
 * <br/>
 * <h1><b>Example:</b></h1>
 *  
 * <p>
 * -Dserver.database-url=jdbc:mysql://localhost:3306/hqdb -Dserver.database-driver=com.mysql.jdbc.Driver -Dserver.database=MySQL -Dserver.database-user=hqadmin -Dserver.database-password=hqadmin -Dserver.admin.username=hqadmin -Dserver.admin.password=hqadmin  
                -Dexport -Dstaging.dir=/work/temp/hq-migration/export-workspace/staging-area -Dhqserver.install.path=/work/temp/hq-migration/servers/server-4.6.5.2.BUILD-SNAPSHOT-EE 
                -Dimport -Dexport.archive.path=/work/temp/hq-migration/export-workspace/hq-migration-4.6.5.1.zip -Dhqserver.install.path=/work/temp/hq-migration/servers/server-5.0-EE  
                -Dpath=\"C:\\Program Files\\dir1\\dir2\\file=.txt\" ;
 * </p>
 * <br/>
 * <p> 
 * All arguments are added to the project's environment.
 * <p/> 
 *  
 */
public class ArgsParser extends Task{

    private final String SPLIT_REGEX = "((?<=%1$2s)|(?=%1$2s))" ;
    private String cmdArgs ;
    private String interArgDelimiter ; 
    private String keyValDelimiter ;
    private String argIndentifier ; 
    
    /**
     * @param interArgDelimiter Example: -Dkey=value[interArgDel]-Dkey=value  
     */
    public final void setArgDel(final String interArgDelimiter) { 
        this.interArgDelimiter = interArgDelimiter ; 
    }//EOM 
    
    /**
     * @param argIndentifier Argument prefix. for instance the '-D' in -Dkey=value
     */
    public final void setArgIdentifier(final String argIndentifier) { 
        this.argIndentifier = argIndentifier ; 
    }//EOM
    
    /**
     * @param keyValDelimiter Example: -Dkey[keyValDel]value -Dkey[keyValDel]value  
     */
    public final void setKeyValDel(final String keyValDelimiter) { 
        this.keyValDelimiter = keyValDelimiter ; 
    }//EOM 
    
    /**
     * @param cmdArgs arguments to parse
     */
    public final void setCMDArgs(final String cmdArgs) { 
        this.cmdArgs = cmdArgs ;  
    }//EOM 
    
    @Override
    public void execute() throws BuildException {
        this.parseArguments(this.cmdArgs) ; 
    }//EOM 
    
    private final void parseArguments(final String args) {
        final String delimiters =  this.buildDelimitersString() ; 
        final String splitRegex = String.format(SPLIT_REGEX, delimiters) ;

        this.log("CMD Args: " + args + ", delimiters split regex: " + splitRegex) ;
        
        final String[] arrSplitArgs = args.trim().split(splitRegex) ;

        //first element would be an empty element
        final Context context = new Context(1/*currIndex*/, this.argIndentifier, this.interArgDelimiter, this.keyValDelimiter) ;
        context.project = this.getProject() ; 
        
        final int iLength = arrSplitArgs.length ;
        ArgPartParserType parser = ArgPartParserType.ArgIdentifier ;
        
        for(; context.currIndex < iLength && parser != null ;) {
            context.previousParser  = parser ;
            parser = parser.parseArgument(arrSplitArgs, context);
        }//EO while there are more elements 
    }//EOM 
    
    private static enum ArgPartParserType { 
        
        Key { 
            
            @Override
            final ArgPartParserType parseArgument(final String[] arrArgParts, final Context context) {
                context.currentKey = arrArgParts[context.currIndex] ;
                if(context.currIndex+1 < arrArgParts.length) context.currIndex++ ;
                
                return Value ;
            }//EOM 
            
            @Override
            final boolean shouldParse(final String argPart, final Context context) { 
                return context.previousParser == ArgIdentifier ; 
            }//EOM 

        },//EO KeyVal
        Value { 
            @Override
            final ArgPartParserType parseArgument(final String[] arrArgParts, final Context context) {
               
                //if there is no keyval delimiter, assign "true" to the value and return
                int iLength = arrArgParts.length ; 
                String value = arrArgParts[context.currIndex] ;
                ArgPartParserType nextParser = null ;
                
                if(!this.shouldParse(value, context) || (context.currIndex+1 < iLength && ArgIdentifier.shouldParse(arrArgParts[context.currIndex+1], context))) { 
                    value = "true" ; 
                    nextParser = InterArg ; 
                }else { 
                  context.currIndex++ ;
                  
                  String argPart = null ; 
                  value = "" ;
                  
                  for(; context.currIndex < iLength; context.currIndex++) { 
                      argPart = arrArgParts[context.currIndex] ; 
                      //if the arg part is not an inter arg del||
                      //arg part is an inter arg del but the one after it is not an arg identifier contact the arg part to the value 
                      if(InterArg.shouldParse(argPart, context) && 
                              (context.currIndex+1 < iLength && ArgIdentifier.shouldParse(arrArgParts[context.currIndex+1], context))){ 
                          nextParser = InterArg ; 
                          break ;
                      }else { 
                          value = value + argPart ; 
                      }//EO else if arg type is not an inter arg delimiter 
                  }//EO while there are more arg parts 

                }//EO else there was a keyval delimiter 
                
                //context.project.log("Setting Property: " + context.currentKey + " with value: " + value + ", Current Index: " +  context.currIndex, Project.MSG_DEBUG);
                context.project.setProperty(context.currentKey, value) ;
                context.currentKey = null ;
                context.currentValue = null ;
                
                return nextParser ; 
            }//EOM 
            
            @Override
            final boolean shouldParse(final String argPart, final Context context) { 
                return context.keyValDelimiter.equals(argPart) ;   
            }//EOM 

        },//EO value 
        InterArg {

            @Override
            final ArgPartParserType parseArgument(final String[] arrArgParts, final Context context) {
                final String argPart = arrArgParts[context.currIndex] ;
                context.currIndex++ ; 
                context.project.log("Index: " + context.currIndex + " Argument Part: '" + argPart + "'", Project.MSG_DEBUG) ;
                
                return ArgIdentifier ; 
            }//EOM 
            
            @Override
            final boolean shouldParse(final String argPart, final Context context) { 
                return context.interArgDelimiter.equals(argPart) ; 
            }//EOM 
            
        },//EO InterArg
        ArgIdentifier {

            @Override
            final ArgPartParserType parseArgument(final String[] arrArgParts, final Context context) {
                final String argPart = arrArgParts[context.currIndex] ; 
                
                if(!this.shouldParse(argPart, context)) throw new BuildException("Argument part: '" + argPart + "' at index: " + context.currIndex + 
                            " is not a valid Argument Identifier") ;

                context.currIndex++ ;
                
                context.project.log("Index: " + context.currIndex + " Argument Part: '" + argPart + "'", Project.MSG_DEBUG) ;
                return Key ; 
            }//EOM 
            
            @Override
            final boolean shouldParse(final String argPart, final Context context) { 
                return context.argIndentifier.equals(argPart) ; 
            }//EOM 
            
        };//EO ArgItentifier 
        
        abstract ArgPartParserType parseArgument(final String[] arrArgParts, final Context context)  ;
        abstract boolean shouldParse(final String argPart, final Context context) ;
            
    }//EO inner enum ArgPartParserType
    
    private final String buildDelimitersString() { 
        final StringBuilder delimiterStringbuilder = new StringBuilder(),  errorMsgBuidler = new StringBuilder() ; 
        
        this.appendDelimiter(this.argIndentifier, "Argument Identifier", errorMsgBuidler, delimiterStringbuilder) ;
        this.appendDelimiter(this.interArgDelimiter, "Inter Argument Delimiter", errorMsgBuidler, delimiterStringbuilder) ; 
        this.appendDelimiter(this.keyValDelimiter, "Key Value Delimiter", errorMsgBuidler, delimiterStringbuilder) ;
        
        if(errorMsgBuidler.length() > 0) throw new BuildException("ArgParserTask is misconfigured:" + errorMsgBuidler.toString()) ;
        else return delimiterStringbuilder.toString() ; 
    }//EOM 
    
    private final StringBuilder appendDelimiter(final String delimiter, final String delimiterName, final StringBuilder errorMsgBuilder, 
            final StringBuilder delimiterStringBuilder) { 
        if(delimiter == null || delimiter.isEmpty()) errorMsgBuilder.append("\n- ").append(delimiterName).append(" Must be defined") ;
        else  { 
            if(delimiterStringBuilder.length() > 0) delimiterStringBuilder.append("|") ;
            delimiterStringBuilder.append(delimiter) ; 
        }//EO else if the delimiter was defined 
        return errorMsgBuilder ;
    }//EOM 
    
    private class Context extends HashMap<Object,Object> { 
        
        private int currIndex ;
        private String argIndentifier ; 
        private String interArgDelimiter ; 
        private String keyValDelimiter ;
        private Project project ; 
        private ArgPartParserType previousParser ;
        private String currentKey ; 
        private String currentValue ; 
        
        Context(final int currIndex, final String argIndentifier, final String interArgDelimiter, final String keyValDelimiter) {
            this.argIndentifier = argIndentifier ; 
            this.interArgDelimiter = interArgDelimiter ;
            this.keyValDelimiter = keyValDelimiter ; 
            this.currIndex = currIndex;
        }//EOM 
        
    }//EO inner class Context ;
    
    //---------------------------------------------------------------------------------------------------------------
    // DEBUG SECTION   DEBUG SECTION   DEBUG SECTION   DEBUG SECTION   DEBUG SECTION   DEBUG SECTION   DEBUG SECTION     
   //----------------------------------------------------------------------------------------------------------------
    
    
    public static void main(String[] args) throws Throwable {
        
        String cmdArgs = "-Dserver.database-url=jdbc:mysql://localhost:3306/hqdb -Dserver.database-driver=com.mysql.jdbc.Driver -Dserver.database=MySQL -Dserver.database-user=hqadmin -Dserver.database-password=hqadmin -Dserver.admin.username=hqadmin -Dserver.admin.password=hqadmin " + 
                "-Dexport -Dstaging.dir=/work/temp/hq-migration/export-workspace/staging-area -Dhqserver.install.path=/work/temp/hq-migration/servers/server-4.6.5.2.BUILD-SNAPSHOT-EE " + 
                "-Dimport -Dexport.archive.path=/work/temp/hq-migration/export-workspace/hq-migration-4.6.5.1.zip -Dhqserver.install.path=/work/temp/hq-migration/servers/server-5.0-EE " + 
                "-Dpath=\"C:\\Program Files\\dir1\\dir2\\file=.txt\"" ; 
        
        //cmdArgs  = "-help -file file.txt -test1 -test2 file2 d -test3 " ;  
        
        final Project project = new Project() { 
           private final Map<String,String> properties = new HashMap<String,String>() ; 
           
           @Override
            public void setProperty(String name, String value) {
                this.properties.put(name, value) ;
            }//EOM 
           
           @Override
            public final String toString() {
                return this.properties.toString().replace(",", "\n") ;
            }//EOM 
        }; 
        
        final ArgsParser parser = new ArgsParser();
        parser.setProject(project) ; 

        parser.setArgDel(" ") ;
        parser.setArgIdentifier("-D") ; 
        parser.setKeyValDel("=") ; 
        
        
        parser.parseArguments(cmdArgs) ;
        System.out.println(project.toString()) ;
    }//EOM 
    
}//EOC 

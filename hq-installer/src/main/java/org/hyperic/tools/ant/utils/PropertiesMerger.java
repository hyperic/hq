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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Merged between two properties files with precedence and filtering whilst keeping the original's comments.
 * @author guy
 *
 */
public class PropertiesMerger extends Properties{

    private static Method saveConvertMethod ;
    
    private final String outputFilePath ; 
    private Map<String,String[]> delta ;    
    private boolean isLoaded ; 
    private String baseFileContent ; 
    private final PropertiesMergerFilter filter ; 
    private Collection<String> pruneProperties ; 
    
    static { 
        try{
            saveConvertMethod = Properties.class.getDeclaredMethod("saveConvert", String.class, boolean.class,  boolean.class) ;
            saveConvertMethod.setAccessible(true) ;
        }catch(Throwable t) { 
            throw (t instanceof RuntimeException ? (RuntimeException) t: new RuntimeException(t)) ; 
        }//EO catch block 
    }//EO static block 
    
    public PropertiesMerger(final String outputFilePath, final PropertiesMergerFilter filter) { 
        this.outputFilePath = outputFilePath ; 
        this.filter = filter ; 
        this.delta = new HashMap<String, String[]>() ; 
    }//EOM 
    
    @Override
    public Object put(Object key, Object value) {
        Object oPrevious = super.put(key, value);
        if(this.isLoaded && !value.equals(oPrevious)) this.delta.put(key.toString(), new String[] { value.toString(), (String) oPrevious}) ; 
        return oPrevious ; 
    }//EOM 
    
    @Override
    public final Object remove(Object key) {
        final Object oExisting = super.remove(key);
        this.delta.remove(key) ; 
        return oExisting ; 
    }//EOM 
    
    public final void setBaseFile(final String path) throws IOException{ 
        this.load(path) ; 
    }//EOM
    
    public final void setPrunePropertiesList(final Collection<String> pruneProperties)  {
        this.pruneProperties = pruneProperties; 
    }//EOM
    
    public final void setOverridePropertyValues(final Map<String,String> mapOverrideValues) { 
        if(mapOverrideValues == null) return ; 
        
        String key = null, value = null, previousValue = null ;  
        for(Map.Entry<String,String> entry : mapOverrideValues.entrySet()) {
            key = (String) entry.getKey(); 
            value = (String) entry.getValue() ;
            
            previousValue = (String) this.getProperty(key) ; 
            if(this.isLoaded && !value.equals(previousValue)) this.delta.put(key.toString(), new String[] { value, previousValue}) ;
        }//EO while there are more override property values 
    }//EOM 
    
    public final void setOverrideFile(final String path) throws IOException{
        
        final Properties overrideProperties = new Properties() ; 
        overrideProperties.load(new FileInputStream(new File(path))) ; 
        
        String key = null, value = null ; 
        for(Map.Entry<Object,Object> entry : overrideProperties.entrySet()) { 
            key = (String) entry.getKey(); 
            value = (String) entry.getValue() ; 
            if(filter != null && filter.apply(key, value, this)) {
                System.out.println("Overriding property: " + key + " with value: " + value);
                this.put(key, value) ; 
            }//EO if should override 
        }//EO while there are more properties to iterate over 
        
    }//EOM 
    
    public void load(final String path) throws IOException {
        if(this.isLoaded) throw new IllegalStateException("Load cannot be invoked more than once, use setOverideFile instead") ; 
        
        InputStream fis = null, fis1 = null ; 
        try{
            final File file = new File(path) ; 
            if(!file.exists()) throw new IOException(file + " does not exist or is not readable") ;
            //else 
            fis = new FileInputStream(file) ;  
            //first read the content into a string 
            final byte[] arrFileContent = new byte[(int)fis.available()] ;
            fis.read(arrFileContent) ; 
            
            this.baseFileContent = new String(arrFileContent) ;
            
            fis1 = new ByteArrayInputStream(arrFileContent) ; 
            this.load(fis1);
            
        }catch(Throwable t) { 
            throw (t instanceof IOException ? (IOException)t : new IOException(t)) ; 
        }finally{ 
            if(fis != null) fis.close() ;
            if(fis1 != null) fis1.close() ;
        }//EO catch block 
    }//EOM 
    
    @Override
    public final void load(final InputStream inStream) throws IOException {
        try{ 
            super.load(inStream);
        }finally{ 
            this.isLoaded = true ; 
        }//EO catch block 
    }//EOM
    
    public final void merge() throws IOException { 
        if(this.delta.isEmpty()) return ; 
        
        FileOutputStream fos = null ;
        String key = null, value = null, oldValue = null, escapedOldValue = null;  
        Pattern pattern = null ; 
        Matcher matcher = null ;
        String[] arrValues = null; 
        try{  
            final String REGEX_ESCAPE_CHARS_REGEX = "([?()+\\\\])" ; 
            final String REGEX_ESCAPE_CHARS_REPLACEMENT = "\\\\$1" ; 
            final String WHITESPACSES_REGEX = "(?<!^)\\s+(?!$)" ; 
            final String WHITESPACES_REPLACEMENT = "(\\\\s*.*\\\\s*)" ;
            
            for(Map.Entry<String,String[]> entry : this.delta.entrySet()) { 
                
                key = (String) saveConvertMethod.invoke(this, entry.getKey(), true/*escapeSpace*/, true /*escUnicode*/);
                arrValues = entry.getValue() ; 
                value = (String) saveConvertMethod.invoke(this, arrValues[0], false/*escapeSpace*/, true /*escUnicode*/);

                //if the arrValues[1] == null then this is a new property 
                if(arrValues[1] == null) { 
                    this.baseFileContent = this.baseFileContent + "\n" + key +  "=" + value ;   
                }else { 
                    //first escape all characters with special meaning then replace all whitespaces which do not appear  
                    //at the beginning or end of the input with multi line whilespace regex
                    //additionally add a replacement alternative if the tuple whereby the properties file reserved chars are escaped 
                    //(viable scenario with auto generated property files)
                    //eventually the replacement regex will follow the format of: <key= value|escaped value>
                    escapedOldValue = (String) saveConvertMethod.invoke(this, arrValues[1], false/*escapeSpace*/, true /*escUnicode*/);
                    
                    oldValue = arrValues[1].replaceAll(REGEX_ESCAPE_CHARS_REGEX, REGEX_ESCAPE_CHARS_REPLACEMENT).replaceAll(WHITESPACSES_REGEX, WHITESPACES_REPLACEMENT) ;
                    
                    escapedOldValue = escapedOldValue.replaceAll(REGEX_ESCAPE_CHARS_REGEX, REGEX_ESCAPE_CHARS_REPLACEMENT).replaceAll(WHITESPACSES_REGEX, WHITESPACES_REPLACEMENT) ;
                    
                    if(!oldValue.equals(escapedOldValue)) { 
                        oldValue = "(?:" + oldValue + "|" +  escapedOldValue + ")" ; 
                    }//EO if escape value has different representation 
                    
                    pattern = Pattern.compile(key+"\\s*=(\\s*.*\\s*)"+ oldValue , Pattern.MULTILINE) ;
                    //pattern = Pattern.compile(key+"\\s*=.*\n", Pattern.MULTILINE) ;
                    matcher = pattern.matcher(this.baseFileContent) ; 
                    this.baseFileContent = matcher.replaceAll(key + "=" + value) ;
                }//EO else if existing property
                
                System.out.println("Adding/Replacing " + key + "-->" + arrValues[1] + " with: " + value)  ;
                
            }//EO while there are more entries ;
            
            this.pruneProperties() ; 
            
            final File outputFile = new File(this.outputFilePath) ; 
            outputFile.delete() ; 
            fos = new FileOutputStream(outputFile)  ; 
            fos.write(this.baseFileContent.getBytes()) ; 
            
        }catch(Throwable t) { 
            throw (t instanceof IOException ? (IOException)t : new IOException(t)) ; 
        }finally{ 
             if(fos != null) { 
                 fos.flush() ;
                 fos.close() ; 
             }//EO if bw was initialized 
        }//EO catch block 
    }//EOM 
    
    private final void pruneProperties() throws Throwable { 
        if(this.pruneProperties == null) return ; 
        
        String value = null ; 
        Matcher matcher = null ; 
        Pattern pattern = null  ;
        for(String property : this.pruneProperties) { 
            if(this.containsKey(property)) { 
                value = (String) this.get(property) ;
                
                value = (value == null ? "" : value.replaceAll("([():+])", "\\\\$1").replaceAll("\\s+", "(\\\\s*.*\\\\s*)")) ;  
                pattern = Pattern.compile(property+"\\s*=(\\s*.*\\s*)"+ value, Pattern.MULTILINE) ;
                //pattern = Pattern.compile(key+"\\s*=.*\n", Pattern.MULTILINE) ;
                matcher = pattern.matcher(this.baseFileContent) ; 
                this.baseFileContent = matcher.replaceAll("") ;
            }//EO if property exists 
        }//EO while there are more properties 
    }//EOM 
    
    public static interface PropertiesMergerFilter { 
        
       boolean apply(String propertyName, String propertyValue, final Properties properties) ; 
        
    }//EO inner interface PropertiesMergerFilter
    
    public static void main(String[] args) throws Throwable {
        //String regex = "server.database-password=ENC(ZsbvmndZgX3mclWtDCjX7g==) 2ndline" ;
        final String input = "#server.database-url=jdbc\\:mysql\\://10.131.9.171\\:3306/HQ\n" +
"#server.encryption-key=password\n" +
"#server.database-user=hqadmin\n" +
"#server.database-password=ENC(ZH4ttiFMPh4tc4kR+F/wFA\\=\\=)\n" +
"server.database-url=jdbc:postgresql://127.0.0.1:9432/hqdb?protocolVersion=2\n" + 
"server.encryption-key=mickymouse\n" +
"server.database-user=firstcit\n" +
"server.database-password=ENC(JN0nmlpNxvjZYotfEgew9MOXG2CXm8wz)\n" +
"\n" +
"CAM_SERVER_VERSION=4.6.0.1\n" +
"CAM_SCHEMA_VERSION=3.210" ; 
        
        final String regex = "server.database-url\\s*=(\\s*.*\\s*)(?:jdbc:postgresql://127.0.0.1:9432/hqdb?protocolVersion=2|jdbc\\:postgresql\\://127.0.0.1\\:9432/hqdb\\?protocolVersion\\=2)" ;
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE) ; 
        final Matcher matcher = pattern.matcher(input) ; 
        System.out.println(matcher.replaceAll("server.database-url=this.is.the.url")) ;
        
       
      /*  String regex1 = "server.database-url=jdbc\\:mysql\\://10.131.9.171\\:3306/FC" ;
        String regex2 = "server.database-url=jdbc:mysql://10.131.9.171:3306/FC" ;
        regex = regex.replaceAll("([()+\\\\])", "\\\\$1").replaceAll("(?<!^)\\s+(?!$)", "(\\\\s*.*\\\\s*)") ;
        regex = regex + "|" + regex1.replaceAll("([()+\\\\])", "\\\\$1").replaceAll("(?<!^)\\s+(?!$)", "(\\\\s*.*\\\\s*)") ;
        System.out.println(regex);
        System.out.println();
         final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE) ; 
        final Matcher matcher = pattern.matcher(input) ; 
        System.out.println(matcher.replaceAll("server.database-url=this.is.the.url")) ;  */
       
       
     
        
    }//EOM 
}//EOC 

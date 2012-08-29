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
import java.util.Enumeration;
import java.util.HashMap;
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
        String key = null, value = null, oldValue = null ; 
        Pattern pattern = null ; 
        Matcher matcher = null ;
        String[] arrValues = null; 
        try{ 
            
            for(Map.Entry<String,String[]> entry : this.delta.entrySet()) { 
                
                key = (String) saveConvertMethod.invoke(this, entry.getKey(), true/*escapeSpace*/, true /*escUnicode*/);
                arrValues = entry.getValue() ; 
                value = (String) saveConvertMethod.invoke(this, arrValues[0], false/*escapeSpace*/, true /*escUnicode*/);

                //if the arrValues[1] == null then this is a new property 
                if(arrValues[1] == null) { 
                    this.baseFileContent = this.baseFileContent + "\n" + key +  "=" + value ;   
                }else { 
                    oldValue = arrValues[1].replaceAll("([():+])", "\\\\$1").replaceAll("\\s+", "(\\\\s*.*\\\\s*)") ; 
                    pattern = Pattern.compile(key+"\\s*=(\\s*.*\\s*)"+ oldValue , Pattern.MULTILINE) ;
                    //pattern = Pattern.compile(key+"\\s*=.*\n", Pattern.MULTILINE) ;
                    matcher = pattern.matcher(this.baseFileContent) ; 
                    this.baseFileContent = matcher.replaceAll(key + "=" + value) ;
                }//EO else if existing property
                
                System.out.println("Adding/Replacing " + key + "-->" + arrValues[1] + " with: " + value)  ;
                
            }//EO while there are more entries ;
            
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
    
    public static interface PropertiesMergerFilter { 
        
       boolean apply(String propertyName, String propertyValue, final Properties properties) ; 
        
    }//EO inner interface PropertiesMergerFilter
    
    public static void main(String[] args) throws Throwable {
        //String regex = "server.database-password=ENC(ZsbvmndZgX3mclWtDCjX7g==) 2ndline" ;
        final String input = "\nserver.java.opts=-Djava.awt.headless=true -XX:MaxPermSize=192m -Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC\n" ; 
        //final String input = "\n" + "-XX:+HeapDumpOnOutOfMemoryError" + "\n" ;
        String regex = input ; 
        
        regex = regex.replaceAll("([():+])", "\\\\$1") ;
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE) ; 
        final Matcher matcher = pattern.matcher(input) ; 
        System.out.println(matcher.replaceAll("server.java.opts=new value")) ;  
        
    }//EOM 
}//EOC 

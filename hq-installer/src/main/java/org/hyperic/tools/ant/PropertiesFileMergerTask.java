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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesFileMergerTask extends Properties{
    
    private static Method saveConvertMethod ; 

    private String fileContent ;
    private Map<String,String[]> delta ;    
    private boolean isLoaded ; 
    
    static { 
        try{
            saveConvertMethod = Properties.class.getDeclaredMethod("saveConvert", String.class, boolean.class,  boolean.class) ;
            saveConvertMethod.setAccessible(true) ;
        }catch(Throwable t) { 
            throw (t instanceof RuntimeException ? (RuntimeException) t: new RuntimeException(t)) ; 
        }//EO catch block 
    }//EO static block 
    
    public PropertiesFileMergerTask() { 
        this.delta = new HashMap<String, String[]>() ; 
    }//EOM 
    
    @Override
    public synchronized Object put(Object key, Object value) {
        Object oPrevious = null ; 
        try{ 
            oPrevious = super.put(key, value);
            if(this.isLoaded && !value.equals(oPrevious)) this.delta.put(key.toString(), new String[] { value.toString(), (String) oPrevious}) ; 
            return oPrevious ; 
        }catch(Throwable t) { 
            t.printStackTrace() ; 
            throw new RuntimeException(t) ; 
        }//EO catch block 
    }//EOM 
    
    @Override
    public final synchronized Object remove(Object key) {
        final Object oExisting = super.remove(key);
        this.delta.remove(key) ; 
        return oExisting ; 
    }//EOM 
    
    public static final PropertiesFileMergerTask load(final File file) throws IOException {
        InputStream fis = null, fis1 = null ; 
        try{
            if(!file.exists()) throw new IOException(file + " does not exist or is not readable") ;
            //else 
            final PropertiesFileMergerTask properties = new PropertiesFileMergerTask() ; 
            
            fis = new FileInputStream(file) ;  
            //first read the content into a string 
            final byte[] arrFileContent = new byte[(int)fis.available()] ;
            fis.read(arrFileContent) ; 
            
            properties.fileContent = new String(arrFileContent) ;
            
            fis1 = new ByteArrayInputStream(arrFileContent) ; 
            properties.load(fis1);
            
          //  System.out.println(properties.fileContent);
            return properties ; 
        }catch(Throwable t) { 
            throw (t instanceof IOException ? (IOException)t : new IOException(t)) ; 
        }finally{ 
            if(fis != null) fis.close() ;
            if(fis1 != null) fis1.close() ;
        }//EO catch block 
    }//EOM
    
     @Override
    public synchronized void load(InputStream inStream) throws IOException {
        try{ 
            super.load(inStream);
        }finally{ 
            this.isLoaded = true ; 
        }//EO catch block 
        
    }//EOm 
    
    public final void store(final File outputFile, final String comments) throws IOException {
       
        if(this.delta.isEmpty()) return ; 
        
        FileOutputStream fos = null ;
        String key = null, value = null ;
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
                    this.fileContent = this.fileContent + "\n" + key +  "=" + value ;   
                }else { 
                
                    //pattern = Pattern.compile(key+"\\s*=(\\s*.*\\s*)"+ arrValues[1].replaceAll("\\s+", "(\\\\s*.*\\\\s*)") , Pattern.MULTILINE) ;
                    pattern = Pattern.compile(key+"\\s*=.*\n", Pattern.MULTILINE) ;
                    matcher = pattern.matcher(this.fileContent) ; 
                    this.fileContent = matcher.replaceAll(key + "=" + value) ;
                }//EO else if existing property
                
                System.out.println("Adding/Replacing " + key + "-->" + arrValues[1] + " with: " + value)  ;
                
            }//EO while there are more entries ;
            
            fos = new FileOutputStream(outputFile)  ; 
            fos.write(this.fileContent.getBytes()) ; 
            
        }catch(Throwable t) { 
            throw (t instanceof IOException ? (IOException)t : new IOException(t)) ; 
        }finally{ 
             if(fos != null) { 
                 fos.flush() ;
                 fos.close() ; 
             }//EO if bw was initialized 
        }//EO catch block 
    }//EOM 
    
    public static void main(String[] args) throws Throwable {
        
      ///FOR DEBUG 
        String s = " 1 2 4 sdf \\\\\nsdfsd" ; 
        final Pattern pattern = Pattern.compile("test.prop2\\s*=.*(?:\\\\?\\s*)(\n)" , Pattern.MULTILINE) ;
        final Matcher matcher = pattern.matcher("test.prop2="+s) ; 
        System.out.println(matcher.replaceAll("test.prop2=" + "newvalue$1")) ;
        ///FOR DEBUG                 
       if(true) return ; 
        
        final String path = "/tmp/confs/hq-server-46.conf" ; 
        final File file = new File(path) ; 
        final PropertiesFileMergerTask properties = PropertiesFileMergerTask.load(file) ;  
        
/*        final Pattern pattern = Pattern.compile("test.prop1\\s*=this(\\s*.*\\s*)is(\\s*.*\\s*)the(\\s*.*\\s*)value" , 
                Pattern.MULTILINE) ;
        final Matcher matcher = pattern.matcher(properties.fileContent) ; 
        System.out.println( matcher.replaceAll("test.prop1=new value") ) ;
        
        System.out.println("\n\n--> " + properties.get("test.prop1")) ;*/
        
        final String overridingConfPath = "/tmp/confs/hq-server-5.conf" ;
        //final Properties overrdingProperties = new Properties() ;
        final FileInputStream fis = new FileInputStream(overridingConfPath) ; 
        properties.load(fis) ; 
       
        fis.close() ; 
        
        ///properties.putAll(overrdingProperties) ;
        final String outputPath = "/tmp/confs/output-hq-server.conf" ;
        final File outputFile = new File(outputPath) ;
        final String comments = "" ; 
        properties.store(outputFile, comments) ; 
    }//EOM 
    
    
}//EOC 

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
package org.hyperic.tools.dbmigrate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class Utils{
    
  public static final String STAGING_DIR = "staging.dir";
  public static final String SERVER_INSTALL_DIR = "hqserver.install.path";
  public static final int DEFAULT_PAGE_SIZE = 1000;
  public static final String FILE_EXT = ".out";
  public static final Object EOF_PLACEHOLDER = FileContentType.EOF;
  public static final String SOURCE_DB_URL_KEY = "source.database.url";
  public static final String SOURCE_DB_USERNAME_KEY = "source.database.username";
  public static final String SOURCE_DB_PASSWORD_KEY = "source.database.password";
  public static final String SOURCE_DB_TYPE_KEY = "source.database.type";
  public static final String SOURCE_DB_DRIVER_KEY = "source.database.driver";
  public static final String TARGET_DB_URL_KEY = "target.database.url";
  public static final String TARGET_DB_USERNAME_KEY = "target.database.username";
  public static final String TARGET_DB_PASSWORD_KEY = "target.database.password";
  public static final String TARGET_DB_TYPE_KEY = "target.database.type";
  public static final String TARGET_DB_DRIVER_KEY = "target.database.driver";
  public static final String DB_SCHEMA_VERSION_KEY = "CAM_SCHEMA_VERSION";
  public static final String HQ_BUILD_VERSION_KEY = "CAM_SERVER_VERSION";
  public static final String EXPORT_ARTIFACTS_OUTPUT_RELATIVE_DIR = "/source-artifacts";
  private static final Map<String, String> dbDrivers = new HashMap<String,String>();
  public static final int NOOP_INSTRUCTION_FLAG = 0;
  public static final int COMMIT_INSTRUCTION_FLAG = 4;
  public static final int ROLLBACK_INSTRUCTION_FLAG = 8;

  static{
      dbDrivers.put("PostgreSQL", "org.postgresql.Driver");
      dbDrivers.put("MySQL", "com.mysql.jdbc.Driver");
      dbDrivers.put("Oracle9i", "oracle.jdbc.driver.OracleDriver");
  }//EO static block
  
  public static final void close(final Object...closeables){
    close(NOOP_INSTRUCTION_FLAG, closeables);
  }//EOM 

  public static final void close(final int specialInstructionsMask, final Object...closeables) {
    for (Object closeable : closeables) {
      if (closeables == null)
        continue;
      try {
        if ((closeable instanceof Connection)) {
          Connection conn = (Connection)closeable;
          try{
            if ((specialInstructionsMask & 0x4) == 4) conn.commit();
            else if ((specialInstructionsMask & 0x8) == 8) conn.rollback(); 
          }catch (Throwable t2) {
            printStackTrace(t2);
          }//EO inner catch block 

          ((Connection)closeable).close();
        } else if ((closeable instanceof PreparedStatement)) { 
            ((PreparedStatement)closeable).close();
        } else if ((closeable instanceof ResultSet)) { 
            ((ResultSet)closeable).close();
        } else if ((closeable instanceof OutputStream)) {
          final OutputStream os = (OutputStream)closeable;
          os.flush();
          os.close();
        } else if ((closeable instanceof InputStream)) {
          final InputStream is = (InputStream)closeable;
          is.close();
        }//EO else if inputstream
      } catch (Throwable t) {
        printStackTrace(t);
      }//EO catch block 
    }//EO while there are more closeables 
  }//EOM 

  public static final void executeUpdate(final Connection conn, final String sqlStatmeent) throws Throwable {
    PreparedStatement ps = null;
    try {
      ps = conn.prepareStatement(sqlStatmeent);
      ps.executeUpdate();
    } finally {
      close(new Object[] { ps });
    }//EO catch block 
  }//EOM 

  public static final void printStackTrace(final Throwable exception) {
      printStackTrace(exception, null) ; 
  }//EOM 
  
  public static final void printStackTrace(final Throwable exception, final String prefixMessage) {
    if(prefixMessage != null) System.err.println(prefixMessage) ; 
    if ((exception instanceof SQLException)) {
      SQLException sqle = (SQLException)exception;
      Throwable t = null;
      do {
        t = sqle;
        t.printStackTrace();
      }while (((sqle = sqle.getNextException()) != null) && (sqle != t)); } else {
      exception.printStackTrace();
    }//EO if instanceof SQLException 
  }//EOM 

  public static final Connection getConnection(final String url, final String username, final String password, final String driverClass) throws Throwable {
    Class.forName(driverClass);
    return DriverManager.getConnection(url, username, password);
  }//EOM 

  public static final Connection getSourceConnection(final Hashtable env) throws Throwable {
    return getConnection(SOURCE_DB_URL_KEY, SOURCE_DB_USERNAME_KEY, SOURCE_DB_PASSWORD_KEY, SOURCE_DB_TYPE_KEY, SOURCE_DB_DRIVER_KEY, env);
  }//EOM 

  public static final Connection getDestinationConnection(Hashtable env) throws Throwable {
    return getConnection(TARGET_DB_URL_KEY, TARGET_DB_USERNAME_KEY, TARGET_DB_PASSWORD_KEY, TARGET_DB_TYPE_KEY, TARGET_DB_DRIVER_KEY, env);
  }//EOM 

  public static final Connection getConnection(final String urlKey, final String usernameKey, final String passwordKey, 
          final String dbTypeKey, final String driverClassKey, final Hashtable env) throws Throwable {
    
      final String dbUrl = (String)env.get(urlKey);
      final String username = (String)env.get(usernameKey);
      final String password = (String)env.get(passwordKey);

      String driver = (String)env.get(driverClassKey);

      final String dbType = (String)env.get(dbTypeKey);
      driver = (String)dbDrivers.get(dbType);

    return getConnection(dbUrl, username, password, driver);
  }//EOM 

  public static final String getEnvProperty(final String key, final String defaultVal, final Hashtable env) {
    final String value = (String)env.get(key);
    return value == null ? defaultVal : value;
  }//EOM 

  public static final void deleteDirectory(final File dir) {
    if (dir.exists()) {
      final File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) { 
              deleteDirectory(file); 
          }else {
            file.delete();
          }//EO if a file  
        }//EO while there are more files 
      }//EO if there were files 
      dir.delete();
    }//EO if the directory exists  
  }//EOM 
  
  public static final String getFileContent(final File file) throws IOException{ 
      FileInputStream fis = null ;
      String content = null ; 
      try{ 
          fis = new FileInputStream(file);
          
          final byte arrBytes[] = new byte[fis.available()];
          fis.read(arrBytes);
          content = new String(arrBytes);
      }finally{ 
          close(fis) ; 
      }//EO catch block 
      
      return content ; 
  }//EOM 
  
  public static enum FileContentType { EOF ; }//EOM 

  //*****************************************************************************************************
  //DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG  
  //*****************************************************************************************************
  
  public static final Connection getMySqlConnection() throws Throwable {
    Class.forName("com.mysql.jdbc.Driver");
    return DriverManager.getConnection("jdbc:mysql://localhost:3306/guytest1", "hqadmin", "hqadmin");
  }//EOM 
  
  public static final Connection getPostgresConnection() throws Throwable {
    Class.forName("org.postgresql.Driver");
    //return DriverManager.getConnection("jdbc:postgresql://10.23.200.18:5432/guytest1?protocolVersion=2", "hqadmin", "hqadmin");
//    return DriverManager.getConnection("jdbc:postgresql://localhost:5432/hqdb?protocolVersion=2", "hqadmin", "hqadmin");
    //return DriverManager.getConnection("jdbc:postgresql://10.23.20.159:5432/HQ?protocolVersion=2", "hqadmin", "hqadmin");
    return DriverManager.getConnection("jdbc:postgresql://10.23.24.101:5432/HQ?protocolVersion=2", "hqadmin", "hqadmin");
  }//EOM 
  
  public static final Connection getOracleCOnnection() throws Throwable { 
      Class.forName("oracle.jdbc.driver.OracleDriver") ;
      return DriverManager.getConnection("jdbc:oracle:thin:@10.148.199.71:1521:nlayers", "hqadmin", "hqadmin") ; 
  }//EOM 
  
  public static final Connection getLocalPostgresConnection() throws Throwable {
      Class.forName("org.postgresql.Driver");
      return DriverManager.getConnection("jdbc:postgresql://localhost:5432/hqdb?protocolVersion=2", "hqadmin", "hqadmin");
    }//EOM 

  public static void main(String[] args) throws Throwable { 
    try{
        
      final File file = new File("/work/temp/dbmigration/staging-confs/testbed.conf");
      final FileOutputStream fos = new FileOutputStream(file, true);
      fos.write("#Schema build version hq.db.schema.vesion=3.211".getBytes());
      fos.flush();
      fos.close();
    } catch (Throwable t) {
      t.printStackTrace();
    }//EO catch block 
  }//EOM 
   
}//EOM 

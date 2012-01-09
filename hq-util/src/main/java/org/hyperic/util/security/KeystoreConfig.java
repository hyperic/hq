/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.util.security;

public class KeystoreConfig {
    private String alias;
    private String filePath;
    private String filePassword;
    private boolean hqDefault;
    private String keyCN="";//not required, just for keystore generation
    private char[] m_arrFilePassword ; 

    public KeystoreConfig(){   
    }
    
    public KeystoreConfig(String alias, String filePath, String filePassword, boolean hqDefault){
        this.alias = alias;
        this.filePath = filePath;
        this.filePassword = filePassword;
        this.hqDefault = hqDefault;
    }

    /**
     * @return the keyCN
     */
    public String getKeyCN() {
        return keyCN;
    }

    /**
     * @param keyCN the keyCN to set
     */
    public void setKeyCN(String keyCN) {
        this.keyCN = keyCN;
    }

    /**
     * @return the hqDefault
     */
    public boolean isHqDefault() {
        return hqDefault;
    }
    /**
     * @param hqDefault the hqDefault to set
     */
    public void setHqDefault(boolean hqDefault) {
        this.hqDefault = hqDefault;
    }
    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }
    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }
    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }
    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    /**
     * @return the filePassword
     */
    public String getFilePassword() {
        return filePassword;
    }
    /**
     * @param filePassword the filePassword to set
     */
    public void setFilePassword(String filePassword) {
        this.filePassword = filePassword;
        if(filePassword != null) { 
            this.m_arrFilePassword = filePassword.toCharArray() ; 
        }//EO if password was provided 
    }//EOM 
    
    public final char[] getFilePasswordCharArray() { 
        return this.m_arrFilePassword ;
    }//EOM 
    
}

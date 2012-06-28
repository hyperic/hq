/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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
package org.hyperic.util.security;

import java.lang.reflect.Field;

import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;
import org.jasypt.encryption.pbe.config.SimplePBEConfig;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author guy
 * Jasypt Encryptor Configurations container. 
 * The class does not not store null password thus allowing for failovers 
 */
public class StrictEnvironmentPBEConfig extends EnvironmentPBEConfig implements InitializingBean {

    @Override
    public final void setPassword(final String password) {
        if(password == null) return ;  
        super.setPassword(password);
    }//EOM 
    
    @Override
    public final void setPasswordSysPropertyName(final String passwordSysPropertyName) {
        if(passwordSysPropertyName == null)  return ;  
        
        final String password = System.getProperty(passwordSysPropertyName) ;  
        this.setPassword(password) ; 
    }//EOM
    
    public void afterPropertiesSet() throws Exception {
        final Field field = SimplePBEConfig.class.getDeclaredField("passwordCleaned") ; 
        field.setAccessible(true) ; 
        field.set(this, false) ;
    }//EOM 
    
}//EOC 

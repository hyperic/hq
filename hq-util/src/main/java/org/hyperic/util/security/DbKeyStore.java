/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2011], VMware, Inc.
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
 */

package org.hyperic.util.security;

import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DbKeyStore extends KeyStore {
    private static Log log = LogFactory.getLog(DbKeyStore.class);

    protected DbKeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
        super(keyStoreSpi, provider, type);
    }

    /**
     * if {@link org.hyperic.hq.context.Bootstrap} exists it will return a DbKeyStore, or else it
     * returns the regular
     * {@link KeyStore}
     */
    public static KeyStore getInstance(String type, AtomicBoolean isDb) 
    throws KeyStoreException {
        DbKeystoreManager dbKeystoreManager = null;
        try {
            // scottmf: Not happy about this, when the agent is springified we can remove this.
            Class<?> bootstrapClass = Class.forName("org.hyperic.hq.context.Bootstrap");
            Method method = bootstrapClass.getMethod("getBean", String.class);
            dbKeystoreManager = (DbKeystoreManager) method.invoke(null, "dbKeystoreManager");
        } catch (Exception e) {
            log.debug("could not instantiate DbKeystoreManager class: " + e,e);
        }
        if (dbKeystoreManager == null) {
            isDb.set(false);
            return KeyStore.getInstance(type);
        }
        try {
            final DbKeyStoreSpi dbKeyStoreSpi = new DbKeyStoreSpi(dbKeystoreManager);
            final Provider[] providers = Security.getProviders("KeyStore." + type) ;
            isDb.set(true);
            return new DbKeyStore(dbKeyStoreSpi, providers[0], type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    

}

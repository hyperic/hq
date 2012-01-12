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
 *
 */

package org.hyperic.util.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DbKeyStoreSpi extends KeyStoreSpi {
    private final Log log = LogFactory.getLog(DbKeyStoreSpi.class);
    public static final String PRIVATE_KEY_ENTRY = "PrivateKeyEntry";
    public static final String TRUSTED_CERT_ENTRY = "trustedCertEntry";
    public static final String SECRET_KEY_ENTRY = "SecretKeyEntry";
    
    private final DbKeystoreManager dbKeystoreManager;
    private final Map<String, Object[]> engineAliases = new HashMap<String, Object[]>();

    public DbKeyStoreSpi(DbKeystoreManager dbKeystoreManager) {
        this.dbKeystoreManager = dbKeystoreManager;
    }
    
    @Override
    public Key engineGetKey(String alias, char[] password)
    throws NoSuchAlgorithmException, UnrecoverableKeyException {
        Object[] objs = engineAliases.get(alias);
        if (objs == null) {
            log.warn("alias=" + alias + " has no associated certificate");
            return null;
        }
        Certificate cert = (Certificate) objs[1];
        return cert.getPublicKey();
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
        Object[] objs = engineAliases.get(alias);
        return (Certificate[]) objs[2];
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
        Object[] objs = engineAliases.get(alias);
        return (Certificate) objs[1];
    }

    @Override
    public Date engineGetCreationDate(String alias) {
        throw new UnsupportedOperationException("engineGetCreationDate() is not supported");
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
    throws KeyStoreException {
        throw new UnsupportedOperationException("engineSetKeyEntry() is not supported");
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
    throws KeyStoreException {
        throw new UnsupportedOperationException("engineSetKeyEntry() is not supported");
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert)
    throws KeyStoreException {
        dbKeystoreManager.create(alias, TRUSTED_CERT_ENTRY, cert, null);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException {
        throw new UnsupportedOperationException("engineDeleteEntry() is not supported");
    }

    @Override
    public Enumeration<String> engineAliases() {
        return new Vector<String>(engineAliases.keySet()).elements();
    }

    @Override
    public boolean engineContainsAlias(String alias) {
        return engineAliases.containsKey(alias);
    }

    @Override
    public int engineSize() {
        return engineAliases.size();
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
        Object[] objs = engineAliases.get(alias);
        if (objs == null) {
            return false;
        }
        String type = (String) objs[0];
        return (type.equals(PRIVATE_KEY_ENTRY) || type.equals(SECRET_KEY_ENTRY));
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
        Object[] objs = engineAliases.get(alias);
        if (objs == null) {
            return false;
        }
        String type = (String) objs[0];
        return type.equals(TRUSTED_CERT_ENTRY);
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
        throw new UnsupportedOperationException("engineGetCertificateAlias() is not supported");
    }

    @Override
    public void engineStore(OutputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException("engineStore() is not supported");
    }

    @Override
    public void engineLoad(InputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException {
        final Collection<? extends KeystoreEntry> entries = dbKeystoreManager.getKeystore();
        final boolean debug = log.isDebugEnabled();
        for (final KeystoreEntry entry : entries) {
            final String alias = entry.getAlias();
            final String type = entry.getType();
            final Certificate cert = entry.getCertificate();
            final Certificate[] chain = entry.getCertificateChain();
            Object[] objs = engineAliases.get(alias);
            if (objs == null) {
                objs = new Object[3];
                objs[0] = type;
                objs[1] = cert;
                objs[2] = chain;
                engineAliases.put(alias, objs);
            }
            if (debug) log.debug("adding alias=" + alias + ",type=" + type);
        }
    }

}

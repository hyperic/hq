/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMWare, Inc.
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

package org.hyperic.hq.security.server.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.security.ServerKeystoreConfig;
import org.hyperic.util.encoding.Base64;
import org.hyperic.util.security.DbKeyStoreSpi;
import org.hyperic.util.security.DbKeystoreManager;
import org.hyperic.util.security.KeystoreEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service("dbKeystoreManager")
public class DbKeystoreManagerImpl implements DbKeystoreManager {
    private Log log = LogFactory.getLog(DbKeystoreManagerImpl.class);
    
    @Autowired
    private DbKeystoreDAO dbKeystoreDao;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ServerKeystoreConfig serverKeystoreConfig;

    @Transactional(readOnly = true)
    public Collection<KeystoreEntry> getKeystore() {
        final Collection<KeystoreEntryImpl> entries = dbKeystoreDao.findAll();
        return getKeystore(entries);
    }

    private Collection<KeystoreEntry> getKeystore(Collection<KeystoreEntryImpl> entries) {
        final Set<KeystoreEntry> rtn = new HashSet<KeystoreEntry>();
        for (final KeystoreEntryImpl k : entries) {
            try {
                k.setCertificate((Certificate) decode(k.getEncodedCertificate()));
                k.setCertificateChain((Certificate[]) decode(k.getEncodedCertificateChain()));
                rtn.add(k);
            } catch (IOException e) {
                log.warn("certificate with alias=" + k.getAlias() +
                         " could not be decoded properly: " + e);
                log.debug(e,e);
            }
        }
        return rtn;
    }
    
    /**
     * This simply adds the certs from hyperic.keystore, saves them to the DB and deletes the certs
     * from the file
     */
    @PostConstruct
    public void initDbKeystore() {
        new HibernateTemplate(sessionFactory, true).execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                @SuppressWarnings("unchecked")
                final Collection<KeystoreEntry> keys =
                    session.createCriteria(KeystoreEntryImpl.class).list();
                final Set<String> aliases = new HashSet<String>();
                for (KeystoreEntry entry : keys) {
                    aliases.add(entry.getAlias());
                }
                final Collection<KeystoreEntry> entries = new ArrayList<KeystoreEntry>();
                try {
                    final KeyStore fileKeystore = getFileKeystore();
                    final Enumeration<String> ksAliases = fileKeystore.aliases();
                    boolean store = false;
                    while (ksAliases.hasMoreElements()) {
                        final String alias = ksAliases.nextElement();
                        final boolean isKey = fileKeystore.isKeyEntry(alias);
                        final KeystoreEntryImpl entry = new KeystoreEntryImpl();
                        final String type =
                            (isKey) ? DbKeyStoreSpi.PRIVATE_KEY_ENTRY : DbKeyStoreSpi.TRUSTED_CERT_ENTRY;
                        entry.setType(type);
                        entry.setAlias(alias);
                        final Certificate cert = fileKeystore.getCertificate(alias);
                        entry.setEncodedCertificate(encode(alias, cert));
                        final Certificate[] chain = fileKeystore.getCertificateChain(alias);
                        entry.setEncodedCertificateChain(encode(alias, chain));
                        if (!aliases.contains(alias)) {
                            entries.add(entry);
                        }
                        if (!isKey) {
                            store = true;
                            fileKeystore.deleteEntry(alias);
                        }
                    }
                    if (store) {
                        store(fileKeystore);
                    }
                    for (final KeystoreEntry entry : entries) {
                        if (!aliases.contains(entry.getAlias())) {
                            session.save(entry);
                        }
                    }
                } catch (Exception e) {
                    log.error("ERROR loading from keystore file: " + e,e);
                }
                return null;
            }
        });
    }

    private void store(KeyStore ks) {
        // get user password and file input stream
        char[] password = serverKeystoreConfig.getFilePassword().toCharArray();
        FileOutputStream fos = null;
        try {
            File file = new File(serverKeystoreConfig.getFilePath());
            fos = new FileOutputStream(file);
            ks.store(fos, password);
        } catch (Exception e) {
            log.error("Error storing to keystore: " + e,e);
        } finally {
            close(fos);
        }
    }

    private KeyStore getFileKeystore()
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        // get user password and file input stream
        char[] password = serverKeystoreConfig.getFilePassword().toCharArray();
        java.io.FileInputStream fis = null;
        try {
            File file = new File(serverKeystoreConfig.getFilePath());
            if (!file.exists()) {
                ks.load(null, password);
            } else {
                fis = new FileInputStream(file);
                ks.load(fis, password);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return ks;
    }

    /**
     * reason for REQUIRES_NEW here is HHQ-4185, spring transaction manager doesn't upgrade
     * session when it comes across a rw transaction from a ro transactional context
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void create(String alias, String type, Certificate cert, Certificate[] chain) {
        final KeystoreEntryImpl keystoreEntry = new KeystoreEntryImpl();
        keystoreEntry.setAlias(alias);
        keystoreEntry.setType(type);
        final String encodedCert = encode(alias,cert);
        keystoreEntry.setEncodedCertificate(encodedCert);
        final String encodedChain = encode(alias,chain);
        keystoreEntry.setEncodedCertificateChain(encodedChain);
        dbKeystoreDao.save(keystoreEntry);
    }
    
    private String encode(String alias, Object obj) {
        if (obj == null) {
            return null;
        }
        ByteArrayOutputStream bos = null;
        ObjectOutputStream out = null;
        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            return new String(Base64.encode(bos.toByteArray()));
        } catch (IOException ex) {
            throw new SystemException(ex);
        } finally {
            close(bos);
            close(out);
        }
    }

    private void close(OutputStream out) {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            log.debug(e,e);
        }
    }
    
    private Object decode(String encoded) throws IOException {
        if (encoded == null) {
            return null;
        }
        byte[] serialized = Base64.decode(encoded);
        ByteArrayInputStream bis = null;
        ObjectInputStream in = null;
        try {
            bis = new ByteArrayInputStream(serialized);
            in = new ObjectInputStream(bis);
            Object rtn = in.readObject();
            return rtn;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (bis != null) bis.close();
            if (in != null) in.close();
        }
    }

}

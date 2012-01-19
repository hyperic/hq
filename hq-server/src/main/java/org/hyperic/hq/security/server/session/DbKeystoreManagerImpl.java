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


import static org.hyperic.util.security.KeyStoreUtils.keyStoreToByteArray;
import static org.hyperic.util.security.KeyStoreUtils.loadKeyStore;
import static org.hyperic.util.security.KeyStoreUtils.persistKeyStore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.security.ServerKeystoreConfig;
import org.hyperic.util.exec.ShutdownType;
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
    private final Log log = LogFactory.getLog(DbKeystoreManagerImpl.class);

    @Autowired
    private DbKeystoreDAO dbKeystoreDao;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ServerKeystoreConfig serverKeystoreConfig;

    @Transactional(readOnly = true)
    public Collection<? extends KeystoreEntry> getKeystore() {
        return dbKeystoreDao.findAll();
    }// EOM

    /**
     * This simply adds the certs from hyperic.keystore, saves them to the DB
     * and deletes the certs from the file
     * <p>
     * <b>Pre-condition:
     * </p>
     * </b> There can be only one private key stored in the database.
     */
    @PostConstruct
    public void initDbKeystore() {
        new HibernateTemplate(sessionFactory, true)
                .execute(new HibernateCallback<Object>() {
                    public Object doInHibernate(Session session)
                            throws HibernateException, SQLException {
                        @SuppressWarnings("unchecked")
                        final Collection<KeystoreEntry> keys = session
                                .createCriteria(KeystoreEntryImpl.class).list();
                        final Map<String, KeystoreEntry> mapKeys = new HashMap<String, KeystoreEntry>();
                        for (KeystoreEntry entry : keys) {
                            mapKeys.put(entry.getAlias(), entry);
                        }

                        final Collection<KeystoreEntry> entries = new ArrayList<KeystoreEntry>();

                        final KeystoreContext ctx = new KeystoreContext();
                        try {
                            ctx.fileKeystore = loadKeyStore(
                                    serverKeystoreConfig.getFilePath(), 
                                    serverKeystoreConfig.getFilePasswordCharArray()  
                                    ) ;

                            final Enumeration<String> ksAliases = ctx.fileKeystore
                                    .aliases();

                            while (ksAliases.hasMoreElements()) {
                                final String alias = ksAliases.nextElement();
                                final boolean isKey = ctx.fileKeystore
                                        .isKeyEntry(alias);
                                final KeystoreEntryImpl entry = new KeystoreEntryImpl();
                                final String type = (isKey) ? DbKeyStoreSpi.PRIVATE_KEY_ENTRY
                                        : DbKeyStoreSpi.TRUSTED_CERT_ENTRY;
                                entry.setType(type);
                                entry.setAlias(alias);
                                final Certificate cert = ctx.fileKeystore
                                        .getCertificate(alias);
                                entry.setCertificate(cert);

                                final Certificate[] chain = ctx.fileKeystore
                                        .getCertificateChain(alias);
                                entry.setCertificateChain(chain);

                                if (!mapKeys.containsKey(alias)) {
                                    entries.add(entry);
                                }

                                if (!isKey) {
                                    ctx.overrideKeystore = ctx.fileKeystore;
                                    ctx.fileKeystore.deleteEntry(alias);
                                } else {
                                    ctx.persistedPKEntry = mapKeys.get(alias);
                                    ctx.newPKEntry = entry;

                                }// EO else if private key entry
                            }
                            
                            // if private key entry, synchronize the
                            // file and persisted keystores
                            handlePK(ctx);

                            // if an override/updated keystore version was
                            // found, store it
                            if (ctx.overrideKeystore != null) {
                                persistKeyStore(ctx.overrideKeystore,  
                                        serverKeystoreConfig.getFilePath(),
                                        serverKeystoreConfig.getFilePasswordCharArray()) ; 
                            }

                            for (final KeystoreEntry entry : entries) {
                                if (!mapKeys.containsKey(entry.getAlias())) {
                                    session.save(entry);
                                }
                            }
                        } catch (Throwable t) {
                            throw new SystemException(t);
                        } finally {
                            // if the system restart flag was set to true, log
                            // and restart
                            if (ctx.shouldRestartJVM) {
                                log.error("********** SYSTEM IS SHUTTING DOWN DUE TO PRIVATE KEY(S) "
                                        + "SYNCHRONIZATION. AUTOMATIC RESTART WOULD ONLY OCCUR IF "
                                        + "WRAPPER WATCHDOG IS INSTALLED ***************************");

                                ShutdownType.Restart.shutdown();
                            }// EO if JVM restart was requested
                        }// EO catch block
                        return null;
                    }
                });
    }

    /**
     * Processes a {@link DbKeyStoreSpi#PRIVATE_KEY_ENTRY} record.
     * 
     * @param ctx
     *            DB kestore processing state containing the file keystore and
     *            persisted<BR>
     *            PrivateKey entries as well as the the file keystore instance.
     * 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableEntryException
     */
    private final void handlePK(final KeystoreContext ctx)
            throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableEntryException {

        // if the key is new, the store the fileKeystore as byte[]
        // in the file member of the newPkEntry so that other server would be
        // able to
        // share this server's private key as a cluster singleton
        if (ctx.persistedPKEntry == null) {

            // load the keystore into byte[] and store it
            final byte[] arrFileKeystoreContent = keyStoreToByteArray(ctx.fileKeystore, 
                    serverKeystoreConfig.getFilePasswordCharArray()) ; 

            ctx.newPKEntry.setFile(arrFileKeystoreContent);
        } else {

            // extract the public key certificate from the persistentPKEntry
            // instance
            // and compare to that of the fileKeyStore's one.
            // if the same (server already shares the private key), do nothing,
            // else, load the keystore file into a keystore instance and replace
            // the server's
            // file keystore (requires JVM bounce)
            final Certificate persistedCertificate = ctx.persistedPKEntry
                    .getCertificate();
            if (!persistedCertificate.equals(ctx.newPKEntry.getCertificate())) {

                final String sPKAlias = ctx.newPKEntry.getAlias();
                final String sMsg = "Private key entry with alias " + sPKAlias
                        + " differs from persisted version";

                log.warn(sMsg + ", overriding local file keystore (REQUIRES SYSTEM RESTART).");

                // load the byte[] into an in-memory keystore and store in the
                // context's overrideKeystore so that it would replace the
                // original one
                ctx.overrideKeystore = loadKeyStore(ctx.persistedPKEntry.getFile(),  
                        serverKeystoreConfig.getFilePasswordCharArray()
                        ) ; 

                // set the restartJvm flag to true to indicate
                // that the changes would not take hold without a restart
                ctx.shouldRestartJVM = true;

            }// EO if persisted certificate is different than the server's local
             // file keystore's one

        }// EO else if private key already exists in persistence store (not
         // first server to boot)

    }// EOM

    /**
     * Helper storing DB Keystore processing state
     * 
     * @author guy
     */
    private static final class KeystoreContext {
        boolean shouldRestartJVM;

        /**
         * Instance corresponds to the {@link DbKeyStoreSpi#PRIVATE_KEY_ENTRY}
         * record
         */
        KeystoreEntry persistedPKEntry; // corresponding to the DB record
        /**
         * Instance corresponds to the file keystore private key entry
         */
        KeystoreEntry newPKEntry;
        KeyStore fileKeystore;
        /**
         * A new keystore to physically replace the server's keystore file
         */
        KeyStore overrideKeystore;
    }// EOM

  

    /**
     * reason for REQUIRES_NEW here is HHQ-4185, spring transaction manager
     * doesn't upgrade session when it comes across a rw transaction from a ro
     * transactional context
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(String alias, String type, Certificate cert,
            Certificate[] chain) throws KeyStoreException {
        final KeystoreEntryImpl keystoreEntry = new KeystoreEntryImpl();
        keystoreEntry.setAlias(alias);
        keystoreEntry.setType(type);
        try {
            keystoreEntry.setCertificate(cert);
            keystoreEntry.setCertificateChain(chain);
        } catch (IOException ioe) {
            throw new KeyStoreException(ioe);
        }// EO catch block
        dbKeystoreDao.save(keystoreEntry);
    }

}

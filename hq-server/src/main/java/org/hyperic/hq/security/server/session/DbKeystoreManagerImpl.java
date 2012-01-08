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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    }//EOM 

    /**
     * This simply adds the certs from hyperic.keystore, saves them to the DB and deletes the certs
     * from the file
     * <p><b>Pre-condition:</p></b> There can be only one private key stored in the database.
     */
    @PostConstruct
    public void initDbKeystore() {
        new HibernateTemplate(sessionFactory, true).execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                @SuppressWarnings("unchecked")
                final Collection<KeystoreEntry> keys =
                    session.createCriteria(KeystoreEntryImpl.class).list();
                final Map<String, KeystoreEntry> mapKeys = new HashMap<String, KeystoreEntry>();
                for (KeystoreEntry entry : keys) {
                    mapKeys.put(entry.getAlias(), entry);
                } 
                
                final Collection<KeystoreEntry> entries = new ArrayList<KeystoreEntry>();
                
                final KeystoreContext ctx = new KeystoreContext() ; 
                try { 
                	ctx.m_fileKeystore = loadKeyStore(IKeystoreLoaderStrategy.KeyStoreAPI) ;
                    
                    final Enumeration<String> ksAliases = ctx.m_fileKeystore.aliases();
                    
                    while (ksAliases.hasMoreElements()) {
                        final String alias = ksAliases.nextElement();
                        final boolean isKey = ctx.m_fileKeystore.isKeyEntry(alias);
                        final KeystoreEntryImpl entry = new KeystoreEntryImpl();
                        final String type =
                            (isKey) ? DbKeyStoreSpi.PRIVATE_KEY_ENTRY : DbKeyStoreSpi.TRUSTED_CERT_ENTRY;
                        entry.setType(type);
                        entry.setAlias(alias);
                        final Certificate cert = ctx.m_fileKeystore.getCertificate(alias);
                        entry.setCertificate(cert) ; 
    
                        final Certificate[] chain = ctx.m_fileKeystore.getCertificateChain(alias);
                        entry.setCertificateChain(chain) ; 
                        
                        if (!mapKeys.containsKey(alias)) {
                            entries.add(entry);
                        }
                        
                        if (!isKey) {
                        	ctx.m_overrideKeystore = ctx.m_fileKeystore ; 
                        	ctx.m_fileKeystore.deleteEntry(alias);
                        }else { 
	                        ctx.m_persistedPKEntry = mapKeys.get(alias) ; 
	                        ctx.m_newPKEntry = entry ; 
	                        		
	                        //if private key entry, synchronize the file and persisted keystores
	                        handlePK(ctx) ;
                        				
                        }//EO else if private key entry 
                    }
                    
                    //if an override/updated keystore version was found, store it 
                    if (ctx.m_overrideKeystore != null) {
                    	persistKeystore(ctx.m_overrideKeystore); 
                    }
                    
                    for (final KeystoreEntry entry : entries) {
                        if (!mapKeys.containsKey(entry.getAlias())) {
                            session.save(entry);
                        }
                    }
                } catch (Exception e) {
                	throw new SystemException(e) ; 
                }finally { 
                	//if the system restart flag was set to true, log and restart 
                	if(ctx.m_bRestartJVM) { 
				         log.error("********** SYSTEM IS SHUTTING DOWN DUE TO PRIVATE KEY(S) " +
				         		"SYNCHRONIZATION. AUTOMATIC RESTART WOULD ONLY OCCUR IF " +
				         		"WRAPPER WATCHDOG IS INSTALLED ***************************") ; 
				         
				         ShutdownType.Restart.shutdown(); 
                	}//EO if JVM restart was requested 
                }//EO catch block 
                return null;
            }
        });
    }

    
    /**
     * Processes a {@link DbKeyStoreSpi#PRIVATE_KEY_ENTRY} record. 
     * 
     * @param ctx DB kestore processing state containing the file keystore and persisted<BR>
     *  PrivateKey entries as well as the the file keystore instance.
     * 
     * @throws KeyStoreException 
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableEntryException
     */
    private final void handlePK(final KeystoreContext ctx) throws 
    			KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, 
    			UnrecoverableEntryException{ 

    	//if the key is new, the store the fileKeystore as byte[] 
    	//in the file member of the newPkEntry so that other server would be able to 
    	//share this server's private key as a cluster singleton
    	if(ctx.m_persistedPKEntry == null) { 
    		
    		//load the keystore into byte[] and store it
    		final byte[] arrFileKeystoreContent = 
    				this.loadKeyStore(IKeystoreLoaderStrategy.FileSystemAPI) ;
    		
    		ctx.m_newPKEntry.setFile(arrFileKeystoreContent) ; 
    	}else { 
    		
    		//extract the public key certificate from the persistentPKEntry instance 
    		//and compare to that of the fileKeyStore's one. 
    		//if the same (server already shares the private key), do nothing, 
    		//else, load the keystore file into a keystore instance and replace the server's 
    		//file keystore (requires JVM bounce) 
    		final Certificate persistedCertificate = ctx.m_persistedPKEntry.getCertificate() ; 
    		if(!persistedCertificate.equals(ctx.m_newPKEntry.getCertificate())) { 
    			
    			final String sPKAlias = ctx.m_newPKEntry.getAlias() ; 
    			final String sMsg = "Private key entry with alias "+ sPKAlias +
    					" differs from persisted version" ; 
    			
    			log.warn(sMsg + ", overriding local file keystore (REQUIRES SYSTEM RESTART).") ;
    			 
    			//load the byte[] into an in-memory keystore and store in the 
    			//context's m_overrideKeystore so that it would replace the original one 
    			ctx.m_overrideKeystore = this.loadKeyStore(ctx.m_persistedPKEntry.getFile()) ;  
    			
    			//set the restartJvm flag to true to indicate 
    			//that the changes would not take hold without a restart
    			ctx.m_bRestartJVM = true ; 
    			
    		}//EO if persisted certificate is different than the server's local file keystore's one
    		
    	}//EO else if private key already exists in persistence store (not first server to boot) 
    	
    }//EOM
    
    /**
     * @param loaderStrategy {@link IKeystoreLoaderStrategy} responsible for the in-memory<br> 
     * representation of the keystore.<br>
     * Uses {@link #serverKeystoreConfig} definitions for the path and credentials. 
     * @return Either byte[] or {@link KeyStore} depending on the loaderStrategy 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    private final <T> T loadKeyStore(final IKeystoreLoaderStrategy<T> loaderStrategy) throws 
				KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		java.io.FileInputStream fis = null;
		T result = null ; 
		
		try { 
			final File keystoreFile = new File(serverKeystoreConfig.getFilePath());
			if(keystoreFile.exists()) fis = new FileInputStream(keystoreFile) ; 
		
			result = loaderStrategy.loadKeyStore(fis, keystoreFile, serverKeystoreConfig) ; 
		} finally {
			if (fis != null) {
				fis.close();
		    }//EO if fis != null
		}//EO catch block 
		
		return result ; 
		
	}//EOM
    
    /**
     * 
     * @param arrKeystoreFileContent file content from which to populate the {@link KeyStore}.<br>
     * Uses {@link #serverKeystoreConfig} definitions for the path and credentials 
     * @return {@link KeyStore} instantiated from the arrKeystoreFileContent content
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
	private final KeyStore loadKeyStore(byte[] arrKeystoreFileContent) throws KeyStoreException,
		NoSuchAlgorithmException, CertificateException, IOException {
		
    	InputStream is = null;
		KeyStore keystore = null ; 
    	 
    	try {
    		is = new ByteArrayInputStream(arrKeystoreFileContent) ; 
    		keystore = IKeystoreLoaderStrategy.KeyStoreAPI.
    					loadKeyStore(is, null/*file*/, serverKeystoreConfig); 
    	} finally {
    		if (is != null) {
    			is.close();
    	    }//EO if fis != null
    	}//EO catch block  fis = null;
    	
    	return keystore ; 
		 
	}//EOM 
    
	/**
	 * @param ks {@link KeyStore} to persist.<br> 
	 * Uses {@link #serverKeystoreConfig} definitions for the path and credentials 
	 * @throws IOException
	 * @throws CertificateException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	private final void persistKeystore(final KeyStore ks) throws IOException, CertificateException, 
					KeyStoreException, NoSuchAlgorithmException{
        // get user password and file input stream
        char[] password = serverKeystoreConfig.getFilePasswordCharArray() ; 
        FileOutputStream fos = null;
        try {
            File file = new File(serverKeystoreConfig.getFilePath());
            fos = new FileOutputStream(file);
            ks.store(fos, password);
        } finally {
            if(fos != null) fos.close() ; 
        }//EO catch block 
    }//EOM 
     
    
    /**
     * Helper storing DB Keystore processing state 
     * @author guy
     */
    private static final class KeystoreContext { 
    	boolean m_bRestartJVM ; 

    	/**
    	 * Instance corresponds to the {@link DbKeyStoreSpi#PRIVATE_KEY_ENTRY} record
    	 */
    	KeystoreEntry m_persistedPKEntry ;  //corresponding to the DB record 
    	/**
    	 * Instance corresponds to the file keystore private key entry 
    	 */
		KeystoreEntry m_newPKEntry ; 
		KeyStore m_fileKeystore ;
		/**
		 * A new keystore to physically replace the server's keystore file 
		 */
		KeyStore m_overrideKeystore ; 
    }//EOM 
    
    /**
     * Contract for keystore loading strategies. 
     * @author guy
     *
     * @param <T> Loaded keystore object type.
     */
    private interface IKeystoreLoaderStrategy<T> { 
    	
    	/**
    	 * Loads a file keystore into memory. The actual data structure depends on the concrete<br> 
    	 * classes implementation 
    	 * @param fis keystore {@link InputStream}.  
    	 * @param file keystore file.  
    	 * @param serverKeystoreConfig metadata defining paths and credentials. 
    	 * @return Keystore in-memory representation as implemented by concrete classes.
    	 * @throws KeyStoreException
    	 * @throws NoSuchAlgorithmException
    	 * @throws CertificateException
    	 * @throws IOException
    	 */
    	T loadKeyStore(final InputStream fis, final File file,
				final ServerKeystoreConfig serverKeystoreConfig) throws KeyStoreException, 
								NoSuchAlgorithmException, CertificateException, IOException ;
    	
    	/**
    	 * File System API loader strategy loading the keystore into a byte[]
    	 */
    	IKeystoreLoaderStrategy<byte[]> FileSystemAPI = new IKeystoreLoaderStrategy<byte[]>(){ 
    		
        	/**
        	 * Loads a file keystore into a byte[]. 
        	 * @param fis keystore {@link InputStream}.  
        	 * @param file keystore file.  
        	 * @param serverKeystoreConfig metadata defining paths and credentials. 
        	 * @return Keystore in-memory representation as byte[].
        	 * @throws KeyStoreException
        	 * @throws NoSuchAlgorithmException
        	 * @throws CertificateException
        	 * @throws IOException
        	 */
    		public final byte[] loadKeyStore(final InputStream fis, final File file,
    				final ServerKeystoreConfig serverKeystoreConfig)
    				throws KeyStoreException, NoSuchAlgorithmException,
    				CertificateException, IOException {
    			 
    			final byte[] arrContent = new byte[(int) file.length()] ;
    			fis.read(arrContent) ; 
    			return arrContent ; 
    			
    		}//EOM 
    	}; 
    	
    	/**
    	 * {@link KeyStore} API loader strategy loading the keystore into a {@link KeyStore}.
    	 */
    	IKeystoreLoaderStrategy<KeyStore> KeyStoreAPI = new IKeystoreLoaderStrategy<KeyStore>(){ 
    	
    		/**
        	 * Loads a file keystore into a {@link KeyStore} 
        	 * @param fis keystore {@link InputStream}.  
        	 * @param file keystore file.  
        	 * @param serverKeystoreConfig metadata defining paths and credentials. 
        	 * @return Keystore in-memory representation as {@link KeyStore}.
        	 * @throws KeyStoreException
        	 * @throws NoSuchAlgorithmException
        	 * @throws CertificateException
        	 * @throws IOException
        	 */
    		public KeyStore loadKeyStore(InputStream fis, File file, 
    				ServerKeystoreConfig serverKeystoreConfig) throws KeyStoreException, 
    								NoSuchAlgorithmException, CertificateException, IOException {
    			
    			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    			// get user password and file input stream
    	    	final char[] password = serverKeystoreConfig.getFilePasswordCharArray();
    	    	
    	    	ks.load(fis, password) ; 
    	    	return ks ; 
    		}//EOM 
    	};
    	
    }//EOI
    
    /**
     * reason for REQUIRES_NEW here is HHQ-4185, spring transaction manager doesn't upgrade
     * session when it comes across a rw transaction from a ro transactional context
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void create(String alias, String type, Certificate cert, Certificate[] chain) 
    																throws KeyStoreException{
        final KeystoreEntryImpl keystoreEntry = new KeystoreEntryImpl();
        keystoreEntry.setAlias(alias);
        keystoreEntry.setType(type);
        try{ 
        	keystoreEntry.setCertificate(cert) ; 
        	keystoreEntry.setCertificateChain(chain) ;
        }catch(IOException ioe){ 
        	throw new KeyStoreException(ioe) ;
        }//EO catch block 
        dbKeystoreDao.save(keystoreEntry);
    }
    

}

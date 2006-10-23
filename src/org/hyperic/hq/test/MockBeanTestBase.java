/*
 * COPYRIGHT.  ZUHLKE ENGINEERING LIMITED 2005.  ALL RIGHTS RESERVED.
 * 
 * This software is provided by the copyright holder "as is" and any express 
 * or implied warranties, including, but not limited to, the implied warranties
 * of merchantability and fitness for a particular purpose are disclaimed. In 
 * no event shall Zuhlke Engineering Limited be liable for any direct, indirect, 
 * incidental, special, exemplary, or consequential damages (including, but not 
 * limited to, procurement of substitute goods or services; loss of use, data, 
 * or profits; or business interruption) however caused and on any theory of 
 * liability, whether in contract, strict liability, or tort (including 
 * negligence or otherwise) arising in any way out of the use of this
 * software, even if advised of the possibility of such damage. 
 */
package org.hyperic.hq.test;

import com.mockrunner.mock.ejb.MockUserTransaction;

import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hyperic.dao.DAOFactory;

import org.mockejb.MDBDescriptor;
import org.mockejb.MockContainer;
import org.mockejb.OptionalCactusTestCase;
import org.mockejb.SessionBeanDescriptor;
import org.mockejb.jms.MockQueue;
import org.mockejb.jms.QueueConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

import org.postgresql.jdbc2.optional.SimpleDataSource;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Abstract base class for all JUnit tests that use Mock EJB
 * to test EJBs.  Factors out common features to allow them to
 * be reused consistently.
 *
 * Modified to be used within the mock jta context for HQ unit testing.
 * Be sure to override the 
 * <code>hibernate.transaction.manager_lookup_class</code>
 * by setting to an empty string in your 
 * <code>${user.home}/.hq/build.properties</code>
 *
 * In addition you must also add the following line to the above 
 * build.properties
 *
 * <code>hq.jta.UserTransaction=javax.transaction.UserTransaction</code>
 * 
 * @author Eoin Woods
 * @author Young Lee
 */
public abstract class MockBeanTestBase extends OptionalCactusTestCase
{
    private Context                _context;
    private MockContainer          _container;
    private QueueConnectionFactory _qcf;
    private Queue                  _queue;
    
    private static final String DS_ORACLE9 = "Oracle9i";
    private static final String DS_POSTGRESQL = "PostgreSQL";

    private String _dsMapping = DS_POSTGRESQL;
    private String _url;
    private String _database;
    private String _username;
    private String _password;
    private String _server;

    public MockBeanTestBase(String testName){
        super(testName);
        
        _dsMapping = System.getProperty("hq.server.ds-mapping");
        _url       = System.getProperty("hq.jdbc.url");
        _database  = System.getProperty("hq.jdbc.name");
        _username  = System.getProperty("hq.jdbc.user");
        _password  = System.getProperty("hq.jdbc.password");
        _server    = System.getProperty("hq.jdbc.server");

        if (_dsMapping == null)
            _dsMapping = DS_POSTGRESQL;

        if (_database == null)
            _database = "hq";
            
        if (_username == null)
            _username = "hq";
            
        if (_password == null)
            _password = "hq";

        if (_server == null)
            _server = "localhost";
    }
    
    public MockBeanTestBase() {
        this("MockBeanTest");
    }

    public void setDatabase(String database) {
        _database = database;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public void setServer(String server) {
        _server = server;
    }

    /**
     * Perform pre-test initialisation
     * @throws Exception if the initialisation fails
     */
    public void setUp() throws Exception {
        initialiseContainer() ;
    }
    
    /**
     * Performs the necessary cleanup by restoring the system properties that
     * were modified by MockContextFactory.setAsInitial().
     * This is needed in case if the test runs inside the container, so it 
     * would not affect the tests that run after it.  
     */
    public void tearDown() throws Exception {
        // Inside the container this method does not do anything
        MockContextFactory.revertSetAsInitial();
    }

    private DataSource getDataSource() throws Exception {
        if (_dsMapping.equals(DS_POSTGRESQL)) {
            SimpleDataSource ds = new SimpleDataSource();
            ds.setDatabaseName(_database);
            ds.setUser(_username);
            ds.setPassword(_password);
            ds.setServerName(_server);
            return ds;
        } else if(_dsMapping.equals(DS_ORACLE9)) {
            OracleDataSource ds = new OracleDataSource();
            ds.setURL(_url);
            ds.setUser(_username);
            ds.setPassword(_password);
            return ds;
        } else {
            throw new IllegalArgumentException("Unknown " +
                                               "hq.server.ds-mapping: " + 
                                               _dsMapping);
        }
    }

    /**
     * Initialising the context and mock container
     * @throws NamingException if the initialisation cannot be completed
     */
    public void initialiseContainer() throws Exception {
        /* We want to use MockEJB JNDI provider only if we run outside of
         * container.  Inside container we want to rely on the "real" JNDI 
         * provided by that container. 
         */
        if (!isRunningOnServer()) {
            /* We need to set MockContextFactory as our JNDI provider.
             * This method sets the necessary system properties. 
             */
            MockContextFactory.setAsInitial();
        }
        // create the initial context that will be used for binding EJBs
        _context = new InitialContext();
        
        // Create an instance of the MockContainer
        _container = new MockContainer(_context);

        // bind jta transaction
        // we use MockTransaction outside of the app server
        MockUserTransaction mockTransaction = new MockUserTransaction();
        _context.rebind("javax.transaction.UserTransaction", mockTransaction);

        // bind datasource
        DataSource ds = getDataSource();
        _context.rebind("java:/HypericDS", ds);

        // set dao factory suitable for out-of-container testing via mockejb
        // be sure to set the mocksession
        DAOFactory.setDefaultDAOFactory(DAOFactory.HIBERNATE_MOCKTEST);
    }

   /**
    * Deploy the session bean that has the specified remote interface
    * class.  The bean must have a remote interface and must follow the
    * naming convention "Service", "ServiceHome", "ServiceBean" for the
    * remote, home and bean classes respectively.
    * 
    * @param jndiName the JNDI name to deploy the bean under (e.g. "ejb/Bean1")
    * @param beanInterfaceName the fully qualified Java class name of the
    *        bean's remote interface (e.g. "com.foo.beans.BeanOne")
    * @throws Exception for any failure
    */
    public void deployRemoteSessionBean(String jndiName, 
                                        String beanInterfaceName) 
    	throws Exception
    {
        if (isRunningOnServer())
            return;

        // if the test runs outside of the container
        /* Create deployment descriptor of our sample bean.
         * MockEjb uses it instead of XML deployment descriptors
         */
        ClassLoader ldr   = this.getClass().getClassLoader() ;
        Class homeClass   = ldr.loadClass(beanInterfaceName + "Home");
        Class remoteClass = ldr.loadClass(beanInterfaceName);
        Class beanClass   = ldr.loadClass(beanInterfaceName + "Bean");
        Object bean = beanClass.newInstance() ;
        SessionBeanDescriptor sampleServiceDescriptor = 
            new SessionBeanDescriptor(jndiName, homeClass, remoteClass, 
                                      bean);
        // Deploy operation creates Home and binds it to JNDI
        _container.deploy(sampleServiceDescriptor);
    }

    protected void deploySessionBean(SessionBeanDescriptor d) 
        throws Exception
    {
        _container.deploy(d);
    }
    
    /**
     * Convenience method for registering session beans
     * @param jndiName
     * @param home
     * @param local
     * @param impl
     * @throws Exception
     */
    public void deploySessionBean(String jndiName, Class home, Class local,
                                  Class impl)
    	throws Exception
    {
        if (isRunningOnServer())
            return;

        SessionBeanDescriptor sampleServiceDescriptor =
            new SessionBeanDescriptor(jndiName, home, local,impl);
        // Deploy operation creates Home and binds it to JNDI
        _container.deploy(sampleServiceDescriptor);
    }
    
    /**
     * Deploy an MDB attached to a queue to the mock container
     * @param factoryName the name of the queue connection factory containing 
     *        the queue
     * @param responseQueueName the name of the queue to attach the bean to
     * @param beanClassName the full classname of the bean to deploy
     * @throws Exception if the deployment cannot be completed
     */
    public void deployQueueMessageDrivenBean(String factoryName, 
                                             String requestQueueName, 
                                             String responseQueueName, 
                                             String beanClassName) 
        throws Exception
    {
        if (isRunningOnServer())
            return;

        // if the test runs outside of the container
        ClassLoader ldr = this.getClass().getClassLoader() ;
        Class beanClass = ldr.loadClass(beanClassName);
        Object beanObj = beanClass.newInstance() ;
        
        _qcf = new QueueConnectionFactoryImpl() ;
        _context.rebind(factoryName, _qcf);
        
        _queue = new MockQueue(requestQueueName) ;
        ((MockQueue)_queue).addMessageListener((MessageListener)beanObj) ;
        _context.rebind(requestQueueName, _queue);
        
        _context.rebind(responseQueueName, 
                        new MockQueue(responseQueueName));
        
        MDBDescriptor mdbDescriptor = 
            new MDBDescriptor(factoryName, requestQueueName, beanObj);
        mdbDescriptor.setIsAlreadyBound(true) ;
        // This will create connection factory and destination, create 
        // MDB and set it as the listener to the destination
        _container.deploy( mdbDescriptor );
    }
    
    /**
     * Return the context object that should be used to locate resources
     * in the J2EE container.
     * @return a context object
     */
    public Context getContext() {
        return _context ;
    }
    
    public QueueConnectionFactory getCurrentQCF() {
        return _qcf ;
    }
    
    public Queue getCurrentQueue() {
        return _queue ;
    }
}

